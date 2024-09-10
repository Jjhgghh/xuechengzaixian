package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

/**
 * XxlJob开发示例（Bean模式）
 * <p>
 * 开发步骤：
 * 1、任务开发：在Spring Bean实例中，开发Job方法；
 * 2、注解配置：为Job方法添加注解 "@XxlJob(value="自定义jobhandler名称", init = "JobHandler初始化方法", destroy = "JobHandler销毁方法")"，注解value值对应的是调度中心新建任务的JobHandler属性的值。
 * 3、执行日志：需要通过 "XxlJobHelper.log" 打印执行日志；
 * 4、任务结果：默认任务结果为 "成功" 状态，不需要主动设置；如有诉求，比如设置任务结果为失败，可以通过 "XxlJobHelper.handleFail/handleSuccess" 自主设置任务结果；
 *
 * @author xuxueli 2019-12-11 21:52:51
 */
@Component
@Slf4j
public class VideoTask {

    @Autowired
    MediaProcessMapper mediaProcessMapper;
    @Autowired
    MediaFileService mediaFileService;
    @Autowired
    MediaFileProcessService mediaFileProcessService;

    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegpath;

    /**
     * 2、分片广播任务
     */
    @XxlJob("videoJobHandler")
    public void shardingJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        int i = Runtime.getRuntime().availableProcessors();
        //查询任务
        List<MediaProcess> mediaProcesses = mediaProcessMapper.selectListByShardIndex(shardTotal, shardIndex, i);
        if (mediaProcesses == null || mediaProcesses.isEmpty()) {
            return;
        }
        int size = mediaProcesses.size();
        log.debug("取出的任务数为:{}", size);
        //开启线程池
        ExecutorService executorService = Executors.newFixedThreadPool(size);
        CountDownLatch countDownLatch = new CountDownLatch(size);


        //抢锁
        mediaProcesses.forEach(mediaProcess -> {
            executorService.execute(() -> {

                try {
                    Long id = mediaProcess.getId();
                    boolean b1 = mediaFileProcessService.startTask(id);
                    if (!b1) {
                        log.debug("抢锁失败");
                        return;
                    }
                    //执行任务
                    log.debug("开始执行任务:{}", mediaProcess);
                    String bucket = mediaProcess.getBucket();
                    String objectName = mediaProcess.getFilePath();
                    String filename = mediaProcess.getFilename();
                    String fileId = mediaProcess.getFileId();
                    File file = mediaFileService.downloadFileFromMinIO(bucket, objectName);
                    if (file == null) {
                        log.debug("下载待处理文件失败,originalFile:{}", mediaProcess.getBucket().concat(mediaProcess.getFilePath()));
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "下载待处理文件失败");
                        return;
                    }
                    //源avi视频的路径
                    String video_path = file.getAbsolutePath();
                    //转换后mp4文件的名称
                    String mp4_name = fileId + ".mp4";
                    //转换后mp4文件的路径
                    File minio = null;
                    try {
                        minio = File.createTempFile("minio", ".mp4");
                    } catch (IOException e) {
                        log.error("创建临时文件失败");
                        mediaFileProcessService.saveProcessFinishStatus(id, "3", fileId, null, "创建临时文件失败");
                        return;
                    }

                    //创建工具类对象
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegpath, video_path, mp4_name, minio.getAbsolutePath());
                    //开始视频转换，成功将返回success
                    String s = videoUtil.generateMp4();
                    if (!s.equals("success")) {
                        log.debug("视频转码失败,bucket:{},objectName:{},错误信息:{}", bucket, objectName, s);
                        mediaFileProcessService.saveProcessFinishStatus(id, "3", fileId, null, "视频转码失败");
                        return;
                    }
                    String filePath = getFilePath(fileId, ".mp4");
                    boolean b = mediaFileService.addMediaFilesToMinIO(minio.getAbsolutePath(), filePath, "video/mp4", bucket);
                    if (!b) {
                        log.debug("上传到minio失败,taskId:{}", id);
                        mediaFileProcessService.saveProcessFinishStatus(id, "3", fileId, null, "上传到minio失败");
                        return;
                    }


                    String url = "/" + bucket + "/" + filePath;
                    mediaFileProcessService.saveProcessFinishStatus(id, "2", fileId, url, "null");
                } finally {
                    countDownLatch.countDown();
                }


            });

        });
        countDownLatch.await(30, TimeUnit.MINUTES);
    }


    private String getFilePath(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

}
