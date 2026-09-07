# 平安班组项目说明

本文档记录对当前仓库的项目画像和开发注意事项，供后续代理或开发者快速接手。内容基于 2026-05-26 对 `D:\1-project\Pingan_Banzu` 的通读结果。

## 项目定位

这是一个“平安班组”安全生产管理系统，仓库同时包含后端、前端、接口文档、部署文档和业务模板生成工具。

核心业务覆盖：

- 系统管理：公司、部门、班组、人员、账户、角色权限、菜单、导入导出、数据权限。
- 一班三查：班组派班、班前会、班前安全活动、班前检查、班中检查、班后检查、重点场所。
- 风险与隐患：风险分级管控、风险四色图、隐患排查、隐患整改、风险隐患库。
- 其他安全业务：特殊作业、宣教培训、考试任务、考试成绩、安全学习、安全积分。
- 多端同步：PC 与小程序的一班三查数据应共享同一后端事实源。

## 顶层目录

| 路径 | 说明 |
| --- | --- |
| `backend/` | Spring Boot 后端服务。 |
| `vben/` | Vue Vben Admin 前端 monorepo，主业务应用是 `apps/web-antd`。 |
| `docs/` | 启动、部署、接口、功能清单、开发计划和历史记录。 |
| `tools/three-check-generator/` | 一班三查模块生成器，用班前会模板扩展其他三查模块。 |
| `docker-compose.storage.yml` | 本地 PostgreSQL + MinIO 存储服务。 |
| `mvnw` / `mvnw.cmd` | Maven Wrapper，后端命令优先使用它。 |
| `docs/archive/development-records/` | 已归档的阶段性开发计划、发现与实施进度。 |

## 后端概览

后端位于 `backend/`，技术栈如下：

- Java 17。
- Spring Boot 3.3.6。
- MyBatis-Plus 3.5.9。
- PostgreSQL 运行时数据库。
- Flyway 管理数据库迁移。
- MinIO 或本地目录存储附件。
- JJWT 处理 JWT。
- Apache POI 处理 Excel 导入导出。
- H2 主要用于测试 profile。

关键结构：

| 路径 | 说明 |
| --- | --- |
| `backend/src/main/java/com/pingan/banzu/BanzuApplication.java` | 后端入口，扫描普通 mapper 和 `system` mapper。 |
| `backend/src/main/java/com/pingan/banzu/controller/` | 平安业务控制器。 |
| `backend/src/main/java/com/pingan/banzu/service/` | 平安业务服务层。 |
| `backend/src/main/java/com/pingan/banzu/domain/` | 业务实体。 |
| `backend/src/main/java/com/pingan/banzu/dto/` | 业务 DTO。 |
| `backend/src/main/java/com/pingan/banzu/system/` | 系统管理独立分层。 |
| `backend/src/main/java/com/pingan/banzu/security/` | JWT、当前用户上下文、认证拦截器。 |
| `backend/src/main/java/com/pingan/banzu/config/` | Web、存储、JWT 等配置。 |
| `backend/src/main/resources/db/migration/` | Flyway 通用迁移；数据库专用迁移位于 `db/vendor/{vendor}`，当前已到 `V71`。 |
| `backend/src/test/java/com/pingan/banzu/` | 后端接口、权限、schema、存储和业务测试。 |

接口约定：

- JSON 返回统一使用 `ApiResponse<T>`：成功 `code=0`、`message=ok`，失败 `code=-1`。
- 分页返回使用 `PageResult<T>`：`items` 和 `total`。
- PC 端通过 `/api/auth/login` 获取 JWT，后续请求携带 `Authorization: Bearer <accessToken>`。
- 鉴权入口是 `WebConfig` 注册的 `AuthInterceptor`，不是 Spring Security FilterChain。
- `/api/system/**` 额外要求当前用户为管理员。
- `WebConfig` 还配置了 CORS，以及本地存储模式下的 `/uploads/**` 静态资源映射。

配置注意：

- `application.yml` 默认端口是 `8080`，默认数据库为 PostgreSQL，默认存储 provider 为 MinIO。
- `application-local.yml` 当前默认连接 `jdbc:postgresql://localhost:15433/pingan_banzu_verify`，存储默认 `local`，目录为 `uploads-local`。
- `application-test.yml` 使用 H2 内存库，兼容 PostgreSQL 模式。
- 启动说明以 `docs/project/getting-started/启动.md` 为准；开发验收统一使用手工验证库 `pingan_banzu_verify`，真实库 `pingan_banzu` 保留不动。

## 前端概览

前端位于 `vben/`，是 Vue Vben Admin 5.7.0 monorepo。

主要技术栈：

- Vue 3。
- TypeScript。
- Vite。
- pnpm workspace。
- Turbo。
- Ant Design Vue。
- Pinia。
- Vue Router。
- Vben request、stores、layouts、styles 等 workspace 包。
- Vitest。

