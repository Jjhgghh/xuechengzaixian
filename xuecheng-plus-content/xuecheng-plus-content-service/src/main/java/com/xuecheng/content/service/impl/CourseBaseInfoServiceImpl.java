package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.service.CourseBaseInfoService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Autowired
    private CourseBaseMapper courseBaseMapper;
    @Autowired
    CourseMarketMapper courseMarketMapper;
    @Autowired
    CourseCategoryMapper courseCategoryMapper;


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
