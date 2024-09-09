package com.xuecheng.system;

import com.xuecheng.system.mapper.DictionaryMapper;
import com.xuecheng.system.model.po.Dictionary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MapperTest {
    @Autowired
    DictionaryMapper dictionaryMapper;

    @Test
    public void test() {
        Dictionary dictionary = dictionaryMapper.selectById(12L);
        System.out.println(dictionary);
    }
}
