# Operation Log Exclude Paths Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow administrators to exclude HTTP operation-log records through `dataroom.operation-log.exclude-paths`, with Spring path patterns such as `/a/**`.

**Architecture:** Extend the existing `DataRoomConfig` nested configuration style with a focused operation-log bean. Compile configured patterns once in an `OperationLogPathMatcher`, then make `OperationLogMethodInterceptor` bypass all log creation and publication when the current HTTP request matches; invocations without an HTTP request continue through the existing MCP logging flow.

**Tech Stack:** Java 17, Spring Boot 3.5.10 configuration properties, Spring 6 `PathPatternParser`, Jakarta Servlet API, JUnit 5, Mockito, Spring mock servlet API, Maven.

## Global Constraints

- Configuration is the only exclusion mechanism; do not add `OperationLogIgnore` or any other annotation.
- Bind the property name exactly as `dataroom.operation-log.exclude-paths`.
- Match `requestURI` after removing `contextPath`; do not include the query string.
- Keep the default exclusion list empty so existing logging behavior is preserved.
- Do not apply HTTP path exclusions to invocations without an HTTP request, including MCP tools.
- A matched request must not publish a log whether it returns normally or throws.
- Invalid Spring path patterns must fail during bean construction; do not silently ignore them.
- Preserve all unrelated user changes in the dirty worktree.
- Do not add or modify Java `catch` blocks.

---

## File Structure

- Create `dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/config/bean/OperationLogConfig.java`: hold the configured URL exclusion list.
- Modify `dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/config/DataRoomConfig.java`: expose the nested `operationLog` configuration bean.
- Create `dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/operationlog/service/OperationLogPathMatcher.java`: normalize request paths, precompile Spring path patterns, and answer exclusion checks.
- Modify `dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/config/DataRoomWebMvcConfigurer.java`: construct the matcher from bound configuration.
- Modify `dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/operationlog/aop/OperationLogAopConfiguration.java`: inject the matcher into the AOP interceptor.
- Modify `dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/operationlog/aop/OperationLogMethodInterceptor.java`: bypass logging for excluded HTTP requests and reuse normalized paths.
- Modify `dataRoomServer/src/main/resources/application-base.yml`: document the empty default exclusion list.
- Create `dataRoomServer/src/test/java/com/gccloud/gcpaas/core/operationlog/OperationLogConfigBindingTest.java`: verify the exact YAML property path binds into `DataRoomConfig`.
- Create `dataRoomServer/src/test/java/com/gccloud/gcpaas/core/operationlog/OperationLogPathMatcherTest.java`: lock down path-pattern and context-path semantics.
- Modify `dataRoomServer/src/test/java/com/gccloud/gcpaas/core/operationlog/OperationLogMethodInterceptorTest.java`: verify AOP bypass, exception propagation, and non-HTTP behavior.

---

### Task 1: Bind and Match Excluded HTTP Paths

**Files:**
- Create: `dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/config/bean/OperationLogConfig.java`
- Modify: `dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/config/DataRoomConfig.java`
- Create: `dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/operationlog/service/OperationLogPathMatcher.java`
- Modify: `dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/config/DataRoomWebMvcConfigurer.java`
- Modify: `dataRoomServer/src/main/resources/application-base.yml`
- Test: `dataRoomServer/src/test/java/com/gccloud/gcpaas/core/operationlog/OperationLogConfigBindingTest.java`
- Test: `dataRoomServer/src/test/java/com/gccloud/gcpaas/core/operationlog/OperationLogPathMatcherTest.java`

**Interfaces:**
- Consumes: Spring Boot nested binding under the existing `@ConfigurationProperties(prefix = "dataroom")` `DataRoomConfig`.
- Produces: `DataRoomConfig#getOperationLog(): OperationLogConfig`, `OperationLogConfig#getExcludePaths(): List<String>`, `OperationLogPathMatcher(List<String>)`, `OperationLogPathMatcher#isExcluded(HttpServletRequest): boolean`, and `OperationLogPathMatcher#resolveRequestPath(HttpServletRequest): String`.

- [ ] **Step 1: Write the failing path matcher tests**

