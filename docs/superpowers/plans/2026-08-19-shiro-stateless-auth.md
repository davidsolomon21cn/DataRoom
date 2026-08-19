# Shiro Stateless Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove Shiro Session participation from DataRoom authentication so same-origin concurrent Token requests never create, restore, rotate, or stop `JSESSIONID` sessions.

**Architecture:** Keep the existing per-request `ShiroAuthFilter` and `ShiroAuthRealm` Token validation flow. Configure `DefaultWebSessionManager` to ignore Session IDs, configure `DefaultSubjectDAO` to avoid persisting Subject state, and prepend Shiro's built-in `noSessionCreation` filter to every chain. Authentication and role authorization remain request-scoped.

**Tech Stack:** Java 17, Spring Boot 3.5.10, Apache Shiro 2.2.1 Jakarta, JUnit 5, Mockito, Spring mock servlet API, Maven.

## Global Constraints

- Do not downgrade Apache Shiro 2.2.1.
- Do not use `JSESSIONID` or Shiro Session for authentication state.
- Every protected request must continue validating its Token.
- Preserve current anonymous paths and `@RequiresRoles` behavior.
- Do not modify frontend Token Cookie or request-header behavior.
- Do not add or modify Java `catch` blocks unless the required stack-trace logging rule is followed.
- Preserve all unrelated user changes in the dirty worktree.

---

## File Structure

- Modify `dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/shiro/ShiroConfiguration.java`: enforce stateless Shiro Session and filter-chain behavior.
- Modify `dataRoomServer/src/test/java/com/gccloud/gcpaas/core/shiro/ShiroConfigurationCasTest.java`: lock down stateless configuration, anonymous paths, protected paths, and concurrent request-scoped login behavior.

No frontend files, dependency versions, YAML configuration, or business controllers are changed.

---

### Task 1: Enforce Stateless Shiro Configuration

**Files:**
- Modify: `dataRoomServer/src/test/java/com/gccloud/gcpaas/core/shiro/ShiroConfigurationCasTest.java`
- Modify: `dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/shiro/ShiroConfiguration.java`

**Interfaces:**
- Consumes: existing `ShiroConfiguration.sessionManager()`, `ShiroConfiguration.securityManager(...)`, and `ShiroConfiguration.shiroFilter(...)` bean factories.
- Produces: a `DefaultWebSessionManager` with Session ID transport disabled, a `DefaultWebSecurityManager` with Subject Session persistence disabled, and filter-chain values `noSessionCreation,anon` or `noSessionCreation,OAUTH`.

- [ ] **Step 1: Write failing configuration contract tests**

Replace `ShiroConfigurationCasTest` with tests that preserve the CAS assertion and add explicit stateless contracts:

```java
package com.gccloud.gcpaas.core.shiro;

import com.gccloud.gcpaas.dataroom.core.config.DataRoomConfig;
import com.gccloud.gcpaas.dataroom.core.shiro.ShiroAuthRealm;
import com.gccloud.gcpaas.dataroom.core.shiro.ShiroConfiguration;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.config.ShiroFilterConfiguration;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class ShiroConfigurationCasTest {

    @Test
    void filterChainsDisableSessionCreationWithoutChangingAccessRules() {
        ShiroFilterFactoryBean filter = new ShiroConfiguration().shiroFilter(
                mock(SecurityManager.class),
                new ShiroFilterConfiguration(),
                dataRoomConfig()
        );

        for (String path : List.of(
                "/dataRoom/captcha/**",
                "/dataRoom/user/login",
                "/dataRoom/user/login/**",
                "/cas/login",
                "/webjars/**",
                "/v3/api-docs/**",
                "/doc.html/**",
                "/h2-console/**",
                "/static/**",
                "/dataRoom/resource/file/**"
        )) {
            assertEquals("noSessionCreation,anon", filter.getFilterChainDefinitionMap().get(path));
        }
        assertEquals("noSessionCreation,OAUTH", filter.getFilterChainDefinitionMap().get("/**"));
    }

    @Test
    void securityManagerDoesNotReadCreateOrPersistSessions() {
        ShiroConfiguration configuration = new ShiroConfiguration();
        SessionManager sessionManager = configuration.sessionManager();
        DefaultWebSessionManager webSessionManager = assertInstanceOf(
                DefaultWebSessionManager.class,
                sessionManager
        );

        assertFalse(webSessionManager.isSessionIdCookieEnabled());
        assertFalse(webSessionManager.isSessionIdUrlRewritingEnabled());

        DefaultWebSecurityManager securityManager = assertInstanceOf(
                DefaultWebSecurityManager.class,
                configuration.securityManager(mock(ShiroAuthRealm.class), sessionManager)
        );
        DefaultSubjectDAO subjectDAO = assertInstanceOf(
                DefaultSubjectDAO.class,
                securityManager.getSubjectDAO()
        );
        DefaultSessionStorageEvaluator evaluator = assertInstanceOf(
                DefaultSessionStorageEvaluator.class,
                subjectDAO.getSessionStorageEvaluator()
        );

        assertFalse(evaluator.isSessionStorageEnabled());
    }

    private static DataRoomConfig dataRoomConfig() {
        DataRoomConfig config = new DataRoomConfig();
        config.getJwt().setTokenKey("customToken");
        return config;
    }
}
```

- [ ] **Step 2: Run the tests and verify the current implementation fails**

Run:

```bash
mvn -q -pl dataRoomServer -Dtest=ShiroConfigurationCasTest test
```

Expected: FAIL because filter chains do not contain `noSessionCreation`, Session ID Cookie remains enabled, and Subject Session persistence remains enabled.

- [ ] **Step 3: Implement stateless Session configuration**

