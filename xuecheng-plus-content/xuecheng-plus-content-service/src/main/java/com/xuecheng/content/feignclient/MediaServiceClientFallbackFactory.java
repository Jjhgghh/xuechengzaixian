package com.xuecheng.content.feignclient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Slf4j
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaServiceClient> {
    @Override
    public MediaServiceClient create(Throwable cause) {
        return new MediaServiceClient() {
            @Override
            public String upload(MultipartFile filedatas, String objectName) {
                log.debug("调用上传文件接口熔断,异常信息为:{}",cause.toString(),cause);
                return null;
            }
        };
    }
}
