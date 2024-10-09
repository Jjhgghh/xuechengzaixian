package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.learning.config.PayNotifyConfig;
import com.xuecheng.learning.service.MyCourseTablesService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author Mr.M
 * @version 1.0
 * @description 接收支付结果
 * @date 2023/2/23 19:04
 */
@Slf4j
@Service
public class ReceivePayNotifyService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    MqMessageService mqMessageService;

    @Autowired
    MyCourseTablesService myCourseTablesService;


    //监听消息队列接收支付结果通知
    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
    public void receive(Message message, Channel channel) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        byte[] body = message.getBody();
        String s = new String(body);
        MqMessage mqMessage = JSON.parseObject(s, MqMessage.class);
        log.debug("学习中心服务接收支付结果:{}", mqMessage);
        String messageType = mqMessage.getMessageType();
        if (PayNotifyConfig.MESSAGE_TYPE.equals(messageType)&&"60201".equals(mqMessage.getBusinessKey2())){
            String businessKey1 = mqMessage.getBusinessKey1();
            boolean b = myCourseTablesService.saveChooseCourseSuccess(businessKey1);
            if (!b){
                //添加选课失败，抛出异常，消息重回队列
                XueChengPlusException.cast("收到支付结果，添加选课失败");
            }
        }



    }


}