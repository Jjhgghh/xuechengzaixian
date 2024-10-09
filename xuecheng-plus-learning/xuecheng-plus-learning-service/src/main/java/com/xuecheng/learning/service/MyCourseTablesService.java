package com.xuecheng.learning.service;

import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;

/**
 * @description 我的课程表service接口
 * @author Mr.M
 * @date 2022/10/2 16:07
 * @version 1.0
 */
public interface MyCourseTablesService {

    /**
 * @description 添加选课
 * @param userId 用户id
 * @param courseId 课程id
 * @return com.xuecheng.learning.model.dto.XcChooseCourseDto
 * @author Mr.M
 * @date 2022/10/24 17:33
*/
 public XcChooseCourseDto addChooseCourse(String userId, Long courseId);

    public XcCourseTablesDto getLearningStatus(String userId, Long courseId);

    public boolean saveChooseCourseSuccess(String chooseCourseId);
}