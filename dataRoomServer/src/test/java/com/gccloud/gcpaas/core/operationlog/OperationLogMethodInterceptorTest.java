package com.gccloud.gcpaas.core.operationlog;

import com.gccloud.gcpaas.dataroom.core.bean.Resp;
import com.gccloud.gcpaas.dataroom.core.operationlog.aop.OperationLogMethodInterceptor;
import com.gccloud.gcpaas.dataroom.core.entity.OperationLogEntity;
import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPersistService;
import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPathMatcher;
import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPolicy;
import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationLogMethodInterceptorTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void extractsSummaryDescriptionAndBusinessModuleFromOperationAndTag() throws Throwable {
        OperationLogPolicy policy = new OperationLogPolicy();
        OperationLogPersistService persistService = mock(OperationLogPersistService.class);
        Executor executor = Runnable::run;
        OperationLogPublisher publisher = new OperationLogPublisher(executor, persistService);
        OperationLogMethodInterceptor interceptor = new OperationLogMethodInterceptor(
                policy, publisher, new OperationLogPathMatcher(List.of()));

        Method method = SampleService.class.getMethod("doWork");
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(method);
        when(invocation.getThis()).thenReturn(null);
        when(invocation.getArguments()).thenReturn(new Object[0]);
        when(invocation.proceed()).thenReturn(Resp.success("ok"));

        interceptor.invoke(invocation);

        ArgumentCaptor<OperationLogEntity> captor = ArgumentCaptor.forClass(OperationLogEntity.class);
        verify(persistService).persist(captor.capture());
        OperationLogEntity context = captor.getValue();
        assertEquals("执行工作", context.getOperationSummary());
        assertEquals("执行业务工作", context.getOperationDescription());
        assertEquals("示例模块", context.getBusinessModule());
        assertEquals("SUCCESS", context.getResultStatus());
    }

    @Test
    void recordsFailureWhenProceedThrows() throws Throwable {
        OperationLogPolicy policy = new OperationLogPolicy();
        OperationLogPersistService persistService = mock(OperationLogPersistService.class);
        OperationLogPublisher publisher = new OperationLogPublisher(Runnable::run, persistService);
        OperationLogMethodInterceptor interceptor = new OperationLogMethodInterceptor(
                policy, publisher, new OperationLogPathMatcher(List.of()));

        Method method = SampleService.class.getMethod("doWork");
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(method);
        when(invocation.getThis()).thenReturn(null);
        when(invocation.getArguments()).thenReturn(new Object[0]);
        when(invocation.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class, () -> interceptor.invoke(invocation));

        ArgumentCaptor<OperationLogEntity> captor = ArgumentCaptor.forClass(OperationLogEntity.class);
        verify(persistService).persist(captor.capture());
        OperationLogEntity context = captor.getValue();
        assertEquals("FAILURE", context.getResultStatus());
        assertEquals("IllegalStateException", context.getExceptionType());
    }

    @Test
    void excludedHttpRequestProceedsWithoutPublishingLog() throws Throwable {
        OperationLogPersistService persistService = mock(OperationLogPersistService.class);
        OperationLogPublisher publisher = new OperationLogPublisher(Runnable::run, persistService);
        OperationLogMethodInterceptor interceptor = new OperationLogMethodInterceptor(
                new OperationLogPolicy(),
                publisher,
                new OperationLogPathMatcher(List.of("/a/**"))
        );
        MethodInvocation invocation = invocationReturning(Resp.success("ok"));
        bindRequest("/runtime/a/work", "/runtime");

        Object result = interceptor.invoke(invocation);

        assertEquals("ok", ((Resp<?>) result).getData());
        verify(invocation).proceed();
        verify(persistService, never()).persist(any(OperationLogEntity.class));
    }

    @Test
    void excludedHttpRequestPropagatesFailureWithoutPublishingLog() throws Throwable {
        OperationLogPersistService persistService = mock(OperationLogPersistService.class);
        OperationLogPublisher publisher = new OperationLogPublisher(Runnable::run, persistService);
        OperationLogMethodInterceptor interceptor = new OperationLogMethodInterceptor(
                new OperationLogPolicy(),
                publisher,
                new OperationLogPathMatcher(List.of("/a/**"))
        );
        MethodInvocation invocation = invocationThrowing(new IllegalStateException("boom"));
        bindRequest("/runtime/a/work", "/runtime");

        assertThrows(IllegalStateException.class, () -> interceptor.invoke(invocation));

        verify(persistService, never()).persist(any(OperationLogEntity.class));
    }

    @Test
    void exclusionPatternsDoNotSuppressInvocationWithoutHttpRequest() throws Throwable {
        OperationLogPersistService persistService = mock(OperationLogPersistService.class);
        OperationLogPublisher publisher = new OperationLogPublisher(Runnable::run, persistService);
        OperationLogMethodInterceptor interceptor = new OperationLogMethodInterceptor(
                new OperationLogPolicy(),
                publisher,
                new OperationLogPathMatcher(List.of("/**"))
        );
        MethodInvocation invocation = invocationReturning(Resp.success("ok"));

        interceptor.invoke(invocation);

        verify(persistService).persist(any(OperationLogEntity.class));
    }

    private static MethodInvocation invocationReturning(Object result) throws Throwable {
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(SampleService.class.getMethod("doWork"));
        when(invocation.getThis()).thenReturn(null);
        when(invocation.getArguments()).thenReturn(new Object[0]);
        when(invocation.proceed()).thenReturn(result);
        return invocation;
    }

    private static MethodInvocation invocationThrowing(Throwable throwable) throws Throwable {
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(SampleService.class.getMethod("doWork"));
        when(invocation.getThis()).thenReturn(null);
        when(invocation.getArguments()).thenReturn(new Object[0]);
        when(invocation.proceed()).thenThrow(throwable);
        return invocation;
    }

    private static void bindRequest(String requestUri, String contextPath) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        request.setContextPath(contextPath);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Tag(name = "示例模块")
    static class SampleService {

        @Operation(summary = "执行工作", description = "执行业务工作")
        public Resp<String> doWork() {
            return Resp.success("ok");
        }
    }
}
