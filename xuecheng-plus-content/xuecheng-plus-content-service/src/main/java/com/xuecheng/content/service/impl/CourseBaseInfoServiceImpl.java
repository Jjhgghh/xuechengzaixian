package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.*;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Autowired
    private CourseBaseMapper courseBaseMapper;
    @Autowired
    CourseMarketMapper courseMarketMapper;
    @Autowired
    CourseCategoryMapper courseCategoryMapper;
    @Autowired
    TeachplanMapper teachplanMapper;
    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;
    @Autowired
    CourseTeacherMapper courseTeacherMapper;


    @Override
    public PageResult<CourseBase> list(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {


        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        LambdaQueryWrapper<CourseBase> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()),CourseBase::getName,queryCourseParamsDto.getCourseName());
        wrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()),CourseBase::getAuditStatus,queryCourseParamsDto.getAuditStatus());
        wrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()),CourseBase::getStatus,queryCourseParamsDto.getPublishStatus());


        Page<CourseBase> pageresult = courseBaseMapper.selectPage(page, wrapper);
        List<CourseBase> items = pageresult.getRecords();


        PageResult<CourseBase> result = new PageResult<>(items, pageresult.getTotal(), page.getCurrent(), page.getSize());
        return result;
    }

    @Override
    @Transactional
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto) {
        //合法性校验
//        if (StringUtils.isBlank(dto.getName())) {
//            XueChengPlusException.cast("课程名称为空");
//        }
//
//        if (StringUtils.isBlank(dto.getMt())) {
//            throw new RuntimeException("课程分类为空");
//        }
//
//        if (StringUtils.isBlank(dto.getSt())) {
//            throw new RuntimeException("课程分类为空");
//        }
//
//        if (StringUtils.isBlank(dto.getGrade())) {
//            throw new RuntimeException("课程等级为空");
//        }
//
//        if (StringUtils.isBlank(dto.getTeachmode())) {
//            throw new RuntimeException("教育模式为空");
//        }
//
//        if (StringUtils.isBlank(dto.getUsers())) {
//            throw new RuntimeException("适应人群为空");
//        }
//
//        if (StringUtils.isBlank(dto.getCharge())) {
//            throw new RuntimeException("收费规则为空");
//        }
        CourseBase courseBase = new CourseBase();
        BeanUtils.copyProperties(dto, courseBase);
        courseBase.setCompanyId(companyId);
        courseBase.setAuditStatus("202002");
        courseBase.setStatus("203001");
        courseBase.setCreateDate(LocalDateTime.now());
        int insert = courseBaseMapper.insert(courseBase);
        if (insert<=0){
            throw new RuntimeException("新增课程基本信息失败");
        }
        CourseMarket newCourseMarket = new CourseMarket();
        BeanUtils.copyProperties(dto, newCourseMarket);
        newCourseMarket.setId(courseBase.getId());
        int i = saveCourseMarket(newCourseMarket);
        if (i<=0){
            throw new RuntimeException("保存课程营销信息失败");
        }
        CourseBaseInfoDto baseInfoDto = getCourseBaseInfo(courseBase.getId());
        return baseInfoDto;


    }
    public  CourseBaseInfoDto getCourseBaseInfo(Long courseBaseId) {
        if (courseBaseMapper.selectById(courseBaseId) == null) {
            return null;
        }

        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        CourseBase courseBase1 = courseBaseMapper.selectById(courseBaseId);
        BeanUtils.copyProperties(courseBase1, courseBaseInfoDto);
        CourseMarket courseMarket = courseMarketMapper.selectById(courseBaseId);
        if(courseMarket != null){
            BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);
        }
        CourseCategory courseCategory = courseCategoryMapper.selectById(courseBaseInfoDto.getMt());
        courseBaseInfoDto.setMtName(courseCategory.getName());
        CourseCategory courseCategory1 = courseCategoryMapper.selectById(courseBaseInfoDto.getSt());
        courseBaseInfoDto.setStName(courseCategory1.getName());
        return courseBaseInfoDto;
    }

    @Override
    @Transactional
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto) {
        Long id = editCourseDto.getId();
        CourseBase courseBase = courseBaseMapper.selectById(id);
        if (courseBase==null){
            XueChengPlusException.cast("课程不存在");
        }
        if (!companyId.equals(courseBase.getCompanyId())){
            XueChengPlusException.cast("只能修改本机构课程");
        }

        BeanUtils.copyProperties(editCourseDto, courseBase);
        courseBase.setChangeDate(LocalDateTime.now());
        int i = courseBaseMapper.updateById(courseBase);
        if (i<=0){
            XueChengPlusException.cast("修改课程基本信息失败");
        }
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(editCourseDto, courseMarket);
        saveCourseMarket(courseMarket);
        CourseBaseInfoDto baseInfoDto = getCourseBaseInfo(id);

        return baseInfoDto;
    }

    @Override
    @Transactional
    public void deleteCourseBase(Long companyId,Long courseId) {

        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (!companyId.equals(courseBase.getCompanyId())){
            XueChengPlusException.cast("只能删除本机构课程");
        }

        if(!courseBase.getAuditStatus().equals("202002")){
            XueChengPlusException.cast("课程的审核状态为未提交时方可删除。");
        }
        courseBaseMapper.deleteById(courseId);
        courseMarketMapper.deleteById(courseId);
        LambdaQueryWrapper<Teachplan> eq = new LambdaQueryWrapper<Teachplan>().eq(Teachplan::getCourseId, courseId);
        List<Teachplan> teachplans = teachplanMapper.selectList(eq);
        if (!CollectionUtils.isEmpty(teachplans)){
            List<Long> collect = teachplans.stream().map(teachplan -> teachplan.getId()).collect(Collectors.toList());
            teachplanMapper.deleteBatchIds(collect);
        }

        LambdaQueryWrapper<TeachplanMedia> eq1 = new LambdaQueryWrapper<TeachplanMedia>().eq(TeachplanMedia::getCourseId, courseId);
        List<TeachplanMedia> teachplanMedias = teachplanMediaMapper.selectList(eq1);
        if (!CollectionUtils.isEmpty(teachplanMedias)){
            List<Long> collect1 = teachplanMedias.stream().map(teachplanMedia -> teachplanMedia.getId()).collect(Collectors.toList());
            teachplanMediaMapper.deleteBatchIds(collect1);
        }

        LambdaQueryWrapper<CourseTeacher> eq2 = new LambdaQueryWrapper<CourseTeacher>().eq(CourseTeacher::getCourseId, courseId);
        List<CourseTeacher> teachers = courseTeacherMapper.selectList(eq2);
        if (!CollectionUtils.isEmpty(teachers)){
            List<Long> collect2 = teachers.stream().map(CourseTeacher::getId).collect(Collectors.toList());
            courseTeacherMapper.deleteBatchIds(collect2);
        }


    }


    private int saveCourseMarket(CourseMarket newCourseMarket) {
        if (StringUtils.isBlank(newCourseMarket.getCharge())){
            throw new RuntimeException("收费规则没有选择");
        }
        if (newCourseMarket.getCharge().equals("201001")){
            if (newCourseMarket.getPrice()==null||newCourseMarket.getPrice().floatValue()<=0){
                XueChengPlusException.cast("课程为收费价格不能为空且必须大于0");
            }
        }
        CourseMarket courseMarket = courseMarketMapper.selectById(newCourseMarket.getId());
        if (courseMarket==null){
            int insert = courseMarketMapper.insert(newCourseMarket);
            return insert;
        }else {
            BeanUtils.copyProperties(newCourseMarket, courseMarket);
            courseMarket.setId(newCourseMarket.getId());
            int i = courseMarketMapper.updateById(courseMarket);
            return i;
        }
    }


}
