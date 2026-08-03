#!/usr/bin/env node

import { lstat, readdir, readFile } from 'node:fs/promises';
import { extname, resolve } from 'node:path';

const allowedExtensions = new Set(['.json', '.log', '.txt']);
const safeValues = new Set(['[redacted]', '<redacted>', 'redacted', 'null', '***', '*****', '********', '*********']);
const credentialAssignments = [
  /["'](password|passwd|pwd|jwt_secret|token|secret|api[_-]?key|gf_security_admin_password|postgres_password)["']\s*:\s*["']?([^\s,"'}]+)/gi,
  /\b(password|passwd|pwd|jwt_secret|token|secret|api[_-]?key|gf_security_admin_password|postgres_password)\b\s*=\s*["']?([^\s,"'}]+)/gi,
];
const rules = [
  ['known demo password', /\btrader123\b/i],
  ['known demo JWT secret', /adv097-local-only-secret-change-me-32-bytes/i],
  ['unredacted bearer credential', /\bauthorization\s*:\s*bearer\s+(?!\[redacted\]|<redacted>|redacted(?:\s|$))\S+/i],
  ['JWT value', /\beyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\b/],
  ['generated security password', /Using generated security password:\s*(?!\[redacted\]|<redacted>|redacted(?:\s|$)|\*{3,}(?:\s|$))\S+/i],
  ['credential in URL', /:\/\/[^\s/:@]+:[^\s/@]+@/],
];

const roots = process.argv.slice(2);
if (roots.length === 0) {
  console.error('Usage: node scripts/load/validate-artifacts.mjs <artifact-path> [...]');
  process.exit(2);
}

const files = [];
async function collect(path, explicitlyRequested = false) {
  const item = await lstat(path);
  if (item.isSymbolicLink()) {
    throw new Error(`Refusing symbolic-link artifact path: ${path}`);
  }
  if (item.isDirectory()) {
    const entries = await readdir(path, { withFileTypes: true });
    for (const entry of entries) {
      await collect(resolve(path, entry.name));
    }
    return;
  }
  if (!item.isFile()) {
    throw new Error(`Refusing non-file artifact path: ${path}`);
  }
  if (explicitlyRequested || allowedExtensions.has(extname(path).toLowerCase())) {
    files.push(path);
  }
}

try {
  for (const root of roots) {
    await collect(resolve(root), true);
  }
} catch (error) {
  console.error(`Artifact credential validation could not inspect all paths: ${error.message}`);
  process.exit(1);
}

const findings = [];
for (const file of files.sort()) {
  const contents = await readFile(file, 'utf8');
  const lines = contents.replaceAll('\\n', '\n').replaceAll('\\t', '\t').split(/\r?\n/);
  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index];
    for (const [name, pattern] of rules) {
      if (pattern.test(line)) findings.push(`${file}:${index + 1}: ${name}`);
    }
    for (const assignment of credentialAssignments) {
      assignment.lastIndex = 0;
      for (const match of line.matchAll(assignment)) {
        const value = match[2].replace(/["']+$/, '').toLowerCase();
        if (!safeValues.has(value) && !/^\$\{[^}]+\}$/.test(value)) {
          findings.push(`${file}:${index + 1}: credential-bearing ${match[1]} assignment`);
        }
      }
    }
  }
}

if (findings.length > 0) {
  console.error('Artifact credential validation failed; values are omitted from diagnostics:');
  for (const finding of [...new Set(findings)]) console.error(`- ${finding}`);
  process.exit(1);
}

console.log(`Artifact credential validation passed for ${files.length} text/JSON/log file(s).`);
