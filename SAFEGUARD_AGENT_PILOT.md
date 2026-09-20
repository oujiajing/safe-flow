# SafeGuard Agent PC Web pilot

首个试点位于 `隐患排查 → 随手拍 → 已上报记录明细`。前端在现有详情弹窗展示只读“AI 辅助研判”卡片，safe-flow 后端按当前登录用户和记录服务端构造上下文，读取记录图片后调用 safeguard-agent 的视觉分析接口。

完整评估升级方案见 [`SAFEGUARD_AGENT_PILOT_MODIFICATION_PLAN.md`](./SAFEGUARD_AGENT_PILOT_MODIFICATION_PLAN.md)。

试点不会修改随手拍记录、触发审核或创建整改工单。真实写入仍由 safe-flow 原有确认、权限和审计流程控制，独立 Agent 写入保持关闭。

## 本地启用

```text
PINGAN_SAFEGUARD_AGENT_ENABLED=true
SAFEGUARD_AGENT_BASE_URL=http://127.0.0.1:9090/api/safeguard-agent
SAFEGUARD_SERVICE_TOKEN=<与 safeguard-agent SAFEGUARD_SERVICE_TOKEN 相同>
```

未配置开关或服务令牌时，能力保持不可用；safe-flow 原有随手拍流程不受影响。

验证命令：

```text
.\mvnw.cmd -q -f backend/pom.xml -DskipTests package
pnpm --filter @vben/web-antd typecheck
pnpm --filter @vben/web-antd build
```
