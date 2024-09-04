package com.xuecheng.content.service;

import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

public interface CourseTeacherService {

    public List<CourseTeacher> getTeacherList(Long courseId);

    public CourseTeacher addOrUpdateTeacher(Long companyId,CourseTeacher courseTeacher);

    public void deleteTeacher(Long companyId,Long courseId,Long teacherId);
}
