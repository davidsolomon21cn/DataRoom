# 组件库图表缩略图与四列布局设计

## 目标

使用 `/Users/liuchengbiao/Downloads/图表图标` 中的 9 张图表素材替换设计器组件库中对应组件的示例缩略图，并将设计器顶部组件库弹框调整为每行固定展示 4 个组件。弹框卡片布局参考现有素材库，保留搜索、多选和批量插入能力。

## 图标映射

| 素材文件 | 前端组件 | 目标文件 |
| --- | --- | --- |
| `柱状图.png` | `DrBarChart` | `components/DrBarChart/images/bar-chart.png` |
| `折线图.png` | `DrLineChart` | `components/DrLineChart/images/line-chart.png` |
| `区域图.png` | `DrAreaChart` | `components/DrAreaChart/images/area-chart.png` |
| `饼图.png` | `DrPieChart` | `components/DrPieChart/images/pie-chart.png` |
| `散点图.png` | `DrBubbleChart` | `components/DrBubbleChart/images/bubble-chart.png` |
| `条形图.png` | `DrHorizontalBarChart` | `components/DrHorizontalBarChart/images/horizontal-bar-chart.png` |
| `雷达图.png` | `DrRadarChart` | `components/DrRadarChart/images/radar-chart.png` |
| `仪表盘.png` | `DrGauge` | `components/DrGauge/images/gauge.png` |
| `词云图.png` | `DrWordCloud` | `components/DrWordCloud/images/word-cloud.png` |

目标文件均位于 `dataRoomFront/src/dataRoom/` 下。直接覆盖现有文件，保持各组件 `plugin.ts` 的导入路径不变。

## 弹框布局

- 保留 `ComponentLib.vue` 现有 `80%` 弹框宽度、600px 内容高度、搜索、多选、已选数量和批量插入逻辑。
- 组件网格使用固定 4 列，每个卡片宽度由弹框可用空间平均分配。
- 卡片参考素材库的结构：上部为固定比例缩略图区，下部为名称栏；使用相同层级的边框、圆角、背景、悬停和选中状态。
- 缩略图使用 `contain`，确保不同尺寸的横向素材完整显示，不裁切图表坐标轴、标签或词云内容。
- 选中标记继续显示在卡片右上角，批量选择顺序和插入行为不变。
- 搜索框靠左放置，宽度与素材库搜索区保持相近的紧凑尺度。

## 样式约束

- 仅使用 Element Plus CSS 变量表达颜色、边框和状态。
- 不新增 `--dr-*` 颜色变量，不硬编码颜色。
- 不覆盖 Element Plus 内部样式，不使用 `:deep(.el-*)`、全局 `.el-*`、`!important` 或负字距。
- 业务样式只负责网格、尺寸、间距、滚动区和自定义卡片容器。

## 兼容性与错误处理

- 图标仍通过 Vite 静态资源导入，无新增运行时请求或失败分支。
- 未提供新素材的其他组件继续使用原缩略图。
- 搜索无结果、未选择组件、取消弹框等现有状态不变。

## 验证

1. 运行 `npm run type-check`，确认静态资源导入和 Vue 模板类型正确。
2. 运行 `npm run lint`，确认 Vue、TypeScript 和 SCSS 符合项目规范。
3. 检查组件库弹框在设计器中每行显示 4 个卡片，滚动、搜索、多选和插入功能正常。
4. 检查 9 个图表组件显示新缩略图，其他组件缩略图不变。
5. 扫描修改样式，确认不存在硬编码颜色、`--dr-*`、Element Plus 内部覆盖、`!important` 和负字距。
