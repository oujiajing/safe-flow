# 平安班组

平安班组是一套面向安全生产管理的多端系统，包含 Spring Boot 后端、Vue/Vben PC 管理端、微信小程序、数据库迁移、部署配置和业务文档。

## 仓库结构

| 路径 | 用途 |
| --- | --- |
| `backend/` | Java 17、Spring Boot 3.3.6 后端服务及 Flyway 迁移。 |
| `vben/` | Vue 3/Vben Admin monorepo，主应用为 `apps/web-antd`。 |
| `mini-program/` | 微信小程序源码、测试和本地构建配置。 |
| `tools/` | CI 脚本和一班三查代码生成工具。 |
| `docs/` | 架构、接口、模块、测试、部署、验收及开发记录。 |
| `.github/` | GitHub Actions 与依赖更新配置。 |

## 快速开始

完整环境要求、存储服务配置和常见问题见[项目启动说明](docs/project/getting-started/启动.md)。日常开发和人工验收使用 `pingan_banzu_verify` 数据库，不要写入真实业务库 `pingan_banzu`。

启动 PostgreSQL 和 MinIO：

```powershell
docker compose -f docker-compose.storage.yml up -d
```

启动后端：

```powershell
$env:PINGAN_DB_URL="jdbc:postgresql://localhost:15433/pingan_banzu_verify"
$env:PINGAN_DB_USERNAME="pingan"
$env:PINGAN_DB_PASSWORD="pingan"
.\mvnw.cmd -f backend\pom.xml spring-boot:run
```

启动 PC 前端：

```powershell
D:\node.js\corepack.cmd pnpm@10.33.0 -C vben -F @vben/web-antd run dev
```

默认访问地址：

- PC 前端：`http://localhost:5666`
- 后端：`http://localhost:8080`
- MinIO 控制台：`http://localhost:9001`
- 默认账号：`admin` / `123456`

微信开发者工具可直接打开仓库根目录，根目录 `project.config.json` 已将 `miniprogramRoot` 指向 `mini-program/`。

## 常用验证

```powershell
# 后端全量测试
.\mvnw.cmd -f backend\pom.xml test

# PC 类型检查
D:\node.js\corepack.cmd pnpm@10.33.0 -C vben -F @vben/web-antd run typecheck

# 小程序测试
npm --prefix mini-program test
```

## 文档入口

- [文档总览](docs/README.md)
- [系统架构](docs/shared/architecture/overview.md)
- [接口总览](docs/shared/api/overview.md)
- [数据模型](docs/shared/data/overview.md)
- [业务模块总览](docs/shared/domains/overview.md)
- [自动化测试](docs/shared/testing/overview.md)
- [部署与运维](docs/shared/operations/overview.md)
- [Codex 自动开发规范](docs/project/workflow/CODEX_AUTO_DEVELOPMENT.md)
- [Git 工作流](docs/project/workflow/GIT_WORKFLOW.md)

## 开发约定

- 数据库结构调整只新增 Flyway 迁移，不修改已发布迁移。
- PC 请求统一使用项目的 `requestClient`。
- PC 与小程序的一班三查数据共享同一后端事实源和状态机。
- 任务开发从 `codex/pingan-integrated` 创建独立 `codex/<task>` 分支和 worktree。
- 不提交依赖、构建产物、日志、上传目录、本地密钥或生产私有数据。

更完整的项目画像和代理开发注意事项见 [AGENTS.md](AGENTS.md)。
