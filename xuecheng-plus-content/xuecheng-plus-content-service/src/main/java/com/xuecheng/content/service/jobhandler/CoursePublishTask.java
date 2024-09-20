package com.xuecheng.content.service.jobhandler;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.feignclient.CourseIndex;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {
    @Autowired
    CoursePublishService coursePublishService;
    @Autowired
    SearchServiceClient searchServiceClient;
    @Autowired
    CoursePublishMapper coursePublishMapper;
    //任务调度入口
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.debug("shardIndex="+shardIndex+",shardTotal="+shardTotal);
        //参数:分片序号、分片总数、消息类型、一次最多取到的任务数量、一次任务调度执行的超时时间
        process(shardIndex,shardTotal,"course_publish",30,60);
    }

    @Override
    public boolean execute(MqMessage mqMessage) {
        Long courseId = Long.valueOf(mqMessage.getBusinessKey1());
        //课程静态化
        generateCourseHtml(mqMessage,courseId);
        //课程索引
        saveCourseIndex(mqMessage,courseId);
        //课程缓存
        saveCourseCache(mqMessage,courseId);
        return true;


    }

    private void generateCourseHtml(MqMessage mqMessage, Long courseId) {
        log.debug("开始进行课程静态化,课程id:{}",courseId);
        //消息id
        Long id = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        int stageOne = mqMessageService.getStageOne(id);
        if (stageOne>0){
            log.debug("课程静态化已处理直接返回，课程id:{}",courseId);
            return ;
        }
        File file = coursePublishService.generateCourseHtml(courseId);
        if (file!=null){
            coursePublishService.uploadCourseHtml(courseId,file);
        }

        //完成静态化
        mqMessageService.completedStageOne(id);
    }

    //保存课程索引信息
    public void saveCourseIndex(MqMessage mqMessage,long courseId){
        log.debug("保存课程索引信息,课程id:{}",courseId);
        //消息id
        Long id = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        int stageOne = mqMessageService.getStageTwo(id);
        if (stageOne>0){
            log.debug("保存课程索引信息已处理直接返回，课程id:{}",courseId);
            return ;
        }

        CourseIndex courseIndex = new CourseIndex();
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        BeanUtils.copyProperties(coursePublish,courseIndex);
        Boolean add = searchServiceClient.add(courseIndex);
        if (!add){
            XueChengPlusException.cast("添加索引失败");
        }

        mqMessageService.completedStageTwo(id);

    }
    //将课程信息缓存至redis
    public void saveCourseCache(MqMessage mqMessage,long courseId){
        log.debug("将课程信息缓存至redis,课程id:{}",courseId);
        //消息id
        Long id = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        int stageOne = mqMessageService.getStageThree(id);
        if (stageOne>0){
            log.debug("课程静态化已处理直接返回，课程id:{}",courseId);
            return ;
        }
        mqMessageService.completedStageThree(id);


    }
}
