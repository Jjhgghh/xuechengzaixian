package com.xuecheng.content.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;

public interface CourseBaseInfoService {

    public PageResult<CourseBase> list(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto);

    public CourseBaseInfoDto createCourseBase(Long companyId,AddCourseDto addCourseDto);

    public CourseBaseInfoDto getCourseBaseInfo(Long id);

    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto);
}
