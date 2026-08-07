# CAS 单点登录集成设计

## 1. 背景与目标

DataRoom 当前同时具备本地账号密码登录和通用第三方 token 鉴权能力，但尚未支持 CAS ticket 回调登录。本次新增 CAS 单点登录入口，通过 CAS REST 协议校验 ticket，不引入 `cas-client` 或其他 CAS SDK。

CAS 只负责证明用户身份。DataRoom 继续负责本地用户、角色、状态校验和 JWT 生命周期，避免引入第二套会话与权限模型。

## 2. 范围

本次包含：

- CAS 2.0 `/serviceValidate` 和 CAS 3.0 `/p3/serviceValidate` ticket 校验。
- CAS 登录回调、本地用户匹配、状态校验和 DataRoom JWT 签发。
- 登录页同时提供本地账号登录和 CAS 登录入口。
- CAS 开启时统一跳转 CAS 注销地址。
- 通用 `/error?code=...` 错误页及稳定错误码映射。
- 后端配置、前端 Vite 环境配置、相关测试和安全校验。

本次不包含：

- CAS 用户自动注册或用户资料同步。
- 根据 CAS 属性映射 DataRoom 角色。
- ticket-token 映射缓存。
- CAS Back-Channel Single Logout。
- 替换或移除现有 `dataroom.sso` 通用第三方 token 鉴权。

## 3. 方案选择

采用独立 CAS 登录模块，CAS 回调成功后签发 DataRoom 本地 JWT。

不将 CAS ticket 处理并入 `ShiroAuthRealm`。Realm 继续负责请求携带 token 后的身份认证，CAS 模块只负责浏览器登录回调。这两个流程使用不同协议和生命周期，保持独立可以降低认证逻辑耦合。

不采用前端接收 ticket 后再调用后端换取 JWT 的方式。服务端直接处理 CAS 回调可以减少跨域、路由启动和 ticket 暴露范围，并与参考实现保持一致。

## 4. 后端架构

新增独立 CAS 功能包，包含以下职责明确的组件：

### 4.1 `CasConfig`

作为 `DataRoomConfig` 的子配置，绑定 `dataroom.cas`：

```yaml
dataroom:
  cas:
    enable: false
    serverUrlPrefix: http://127.0.0.1:8888/cas
    service: http://127.0.0.1:8081/dataRoom/cas/login
    uiUrl: http://127.0.0.1:5173
    serviceValidateSuffix: /p3/serviceValidate
    connectTimeout: 3s
    readTimeout: 5s
```

字段语义：

- `enable`：后端 CAS 开关，默认关闭。
- `serverUrlPrefix`：CAS Server 根地址。
- `service`：CAS Server 回调 DataRoom 时使用的完整地址。
- `uiUrl`：DataRoom 前端访问地址，不包含 Hash 路由。
- `serviceValidateSuffix`：ticket 校验接口后缀，默认 `/p3/serviceValidate`，可改为 `/serviceValidate`。
- `connectTimeout`、`readTimeout`：CAS REST 请求连接和读取超时。

`service` 必须与前端 CAS 登录地址中传给 CAS Server 的 `service` 参数完全一致。

### 4.2 `CasController`

提供匿名访问的 `GET /cas/login`。结合应用上下文 `/dataRoom` 后，默认完整回调地址为 `/dataRoom/cas/login`。

控制器负责：

- 检查 CAS 是否启用。
- 检查 ticket 是否存在。
- 调用 `CasTicketValidator` 校验 ticket。
- 调用 `CasLoginService` 完成本地登录。
- 根据结果重定向前端首页或错误页。

控制器不承担 XML 解析、用户查询或 JWT 签发细节。

### 4.3 `CasTicketValidator`

通过现有 Spring/JDK HTTP 能力调用：

```text
{serverUrlPrefix}{serviceValidateSuffix}?service={encoded-service}&ticket={encoded-ticket}
```

验证器负责：

- 使用 URI 构造器编码查询参数。
- 应用连接和读取超时。
- 识别非 2xx 响应和 CAS `authenticationFailure`。
- 兼容 CAS 2.0/3.0 XML 命名空间并提取 `<cas:user>`。
- 将协议结果转换为内部成功结果或受控异常，不向控制器泄露 XML 解析细节。

XML 解析必须禁用 DTD、外部通用实体、外部参数实体和外部 schema，防止 XXE。

