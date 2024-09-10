package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.model.po.MediaProcessHistory;
import com.xuecheng.media.service.MediaFileProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/9/14 14:41
 * @version 1.0
 */
@Slf4j
@Service
public class MediaFileProcessServiceImpl implements MediaFileProcessService {

 @Autowired
 MediaFilesMapper mediaFilesMapper;

 @Autowired
 MediaProcessMapper mediaProcessMapper;

 @Autowired
 MediaProcessHistoryMapper mediaProcessHistoryMapper;


 @Override
 public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count) {
  List<MediaProcess> mediaProcesses = mediaProcessMapper.selectListByShardIndex(shardTotal, shardIndex, count);
   return mediaProcesses;
 }
 //实现如下
 public boolean startTask(long id) {
  int result = mediaProcessMapper.startTask(id);
  return result<=0?false:true;
 }

 @Override
 public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {
  MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
  if (mediaProcess == null) {
   return;
  }
  if (status.equals("3")){
    mediaProcess.setStatus(status);
    mediaProcess.setErrormsg(errorMsg);
    mediaProcess.setFailCount(mediaProcess.getFailCount()+1);
    mediaProcessMapper.updateById(mediaProcess);
    return;
  }
  MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
  if (mediaFiles != null) {
   mediaFiles.setUrl(url);
   mediaFilesMapper.updateById(mediaFiles);
  }
  mediaProcess.setStatus(status);
  mediaProcess.setUrl(url);
  mediaProcess.setFinishDate(LocalDateTime.now());
  mediaProcessMapper.updateById(mediaProcess);
  MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();
  BeanUtils.copyProperties(mediaProcess, mediaProcessHistory);
  mediaProcessHistoryMapper.insert(mediaProcessHistory);
  mediaProcessMapper.deleteById(taskId);

 }


}