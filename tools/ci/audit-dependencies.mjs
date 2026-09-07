import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const isWindows = process.platform === 'win32';
const baselinePath = path.join(root, 'tools', 'ci', 'dependency-audit-baseline.json');
const baseline = JSON.parse(fs.readFileSync(baselinePath, 'utf8'));
const registry = 'https://registry.npmjs.org';

function audit(label, command, args, allowedAdvisories) {
  const result = spawnSync(command, args, {
    cwd: root,
    encoding: 'utf8',
    shell: isWindows,
    maxBuffer: 20 * 1024 * 1024,
  });
  if (result.error) {
    throw result.error;
  }

  let report;
  try {
    report = JSON.parse(result.stdout);
  } catch {
    console.error(result.stdout);
    console.error(result.stderr);
    throw new Error(`${label} 未返回可解析的 audit JSON`);
  }

  const findings = new Map();
  for (const [id, advisory] of Object.entries(report.advisories ?? {})) {
    if (advisory.severity === 'high' || advisory.severity === 'critical') {
      findings.set(String(id), `${advisory.severity}:${advisory.module_name}`);
    }
  }
  for (const [moduleName, vulnerability] of Object.entries(report.vulnerabilities ?? {})) {
    for (const via of vulnerability.via ?? []) {
      if (
        typeof via === 'object'
        && (via.severity === 'high' || via.severity === 'critical')
        && via.source
      ) {
        findings.set(String(via.source), `${via.severity}:${moduleName}`);
      }
    }
  }

  const allowed = new Set(allowedAdvisories.map(String));
  const newFindings = [...findings.entries()].filter(([id]) => !allowed.has(id));
  const resolvedBaseline = [...allowed].filter(id => !findings.has(id));

  console.log(`${label}：当前 ${findings.size} 个 high/critical 公告，基线允许 ${allowed.size} 个`);
  if (resolvedBaseline.length) {
    console.log(`已不再命中的基线项（应在后续提交删除）：${resolvedBaseline.join(', ')}`);
  }
  if (newFindings.length) {
    console.error('发现未进入基线的 high/critical 依赖漏洞：');
    for (const [id, description] of newFindings) {
      console.error(`- ${id} ${description}`);
    }
    return false;
  }
  return true;
}

const pcPassed = audit(
  'PC',
  isWindows ? 'corepack.cmd' : 'corepack',
  [
    'pnpm@10.33.0',
    '-C',
    'vben',
    'audit',
    '--prod',
    '--json',
    '--registry',
    registry,
  ],
  baseline.pc.allowedAdvisories,
);

const miniPassed = audit(
  '小程序',
  isWindows ? 'npm.cmd' : 'npm',
  [
    'audit',
    '--prefix',
    'mini-program',
    '--omit',
    'dev',
    '--json',
    '--registry',
    registry,
  ],
  baseline.miniProgram.allowedAdvisories,
);

if (!pcPassed || !miniPassed) {
  process.exit(1);
}
console.log('依赖漏洞增量门禁通过');