### 4.4 `CasLoginService`

使用 CAS 返回的用户名精确查询 `UserEntity.account`，不自动创建用户。匹配成功后继续检查：

- 用户状态必须为 `UserStatus.NORMAL`。
- 用户不能处于锁定状态。
- 用户不能已过期。

校验通过后调用现有 `TokenService.createToken(account)` 签发 DataRoom JWT。用户角色继续来自本地 `UserEntity.role`，CAS 返回属性不参与权限映射。

### 4.5 Shiro 放行

在 `ShiroConfiguration` 中仅放行 CAS 回调路径 `/cas/login`。其他接口继续使用现有 Shiro 过滤链，不扩大匿名访问范围。

现有 `dataroom.sso`、`ISsoAdapterService` 和外部 bearer token 认证行为保持不变。

## 5. 前端架构

### 5.1 构建时配置

CAS 前端配置使用 Vite 环境变量，不新增运行时 `starter.js`：

```env
VITE_CAS_ENABLE=false
VITE_CAS_LOGIN_URL=
VITE_CAS_LOGOUT_URL=
```

`.env.development` 和 `.env.production` 均声明以上配置，`env.d.ts` 提供类型定义。

- `VITE_CAS_ENABLE` 仅在值为字符串 `true` 时视为开启。
- `VITE_CAS_LOGIN_URL` 是完整 CAS 登录地址，并包含 URL 编码后的 DataRoom `service`。
- `VITE_CAS_LOGOUT_URL` 是完整 CAS 注销地址。

修改这些配置后需要重新构建前端，这是选择 Vite 构建时配置的明确约束。

### 5.2 登录页

保留现有本地账号、密码和验证码登录表单。

当 `VITE_CAS_ENABLE=true` 且 `VITE_CAS_LOGIN_URL` 非空时，显示“CAS 登录”按钮。点击后使用浏览器整页跳转到 CAS 登录地址。CAS 配置不完整时不发起空地址跳转，并给出受控提示。

前端样式继续遵循 `docs/design/DESIGN.md`：使用 Element Plus 默认按钮和表单组件，不覆盖 `.el-*` 内部样式，不新增硬编码颜色或私有颜色变量。

### 5.3 JWT 回调落地

CAS 登录成功后，后端重定向到：

```text
{uiUrl}/#/?dataRoomToken={url-encoded-token}
```

现有路由守卫负责读取 `dataRoomToken` 并写入 Cookie。写入成功后必须使用 `router.replace` 删除当前地址中的 token 查询参数，同时保留其他无关查询参数，避免 JWT 长期停留在地址栏和浏览历史中。

### 5.4 错误页

新增 `dataRoomFront/src/dataRoom/error.vue`，并注册路由：

```text
/error?code={error-code}
```

后端实际重定向地址为：

```text
{uiUrl}/#/error?code={url-encoded-error-code}
```

错误页只根据前端白名单映射展示标题和说明，不直接回显查询参数文本。页面固定提供两个操作：

- “返回登录”：跳转 `/login`。
- “返回首页”：跳转 `/`。

错误页不展示 ticket、JWT、CAS 原始响应或服务端异常详情。

### 5.5 退出

退出不区分用户是通过本地账号还是 CAS 登录。

退出流程为：

1. 尝试调用 `/dataRoom/user/logout` 使当前 DataRoom JWT 失效。
2. 删除前端 token Cookie。
3. `VITE_CAS_ENABLE=true` 时，无论本地注销请求成功或失败，最终整页跳转 `VITE_CAS_LOGOUT_URL`。
4. CAS 未开启时跳转本地 `/login`。

本地注销失败时仍继续清理浏览器 Cookie 和执行最终导航，避免用户被阻塞在当前页面。

## 6. 完整登录数据流

1. 用户访问 DataRoom 登录页。
2. 用户可以继续使用本地账号密码登录，或点击“CAS 登录”。
3. CAS 登录按钮跳转 `VITE_CAS_LOGIN_URL`。
4. CAS Server 完成认证后携带 ticket 回调 `/dataRoom/cas/login`。
5. 后端使用固定配置的 `service` 和 ticket 调用 CAS REST 校验接口。
6. 后端安全解析 XML 并取得 CAS 用户名。
7. 后端按本地 `account` 精确查找用户并校验状态和有效期。
8. 后端签发 DataRoom JWT，重定向前端首页并携带 token。
9. 前端将 token 写入 Cookie 并立即清除 URL 中的 token 参数。
10. 后续请求继续使用现有 `dataRoomToken` 请求头和 Shiro/JWT 鉴权链路。

