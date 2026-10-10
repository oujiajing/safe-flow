# 随手拍 AI 研判页面设计 QA

## Source visual truth

- Source visual: `C:/Users/ojj/AppData/Local/Temp/codex-clipboard-a63295a4-ad0c-4df4-a8fc-84521da74633.png`
- Intended state: 随手拍详情中的 AI 研判结果已生成，展示现场发现、风险评估、整改建议、验收要点、法规依据和人工复核表单。

## Implementation evidence

- Implementation: `D:/1-project/safe-flow/vben/apps/web-antd/src/views/pingan/pre-shift-meeting/index.vue`
- Running implementation: safe-flow frontend on `http://127.0.0.1:15666`
- Implementation screenshot: unavailable; the in-app browser was blocked by the Windows sandbox with `helper_unknown_error: apply deny-read ACLs`.
- Viewport and pixel density comparison: unavailable because the implementation screenshot could not be captured.

## Implemented visual structure

- Added the screenshot-style AI header with icon, subtitle, and re-run action.
- Added six summary cards for hazard type, confidence, assessment status, image count, risk level, and human review status.
- Reworked the candidate area into a light-blue workspace with a two-column discovery and human review arrangement.
- Added radio-style review decisions and editable hazard description, risk level, and rectification measures.
- Added card sections for risk assessment, rectification suggestions, acceptance criteria, and legal evidence.
- Added responsive collapse for narrow screens.
- Removed the standalone 隐患图片 card and moved image preview/upload into the 现场发现 card.
- Re-aligned the left card sequence and bottom legal evidence block to the latest reference screenshot.

## Findings

- [P1] Visual comparison is blocked.
  Location: rendered AI review page.
  Evidence: the reference image is available, but the browser automation runtime could not start, so no implementation screenshot was available for side-by-side comparison.
  Impact: exact typography, spacing, image crop, viewport proportions, and responsive behavior cannot be verified from the rendered page.
  Fix: reopen the page in a working in-app browser session and capture the same state at the reference viewport, then compare and iterate.

## Comparison history

- Initial implementation: not visually capturable because browser startup was blocked.
- Current implementation: typecheck and production build passed; visual comparison remains blocked for the same browser runtime issue.

## Required fidelity surfaces

- Fonts and typography: mapped to the existing application typography; rendered comparison pending.
- Spacing and layout rhythm: implemented from the reference structure; rendered comparison pending.
- Colors and visual tokens: implemented with the reference light-blue canvas, white cards, blue action color, and semantic orange/green/purple accents; rendered comparison pending.
- Image quality and asset fidelity: uses the existing uploaded attachment and the project icon library; rendered crop comparison pending.
- Copy and content: aligned to the screenshot labels and the existing AI workflow terminology.

## Final result

blocked
