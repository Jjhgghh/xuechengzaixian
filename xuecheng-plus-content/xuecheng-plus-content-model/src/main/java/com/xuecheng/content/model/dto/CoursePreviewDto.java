package com.xuecheng.content.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class CoursePreviewDto {

    private CourseBaseInfoDto courseBase;


    //课程计划信息
    List<TeachplanDto> teachplans;
}
