package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class CourseTeacherServiceImpl implements CourseTeacherService {
    @Autowired
    private CourseTeacherMapper courseTeacherMapper;
    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Override
    public List<CourseTeacher> getTeacherList(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> eq = new LambdaQueryWrapper<CourseTeacher>().eq(CourseTeacher::getCourseId, courseId);
        List<CourseTeacher> teachers = courseTeacherMapper.selectList(eq);

        return teachers;
    }

    @Override
    public CourseTeacher addOrUpdateTeacher(Long companyId, CourseTeacher courseTeacher) {
        Long courseId = courseTeacher.getCourseId();
        Long companyId1 = courseBaseMapper.selectById(courseId).getCompanyId();
        if (!companyId1.equals(companyId)){
            XueChengPlusException.cast("只能向自身机构的课程添加或修改老师");
        }
        if (courseTeacher.getId()==null){
            courseTeacher.setCreateDate(LocalDateTime.now());
            courseTeacherMapper.insert(courseTeacher);
            CourseTeacher courseTeacher1 = courseTeacherMapper.selectById(courseTeacher.getId());

            return courseTeacher1;
        }else {

            courseTeacherMapper.updateById(courseTeacher);
            CourseTeacher courseTeacher2 = courseTeacherMapper.selectById(courseTeacher.getId());
            return courseTeacher2;
        }

    }

    @Override
    public void deleteTeacher(Long companyId, Long courseId, Long teacherId) {
        Long companyId1 = courseBaseMapper.selectById(courseId).getCompanyId();
        if (!companyId1.equals(companyId)){
            XueChengPlusException.cast("只能向自身机构的课程添加或修改老师");
        }
        courseTeacherMapper.deleteById(teacherId);
    }


}
