import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const isWindows = process.platform === 'win32';
const requested = process.argv.slice(2);
const groups = requested.length ? requested : ['all'];
const selected = new Set(groups.includes('all')
  ? ['backend', 'pc', 'mini', 'contracts']
  : groups);
const supported = new Set(['backend', 'pc', 'mini', 'contracts']);

for (const group of selected) {
  if (!supported.has(group)) {
    console.error(`未知门禁分组：${group}`);
    process.exit(2);
  }
}

function run(label, command, args, cwd = root) {
  console.log(`\n[quality-gate] ${label}`);
  const result = spawnSync(command, args, {
    cwd,
    encoding: 'utf8',
    shell: isWindows,
    stdio: 'inherit',
  });
  if (result.error) {
    console.error(result.error.message);
  }
  if (result.status !== 0) {
    process.exit(result.status ?? 1);
  }
}

if (selected.has('backend')) {
  run(
    '后端单测、真实 PostgreSQL/Flyway、MinIO 与关键 E2E',
    isWindows ? 'mvnw.cmd' : './mvnw',
    ['-f', 'backend/pom.xml', '-Pintegration', 'clean', 'verify'],
  );
}

if (selected.has('pc')) {
  const corepack = isWindows ? 'corepack.cmd' : 'corepack';
  const pnpm = ['pnpm@10.33.0', '-C', 'vben'];
  if (process.env.CI === 'true' || !fs.existsSync(path.join(root, 'vben', 'node_modules'))) {
    run('安装 PC 锁定依赖', corepack, [...pnpm, 'install', '--frozen-lockfile']);
  } else {
    console.log('\n[quality-gate] 复用本地已有 PC 依赖；CI 始终执行锁文件安装');
  }
  run('PC 类型检查', corepack, [...pnpm, '-F', '@vben/web-antd', 'run', 'typecheck']);
  run('PC 全量 Vitest', corepack, [...pnpm, 'exec', 'vitest', 'run', 'apps/web-antd', '--dom']);
  run('PC 生产构建', corepack, [...pnpm, '-F', '@vben/web-antd', 'run', 'build']);
}

if (selected.has('mini')) {
  const npm = isWindows ? 'npm.cmd' : 'npm';
  if (process.env.CI === 'true' || !fs.existsSync(path.join(root, 'mini-program', 'node_modules'))) {
    run('安装小程序锁定依赖', npm, ['ci'], path.join(root, 'mini-program'));
  } else {
    console.log('\n[quality-gate] 复用本地已有小程序依赖；CI 始终执行锁文件安装');
  }
  run('小程序全量测试', npm, ['test'], path.join(root, 'mini-program'));
  run('小程序包体和路由检查', npm, ['run', 'check:size'], path.join(root, 'mini-program'));
}

if (selected.has('contracts')) {
  run(
    '仓库关键交付契约与高置信度 Secret 检查',
    process.execPath,
    [path.join(root, 'tools', 'ci', 'check-repository-contracts.mjs')],
  );
}

console.log('\n[quality-gate] 所选门禁全部通过');
