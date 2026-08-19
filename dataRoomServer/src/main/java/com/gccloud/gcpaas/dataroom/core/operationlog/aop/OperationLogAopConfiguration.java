package com.gccloud.gcpaas.dataroom.core.operationlog.aop;

import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPathMatcher;
import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPolicy;
import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPublisher;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;

@Configuration
public class OperationLogAopConfiguration {

    @Bean
    public OperationLogMethodInterceptor operationLogMethodInterceptor(OperationLogPolicy operationLogPolicy,
                                                                       OperationLogPublisher operationLogPublisher,
                                                                       OperationLogPathMatcher operationLogPathMatcher) {
        return new OperationLogMethodInterceptor(operationLogPolicy, operationLogPublisher, operationLogPathMatcher);
    }

    @Bean
    public Advisor operationLogAdvisor(OperationLogMethodInterceptor operationLogMethodInterceptor) {
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(new OperationLogOperationPointcut(), operationLogMethodInterceptor);
        advisor.setOrder(Ordered.LOWEST_PRECEDENCE - 20);
        return advisor;
    }

    /**
     * 仅拦截方法上标注了 Swagger {@link Operation} 注解的方法，统一记录 HTTP 与 MCP(@Tool) 调用。
     */
    private static class OperationLogOperationPointcut extends StaticMethodMatcherPointcut {
        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            return AnnotatedElementUtils.hasAnnotation(method, Operation.class);
        }
    }
}
