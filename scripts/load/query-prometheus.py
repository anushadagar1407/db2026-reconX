import json
import math
import os
import time
from datetime import datetime, timezone
from urllib.parse import quote
from urllib.request import urlopen


PROMETHEUS_URL = os.environ.get("PROMETHEUS_URL", "http://localhost:9090").rstrip("/")
TRADE_URI = "/v1/trades"
CREATION_COUNT_QUERY = (
    'sum(http_server_requests_seconds_count{'
    'uri="/v1/trades",method="POST",status="201"})'
)
REQUEST_RATE_QUERY = (
    'sum(rate(http_server_requests_seconds_count{'
    'uri="/v1/trades",method="POST",status="201"}[1m])) by (uri)'
)
P95_QUERY = (
    '1000 * histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{'
    'uri="/v1/trades",method="POST",status="201"}[5m])) by (le, uri))'
)


def query(promql):
    url = f"{PROMETHEUS_URL}/api/v1/query?query={quote(promql, safe='')}"
    with urlopen(url, timeout=10) as response:
        payload = json.load(response)
    if payload.get("status") != "success":
        raise RuntimeError(f"Prometheus query failed: {promql}")
    return payload


def scalar_value(payload):
    results = payload.get("data", {}).get("result", [])
    if not results:
        return None
    return float(results[0]["value"][1])


def vector_values(payload):
    values = {}
    for result in payload.get("data", {}).get("result", []):
        values[result.get("metric", {}).get("uri", "unknown")] = float(result["value"][1])
    return values


deadline = time.monotonic() + 45
creation_payload = None
creation_count = None
while time.monotonic() < deadline:
    try:
        creation_payload = query(CREATION_COUNT_QUERY)
        creation_count = scalar_value(creation_payload)
        if creation_count is not None and creation_count >= 100:
            break
    except Exception:
        pass
    time.sleep(2)

if creation_count is None or creation_count < 100:
    raise SystemExit(
        f"Prometheus did not observe 100 HTTP 201 trade creations (observed={creation_count})"
    )

request_rate_payload = query(REQUEST_RATE_QUERY)
p95_payload = query(P95_QUERY)
request_rates = vector_values(request_rate_payload)
p95_milliseconds = scalar_value(p95_payload)
request_rate = sum(request_rates.values())
if (
    not request_rates
    or request_rate <= 0
    or p95_milliseconds is None
    or not math.isfinite(request_rate)
    or not math.isfinite(p95_milliseconds)
):
    raise SystemExit("Prometheus returned no request-rate or P95 panel series")

evidence = {
    "ticket": "TICKET-ADV097",
    "observedAt": datetime.now(timezone.utc).isoformat(),
    "tradeUri": TRADE_URI,
    "observedHttp201Creations": creation_count,
    "requestRate": {
        "value": request_rate,
        "unit": "requests/second",
        "window": "1m",
        "query": REQUEST_RATE_QUERY,
        "series": request_rates,
    },
    "p95": {
        "valueSeconds": p95_milliseconds / 1000,
        "valueMilliseconds": p95_milliseconds,
        "unit": "milliseconds",
        "window": "5m",
        "query": P95_QUERY,
    },
    "creationCountQuery": {
        "value": creation_count,
        "window": "instant",
        "query": CREATION_COUNT_QUERY,
    },
    "rawResponses": {
        "creationCount": creation_payload,
        "requestRate": request_rate_payload,
        "p95": p95_payload,
    },
}

with open("/results/prometheus-query-evidence.json", "w", encoding="utf-8") as output:
    json.dump(evidence, output, indent=2)
    output.write("\n")

print("ADV097 Prometheus panel-query evidence")
print(f"HTTP 201 creations observed: {creation_count:.0f}/100")
print(f"request-rate panel: {request_rate} requests/second (1m rate)")
print(f"P95 panel: {p95_milliseconds} milliseconds (5m histogram)")
