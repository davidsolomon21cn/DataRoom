package com.gccloud.gcpaas.dataroom.core.config;

import com.gccloud.gcpaas.dataroom.core.config.converter.PageStatusConverter;
import com.gccloud.gcpaas.dataroom.core.config.converter.PageTypeConverter;
import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPathMatcher;
import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPersistService;
import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPolicy;
import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;

@Configuration
public class DataRoomWebMvcConfigurer implements WebMvcConfigurer {
    /**
     * 添加全局类型格式化器
     *
     * @param registry
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new PageStatusConverter());
        registry.addConverter(new PageTypeConverter());
    }

    @Bean("operationLogExecutor")
    public Executor operationLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("operation-log-");
        executor.initialize();
        return executor;
    }

    @Bean
    public OperationLogPolicy operationLogPolicy() {
        return new OperationLogPolicy();
    }

    @Bean
    public OperationLogPathMatcher operationLogPathMatcher(DataRoomConfig dataRoomConfig) {
        return new OperationLogPathMatcher(dataRoomConfig.getOperationLog().getExcludePaths());
    }

    @Bean
    public OperationLogPublisher operationLogPublisher(Executor operationLogExecutor,
                                                       OperationLogPersistService operationLogPersistService) {
        return new OperationLogPublisher(operationLogExecutor, operationLogPersistService);
    }
}
