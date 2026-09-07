#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const rootDir = process.cwd();
const generatorDir = path.join(rootDir, 'tools', 'three-check-generator');

const args = new Map();
for (let index = 2; index < process.argv.length; index += 1) {
  const arg = process.argv[index];
  if (arg.startsWith('--')) {
    const key = arg.slice(2);
    const next = process.argv[index + 1];
    if (!next || next.startsWith('--')) {
      args.set(key, true);
    } else {
      args.set(key, next);
      index += 1;
    }
  }
}

const moduleKey = args.get('module');
const dryRun = args.has('dry-run');
const force = args.has('force');

if (!moduleKey || typeof moduleKey !== 'string') {
  fail('Usage: node tools/three-check-generator/generate.mjs --module <module-key> [--dry-run] [--force]');
}

const modulePath = path.join(generatorDir, 'modules', `${moduleKey}.json`);
if (!fs.existsSync(modulePath)) {
  fail(`Unknown three-check module: ${moduleKey}`);
}

const moduleConfig = JSON.parse(fs.readFileSync(modulePath, 'utf8'));
validateModule(moduleConfig);

const replacements = {
  ...moduleConfig,
  camelEntityName:
    moduleConfig.entityName.charAt(0).toLowerCase() + moduleConfig.entityName.slice(1),
  miniApiBase: moduleConfig.sync.miniApiBase,
};

const targets = [
  {
    path: `backend/src/main/java/com/pingan/banzu/domain/${moduleConfig.entityName}.java`,
    template: 'backend/domain.java.tpl',
  },
  {
    path: `backend/src/main/java/com/pingan/banzu/mapper/${moduleConfig.entityName}Mapper.java`,
    template: 'backend/mapper.java.tpl',
  },
  {
    path: `backend/src/main/java/com/pingan/banzu/dto/${moduleConfig.entityName}Query.java`,
    template: null,
  },
  {
    path: `backend/src/main/java/com/pingan/banzu/service/${moduleConfig.entityName}Service.java`,
    template: 'backend/service.java.tpl',
  },
  {
    path: `backend/src/main/java/com/pingan/banzu/controller/${moduleConfig.entityName}Controller.java`,
    template: 'backend/controller.java.tpl',
  },
  {
    path: `backend/src/test/java/com/pingan/banzu/${moduleConfig.entityName}ApiTest.java`,
    template: null,
  },
  {
    path: `vben/apps/web-antd/src/api/pingan/${moduleConfig.moduleKey}.ts`,
    template: 'frontend/api.ts.tpl',
  },
  {
    path: `vben/apps/web-antd/src/views/pingan/three-checks/workbench/module-config.ts`,
    template: 'frontend/module-config.ts.tpl',
  },
  {
    path: `vben/apps/web-antd/src/views/pingan/three-checks/workbench/${moduleConfig.moduleKey}.ts`,
    template: null,
  },
  {
    path: `vben/apps/web-antd/src/views/pingan/three-checks/workbench/${moduleConfig.moduleKey}.test.ts`,
    template: null,
  },
];

if (dryRun) {
  for (const target of targets) {
    console.log(target.path);
  }
  process.exit(0);
}

for (const target of targets) {
  const absoluteTarget = path.join(rootDir, target.path);
  if (fs.existsSync(absoluteTarget) && !force) {
    fail(`Refusing to overwrite existing file: ${target.path}\nUse --force to overwrite generated files.`);
  }
}

for (const target of targets) {
  const absoluteTarget = path.join(rootDir, target.path);
  fs.mkdirSync(path.dirname(absoluteTarget), { recursive: true });
  fs.writeFileSync(absoluteTarget, renderTarget(target), 'utf8');
  console.log(target.path);
}

function validateModule(config) {
  const required = [
    'moduleKey',
    'bizType',
    'entityName',
    'tableName',
    'routePath',
    'apiBase',
    'title',
    'primaryNoField',
    'businessDateField',
    'sync',
    'extraFields',
  ];
  const missing = required.filter((key) => config[key] === undefined);
  if (missing.length > 0) {
    fail(`Invalid module config. Missing: ${missing.join(', ')}`);
  }
  const syncRequired = [
    'sourceChannels',
    'idempotencyField',
    'clientRecordField',
    'miniApiBase',
  ];
  const missingSync = syncRequired.filter((key) => config.sync[key] === undefined);
  if (missingSync.length > 0) {
    fail(`Invalid module sync config. Missing: ${missingSync.join(', ')}`);
  }
}

function renderTarget(target) {
  if (!target.template) {
    return `// Generated placeholder for ${moduleConfig.title}.\n`;
  }
  const templatePath = path.join(generatorDir, 'templates', target.template);
  const template = fs.readFileSync(templatePath, 'utf8');
  return template.replaceAll(/\{\{(\w+)\}\}/g, (_, key) => replacements[key] ?? '');
}

function fail(message) {
  console.error(message);
  process.exit(1);
}
