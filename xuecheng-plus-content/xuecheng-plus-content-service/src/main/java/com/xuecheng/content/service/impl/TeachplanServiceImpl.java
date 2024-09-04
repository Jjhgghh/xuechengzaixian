package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collections;
import java.util.List;

@Service
public class TeachplanServiceImpl implements TeachplanService {
    @Autowired
    private TeachplanMapper teachplanMapper;
    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;

    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto){
        Long id = saveTeachplanDto.getId();
        if (id==null){
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);

            Long parentid = saveTeachplanDto.getParentid();
            Long courseId = saveTeachplanDto.getCourseId();
            Integer sorted = getTeachplanCount(parentid, courseId);
            teachplan.setOrderby(sorted);
            teachplanMapper.insert(teachplan);

        }else {
            Teachplan teachplan = teachplanMapper.selectById(id);
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            teachplanMapper.updateById(teachplan);

        }
    }

    @Transactional
    @Override
    public Boolean deleteTeachplan(Long teachplanId) {
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if (teachplan.getGrade()==1){
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper = queryWrapper.eq(Teachplan::getParentid, teachplanId);
            List<Teachplan> teachplans = teachplanMapper.selectList(queryWrapper);
            if (teachplanMapper.selectList(queryWrapper)!=null&&teachplanMapper.selectList(queryWrapper).size()>0){
                return false;
            }
            teachplanMapper.deleteById(teachplanId);
        }else {
            teachplanMapper.deleteById(teachplanId);
            LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper=queryWrapper.eq(TeachplanMedia::getTeachplanId, teachplanId);
            TeachplanMedia teachplanMedia = teachplanMediaMapper.selectOne(queryWrapper);
            if (teachplanMedia!=null){
                teachplanMediaMapper.deleteById(teachplanMedia.getId());
            }

        }


        return true;
    }

    @Override
    @Transactional
    public void moveup(Long teachplanId) {
        Teachplan plan = teachplanMapper.selectById(teachplanId);
        if (plan.getGrade()==1){
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);

            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper=queryWrapper.eq(Teachplan::getCourseId, teachplan.getCourseId())
                    .eq(Teachplan::getGrade, teachplan.getGrade())
                    .eq(Teachplan::getOrderby, teachplan.getOrderby()-1);
            Teachplan upTeachplan = teachplanMapper.selectOne(queryWrapper);
            if (upTeachplan==null){
                return;
            }else {
                upTeachplan.setOrderby(upTeachplan.getOrderby()+1);
                teachplan.setOrderby(teachplan.getOrderby()-1);
                teachplanMapper.updateById(teachplan);
                teachplanMapper.updateById(upTeachplan);
            }
        }else {
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);

            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper = queryWrapper.eq(Teachplan::getCourseId, teachplan.getCourseId())
                    .eq(Teachplan::getGrade, teachplan.getGrade())
                    .eq(Teachplan::getOrderby, teachplan.getOrderby() - 1)
                    .eq(Teachplan::getParentid, teachplan.getParentid());
            Teachplan upTeachplan = teachplanMapper.selectOne(queryWrapper);
            if (upTeachplan == null) {
                return;
            } else {
                upTeachplan.setOrderby(upTeachplan.getOrderby() + 1);
                teachplan.setOrderby(teachplan.getOrderby() - 1);
                teachplanMapper.updateById(teachplan);
                teachplanMapper.updateById(upTeachplan);
            }
        }
    }

    @Override
    public void movedown(Long teachplanId) {
        Teachplan plan = teachplanMapper.selectById(teachplanId);
        if (plan.getGrade()==1){
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);

            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper=queryWrapper.eq(Teachplan::getCourseId, teachplan.getCourseId())
                    .eq(Teachplan::getGrade, teachplan.getGrade())
                    .eq(Teachplan::getOrderby, teachplan.getOrderby()+1);
            Teachplan upTeachplan = teachplanMapper.selectOne(queryWrapper);
            if (upTeachplan==null){
                return;
            }else {
                upTeachplan.setOrderby(upTeachplan.getOrderby()-1);
                teachplan.setOrderby(teachplan.getOrderby()+1);
                teachplanMapper.updateById(teachplan);
                teachplanMapper.updateById(upTeachplan);
            }
        }else {
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);

            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper = queryWrapper.eq(Teachplan::getCourseId, teachplan.getCourseId())
                    .eq(Teachplan::getGrade, teachplan.getGrade())
                    .eq(Teachplan::getOrderby, teachplan.getOrderby() + 1)
                    .eq(Teachplan::getParentid, teachplan.getParentid());
            Teachplan upTeachplan = teachplanMapper.selectOne(queryWrapper);
            if (upTeachplan == null) {
                return;
            } else {
                upTeachplan.setOrderby(upTeachplan.getOrderby() - 1);
                teachplan.setOrderby(teachplan.getOrderby() + 1);
                teachplanMapper.updateById(teachplan);
                teachplanMapper.updateById(upTeachplan);
            }
        }
    }

    public Integer getTeachplanCount(Long parentid,Long courseId){
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(Teachplan::getParentid, parentid).eq(Teachplan::getCourseId, courseId);
        Integer i = teachplanMapper.selectCount(queryWrapper);
        return i+1;
    }


}
