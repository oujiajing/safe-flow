import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const errors = [];

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function requireFile(relativePath) {
  if (!fs.existsSync(path.join(root, relativePath))) {
    errors.push(`缺少必需文件：${relativePath}`);
  }
}

[
  'backend/pom.xml',
  'vben/pnpm-lock.yaml',
  'mini-program/package-lock.json',
  'docker-compose.storage.yml',
].forEach(requireFile);

const migrationDirectories = [
  'backend/src/main/resources/db/migration',
  'backend/src/main/resources/db/vendor/postgresql',
  'backend/src/main/resources/db/vendor/h2',
];
const migrations = migrationDirectories.flatMap(relativePath =>
  fs.readdirSync(path.join(root, relativePath))
);
const versions = migrations
  .map(name => /^V(\d+)__/.exec(name))
  .filter(Boolean)
  .map(match => Number(match[1]));
const latestMigration = Math.max(...versions);
const postgresVersions = fs.readdirSync(path.join(root, migrationDirectories[1]))
  .map(name => /^V(\d+)__/.exec(name))
  .filter(Boolean)
  .map(match => Number(match[1]));
const h2Versions = fs.readdirSync(path.join(root, migrationDirectories[2]))
  .map(name => /^V(\d+)__/.exec(name))
  .filter(Boolean)
  .map(match => Number(match[1]));
if (postgresVersions.join(',') !== h2Versions.join(',')) {
  errors.push('PostgreSQL 与 H2 vendor 迁移版本不一致');
}

const compose = read('docker-compose.storage.yml');
if (/^\s*image:\s*\S+:latest\s*$/m.test(compose)) {
  errors.push('docker-compose.storage.yml 不得使用浮动 :latest 镜像');
}

const tracked = execFileSync('git', ['ls-files', '-z'], { cwd: root })
  .toString('utf8')
  .split('\0')
  .filter(Boolean);
const secretPatterns = [
  ['PEM 私钥', /-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/],
  ['GitHub classic token', /\bghp_[A-Za-z0-9]{36}\b/],
  ['GitHub fine-grained token', /\bgithub_pat_[A-Za-z0-9_]{70,}\b/],
  ['AWS access key', /\bAKIA[0-9A-Z]{16}\b/],
  ['OpenAI API key', /\bsk-[A-Za-z0-9]{40,}\b/],
];

for (const relativePath of tracked) {
  const absolutePath = path.join(root, relativePath);
  if (!fs.existsSync(absolutePath) || fs.statSync(absolutePath).size > 2 * 1024 * 1024) {
    continue;
  }
  const buffer = fs.readFileSync(absolutePath);
  if (buffer.includes(0)) {
    continue;
  }
  const content = buffer.toString('utf8');
  for (const [label, pattern] of secretPatterns) {
    if (pattern.test(content)) {
      errors.push(`${label} 高置信度命中：${relativePath}`);
    }
  }
}

if (errors.length) {
  console.error(errors.map(error => `- ${error}`).join('\n'));
  process.exit(1);
}

console.log(`Flyway 版本 V${latestMigration}、固定镜像和高置信度 Secret 检查通过`);
