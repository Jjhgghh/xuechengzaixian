package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CoursePublishServiceImpl implements CoursePublishService {
   @Autowired
    CourseBaseInfoService courseBaseInfoService;
   @Autowired
   TeachplanService teachplanService;
   @Autowired
    CourseBaseMapper courseBaseMapper;
   @Autowired
    CourseMarketMapper courseMarketMapper;
   @Autowired
    CoursePublishPreMapper coursePublishPreMapper;
    @Autowired
    CoursePublishMapper coursePublishMapper;
    @Autowired
    MqMessageService mqMessageService;
    @Autowired
    MediaServiceClient mediaServiceClient;


    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        CourseBaseInfoDto baseInfoDto = courseBaseInfoService.getCourseBaseInfo(courseId);
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        coursePreviewDto.setCourseBase(baseInfoDto);
        coursePreviewDto.setTeachplans(teachplanTree);

        return coursePreviewDto;
    }

    @Override
    @Transactional
    public void commitAudit(Long companyId, Long courseId) {
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase.getAuditStatus().equals("202003")){
            XueChengPlusException.cast("课程审核中,审核完成可再次提交");
        }
        if (!courseBase.getCompanyId().equals(companyId)){
            XueChengPlusException.cast("只能提交自身机构的课程");
        }
        if (courseBase.getPic()==null){
            XueChengPlusException.cast("图片不能为空");
        }
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        if (teachplanTree==null||teachplanTree.size()<=0){
            XueChengPlusException.cast("课程信息不能为空");
        }
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        CourseBaseInfoDto baseInfoDto = courseBaseInfoService.getCourseBaseInfo(courseId);
        BeanUtils.copyProperties(baseInfoDto, coursePublishPre);
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        String jsonStringMarket = JSON.toJSONString(courseMarket);
        coursePublishPre.setMarket(jsonStringMarket);
        String jsonStringTeach = JSON.toJSONString(teachplanTree);
        coursePublishPre.setTeachplan(jsonStringTeach);
        coursePublishPre.setStatus("202003");
        coursePublishPre.setCompanyId(companyId);
        coursePublishPre.setCreateDate(LocalDateTime.now());
        CoursePublishPre coursePublishPre1 = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre1==null){
            coursePublishPreMapper.insert(coursePublishPre);
        }else {
            coursePublishPreMapper.updateById(coursePublishPre);
        }
        courseBase.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBase);

    }

    @Override
    @Transactional
    public void publish(Long companyId, Long courseId) {
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre==null){
            XueChengPlusException.cast("课程需要审核才能发布");
        }
        if(!coursePublishPre.getCompanyId().equals(companyId)){
            XueChengPlusException.cast("不允许提交其它机构的课程。");
        }
        String status = coursePublishPre.getStatus();
        if (!status.equals("202004")){
            XueChengPlusException.cast("课程需要审核通过才能发布");
        }
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre, coursePublish);
        coursePublish.setCreateDate(LocalDateTime.now());
        CoursePublish coursePublish1 = coursePublishMapper.selectById(courseId);
        if (coursePublish1==null){
            coursePublishMapper.insert(coursePublish);
        }else {
            coursePublishMapper.updateById(coursePublish);
        }
        saveCoursePublishMessage(courseId);

        coursePublishPreMapper.deleteById(courseId);
    }

    @Override
    public File generateCourseHtml(Long courseId) {
        File file=null;
        try {
            Configuration configuration = new Configuration(Configuration.getVersion());
            String classpath = this.getClass().getResource("/").getPath();
            configuration.setDirectoryForTemplateLoading(new File(classpath+"/templates/"));
            configuration.setDefaultEncoding("utf-8");
            Template template = configuration.getTemplate("course_template.ftl");
            Map<String, Object> data = new HashMap<>();
            CoursePreviewDto coursePreviewInfo = getCoursePreviewInfo(courseId);
            data.put("model", coursePreviewInfo);
            String content  = FreeMarkerTemplateUtils.processTemplateIntoString(template, data);
            System.out.println(content);
            InputStream inputStream = IOUtils.toInputStream(content, "utf-8");
            file=File.createTempFile("course",".html");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            IOUtils.copy(inputStream, fileOutputStream);
        } catch (Exception e) {
            log.error("课程静态化异常,课程ID为:{}",courseId);
            XueChengPlusException.cast("课程静态化异常");
        }
        return file;
    }

    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        String upload = mediaServiceClient.upload(multipartFile, "course/" + courseId + ".html");
        if (upload==null){
            XueChengPlusException.cast("上传静态文件异常");
        }
    }

    private void saveCoursePublishMessage(Long courseId){
        MqMessage mqMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if(mqMessage==null){
            XueChengPlusException.cast(CommonError.UNKOWN_ERROR);
        }
    }
}
