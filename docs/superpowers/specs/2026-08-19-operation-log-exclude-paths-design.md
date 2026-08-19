# 操作日志 URL 排除配置设计

## 背景

当前操作日志通过 AOP 拦截带有 Swagger `@Operation` 注解的方法，并统一记录 HTTP 接口和 MCP 工具调用。部分 HTTP 接口不需要写入操作日志，需要提供集中、可按环境调整的 URL 排除配置，并支持 `/a/**` 形式的路径模式。

## 目标

- 在 `application.yml` 中配置不记录操作日志的 HTTP 路径。
- 支持 Spring 路径模式，例如 `/a/**`。
- 不要求修改 Controller，也不新增 `OperationLogIgnore` 等注解。
- 未配置排除路径时保持现有记录行为不变。
- MCP 工具调用继续按现有逻辑记录，不受 HTTP 路径配置影响。

## 配置契约

配置前缀为 `dataroom.operation-log`，排除路径字段为 `exclude-paths`：

```yaml
dataroom:
  operation-log:
    exclude-paths:
      - /dataRoom/captcha/**
      - /dataRoom/operationLog/**
      - /a/**
```

`exclude-paths` 默认为空列表。列表中的任意模式匹配当前 HTTP 请求路径时，该次调用不写入操作日志。

## 匹配语义

- 使用 Spring 6 的 `PathPatternParser`，不自行实现通配符解析。
- 匹配对象为 `HttpServletRequest.getRequestURI()` 去除 `contextPath` 后的路径。
- 查询参数不参与匹配。
- `/a/**` 匹配 `/a`、`/a/`、`/a/b` 和更深层路径。
- `/a/**` 不匹配 `/ab` 或 `/ab/c`。
- 配置项为空、空白或重复时不影响启动；空白项忽略，重复项按等价模式处理。
- 非法路径模式属于配置错误，应用启动时应明确失败，避免实际记录范围与配置预期不一致。

## 后端设计

新增操作日志配置属性类，使用 `@ConfigurationProperties` 绑定 `dataroom.operation-log.exclude-paths`。新增独立的 HTTP 路径排除判断组件，负责预编译配置模式并判断当前请求是否命中。

`OperationLogMethodInterceptor` 在取得当前请求后、创建日志实体前执行排除判断：

1. 当前调用存在 HTTP 请求且路径命中排除模式时，直接调用 `invocation.proceed()`。
2. 未命中时，继续现有的日志实体创建、结果记录和异步发布流程。
3. 当前调用没有 HTTP 请求时，不应用 URL 排除规则，因此 MCP 调用继续记录。

排除判断必须发生在业务方法执行前，并且命中后不进入日志发布的 `finally` 块。因此，被排除接口无论正常返回还是抛出异常，都不会产生操作日志。

## 错误处理

- 路径匹配过程不捕获并忽略配置异常。
- 非法模式在配置组件初始化阶段抛出明确异常，阻止应用以错误的审计范围启动。
- 业务接口自身的异常处理保持不变；排除规则只控制操作日志记录，不改变异常传播和响应行为。

## 测试设计

新增聚焦测试覆盖：

- 空排除配置时仍发布日志。
- `/a/**` 匹配 `/a`、`/a/b` 和 `/a/b/c`。
- `/a/**` 不匹配 `/ab/c`。
- context path 会在匹配前移除。
- query string 不参与匹配。
- 命中排除规则时不会调用日志持久化服务。
- 被排除接口抛出异常时，异常照常传播且不发布日志。
- 无 HTTP 请求的 MCP 调用不受排除配置影响。
- 非法路径模式导致配置初始化失败。

实施后至少运行相关操作日志单元测试。若新增或修改 Java `catch` 块，还需运行仓库约定的 `CatchBlockLoggingTest`。

## 非目标

- 不新增接口级或 Controller 级忽略注解。
- 不提供数据库动态配置或管理页面。
- 不支持按 HTTP 方法、用户、角色、响应状态或业务模块排除。
- 不改变当前通过 `@Operation` 选择日志记录方法的切点规则。
