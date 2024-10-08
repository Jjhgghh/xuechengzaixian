package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.AuthService;
import com.xuecheng.ucenter.service.WxAuthService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service("wx_authservice")
public class WxAuthServiceImpl implements AuthService, WxAuthService {
    @Autowired
    XcUserMapper xcUserMapper;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    XcUserRoleMapper xcUserRoleMapper;
    @Autowired
    WxAuthServiceImpl currentProxy;
    @Value("${weixin.appid}")
    private String appid;
    @Value("${weixin.secret}")
    private String secret;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, authParamsDto.getUsername()));
       if (xcUser == null) {
           //返回空表示用户不存在
           throw new RuntimeException("账号不存在");
       }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser, xcUserExt);
        return xcUserExt;
    }

    @Override
    public XcUser wxAuth(String code) {
       //申请令牌
        Map<String, String> accessToken_map = getAccess_token(code);
        //获取用户信息
        String accessToken = accessToken_map.get("access_token");
        String openid = accessToken_map.get("openid");
        Map<String, String> userinfo = getUserinfo(accessToken, openid);
        //保存数据库
        XcUser xcUser = currentProxy.addWxUser(userinfo);

        return xcUser;
    }
//https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code
    private Map<String,String> getAccess_token(String code){
        String url_model="https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
        String url=String.format(url_model,appid,secret,code);
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, null, String.class);
        String body = exchange.getBody();
        Map<String,String> map = JSON.parseObject(body, Map.class);
        return map;
    }

    private Map<String,String> getUserinfo(String access_token,String openid) {
        String wxUrl_template = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
        //请求微信地址
        String wxUrl = String.format(wxUrl_template, access_token,openid);
        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.GET, null, String.class);
        String body = exchange.getBody();
        String s = new String(body.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        Map<String,String> map = JSON.parseObject(s, Map.class);
        return map;
    }

    @Transactional
    public XcUser addWxUser(Map userInfo_map){
        String unionid = userInfo_map.get("unionid").toString();
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getWxUnionid, unionid));
        if(xcUser != null){
            return xcUser;
        }
        String userId = UUID.randomUUID().toString();
        xcUser = new XcUser();
        xcUser.setId(userId);
        xcUser.setWxUnionid(unionid);
        //记录从微信得到的昵称
        xcUser.setNickname(userInfo_map.get("nickname").toString());
        xcUser.setUserpic(userInfo_map.get("headimgurl").toString());
        xcUser.setName(userInfo_map.get("nickname").toString());
        xcUser.setUsername(unionid);
        xcUser.setPassword(unionid);
        xcUser.setUtype("101001");//学生类型
        xcUser.setStatus("1");//用户状态
        xcUser.setCreateTime(LocalDateTime.now());
        xcUserMapper.insert(xcUser);
        XcUserRole xcUserRole = new XcUserRole();
        xcUserRole.setId(UUID.randomUUID().toString());
        xcUserRole.setUserId(userId);
        xcUserRole.setRoleId("17");//学生角色
        xcUserRoleMapper.insert(xcUserRole);
        return xcUser;
    }
}