主应用：

- `vben/apps/web-antd` 是当前平安班组业务主应用。
- `vben/apps/web-antd/package.json` 的脚本包括 `dev`、`build`、`preview`、`typecheck`。
- `vben/apps/web-antd/vite.config.ts` 将 `/api` 和 `/uploads` 代理到 `http://localhost:8080`。

关键结构：

| 路径 | 说明 |
| --- | --- |
| `vben/apps/web-antd/src/api/request.ts` | 统一请求客户端，注入 Bearer Token 和 `Accept-Language`。 |
| `vben/apps/web-antd/src/api/pingan/` | 平安业务 API 与测试。 |
| `vben/apps/web-antd/src/api/system-management/` | 系统管理 API、类型、测试。 |
| `vben/apps/web-antd/src/views/pingan/` | 平安业务页面。 |
| `vben/apps/web-antd/src/views/system-management/` | 系统管理页面。 |
| `vben/apps/web-antd/src/router/routes/modules/pingan.ts` | 平安业务路由。 |
| `vben/apps/web-antd/src/router/routes/modules/system-management.ts` | 系统管理路由。 |

前端业务路由覆盖：

- 平安班组监控中心。
- 组织架构。
- 风险管控。
- 一班三查。
- 隐患排查。
- 特殊作业。
- 宣教培训。
- 安全积分。
- 安全台账。
- 数据库。
- 系统管理。

部分菜单仍是 `ComingSoon` 或复用已有工作台页面，开发时要确认页面是否已真实实现。

## 本地启动

优先参考 `docs/project/getting-started/启动.md`。后续开发和人工验收统一使用手工验证库 `pingan_banzu_verify`；真实业务库 `pingan_banzu` 只保留，不作为日常开发写入目标。

先启动 PostgreSQL + MinIO：

```powershell
docker compose -f docker-compose.storage.yml up -d
```

首次使用手工验证库时创建一次：

```powershell
docker exec pingan-banzu-postgres createdb -U pingan pingan_banzu_verify
```

后端：

```powershell
$env:PINGAN_DB_URL="jdbc:postgresql://localhost:15433/pingan_banzu_verify"
$env:PINGAN_DB_USERNAME="pingan"
$env:PINGAN_DB_PASSWORD="pingan"
$env:PINGAN_STORAGE_PROVIDER="minio"
$env:PINGAN_MINIO_ENDPOINT="http://localhost:9000"
$env:PINGAN_MINIO_PUBLIC_ENDPOINT="http://localhost:9000"
$env:PINGAN_MINIO_BUCKET="pingan-banzu-verify"
$env:PINGAN_MINIO_ACCESS_KEY="minioadmin"
$env:PINGAN_MINIO_SECRET_KEY="minioadmin"
.\mvnw.cmd -f backend\pom.xml spring-boot:run
```

前端：

```powershell
D:\node.js\corepack.cmd pnpm@10.33.0 -C vben -F @vben/web-antd run dev
```

默认地址：

- 后端：`http://localhost:8080`
- 前端：`http://localhost:5666`

默认登录：

```text
账号：admin
密码：123456
```

默认端口：

- PostgreSQL：`localhost:15433`
- MinIO API：`http://localhost:9000`
- MinIO Console：`http://localhost:9001`

## 验证命令

后端全量测试：

```powershell
.\mvnw.cmd -f backend/pom.xml test
```

后端专项测试示例：

```powershell
.\mvnw.cmd -f backend/pom.xml test "-Dtest=PreShiftMeetingApiTest"
```

前端类型检查：

```powershell
D:\node.js\corepack.cmd pnpm@10.33.0 -C vben -F @vben/web-antd run typecheck
```

前端单测示例：

```powershell
D:\node.js\corepack.cmd pnpm@10.33.0 -C vben exec vitest run apps/web-antd/src/router/routes/__tests__/pingan.test.ts --dom
```

## 文档入口

| 路径 | 说明 |
| --- | --- |
| `docs/project/workflow/CODEX_AUTO_DEVELOPMENT.md` | Codex 全自动开发、worktree 隔离、任务类型流程和人工验收分工总规范。 |
| `docs/project/getting-started/启动.md` | 本地启动、数据存储、前后端重启。 |
| `docs/project/workflow/GIT_WORKFLOW.md` | Codex 自动开发与 Git 边界。 |
| `docs/shared/api/overview.md` 与 `docs/shared/api/*.md` | 当前接口导航及各模块详细契约。 |
| `docs/archive/snapshots/2026-05-15-interfaces-and-models.md` | 截至 2026-05-15 的接口与模型历史综合快照。 |
| `docs/shared/api/system-management.md` | 系统管理接口文档。 |
| `docs/shared/api/one-shift-three-checks-pre-shift-meeting.md` | 一班三查班前会接口文档。 |
| `docs/mini-program/api/mini-program-one-shift-three-checks-sync.md` | 小程序同步接口文档。 |
| `docs/shared/operations/postgres-minio-storage.md` | PostgreSQL + MinIO 部署和生产建议。 |
| `docs/archive/legacy-workflow/plans/` | 历史开发计划。 |
| `docs/archive/legacy-workflow/reviews/` | 历史评审记录。 |