Create `OperationLogPathMatcherTest.java`:

```java
package com.gccloud.gcpaas.core.operationlog;

import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPathMatcher;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.util.pattern.PatternParseException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationLogPathMatcherTest {

    @Test
    void springDoubleWildcardMatchesPathAndNestedPaths() {
        OperationLogPathMatcher matcher = new OperationLogPathMatcher(List.of("/a/**"));

        assertTrue(matcher.isExcluded(request("/a", "")));
        assertTrue(matcher.isExcluded(request("/a/", "")));
        assertTrue(matcher.isExcluded(request("/a/b", "")));
        assertTrue(matcher.isExcluded(request("/a/b/c", "")));
        assertFalse(matcher.isExcluded(request("/ab/c", "")));
    }

    @Test
    void removesContextPathAndIgnoresQueryString() {
        OperationLogPathMatcher matcher = new OperationLogPathMatcher(List.of("/a/**"));
        MockHttpServletRequest request = request("/runtime/a/b", "/runtime");
        request.setQueryString("page=1");

        assertTrue(matcher.isExcluded(request));
    }

    @Test
    void emptyBlankAndDuplicatePatternsDoNotExcludeUnrelatedRequests() {
        OperationLogPathMatcher matcher = new OperationLogPathMatcher(List.of("", "  ", "/a/**", "/a/**"));

        assertFalse(matcher.isExcluded(request("/b", "")));
        assertFalse(new OperationLogPathMatcher(List.of()).isExcluded(request("/a/b", "")));
        assertFalse(matcher.isExcluded(null));
    }

    @Test
    void rejectsInvalidSpringPathPattern() {
        assertThrows(PatternParseException.class,
                () -> new OperationLogPathMatcher(List.of("/a/**/b")));
    }

    private static MockHttpServletRequest request(String requestUri, String contextPath) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        request.setContextPath(contextPath);
        return request;
    }
}
```

- [ ] **Step 2: Run the matcher test and verify it fails**

Run:

```bash
mvn -q -pl dataRoomServer -Dtest=OperationLogPathMatcherTest -DforkCount=0 test
```

Expected: FAIL during test compilation because `OperationLogPathMatcher` does not exist.

- [ ] **Step 3: Add the nested operation-log configuration bean**

Create `OperationLogConfig.java`:

```java
package com.gccloud.gcpaas.dataroom.core.config.bean;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OperationLogConfig {

    /**
     * 不记录操作日志的 HTTP 请求路径，使用 Spring PathPattern 语法。
     */
    private List<String> excludePaths = new ArrayList<>();
}
```

Add the import and initialized property to `DataRoomConfig.java`:

```java
import com.gccloud.gcpaas.dataroom.core.config.bean.OperationLogConfig;
```

```java
/**
 * 操作日志配置。
 */
private OperationLogConfig operationLog = new OperationLogConfig();
```

This keeps binding under the existing `dataroom` prefix, so YAML `operation-log.exclude-paths` maps to `operationLog.excludePaths` through Spring Boot relaxed binding.

- [ ] **Step 4: Implement the path matcher**

Create `OperationLogPathMatcher.java`:

```java
package com.gccloud.gcpaas.dataroom.core.operationlog.service;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

public class OperationLogPathMatcher {

    private final List<PathPattern> excludePatterns;

    public OperationLogPathMatcher(List<String> excludePaths) {
        List<String> paths = excludePaths == null ? List.of() : excludePaths;
        this.excludePatterns = paths.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .map(PathPatternParser.defaultInstance::parse)
                .toList();
    }

    public boolean isExcluded(HttpServletRequest request) {
        if (request == null || excludePatterns.isEmpty()) {
            return false;
        }
        PathContainer requestPath = PathContainer.parsePath(resolveRequestPath(request));
        return excludePatterns.stream().anyMatch(pattern -> pattern.matches(requestPath));
    }

    public String resolveRequestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.isNotBlank(contextPath) && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }
}
```

Do not catch `PatternParseException`; constructor failure is the required behavior for invalid configuration.

- [ ] **Step 5: Register the matcher from bound configuration**

Add imports to `DataRoomWebMvcConfigurer.java`:

