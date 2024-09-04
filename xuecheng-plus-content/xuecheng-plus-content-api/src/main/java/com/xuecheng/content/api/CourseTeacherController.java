package com.xuecheng.content.api;

import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.model.result.Result;
import com.xuecheng.content.service.CourseTeacherService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CourseTeacherController {

    @Autowired
    private CourseTeacherService courseTeacherService;

    @ApiOperation("查询教师")
    @GetMapping("/courseTeacher/list/{courseId}")
    public List<CourseTeacher> getTeacherList(@PathVariable Long courseId) {
        return courseTeacherService.getTeacherList(courseId);
    }

    ///courseTeacher
    @ApiOperation("添加教师")
    @PostMapping("/courseTeacher")
    public CourseTeacher addTeacher(@RequestBody @Validated CourseTeacher courseTeacher) {
        Long companyId=1232141425L;
        return courseTeacherService.addOrUpdateTeacher(companyId,courseTeacher);
    }
    ///courseTeacher
    @ApiOperation("修改教师")
    @PutMapping("/courseTeacher")
    public CourseTeacher updateTeacher(@RequestBody CourseTeacher courseTeacher) {
        Long companyId=1232141425L;
        return courseTeacherService.addOrUpdateTeacher(companyId,courseTeacher);

    }
    ///courseTeacher/course/75/26
    @ApiOperation("删除教师")
    @DeleteMapping("/courseTeacher/course/{courseId}/{teacherId}")
    public Result deleteTeacher(@PathVariable Long courseId, @PathVariable Long teacherId) {
        Long companyId=1232141425L;
        courseTeacherService.deleteTeacher(companyId,courseId,teacherId);
        return Result.success(null);
    }

}
