package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcMenuMapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcMenu;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserServiceImpl implements UserDetailsService {
    @Autowired
    XcUserMapper xcUserMapper;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    XcMenuMapper xcMenuMapper;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthParamsDto authParamsDto=null;
        try {
            authParamsDto=JSON.parseObject(username, AuthParamsDto.class);
        } catch (Exception e) {
            throw new RuntimeException("认证请求数据格式不对");
        }
        String authType = authParamsDto.getAuthType();
        AuthService bean = applicationContext.getBean(authType + "_authservice", AuthService.class);
        XcUserExt xcUserExt = bean.execute(authParamsDto);



        return getUserPrincipal(xcUserExt);


    }
    public UserDetails getUserPrincipal(XcUserExt xcUser){
        String password = xcUser.getPassword();
        String[] authorities= {"p1"};
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId(xcUser.getId());
        List<String> list=new ArrayList<>();
        if (xcMenus!=null&&xcMenus.size()>0){
            xcMenus.forEach(m->{
                list.add(m.getCode());
            });
        }
        authorities= list.toArray(new String[0]);

        xcUser.setPassword(null);
        String jsonString = JSON.toJSONString(xcUser);

        UserDetails userDetails = User.withUsername(jsonString).password(password).authorities(authorities).build();
        return userDetails;
    }
}