```java
import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPathMatcher;
```

Add this bean next to the other operation-log beans:

```java
@Bean
public OperationLogPathMatcher operationLogPathMatcher(DataRoomConfig dataRoomConfig) {
    return new OperationLogPathMatcher(dataRoomConfig.getOperationLog().getExcludePaths());
}
```

No additional `@ConfigurationProperties` registration is needed because `DataRoomConfig` is already a scanned `@Configuration` bean.

- [ ] **Step 6: Document the empty default YAML configuration**

Under the top-level `dataroom:` section in `application-base.yml`, add:

```yaml
  operation-log:
    # 不记录操作日志的 HTTP 路径，支持 Spring PathPattern，例如 /a/**
    exclude-paths: []
```

Do not add any active default exclusions because that would change existing logging behavior.

- [ ] **Step 7: Add a configuration binding contract test**

Create `OperationLogConfigBindingTest.java`:

```java
package com.gccloud.gcpaas.core.operationlog;

import com.gccloud.gcpaas.dataroom.core.config.DataRoomConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationLogConfigBindingTest {

    @Test
    void bindsOperationLogExcludePathsFromDocumentedPropertyName() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "dataroom.operation-log.exclude-paths[0]", "/a/**",
                "dataroom.operation-log.exclude-paths[1]", "/dataRoom/captcha/**"
        ));

        DataRoomConfig config = new Binder(source)
                .bind("dataroom", Bindable.of(DataRoomConfig.class))
                .orElseThrow();

        assertEquals(List.of("/a/**", "/dataRoom/captcha/**"),
                config.getOperationLog().getExcludePaths());
    }
}
```

- [ ] **Step 8: Run the configuration and matcher tests and verify they pass**

Run:

```bash
mvn -q -pl dataRoomServer \
  -Dtest=OperationLogConfigBindingTest,OperationLogPathMatcherTest \
  -DforkCount=0 test
```

Expected: PASS, including exact property binding, context-path normalization, and invalid-pattern failure.

- [ ] **Step 9: Commit the configuration and matcher task**

```bash
git add dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/config/bean/OperationLogConfig.java \
  dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/config/DataRoomConfig.java \
  dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/operationlog/service/OperationLogPathMatcher.java \
  dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/config/DataRoomWebMvcConfigurer.java \
  dataRoomServer/src/main/resources/application-base.yml \
  dataRoomServer/src/test/java/com/gccloud/gcpaas/core/operationlog/OperationLogConfigBindingTest.java \
  dataRoomServer/src/test/java/com/gccloud/gcpaas/core/operationlog/OperationLogPathMatcherTest.java
git commit -m "feat(log): add operation log path matcher"
```

---

### Task 2: Bypass AOP Logging for Excluded Requests

**Files:**
- Modify: `dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/operationlog/aop/OperationLogAopConfiguration.java`
- Modify: `dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/operationlog/aop/OperationLogMethodInterceptor.java`
- Modify: `dataRoomServer/src/test/java/com/gccloud/gcpaas/core/operationlog/OperationLogMethodInterceptorTest.java`

**Interfaces:**
- Consumes: `OperationLogPathMatcher#isExcluded(HttpServletRequest): boolean` and `OperationLogPathMatcher#resolveRequestPath(HttpServletRequest): String` from Task 1.
- Produces: `OperationLogMethodInterceptor(OperationLogPolicy, OperationLogPublisher, OperationLogPathMatcher)` with early bypass for matching HTTP requests.

- [ ] **Step 1: Extend interceptor tests with request-context cleanup and matcher construction**

In `OperationLogMethodInterceptorTest.java`, add imports:

```java
import com.gccloud.gcpaas.dataroom.core.operationlog.service.OperationLogPathMatcher;
import org.junit.jupiter.api.AfterEach;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
```

Add cleanup so servlet request state never leaks between tests:

```java
@AfterEach
void clearRequestContext() {
    RequestContextHolder.resetRequestAttributes();
}
```

Update both existing interceptor constructions to pass an empty matcher:

```java
new OperationLogMethodInterceptor(policy, publisher, new OperationLogPathMatcher(List.of()))
```