## 业务开发注意事项

- 新业务应优先复用现有 Spring Boot 分层、统一响应、异常处理、JWT 当前用户上下文和数据权限规则。
- 涉及数据库结构时，通过 Flyway 新增迁移，不要直接修改已发布迁移。
- 系统管理代码优先放在 `backend/src/main/java/com/pingan/banzu/system` 对应分层。
- 平安业务代码优先放在普通 `controller/domain/dto/mapper/service` 分层。
- 附件不要直接暴露存储凭证；MinIO 模式下通过后端返回短期预签名 URL。
- PC 与小程序的一班三查数据必须共享同一业务表和状态机，避免做两套数据再同步。
- 前端平安业务优先放在 `vben/apps/web-antd/src/api/pingan` 与 `src/views/pingan`。
- 前端系统管理优先放在 `src/api/system-management` 与 `src/views/system-management`。
- 前端请求统一使用 `requestClient`，让 token、语言和响应结构处理保持一致。
- 路由变更后补充或更新 `src/router/routes/__tests__` 下的测试。
- 页面开发前先确认目标菜单是已实现页面、复用工作台，还是 `ComingSoon` 占位。

## 小程序 UI 开发注意事项

- 小程序页面若需要严格对齐截图，尤其是“创建”“筛选”“明细”等像素敏感按钮，不要使用 `t-button + slot 图标文字 + 外部 CSS 硬压尺寸`。
- 这类按钮优先使用普通 `view` 作为视觉按钮容器，内部放 `t-icon + text`，点击事件使用 `bindtap`；TDesign 只提供图标，不负责按钮容器尺寸。
- 如果必须使用 `t-button`，要优先使用 `size="extra-small"`、`customStyle` 或组件明确支持的 `icon` / `content` 属性，并在微信开发者工具里确认真实渲染效果；不要只依赖页面 WXSS 覆盖组件内部结构。
- 对截图严格对齐的按钮，测试不能只检查 CSS 字符串，必须补充结构测试，例如断言这些按钮没有继续使用 `t-button` 承载，并检查实际 WXML 结构为 `view + t-icon + text`。
- 发现按钮大小、位置多次修改无效时，先排查是否被 TDesign 组件默认 `size`、内部 `.t-button__content`、slot 包裹层或全局 `button` 样式影响；不要继续盲目调外层宽高。

## Git 与产物边界

按 `docs/project/workflow/CODEX_AUTO_DEVELOPMENT.md` 和 `docs/project/workflow/GIT_WORKFLOW.md`：

- 默认从 `codex/pingan-integrated` 新建任务分支和独立 worktree。
- `codex/pingan-integrated` 是集成基准和集成目标，不是日常开发工作区。
- 不在业务开发中直接修改 `main` 或 `master`。
- 一个完整功能点、修复点或文档更新完成后应提交一次。
- 提交前执行与改动范围匹配的验证。
- Codex 不自动 push、不自动创建 PR、不自动合并，除非用户明确授权。

不要提交：

- `node_modules/`
- `target/`
- `dist/`
- `.codex-run/`
- `.agent/`、`.agents/`、`.claude/`
- 日志、缓存、临时导出文件
- `backend/uploads/`
- `backend/uploads-local/`
- 本地密钥、token、生产私有数据

## 已知容易踩坑

- PowerShell 下 Maven `-D...` 参数要加引号，例如 `"-Dspring-boot.run.profiles=local"`。
- 日常开发不要直接写入真实业务库 `pingan_banzu`；使用手工验证库 `pingan_banzu_verify`。
- 如果手工验证库不存在，后端会报 `database "pingan_banzu_verify" does not exist`，先执行 `docker exec pingan-banzu-postgres createdb -U pingan pingan_banzu_verify`。
- 如果 8080 或 5666 已被占用，先检查是否已有本项目后端或 Vite 前端在运行，不要盲目重复启动。
- 安全配置入口不要找 `SecurityConfig.java`；当前是 `config/WebConfig.java` + `security/AuthInterceptor.java`。
- 一班三查有旧路径 `/pingan/pre-shift-meeting` 重定向到 `/pingan/three-checks/pre-shift-meeting`，路由修改时注意兼容。
- `tools/three-check-generator` 默认不会覆盖已有文件，需要覆盖时才传 `--force`。
