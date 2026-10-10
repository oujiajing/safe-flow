# 隐患整改界面 Design QA

## 对比目标

- source visual truth path: `C:/Users/ojj/Desktop/隐患整改UI/`
- implementation screenshot path: `C:/Users/ojj/.codex/visualizations/2026/09/29/01a0ed71-5426-7723-b96d-25d92dc1177e/safe-flow-rectification-ui-folder-reference-final/`
- viewport: `1536 × 1024` CSS px
- density: `deviceScaleFactor = 1`
- state: 已关闭详情、处理记录、基础信息、待派发下发整改、待整改提交整改结果、待验收审核验收
- dimensions: 设计图为 `1536 × 1024/1056` px；实现截图为 `1536 × 1024` px。并排比较时均按各自完整画布等比缩放，没有拉伸或裁剪。

## Full-view comparison evidence

- `comparisons/detail-comparison.png`
- `comparisons/issue-comparison.png`
- `comparisons/result-comparison.png`
- `comparisons/accept-comparison.png`

详情页已对齐“顶部状态总览 + 五个页签 + 双栏证据 + 整改措施 + AI 条 + 验收摘要 + 紧凑处理记录”的层级。三个操作状态均采用居中宽卡片、浅色表单和固定底部动作区，不再使用贴右侧长时间轴。

## Focused region comparison evidence

- 下发整改：标题、隐患摘要、责任与时限、整改要求、验收标准及底部动作区均可在 `issue-comparison.png` 中逐区核对。
- 提交整改结果：整改前后证据、结果说明、AI 辅助检查及提交动作均可在 `result-comparison.png` 中逐区核对。
- 审核验收：前后证据、验收核对、通过/退回决策、验收人和意见均可在 `accept-comparison.png` 中逐区核对。
- 其余页签已单独捕获为 `detail-history.png`、`detail-basic.png`，不需要额外裁切；全尺寸截图中的文字与控件已足够辨认。

## Required fidelity surfaces

- Fonts and typography: 沿用项目中文字体栈，标题、标签、正文和辅助文案的字号/字重层级与设计图一致；浅色弹层在深色宿主主题中强制使用深色正文，避免主题变量造成白字。
- Spacing and layout rhythm: 详情宽度为 `min(1320px, 100vw - 48px)`，操作卡为 `min(1180px, 100vw - 48px)`；卡片间距、边框、圆角和底部动作区按基准图收敛。
- Colors and visual tokens: 主色使用项目蓝，完成/验收使用语义绿，警示使用橙/红；弹层表面固定为白色、边框为浅灰蓝。
- Image quality and asset fidelity: 生产页面继续使用真实业务附件 URL 和项目 Iconify/Lucide 图标，没有把设计图或 QA mock 写入业务代码。QA mock 只用于验证比例、裁切和布局。
- Copy and content: 五个页签与三个操作名称和设计基准一致；“验收标准”已连接现有表单并进入真实动作载荷。
- Responsiveness and accessibility: 900px/640px 断点保留；页签和验收结论使用原生按钮，图片含替代文本，焦点样式保留。

## Comparison history

1. P1：详情由抽屉改为居中 Modal 后，操作 Modal 与详情同层，详情遮挡操作卡。
   - Fix: 操作卡打开时暂时关闭详情，取消或提交后恢复详情；同时保留独立 wrap 层级。
   - Evidence: 最终 `action-issue.png`、`action-result.png`、`action-accept.png` 均为单层可操作卡片。
2. P2：深色宿主主题使浅色操作卡标题和字段标签对比度不足。
   - Fix: 为操作卡标题、字段标签、说明和证据标题设置明确的浅色界面文字 token。
   - Evidence: 最终三个 action 截图中标题和标签均为深色高对比文本。
3. P2：操作卡横向比例偏窄，下发整改缺少基准图中的验收标准。
   - Fix: 操作卡宽度提升至 1180px，并将验收标准接入表单、校验和提交载荷。
   - Evidence: 最终 `comparisons/issue-comparison.png`。

## Findings

没有剩余可执行的 P0/P1/P2 差异。设计图中的“通知方式、补充附件、保存草稿”当前没有对应后端字段或接口，未制作不可用的假控件；这是有意的产品能力边界，不是视觉遗漏。

## Interaction and engineering checks

- 登录、列表加载、打开详情、切换处理记录/基础信息、打开与关闭三种操作卡均通过。
- Chrome 控制台错误：0。
- 页面测试：25/25 通过。
- Vue/TypeScript 类型检查：通过。
- Vite production build：通过；仅有独立工作树未提供 `VITE_APP_TITLE` 的既有环境提示。

## Follow-up polish

- P3：待后端提供草稿、通知渠道和通用附件能力后，再补齐参考图中的次要控件。
- P3：接入真实业务图片后再按各来源模块检查实际裁切焦点。

## 整改结果页逐页精修（2026-10-09）