- [ ] **Step 2: Write failing AOP exclusion tests**

Add these tests to `OperationLogMethodInterceptorTest.java`:

```java
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
```

Add these helpers inside the test class:

```java
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
```

- [ ] **Step 3: Run the interceptor test and verify it fails**

Run:

```bash
mvn -q -pl dataRoomServer -Dtest=OperationLogMethodInterceptorTest -DforkCount=0 test
```

Expected: FAIL during compilation because the interceptor constructor does not yet accept `OperationLogPathMatcher`, or FAIL because matching requests are still published.

- [ ] **Step 4: Inject the matcher through AOP configuration**

In `OperationLogAopConfiguration.java`, import `OperationLogPathMatcher` and change the bean factory to:

```java
@Bean
public OperationLogMethodInterceptor operationLogMethodInterceptor(OperationLogPolicy operationLogPolicy,
                                                                   OperationLogPublisher operationLogPublisher,
                                                                   OperationLogPathMatcher operationLogPathMatcher) {
    return new OperationLogMethodInterceptor(operationLogPolicy, operationLogPublisher, operationLogPathMatcher);
}
```

- [ ] **Step 5: Add the early exclusion check to the interceptor**

In `OperationLogMethodInterceptor.java`, add the field:

```java
private final OperationLogPathMatcher operationLogPathMatcher;
```

In `invoke(...)`, obtain the request and bypass logging immediately after the defensive `@Operation` check:

```java
HttpServletRequest request = currentRequest();
if (operationLogPathMatcher.isExcluded(request)) {
    return invocation.proceed();
}

long startNanos = System.nanoTime();
```

Remove the later duplicate `HttpServletRequest request = currentRequest();` declaration.

In `resolveRequestUri(...)`, replace the HTTP normalization block with:

```java
if (request != null) {
    return operationLogPathMatcher.resolveRequestPath(request);
}
```

Keep the existing MCP `/mcp/tool/{name}` and class/method fallbacks unchanged.

- [ ] **Step 6: Run focused operation-log tests**

Run:

```bash
mvn -q -pl dataRoomServer \
  -Dtest=OperationLogConfigBindingTest,OperationLogPathMatcherTest,OperationLogMethodInterceptorTest,OperationLogPolicyTest,OperationLogPublisherTest \
  -DforkCount=0 test
```

Expected: PASS. The excluded normal and exceptional HTTP requests do not persist logs, while an invocation without a request still does.

- [ ] **Step 7: Run the complete backend test suite for the module**

Run:

```bash
mvn -q -pl dataRoomServer -DforkCount=0 test
```

Expected: PASS. No `CatchBlockLoggingTest` violation is introduced because this change does not add or modify a Java `catch` block.

- [ ] **Step 8: Inspect the final diff for scope and formatting**

Run:

```bash
git diff --check
git status --short
git diff -- dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/config/bean/OperationLogConfig.java \
  dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/config/DataRoomConfig.java \
  dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/config/DataRoomWebMvcConfigurer.java \
  dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/operationlog/service/OperationLogPathMatcher.java \
  dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/operationlog/aop/OperationLogAopConfiguration.java \
  dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/operationlog/aop/OperationLogMethodInterceptor.java \
  dataRoomServer/src/main/resources/application-base.yml \
  dataRoomServer/src/test/java/com/gccloud/gcpaas/core/operationlog/OperationLogConfigBindingTest.java \
  dataRoomServer/src/test/java/com/gccloud/gcpaas/core/operationlog/OperationLogPathMatcherTest.java \
  dataRoomServer/src/test/java/com/gccloud/gcpaas/core/operationlog/OperationLogMethodInterceptorTest.java
```

Expected: no whitespace errors; only the planned files contain task changes. Existing unrelated dirty-worktree changes remain untouched.

- [ ] **Step 9: Commit the AOP integration**

```bash
git add dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/operationlog/aop/OperationLogAopConfiguration.java \
  dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/operationlog/aop/OperationLogMethodInterceptor.java \
  dataRoomServer/src/test/java/com/gccloud/gcpaas/core/operationlog/OperationLogMethodInterceptorTest.java
git commit -m "feat(log): exclude configured request paths"
```
