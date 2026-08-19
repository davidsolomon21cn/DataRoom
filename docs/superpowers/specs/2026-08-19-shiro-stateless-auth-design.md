# Shiro 无状态 Token 认证设计

## 背景

DataRoom 使用 JWT、CAS 换取的 Token、第三方 SSO Token 和页面分享 Token 完成认证，不使用 `JSESSIONID`，业务代码也不依赖 Shiro Session。

Shiro 从 2.1.0 升级到 2.2.1 后，`DefaultSecurityManager.login()` 会在成功登录前轮换已有 Session。当前 `ShiroAuthFilter` 对每个受保护请求执行 `Subject.login()`。前端与后端同源部署时，浏览器会保存并并发携带 Shiro 生成的 `JSESSIONID`，多个请求可能同时停止同一个 Session，触发 `StoppedSessionException`。

## 目标

- Shiro 不读取、不创建、不持久化 Session。
- 服务端不生成 `JSESSIONID`。
- 每个受保护请求都重新校验 Token。
- Token 过期、退出登录撤销和分享 Token 停用立即生效。
- 现有 `@RequiresRoles` 授权保持有效。
- 静态资源、登录、验证码、CAS 回调等匿名路径保持原有访问规则。

## 非目标

- 不回退 Shiro 版本。
- 不覆盖或绕过 Shiro 2.2.1 的 Session 固定攻击防护。
- 不使用 Shiro Session 缓存登录状态。
- 不修改前端 Token Cookie 和请求头协议。

## 方案

### 禁止 Session ID 传输

继续使用 `DefaultWebSessionManager`，但关闭 Session ID Cookie 和 URL 重写：

```java
sessionManager.setSessionIdCookieEnabled(false);
sessionManager.setSessionIdUrlRewritingEnabled(false);
```

关闭 Cookie 后，Shiro 不再从请求中的历史 `JSESSIONID` 恢复 Session，也不会向响应写入新的 `JSESSIONID`。

### 禁止 Subject 状态持久化

为 `DefaultWebSecurityManager` 配置 `DefaultSubjectDAO`，并将其 `DefaultSessionStorageEvaluator` 关闭：

```java
DefaultSessionStorageEvaluator evaluator = new DefaultSessionStorageEvaluator();
evaluator.setSessionStorageEnabled(false);

DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
subjectDAO.setSessionStorageEvaluator(evaluator);
securityManager.setSubjectDAO(subjectDAO);
```

认证成功后的 principals 和 authenticated 状态只在当前请求的 Subject 中有效，不跨请求保存。

### 禁止请求期间创建 Session

在所有 Shiro 过滤链前增加内置 `noSessionCreation` 过滤器。匿名路径使用 `noSessionCreation,anon`，受保护路径使用 `noSessionCreation,OAUTH`。

这项约束用于阻止过滤器、授权逻辑或未来新增代码意外调用 `Subject.getSession()` 创建 Session。因为系统明确为完全无状态模式，意外创建 Session 应立即失败，而不是静默恢复有状态行为。

### 保持逐请求认证

`ShiroAuthFilter.isAccessAllowed()` 继续只放行 OPTIONS，请求 Token 存在时继续调用 `executeLogin()`。不能改为根据 `Subject.isAuthenticated()` 跳过认证，否则会重新引入跨请求 Session 状态，并可能使已撤销或过期的 Token 继续访问。

## 请求数据流

1. 浏览器从 DataRoom Token Cookie 读取 Token，并通过配置的 Token 请求头发送。
2. Shiro 创建仅当前请求有效的 Subject，不读取 `JSESSIONID`。
3. `ShiroAuthFilter` 从参数、请求头或 DataRoom Token Cookie 获取 Token。
4. `ShiroAuthRealm` 校验 Token，并构造当前请求的 `LoginUser` principals。
5. Controller 上的 `@RequiresRoles` 使用当前请求 Subject 完成授权。
6. 请求结束后 Subject 状态丢弃，不写入 Session。

## 错误处理与安全边界

- Token 缺失、无效、过期或已撤销时继续返回现有 401 业务响应。
- 请求携带任意旧 `JSESSIONID` 时必须忽略，不能影响认证结果。
- 退出登录仍通过 `TokenService.removeToken()` 撤销 Token；后续请求必须重新校验并失败。
- 不捕获或吞掉 Session 相关异常作为修复手段，根因应通过禁止 Session 消除。

## 测试设计

### 配置契约测试

- `DefaultWebSessionManager.isSessionIdCookieEnabled()` 为 `false`。
- `DefaultWebSessionManager.isSessionIdUrlRewritingEnabled()` 为 `false`。
- `DefaultSubjectDAO` 的 `DefaultSessionStorageEvaluator` 已关闭。
- 受保护路径过滤链包含 `noSessionCreation,OAUTH`。
- 匿名路径过滤链包含 `noSessionCreation,anon`。

### 认证回归测试

- 同一个有效 Token 并发请求两个受保护接口，均成功且不出现 `StoppedSessionException`。
- 请求携带旧或伪造 `JSESSIONID` 时，认证结果仅由 DataRoom Token 决定。
- 响应不包含 `Set-Cookie: JSESSIONID`。
- Token 撤销后再次访问返回 401。
- `@RequiresRoles` 对 manager、developer、sharer 继续生效。
- `/static/front/index.html`、登录、验证码和 CAS 回调保持匿名可访问。

## 验证命令

```bash
mvn -q -pl dataRoomServer -Dtest=ShiroConfigurationCasTest,ShiroAuthFilterTest test
mvn -q -pl dataRoomServer test
```

若新增或修改 Java `catch` 块，还必须运行：

```bash
mvn -q -pl dataRoomServer -Dtest=CatchBlockLoggingTest -DforkCount=0 test
```