- source visual truth：`C:/Users/ojj/Desktop/隐患整改UI/exec-74d14ac5-7bff-4a09-83f9-9f118f25b8b7.png`
- implementation screenshot：`C:/Users/ojj/.codex/visualizations/2026/09/29/01a0ed71-5426-7723-b96d-25d92dc1177e/safe-flow-rectification-result-page-final/result-page.png`
- side-by-side evidence：`C:/Users/ojj/.codex/visualizations/2026/09/29/01a0ed71-5426-7723-b96d-25d92dc1177e/safe-flow-rectification-result-page-final/result-comparison.png`
- viewport：`1536 × 1056` CSS px，`deviceScaleFactor = 1`
- 结果：状态摘要、前后证据、结构化整改说明、AI 复核意见和提交记录均在首屏完整呈现；页面横向溢出为 0，弹窗正文滚动差值为 0，控制台错误为 0。

## 验收结论页逐页精修（2026-10-09）

- source visual truth：`C:/Users/ojj/Desktop/隐患整改UI/exec-1aab7162-d606-4594-9c0d-28107772a319.png`
- implementation screenshot：`C:/Users/ojj/.codex/visualizations/2026/09/29/01a0ed71-5426-7723-b96d-25d92dc1177e/safe-flow-rectification-acceptance-page-final/acceptance-page.png`
- side-by-side evidence：`C:/Users/ojj/.codex/visualizations/2026/09/29/01a0ed71-5426-7723-b96d-25d92dc1177e/safe-flow-rectification-acceptance-page-final/acceptance-comparison.png`
- viewport：`1536 × 1056` CSS px，`deviceScaleFactor = 1`
- 结果：验收摘要、三项验收核对、验收意见与信息、证据引用、AI 辅助复核和闭环状态均在首屏完整呈现；页面横向溢出为 0，弹窗正文滚动差值为 0，控制台错误为 0。

## 处理记录页逐页精修（2026-10-09）

- source visual truth：`C:/Users/ojj/Desktop/隐患整改UI/exec-1861eb35-e057-4e06-bf7b-0631e3029360.png`
- implementation screenshot：`C:/Users/ojj/.codex/visualizations/2026/09/29/01a0ed71-5426-7723-b96d-25d92dc1177e/safe-flow-rectification-history-page-final/history-page.png`
- side-by-side evidence：`C:/Users/ojj/.codex/visualizations/2026/09/29/01a0ed71-5426-7723-b96d-25d92dc1177e/safe-flow-rectification-history-page-final/history-comparison.png`
- viewport：`1536 × 1056` CSS px，`deviceScaleFactor = 1`
- 结果：处理记录标题、类型/操作人/关键字筛选、五节点流程表、最终闭环节点、系统操作日志折叠区与导出入口均与参考稿层级一致；筛选与 CSV 导出可交互，页面横向溢出为 0，弹窗正文滚动差值为 0，控制台错误为 0。

## 基础信息页逐页精修（2026-10-09）

- source visual truth：`C:/Users/ojj/Desktop/隐患整改UI/exec-6dd56520-8727-4ff8-855e-a649a31bf3fb.png`
- implementation screenshot：`C:/Users/ojj/.codex/visualizations/2026/09/29/01a0ed71-5426-7723-b96d-25d92dc1177e/safe-flow-rectification-basic-page-final/basic-page.png`
- side-by-side evidence：`C:/Users/ojj/.codex/visualizations/2026/09/29/01a0ed71-5426-7723-b96d-25d92dc1177e/safe-flow-rectification-basic-page-final/basic-comparison.png`
- viewport：`1536 × 1056` CSS px，`deviceScaleFactor = 1`
- 结果：工单信息、业务归属、责任与验收、时效信息及扩展信息的分组、顺序和密度均与参考稿一致；复制工单编号和扩展信息展开/收起可交互，页面横向溢出为 0，弹窗正文滚动差值为 0，控制台错误为 0。

## 最终联调与代码审查（2026-10-09）

- 五个详情页签与三类操作卡已完成整链路静态回归，模块测试 `25/25` 通过。
- `vue-tsc` 类型检查和 Vite production build 通过；构建仅保留独立工作树未配置 `VITE_APP_TITLE` 的既有提示。
- 定向 `ESLint`、`Stylelint` 与 `Oxlint` 均为 `0 warning / 0 error`。
- 补齐处理记录筛选、证据视频和图片上传控件的可访问名称；证据图片声明固有尺寸，非首屏图片启用懒加载；详情和操作弹窗增加滚动边界控制。
- CSV 导出改为先挂载下载节点、触发后移除，并延后释放对象 URL，避免浏览器下载时序差异。
- 仓库全量单测共执行 84 个测试文件：68 个文件、565 项测试通过；16 个文件因隔离工作树未安装/链接 `pinia`、`vue-router`、`axios`、`@vben-core/shared/utils` 等既有工作区依赖而在收集阶段失败，与本次模块代码无关。模块定向测试、类型检查和生产构建均不受影响。

final result: passed
