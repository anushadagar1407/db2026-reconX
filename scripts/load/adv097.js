import http from 'k6/http';
import { check, fail } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import exec from 'k6/execution';
import { sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TRADE_REQUESTS = new Counter('adv097_trade_requests');
const TRADE_CREATED = new Counter('adv097_trade_created');
const TRADE_FAILURES = new Counter('adv097_trade_failures');
const TRADE_DURATION = new Trend('adv097_trade_duration', true);

export const options = {
  vus: 10,
  iterations: 100,
  thresholds: {
    checks: ['rate==1'],
    http_req_failed: ['rate==0'],
    adv097_trade_requests: ['count==100'],
    adv097_trade_created: ['count==100'],
    adv097_trade_failures: ['count==0'],
  },
};

export function setup() {
  const response = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email: 'trader@db.com', password: 'trader123' }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  const loginSucceeded = check(response, {
    'login status is 200': (result) => result.status === 200,
    'login response contains token': (result) => Boolean(result.json('token')),
  });

  if (!loginSucceeded) {
    fail(`login failed with HTTP ${response.status}`);
  }

  return { token: response.json('token') };
}

export default function (data) {
  const iteration = exec.scenario.iterationInTest + 1;
  const tradeRef = `ADV-20260101-${String(iteration).padStart(4, '0')}`;
  const response = http.post(
    `${BASE_URL}/api/v1/trades`,
    JSON.stringify({
      tradeRef,
      instrumentId: 1,
      counterpartyId: 1,
      assetClass: 'EQUITY',
      side: 'BUY',
      quantity: 100,
      price: 245.50,
      tradeDate: '2026-01-01',
    }),
    {
      headers: {
        Authorization: `Bearer ${data.token}`,
        'Content-Type': 'application/json',
      },
      tags: { adv097: 'trade-create' },
    },
  );

  TRADE_REQUESTS.add(1);
  TRADE_DURATION.add(response.timings.duration);
  const created = response.status === 201;
  TRADE_CREATED.add(created ? 1 : 0);
  TRADE_FAILURES.add(created ? 0 : 1);

  check(response, { 'trade status is 201': (result) => result.status === 201 });
  sleep(0.75);
}

function metricValues(data, name) {
  return data.metrics[name]?.values ?? {};
}

export function handleSummary(data) {
  const requests = metricValues(data, 'adv097_trade_requests');
  const created = metricValues(data, 'adv097_trade_created');
  const failures = metricValues(data, 'adv097_trade_failures');
  const duration = metricValues(data, 'adv097_trade_duration');
  const summary = {
    ticket: 'TICKET-ADV097',
    vus: 10,
    iterations: 100,
    measuredTradeRequests: requests.count ?? 0,
    successfulTradeCreations: created.count ?? 0,
    failedTradeRequests: failures.count ?? 0,
    requestsPerSecond: requests.rate ?? null,
    p95Milliseconds: duration['p(95)'] ?? null,
    throughputUnit: 'requests/second',
    latencyUnit: 'milliseconds',
  };

  const readable = [
    '',
    'ADV097 container load summary',
    `trade requests: ${summary.measuredTradeRequests}/100`,
    `HTTP 201 creations: ${summary.successfulTradeCreations}/100`,
    `failed trade requests: ${summary.failedTradeRequests}`,
    `throughput: ${summary.requestsPerSecond ?? 'unavailable'} requests/second`,
    `P95: ${summary.p95Milliseconds ?? 'unavailable'} milliseconds`,
    '',
  ].join('\n');

  return {
    stdout: readable,
    '/results/k6-summary.json': JSON.stringify(summary, null, 2),
  };
}
