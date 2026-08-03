import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const repositoryRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const read = (relativePath) => readFile(resolve(repositoryRoot, relativePath), 'utf8');

const [k6Script, queryScript, exportScript, loadCompose, posixWrapper, powershellWrapper, dashboard, tradeMetrics, loadPrometheus, prometheus] = await Promise.all([
  read('scripts/load/adv097.js'),
  read('scripts/load/query-prometheus.py'),
  read('scripts/load/export-artifacts.py'),
  read('docker-compose.load.yml'),
  read('scripts/verify'),
  read('scripts/verify.ps1'),
  read('monitoring/grafana/provisioning/dashboards/reconx-overview.json'),
  read('backend/src/main/java/com/dbtraining/reconx/observability/TradeMetrics.java'),
  read('monitoring/prometheus/prometheus-load.yml'),
  read('monitoring/prometheus/prometheus.yml'),
]);

assert.match(k6Script, /const VUS = 10/);
assert.match(k6Script, /const ITERATIONS_PER_VU = 10/);
assert.match(k6Script, /executor: 'per-vu-iterations'/);
assert.match(k6Script, /TOTAL_ITERATIONS = VUS \* ITERATIONS_PER_VU/);
assert.match(k6Script, /PACED_INTERVAL_SECONDS = 6/);
assert.match(k6Script, /\/api\/auth\/login/);
assert.match(k6Script, /email: 'trader@db\.com'/);
assert.match(k6Script, /result\.json\('token'\)/);
assert.match(k6Script, /instrumentId:\s*1/);
assert.match(k6Script, /counterpartyId:\s*1/);
assert.match(k6Script, /assetClass: 'EQUITY'/);
assert.match(k6Script, /side: 'BUY'/);
assert.match(k6Script, /tradeRef/);
assert.doesNotMatch(k6Script, /accessToken|instrumentSymbol|username/);
assert.match(k6Script, /adv097_trade_created: \[`count==\$\{TOTAL_ITERATIONS\}`\]/);
assert.match(k6Script, /adv097_trade_failures: \['count==0'\]/);
assert.match(k6Script, /k6-summary\.json/);
assert.match(k6Script, /runDurationMilliseconds/);

const requestRateQuery = 'sum(rate(http_server_requests_seconds_count[1m])) by (uri)';
const p95Query = '1000 * histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[1m])) by (le, uri))';
assert.match(queryScript, /http_server_requests_seconds_count/);
assert.match(queryScript, /http_server_requests_seconds_bucket/);
assert.match(queryScript, /CREATION_COUNTER_SELECTOR/);
assert.match(queryScript, /BUSINESS_COUNTER_QUERY = "trade_created_total"/);
assert.match(queryScript, /BUSINESS_RATE_QUERY = "sum\(rate\(trade_created_total\[1m\]\)\)"/);
assert.match(queryScript, /status="201"/);
assert.match(queryScript, /THROUGHPUT_TOLERANCE_PERCENT = 20/);
assert.match(queryScript, /creation_delta != 100/);
assert.match(queryScript, /business_delta != 100/);
assert.match(queryScript, /within_tolerance/);
assert.match(queryScript, /prometheus-query-evidence\.json/);
assert.match(exportScript, /k6-summary\.json/);
assert.match(exportScript, /k6-raw-summary\.json/);
assert.match(exportScript, /prometheus-query-evidence\.json/);
assert.match(exportScript, /adv097_trade_created/);
assert.match(exportScript, /withinTolerance/);
assert.match(exportScript, /prometheusTradeCreatedDelta/);
assert.match(exportScript, /setup_data/);
assert.match(tradeMetrics, /Counter\.builder\("trade_created_total"\)/);
assert.match(tradeMetrics, /incrementTradeCreated/);
assert.match(loadPrometheus, /replacement: trade_created_total/);
assert.match(prometheus, /replacement: trade_created_total/);

const dashboardDocument = JSON.parse(dashboard);
const requestRatePanel = dashboardDocument.panels.find(({ title }) => title.includes('ADV087'));
const p95Panel = dashboardDocument.panels.find(({ title }) => title.includes('ADV088'));
const tradeRatePanel = dashboardDocument.panels.find(({ title }) => title.includes('ADV089'));
assert.equal(requestRatePanel.targets[0].expr, requestRateQuery);
assert.equal(requestRatePanel.fieldConfig.defaults.unit, 'reqps');
assert.equal(p95Panel.targets[0].expr, p95Query);
assert.equal(p95Panel.fieldConfig.defaults.unit, 'ms');
assert.equal(tradeRatePanel.targets[0].expr, 'sum(rate(trade_created_total[1m]))');
assert.equal(tradeRatePanel.fieldConfig.defaults.unit, 'reqps');

assert.match(loadCompose, /grafana\/k6:0\.55\.2/);
assert.match(loadCompose, /python:3\.13\.1-alpine/);
assert.match(loadCompose, /busybox:1\.36\.1/);
assert.match(loadCompose, /profiles: \["load"\]/);
assert.match(loadCompose, /tmpfs:/);
assert.match(loadCompose, /load_results:\/results/);
assert.match(loadCompose, /load-results-init/);
assert.match(loadCompose, /load-export/);
assert.match(posixWrapper, /all\|backend\|frontend\|load/);
assert.match(powershellWrapper, /all", "backend", "frontend", "load"/);
assert.match(posixWrapper, /prepare_owned_directory/);
assert.match(posixWrapper, /Refusing cleanup without wrapper ownership marker/);
assert.match(posixWrapper, /--profile load down --volumes/);
assert.doesNotMatch(posixWrapper, /rm -rf "\$report_root\/(backend|frontend)"/);
assert.match(powershellWrapper, /Prepare-OwnedDirectory/);
assert.match(powershellWrapper, /Refusing cleanup without wrapper ownership marker/);
assert.match(powershellWrapper, /"--profile", "load", "down"/);
assert.doesNotMatch(powershellWrapper, /Remove-Item -LiteralPath \(Join-Path \$ReportRoot/);

console.log('ADV097 static wiring validation passed.');
