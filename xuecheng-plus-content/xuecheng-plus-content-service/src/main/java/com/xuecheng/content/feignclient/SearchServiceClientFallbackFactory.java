package com.xuecheng.content.feignclient;

import feign.hystrix.FallbackFactory;
import groovy.util.logging.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SearchServiceClientFallbackFactory implements FallbackFactory<SearchServiceClient> {
    private static final Logger log = LoggerFactory.getLogger(SearchServiceClientFallbackFactory.class);

    @Override
    public SearchServiceClient create(Throwable cause) {
        return new SearchServiceClient() {
            @Override
            public Boolean add(CourseIndex courseIndex) {
                log.error("调用搜素服务添加索引发生熔断走降级方法,熔断异常:{}",cause.getMessage());
                return false;
            }
        };
    }
}
