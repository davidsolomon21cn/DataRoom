# 组件数据处理脚本设计

## 背景

设计器右侧配置面板的「数据处理 / 处理脚本」当前只保存到 `chart.dataset.script`，没有参与组件数据刷新。该字段应在组件获取数据集结果后执行，用于对数据集返回数据做二次格式化处理，并保证组件最终收到的是脚本处理后的数据。

本设计覆盖普通数据刷新和实时数据订阅两条链路，避免只支持柱状图或只支持部分组件。

## 目标

- 组件数据集返回后、进入组件 `changeData` 前执行 `chart.dataset.script`。
- 普通数据刷新和实时数据订阅复用同一个 `transformChartData` 逻辑。
- 脚本使用 JavaScript 函数体写法。
- 脚本上下文访问方式和组件事件交互的高代码保持一致，通过 `bep` 对象访问。
- 脚本执行失败时返回空数组，并同时输出控制台错误和 Element Plus 错误提示。
- 未来新增标准组件时，只要继续使用现有数据接入约定，不需要单独处理脚本能力。

## 非目标

- 不新增后端脚本执行能力。
- 不改变数据集自身的 SQL、HTTP、JSON、Excel、ES、WebSocket、MQTT 执行逻辑。
- 不改造组件专属配置面板。
- 不引入额外脚本沙箱库。

## 方案选择

采用前端通用转换模块方案：

1. 新增 `dataRoomFront/src/dataRoom/hooks/use-dr-component/chart-data-transform.ts`。
2. 普通刷新链路 `useDrComponent.autoRefreshData()` 在 `datasetApi.run4Chart()` 后调用转换模块。
3. 实时订阅链路 `useRealtimeDataset` 在收到服务端推送数据、调用目标组件 `changeData` 前调用同一个转换模块。

该方案把脚本能力放在组件数据入口处，不需要各图表组件单独处理，也不需要改变后端接口。

## 脚本写法

用户在右侧配置面板中填写 JavaScript 函数体，必须显式 `return` 处理后的数据。

示例：

```js
const region = bep.globals.get('region')

return bep.data
  .filter(item => !region || item.region === region)
  .map(item => ({
    time: item.month,
    value: Number(item.amount)
  }))
```

如果脚本为空，转换模块直接返回原始数据。

如果脚本没有返回值，视为执行结果无效，返回空数组并在控制台输出警告。

## `bep` 上下文

转换模块执行脚本时只注入一个参数 `bep`，保持和组件事件交互高代码一致。

`bep` 结构：

```ts
{
  canvasInst,
  chart,
  data,
  params,
  globals: {
    get(name: string): unknown,
    set(name: string, value: string): void
  }
}
```

字段说明：

- `canvasInst`：当前画布实例，复用已有能力。
- `chart`：当前组件配置。
- `data`：数据集返回的数据。普通刷新场景为 `run4Chart` 归一化后的数据；实时场景为订阅消息推送给组件前的数据。
- `params`：本次数据请求或订阅使用的参数，来自 `canvasInst.fillDatasetParams(chart)`。
- `globals.get(name)`：读取全局变量，内部复用 `canvasInst.getGlobalVariableValue(name)`。
- `globals.set(name, value)`：更新静态全局变量，内部复用 `canvasInst.updateGlobalVariableValue(name, value)`。

不额外注入散落的 `data`、`params`、`globals` 顶层变量，避免和高代码动作形成两套写法。

## 普通刷新数据流

`useDrComponent.autoRefreshData()` 的目标数据流：

```text
fillDatasetParams(chart)
  -> datasetApi.run4Chart({ datasetCode, paramMap })
  -> transformChartData({ chart, canvasInst, data: res.data, params: paramMap })
  -> changeData(transformedData)
```

普通数据集包括 SQL、HTTP、JSON、Excel、ES 等通过 `/dataRoom/dataset/run` 拉取数据的场景。

## 实时订阅数据流

`useRealtimeDataset` 的目标数据流：

```text
fillDatasetParams(chart)
  -> 建立或更新实时订阅
  -> 收到服务端推送数据
  -> transformChartData({ chart, canvasInst, data: pushedData, params: paramMap })
  -> 目标组件 changeData(transformedData)
```

实时数据集包括 WebSocket、MQTT 等通过订阅推送数据的场景。后端流式数据集自身的 Groovy 处理仍保持不变；组件级处理脚本是在前端收到流式结果之后再执行的二次处理。

## 错误处理

脚本执行异常时：

1. 使用 `console.error` 输出组件类型、组件 ID 和异常对象。
2. 使用 `ElMessage.error('数据处理脚本执行失败')` 提示用户。
3. 返回 `[]`。
4. 继续调用组件 `changeData([])`，使组件进入空数据状态。

脚本没有 `return` 时：

1. 使用 `console.warn` 输出组件类型和组件 ID。
2. 返回 `[]`。

## 组件兼容性

所有使用 `useDrComponent` 的普通数据刷新组件自动支持该能力。

所有通过 `useRealtimeDataset` 接收实时数据的标准组件也应支持该能力。

未来新增组件不需要单独处理脚本，只需继续遵循现有组件约定：

```ts
useDrComponent({
  chart,
  changeData,
})
```

特殊组件如果完全绕过 `useDrComponent` 和 `useRealtimeDataset`，则不自动获得该能力，需要在其自定义数据入口显式调用 `transformChartData`。

## 测试与验证

前端增加聚焦回归测试或静态执行测试，覆盖：

- 空脚本透传原始数据。
- 正常脚本返回转换后的数据。
- 脚本通过 `bep.globals.get` 读取全局变量。
- 脚本通过 `bep.params` 读取请求参数。
- 脚本异常时返回 `[]`。
- 脚本未返回值时返回 `[]`。
- 普通刷新链路调用 `transformChartData` 后再调用 `changeData`。
- 实时订阅链路调用 `transformChartData` 后再调用目标组件 `changeData`。

验证命令：

```bash
cd dataRoomFront
npm run type-check
```

本功能不涉及样式改动，通常不需要运行 `npm run lint`。如果实现过程中修改了 Vue/SCSS/CSS 或 Element Plus 相关结构，则额外运行：

```bash
cd dataRoomFront
npm run lint
```
