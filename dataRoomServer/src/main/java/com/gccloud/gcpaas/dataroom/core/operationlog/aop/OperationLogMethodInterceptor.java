package com.gccloud.gcpaas.dataroom.core.operationlog.aop;

import com.gccloud.gcpaas.dataroom.core.bean.Resp;
import com.gccloud.gcpaas.dataroom.core.entity.OperationLogEntity;
import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPathMatcher;
import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPolicy;
import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPublisher;
import com.gccloud.gcpaas.dataroom.core.shiro.LoginUser;
import com.gccloud.gcpaas.dataroom.core.util.LoginUserUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class OperationLogMethodInterceptor implements MethodInterceptor {

    private final OperationLogPolicy operationLogPolicy;
    private final OperationLogPublisher operationLogPublisher;
    private final OperationLogPathMatcher operationLogPathMatcher;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Class<?> targetClass = invocation.getThis() == null ? method.getDeclaringClass() : ClassUtils.getUserClass(invocation.getThis());

        Operation operation = AnnotatedElementUtils.findMergedAnnotation(method, Operation.class);
        // 切点已保证方法上存在 @Operation，这里仅做防御性放行
        if (operation == null) {
            return invocation.proceed();
        }

        HttpServletRequest request = currentRequest();
        if (operationLogPathMatcher.isExcluded(request)) {
            return invocation.proceed();
        }

        long startNanos = System.nanoTime();

        OperationLogEntity entity = new OperationLogEntity();
        entity.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        entity.setRequestTime(new Date());
        entity.setOperationSummary(operation.summary());
        entity.setOperationDescription(operation.description());
        Tag tag = AnnotationUtils.findAnnotation(targetClass, Tag.class);
        entity.setBusinessModule(tag != null ? tag.name() : null);
        entity.setRequestUri(resolveRequestUri(targetClass, method, request));
        entity.setRequestMethod(isToolInvocation(method) ? "MCP" : (request != null ? request.getMethod() : "UNKNOWN"));
        if (request != null) {
            entity.setClientIp(request.getRemoteAddr());
            entity.setUserAgent(request.getHeader("User-Agent"));
            entity.setContentType(request.getContentType());
            entity.setQueryParams(operationLogPolicy.sanitizeQueryString(request.getQueryString()));
        }
        fillOperator(entity);

        try {
            Object result = invocation.proceed();
            fillResult(entity, result);
            return result;
        } catch (Throwable throwable) {
            log.error(ExceptionUtils.getStackTrace(throwable));
            entity.setResultStatus("FAILURE");
            entity.setResponseCode(500);
            entity.setResponseMessage(StringUtils.defaultIfBlank(throwable.getMessage(), "服务器异常"));
            entity.setExceptionType(throwable.getClass().getSimpleName());
            entity.setExceptionStack(operationLogPolicy.truncateStack(ExceptionUtils.getStackTrace(throwable)));
            throw throwable;
        } finally {
            entity.setDurationMs((System.nanoTime() - startNanos) / 1_000_000);
            operationLogPublisher.publish(entity);
        }
    }

    private void fillResult(OperationLogEntity entity, Object result) {
        if (result instanceof Resp<?> resp) {
            entity.setResponseCode(resp.getCode());
            entity.setResponseMessage(resp.getMessage());
            entity.setResultStatus(resp.getCode() != null && resp.getCode() == 200 ? "SUCCESS" : "FAILURE");
            return;
        }
        entity.setResponseCode(200);
        entity.setResultStatus("SUCCESS");
    }

    private void fillOperator(OperationLogEntity entity) {
        LoginUser currentUser = LoginUserUtils.getCurrentUser();
        if (currentUser == null) {
            return;
        }
        entity.setOperatorId(currentUser.getId());
        entity.setOperatorName(currentUser.getUsername());
        entity.setOperatorRole(currentUser.getRole());
        entity.setTenantCode(currentUser.getTenantCode());
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private boolean isToolInvocation(Method method) {
        return AnnotatedElementUtils.findMergedAnnotation(method, Tool.class) != null;
    }

    private String resolveRequestUri(Class<?> targetClass, Method method, HttpServletRequest request) {
        if (request != null) {
            return operationLogPathMatcher.resolveRequestPath(request);
        }
        Tool tool = AnnotatedElementUtils.findMergedAnnotation(method, Tool.class);
        if (tool != null && StringUtils.isNotBlank(tool.name())) {
            return "/mcp/tool/" + tool.name();
        }
        return targetClass.getName() + "#" + method.getName();
    }
}
