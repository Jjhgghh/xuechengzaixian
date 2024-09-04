package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {
    @Autowired
    CourseCategoryMapper courseCategoryMapper;


    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        List<CourseCategoryTreeDto> categoryTreeDtoList=courseCategoryMapper.selectTreeNodes(id);
        Map<String, CourseCategoryTreeDto> map = categoryTreeDtoList.stream().filter(item -> !item.getId().equals(id)).collect(Collectors.toMap(key -> key.getId(),
                value -> value, (key1, key2) -> key2));
        List<CourseCategoryTreeDto> treeDtoArrayList = new ArrayList<>();
        categoryTreeDtoList.stream().filter(item -> !item.getId().equals(id)).forEach(item->{
            if (item.getParentid().equals(id)){
                treeDtoArrayList.add(item);
            }
            CourseCategoryTreeDto courseCategoryTreeDto = map.get(item.getParentid());
            if (courseCategoryTreeDto != null){
                if (courseCategoryTreeDto.getChildrenTreeNodes()==null){
                    courseCategoryTreeDto.setChildrenTreeNodes(new ArrayList<>());
                }
                courseCategoryTreeDto.getChildrenTreeNodes().add(item);
            }
        });
        return treeDtoArrayList;

    }
}