In `ShiroConfiguration.java`, replace the access-rule constants with:

```java
private static final String STATELESS_ANON = "noSessionCreation,anon";

private static final String STATELESS_OAUTH = "noSessionCreation,OAUTH";
```

Add imports:

```java
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
```

Update `sessionManager()` so the existing validation settings remain and Session ID transport is disabled:

```java
@Bean
public SessionManager sessionManager() {
    DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
    sessionManager.setSessionValidationSchedulerEnabled(false);
    sessionManager.setDeleteInvalidSessions(false);
    sessionManager.setSessionIdCookieEnabled(false);
    sessionManager.setSessionIdUrlRewritingEnabled(false);
    return sessionManager;
}
```

Update `securityManager(...)` to disable Subject Session persistence:

```java
@Bean
public SecurityManager securityManager(ShiroAuthRealm shiroAuthRealm, SessionManager sessionManager) {
    DefaultSessionStorageEvaluator sessionStorageEvaluator = new DefaultSessionStorageEvaluator();
    sessionStorageEvaluator.setSessionStorageEnabled(false);
    DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
    subjectDAO.setSessionStorageEvaluator(sessionStorageEvaluator);

    DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
    securityManager.setRealm(shiroAuthRealm);
    securityManager.setSessionManager(sessionManager);
    securityManager.setSubjectDAO(subjectDAO);
    securityManager.setRememberMeManager(null);
    return securityManager;
}
```

Replace every anonymous filter-map value with `STATELESS_ANON`, and replace the final protected rule with:

```java
filterMap.put("/**", STATELESS_OAUTH);
```

- [ ] **Step 4: Run the configuration tests and verify they pass**

Run:

```bash
mvn -q -pl dataRoomServer -Dtest=ShiroConfigurationCasTest test
```

Expected: PASS.

- [ ] **Step 5: Commit the stateless configuration**

```bash
git add dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/shiro/ShiroConfiguration.java \
  dataRoomServer/src/test/java/com/gccloud/gcpaas/core/shiro/ShiroConfigurationCasTest.java
git commit -m "fix(auth): disable Shiro session state"
```

---

### Task 2: Add Concurrent Authentication Regression Coverage

**Files:**
- Modify: `dataRoomServer/src/test/java/com/gccloud/gcpaas/core/shiro/ShiroConfigurationCasTest.java`

**Interfaces:**
- Consumes: the stateless `DefaultWebSecurityManager` produced by Task 1 and existing `ShiroAuthToken(String token)`.
- Produces: regression coverage proving concurrent logins ignore a supplied `JSESSIONID`, create no Shiro Session, and emit no replacement Session Cookie.

- [ ] **Step 1: Add the concurrent regression test and accepting test realm**

Add these imports to `ShiroConfigurationCasTest`:

```java
import com.gccloud.gcpaas.dataroom.core.shiro.ShiroAuthToken;
import jakarta.servlet.http.Cookie;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.subject.WebSubject;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertNull;
```

Add the test method:

```java
@Test
void concurrentTokenLoginsIgnoreJsessionIdAndCreateNoSession() throws Exception {
    ShiroConfiguration configuration = new ShiroConfiguration();
    SessionManager sessionManager = configuration.sessionManager();
    DefaultWebSecurityManager securityManager = assertInstanceOf(
            DefaultWebSecurityManager.class,
            configuration.securityManager(acceptingRealm(), sessionManager)
    );
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    Callable<MockHttpServletResponse> login = () -> {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("JSESSIONID", "already-stopped-session"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        Subject subject = new WebSubject.Builder(securityManager, request, response).buildWebSubject();
        ready.countDown();
        start.await();

        subject.login(new ShiroAuthToken("valid-token"));

        assertNull(subject.getSession(false));
        return response;
    };

    try {
        List<Future<MockHttpServletResponse>> futures = List.of(
                executor.submit(login),
                executor.submit(login)
        );
        ready.await();
        start.countDown();

        for (Future<MockHttpServletResponse> future : futures) {
            assertNull(future.get().getCookie("JSESSIONID"));
        }
    } finally {
        executor.shutdownNow();
    }
}
```

Add the test realm helper:

```java
private static ShiroAuthRealm acceptingRealm() {
    return new ShiroAuthRealm() {
        @Override
        protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) {
            return new SimpleAuthenticationInfo(token.getPrincipal(), token.getCredentials(), getName());
        }
    };
}
```

- [ ] **Step 2: Run the Shiro configuration and filter tests**

Run:

```bash
mvn -q -pl dataRoomServer -Dtest=ShiroConfigurationCasTest,ShiroAuthFilterTest test
```

Expected: PASS, including two concurrent request-scoped logins with the same stale `JSESSIONID` value.

- [ ] **Step 3: Run the complete backend module test suite**

Run:

```bash
mvn -q -pl dataRoomServer test
```

Expected: PASS. If unrelated tests fail because of pre-existing dirty-worktree changes, record the exact failing classes and preserve those changes.

- [ ] **Step 4: Check formatting and scoped diff**

Run:

```bash
git diff --check
git diff -- dataRoomServer/src/main/java/com/gccloud/gcpaas/dataroom/core/shiro/ShiroConfiguration.java \
  dataRoomServer/src/test/java/com/gccloud/gcpaas/core/shiro/ShiroConfigurationCasTest.java
```

Expected: no whitespace errors; the diff contains only the approved Shiro stateless configuration and regression tests.

- [ ] **Step 5: Commit the regression coverage**

```bash
git add dataRoomServer/src/test/java/com/gccloud/gcpaas/core/shiro/ShiroConfigurationCasTest.java
git commit -m "test(auth): cover stateless concurrent login"
```
