package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.MediaServiceClient;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.service.LearningService;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Slf4j
public class LearningServiceImpl implements LearningService {
   @Autowired
    ContentServiceClient contentServiceClient;
   @Autowired
    MediaServiceClient mediaServiceClient;
   @Autowired
    MyCourseTablesService myCourseTablesService;

    @Override
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId) {
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish == null) {
            return RestResponse.validfail("课程不存在");
        }
        String isPreview = contentServiceClient.getIsPreview(teachplanId);
        if ("1".equals(isPreview)){
            RestResponse<String> urlByMediaId = mediaServiceClient.getPlayUrlByMediaId(mediaId);
            return urlByMediaId;
        }


        if(!StringUtils.isEmpty(userId)){
            XcCourseTablesDto learningStatus = myCourseTablesService.getLearningStatus(userId, courseId);
            String learnStatus = learningStatus.getLearnStatus();
            if ("702002".equals(learnStatus)) {
                return RestResponse.validfail("没有选课或选课后没有支付");
            }else if ("702003".equals(learnStatus)) {
                return RestResponse.validfail("已过期需要申请续期或重新支付");
            }else {
                RestResponse<String> urlByMediaId = mediaServiceClient.getPlayUrlByMediaId(mediaId);
                return urlByMediaId;
            }
        }
        String charge = coursepublish.getCharge();
        if (charge.equals("201000")) {//免费可以正常学习
            return mediaServiceClient.getPlayUrlByMediaId(mediaId);
        }

        return RestResponse.validfail("请购买课程后继续学习");


    }
}
