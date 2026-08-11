# 大屏设计器 Iframe 交互开关设计

## 背景

`DrIframe` 内容运行在独立的 iframe 文档中。用户在 iframe 内点击、滚动或按下指针时，事件不会冒泡到父页面的组件包装层，导致像素级大屏设计器无法通过画布点击选中 iframe 组件。

浏览器不允许父页面改变 iframe 内部事件的冒泡边界，因此本功能不实现真正的“iframe 事件冒泡”，而是在设计态切换指针事件由设计器或 iframe 接管。

## 用户交互

在像素级大屏设计器顶部的“工具”下拉菜单中新增“Iframe交互”开关，使用 Element Plus 默认的小尺寸 `el-switch`。

- 关闭：设计器在 `DrIframe` 上方渲染透明交互层。点击、拖动、框选和右键事件由设计器处理。
- 开启：移除透明交互层。用户可以滚动、点击和操作 iframe 内部网页。
- 开启后，直接点击 iframe 不会选中组件；用户可通过图层面板选中组件，或关闭“Iframe交互”。
- 开关对当前大屏内所有 `DrIframe` 组件生效，不作用于 `DrHtml` 或其他组件。

## 配置与兼容性

在 `VisualScreenPageBasicConfig` 中增加可选布尔字段 `iframeInteractionEnabled`。

- 新建大屏默认为 `false`。
- 历史页面缺少字段时归一化为 `false`。
- 切换开关直接更新 `basicConfig.iframeInteractionEnabled`。
- 字段随现有页面配置保存、历史备份和配置哈希链路持久化，不新增单独接口或本地存储。
- 加载页面配置时显式合并该字段，避免当前 `basicConfig` 按字段重建时丢失它。

该字段是设计器编辑偏好，虽然持久化在页面基础配置中，但不改变预览和发布态的渲染行为。

## 渲染架构

`VisualScreenDesigner` 从 `basicConfig` 读取开关状态，并将状态传给 `VisualScreenChartTree`。`VisualScreenChartTree` 在递归渲染组合组件子树时继续传递该状态，确保组合内的 iframe 也遵循全局开关。

当前渲染节点满足以下条件时，在组件内容之后渲染透明交互层：

1. 渲染模式为 `designer`。
2. 组件类型为 `DrIframe`。
3. `iframeInteractionEnabled` 为 `false`。

透明交互层使用绝对定位覆盖组件内容，不设置颜色、边框或文字，不覆盖 Element Plus 内部样式。其指针事件沿父页面 DOM 正常冒泡到 `.chart-wrapper`，复用现有选中、Selecto、Moveable 和右键菜单链路。

`preview` 模式不渲染交互层，因此预览和发布态的 iframe 始终允许内部交互。

## 错误和边界处理

- iframe 加载失败、跨域限制或目标页面的 `X-Frame-Options` / CSP 限制不由本开关处理。
- 本开关不监听 iframe 内部 DOM 事件，不引入 `postMessage` 协议。
- 本开关不修改 `DrIframe` 的 sandbox 权限。
- 本次不在网格化 `PageDesigner` 顶部新增工具菜单。

## 测试与验证

实施时按 TDD 增加最小回归测试：

- 历史配置缺少开关字段时返回 `false`。
- 配置字段为 `true` 时保留开启状态。
- `designer + DrIframe + 开关关闭` 时需要交互层。
- 开关开启、非 `DrIframe` 组件或 `preview` 模式不需要交互层。
- 工具菜单开关与 `basicConfig.iframeInteractionEnabled` 双向绑定。

完成后运行：

```bash
cd dataRoomFront
npm run type-check
npm run lint
```

并在大屏设计器中手工验证开关关闭、开启、保存后重新进入，以及预览态 iframe 交互。
