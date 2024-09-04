package com.xuecheng.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class CourseBaseMapperTests {

    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Test
    void testCourseBaseMapper(){
        CourseBase courseBase = courseBaseMapper.selectById(74L);
        Assertions.assertNotNull(courseBase);
        Page<CourseBase> courseBasePage = new Page<>();
        courseBasePage.setCurrent(1);
        courseBasePage.setSize(10);
        LambdaQueryWrapper<CourseBase> wrapper = new LambdaQueryWrapper<>();
        QueryCourseParamsDto queryCourseParamsDto = new QueryCourseParamsDto();
        queryCourseParamsDto.setCourseName("java");
        wrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()),CourseBase::getName,queryCourseParamsDto.getCourseName());
        wrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()),CourseBase::getAuditStatus,queryCourseParamsDto.getAuditStatus());
        wrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()),CourseBase::getStatus,queryCourseParamsDto.getPublishStatus());


        Page<CourseBase> page = courseBaseMapper.selectPage(courseBasePage, wrapper);
        List<CourseBase> items = page.getRecords();


        PageResult<CourseBase> result = new PageResult<>(items, courseBasePage.getTotal(), courseBasePage.getCurrent(), courseBasePage.getSize());
        System.out.println(result);


    }
}
