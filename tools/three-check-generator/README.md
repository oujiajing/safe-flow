# 一班三查模块生成器

该生成器用于沉淀班前会模板，后续班前检查、班中检查、班后会按同一字段和同步契约扩展。

```powershell
node tools\three-check-generator\generate.mjs --module pre-shift-meeting --dry-run
node tools\three-check-generator\generate.mjs --module pre-shift-inspection --dry-run
```

默认写入时不会覆盖已有文件；确需覆盖生成文件时传入 `--force`。