## 7. 错误码

CAS 回调失败统一重定向 `/error?code=...`，使用以下稳定错误码：

| 错误码 | 含义 |
| --- | --- |
| `disabled` | 后端未开启 CAS |
| `ticketMissing` | 回调缺少 ticket |
| `ticketInvalid` | CAS 拒绝 ticket 或响应中没有用户 |
| `serviceUnavailable` | CAS 请求超时、网络失败或返回异常状态 |
| `userNotFound` | DataRoom 不存在对应账号 |
| `userUnavailable` | 本地用户被禁用、锁定或已过期 |
| `loginError` | 其他不可公开的内部错误 |

前端对未知错误码统一显示通用“单点登录失败”信息。错误响应不会触发自动 CAS 跳转，因此不会形成登录重定向循环。

## 8. 安全与日志

- ticket、JWT 和完整 CAS XML 不写入应用日志。
- 日志可记录错误类别、CAS 服务地址和请求耗时，但不记录敏感查询参数。
- `uiUrl`、`service`、CAS Server 地址和校验后缀均来自服务端配置，回调请求不能覆盖这些值。
- 重定向目标仅由服务端 `uiUrl` 和固定路由组成，防止开放重定向。
- CAS REST 查询参数统一编码，不使用原始字符串拼接。
- CAS XML 使用安全解析配置，禁止任何外部资源加载。
- Java `catch` 块先通过 `ExceptionUtils.getStackTrace(e)` 记录完整异常栈，再执行错误转换或重定向。
- CAS 登录成功和失败应纳入现有操作日志或等价审计日志，日志中只记录本地账号、结果、错误类别和耗时。

## 9. 测试设计

### 9.1 后端

- 配置默认关闭及配置绑定测试。
- CAS 2.0 和 3.0 成功 XML、带命名空间响应解析测试。
- `authenticationFailure`、缺少用户、非法 XML、非 2xx 和超时测试。
- DTD、外部实体和外部 schema 输入拒绝测试。
- `service` 和 ticket 查询参数编码测试。
- 用户不存在、禁用、锁定和过期测试。
- 正常用户签发 JWT 和成功重定向测试。
- 错误码与错误页重定向测试。
- `/cas/login` 匿名访问及现有 Shiro 路径不回归测试。
- 日志不包含 ticket、JWT 和完整 XML 的安全测试或代码级断言。
- 修改 `catch` 块后运行 `CatchBlockLoggingTest`。

### 9.2 前端

- CAS 关闭时不显示 CAS 登录按钮。
- CAS 开启且地址有效时显示按钮并正确跳转。
- CAS 配置不完整时不执行无效跳转。
- JWT 写入 Cookie 后从 URL 中移除，同时保留其他查询参数。
- `/error?code=...` 已知和未知错误码展示测试。
- “返回登录”和“返回首页”按钮导航测试。
- CAS 开启和关闭时的退出导航测试。
- 本地注销请求失败时仍删除 Cookie 并完成最终导航。

### 9.3 验证命令

实现阶段至少运行：

```bash
mvn -pl dataRoomServer -Dtest=CasControllerTest,CasTicketValidatorTest,CasLoginServiceTest test
mvn -q -pl dataRoomServer -Dtest=CatchBlockLoggingTest -DforkCount=0 test

cd dataRoomFront
npm run type-check
npm run lint
```

## 10. 验收标准

- CAS 关闭时，现有本地登录、退出和第三方 token 鉴权行为不变。
- CAS 开启时，用户可以在登录页主动选择本地登录或 CAS 登录。
- CAS 用户只有在 DataRoom 存在同名正常账号且未过期时才能登录。
- CAS 登录成功后使用 DataRoom 本地 JWT 访问现有接口，无需修改业务 API。
- CAS 登录失败进入通用错误页，并显示与稳定错误码对应的信息。
- 退出时只依据 CAS 全局开关决定是否跳转 CAS 注销地址。
- 项目不新增 `cas-client` 或其他 CAS SDK 依赖。
- ticket、JWT、完整认证响应和服务端异常详情不会暴露到前端错误页或日志。
