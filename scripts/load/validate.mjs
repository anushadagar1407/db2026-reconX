import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const repositoryRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const read = (relativePath) => readFile(resolve(repositoryRoot, relativePath), 'utf8');

const [k6Script, queryScript, loadCompose, posixWrapper, powershellWrapper, dashboard] = await Promise.all([
  read('scripts/load/adv097.js'),
  read('scripts/load/query-prometheus.py'),
  read('docker-compose.load.yml'),
  read('scripts/verify'),
  read('scripts/verify.ps1'),
  read('monitoring/grafana/provisioning/dashboards/reconx-overview.json'),
]);

assert.match(k6Script, /vus:\s*10/);
assert.match(k6Script, /iterations:\s*100/);
assert.match(k6Script, /\/api\/auth\/login/);
assert.match(k6Script, /email: 'trader@db\.com'/);
assert.match(k6Script, /result\.json\('token'\)/);
assert.match(k6Script, /instrumentId:\s*1/);
assert.match(k6Script, /counterpartyId:\s*1/);
assert.match(k6Script, /assetClass: 'EQUITY'/);
assert.match(k6Script, /side: 'BUY'/);
assert.match(k6Script, /tradeRef/);
assert.doesNotMatch(k6Script, /accessToken|instrumentSymbol|username/);
assert.match(k6Script, /adv097_trade_created: \['count==100'\]/);
assert.match(k6Script, /adv097_trade_failures: \['count==0'\]/);
assert.match(k6Script, /k6-summary\.json/);

const requestRateQuery = 'sum(rate(http_server_requests_seconds_count{uri="/v1/trades",method="POST",status="201"}[1m])) by (uri)';
const p95Query = '1000 * histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri="/v1/trades",method="POST",status="201"}[5m])) by (le, uri))';
assert.match(queryScript, /http_server_requests_seconds_count/);
assert.match(queryScript, /http_server_requests_seconds_bucket/);
assert.match(queryScript, /status="201"/);
assert.match(queryScript, /prometheus-query-evidence\.json/);

const dashboardDocument = JSON.parse(dashboard);
const requestRatePanel = dashboardDocument.panels.find(({ title }) => title.includes('ADV087'));
const p95Panel = dashboardDocument.panels.find(({ title }) => title.includes('ADV088'));
const tradeRatePanel = dashboardDocument.panels.find(({ title }) => title.includes('ADV089'));
assert.equal(requestRatePanel.targets[0].expr, requestRateQuery);
assert.equal(requestRatePanel.fieldConfig.defaults.unit, 'reqps');
assert.equal(p95Panel.targets[0].expr, p95Query);
assert.equal(p95Panel.fieldConfig.defaults.unit, 'ms');
assert.equal(tradeRatePanel.targets[0].expr, 'sum(rate(trade_total[1m]))');

assert.match(loadCompose, /grafana\/k6:0\.55\.2/);
assert.match(loadCompose, /python:3\.13\.1-alpine/);
assert.match(loadCompose, /profiles: \["load"\]/);
assert.match(loadCompose, /tmpfs:/);
assert.match(loadCompose, /RECONX_LOAD_ARTIFACT_DIR/);
assert.match(posixWrapper, /all\|backend\|frontend\|load/);
assert.match(powershellWrapper, /all", "backend", "frontend", "load"/);

console.log('ADV097 static wiring validation passed.');
