import json
import math
import os
import time
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import urlopen


PROMETHEUS_URL = os.environ.get("PROMETHEUS_URL", "http://localhost:9090").rstrip("/")
TRADE_URI = "/v1/trades"
RATE_WINDOW_SECONDS = 60
THROUGHPUT_TOLERANCE_PERCENT = 20
RESULTS_DIR = Path("/results")
BASELINE_PATH = RESULTS_DIR / "prometheus-baseline.json"
K6_SUMMARY_PATH = RESULTS_DIR / "k6-summary.json"
K6_RAW_SUMMARY_PATH = RESULTS_DIR / "k6-raw-summary.json"

CREATION_COUNTER_SELECTOR = (
    'http_server_requests_seconds_count{'
    'uri="/v1/trades",method="POST",status="201"}'
)
BASELINE_QUERY = f"{CREATION_COUNTER_SELECTOR} or vector(0)"
CREATION_COUNT_QUERY = CREATION_COUNTER_SELECTOR
REQUEST_RATE_QUERY = "sum(rate(http_server_requests_seconds_count[1m])) by (uri)"
P95_QUERY = (
    "1000 * histogram_quantile(0.95, "
    "sum(rate(http_server_requests_seconds_bucket[1m])) by (le, uri))"
)


def query(promql, at_time=None):
    parameters = {"query": promql}
    if at_time is not None:
        parameters["time"] = f"{at_time:.3f}"
    url = f"{PROMETHEUS_URL}/api/v1/query?{urlencode(parameters)}"
    with urlopen(url, timeout=10) as response:
        payload = json.load(response)
    if payload.get("status") != "success":
        raise RuntimeError(f"Prometheus query failed: {promql}")
    return payload


def read_json(path):
    if not path.is_file() or path.stat().st_size == 0:
        raise RuntimeError(f"Required JSON artifact is missing or empty: {path.name}")
    try:
        with path.open(encoding="utf-8") as source:
            return json.load(source)
    except (OSError, json.JSONDecodeError) as error:
        raise RuntimeError(f"Required JSON artifact is not parseable: {path.name}") from error


def finite_number(value, name):
    try:
        number = float(value)
    except (TypeError, ValueError) as error:
        raise RuntimeError(f"{name} is not numeric") from error
    if not math.isfinite(number):
        raise RuntimeError(f"{name} is not finite")
    return number


def metric_value(result, name):
    values = result.get("value")
    if not isinstance(values, list) or len(values) != 2:
        raise RuntimeError(f"Prometheus result has no scalar value for {name}")
    return finite_number(values[1], name)


def single_counter_value(payload, name, allow_zero_vector=False):
    results = payload.get("data", {}).get("result", [])
    if len(results) != 1:
        raise RuntimeError(f"Expected exactly one Prometheus counter series for {name}")
    metric = results[0].get("metric", {})
    if not isinstance(metric, dict):
        raise RuntimeError(f"Prometheus counter labels are malformed for {name}")
    if metric:
        if (
            metric.get("uri") != TRADE_URI
            or metric.get("method") != "POST"
            or metric.get("status") != "201"
        ):
            raise RuntimeError(f"Unexpected Prometheus counter labels for {name}")
    elif not allow_zero_vector:
        raise RuntimeError(f"Prometheus counter series is missing labels for {name}")
    return metric_value(results[0], name)


def vector_values(payload, name):
    results = payload.get("data", {}).get("result", [])
    if not results:
        raise RuntimeError(f"Prometheus returned no series for {name}")
    values = {}
    for result in results:
        metric = result.get("metric", {})
        if set(metric) != {"uri"} or not metric.get("uri"):
            raise RuntimeError(f"Unexpected Prometheus labels for {name}")
        uri = metric["uri"]
        if uri in values:
            raise RuntimeError(f"Prometheus returned duplicate URI series for {name}: {uri}")
        values[uri] = metric_value(result, f"{name}[{uri}]")
    return values


def metric_values(summary, metric_name):
    metrics = summary.get("metrics")
    if not isinstance(metrics, dict) or metric_name not in metrics:
        raise RuntimeError(f"k6 raw summary is missing metric: {metric_name}")
    values = metrics[metric_name].get("values")
    if not isinstance(values, dict):
        raise RuntimeError(f"k6 raw summary metric has no values: {metric_name}")
    return values


def exact_count(values, key, expected, name):
    actual = finite_number(values.get(key), f"k6 {name}.{key}")
    if actual != expected:
        raise RuntimeError(f"k6 {name}.{key} must be {expected}, observed {actual}")
    return actual


def validate_k6_artifacts():
    summary = read_json(K6_SUMMARY_PATH)
    raw_summary = read_json(K6_RAW_SUMMARY_PATH)

    if summary.get("ticket") != "TICKET-ADV097":
        raise RuntimeError("k6 summary has the wrong ticket identifier")
    if summary.get("vus") != 10 or summary.get("iterations") != 100:
        raise RuntimeError("k6 summary does not prove 10 VUs and 100 iterations")
    if summary.get("iterationsPerVu") != 10:
        raise RuntimeError("k6 summary does not prove 10 iterations per VU")
    if summary.get("measuredTradeRequests") != 100:
        raise RuntimeError("k6 summary does not prove 100 measured trade requests")
    if summary.get("successfulTradeCreations") != 100:
        raise RuntimeError("k6 summary does not prove 100 HTTP 201 trade creations")
    if summary.get("failedTradeRequests") != 0:
        raise RuntimeError("k6 summary reports failed trade requests")

    throughput = finite_number(summary.get("requestsPerSecond"), "k6 throughput")
    p95 = finite_number(summary.get("p95Milliseconds"), "k6 P95")
    duration_ms = finite_number(summary.get("runDurationMilliseconds"), "k6 run duration")
    run_finished = finite_number(
        summary.get("runFinishedAtEpochSeconds"), "k6 run end timestamp"
    )
    duration_seconds = duration_ms / 1000
    if duration_seconds < RATE_WINDOW_SECONDS or duration_seconds >= 75:
        raise RuntimeError(
            "k6 run duration must cover the complete 60-second panel window "
            f"(observed {duration_seconds:.3f}s)"
        )
    if p95 <= 0:
        raise RuntimeError("k6 P95 must be non-zero")

    exact_count(metric_values(raw_summary, "adv097_trade_requests"), "count", 100, "trade requests")
    exact_count(metric_values(raw_summary, "adv097_trade_created"), "count", 100, "trade creations")
    exact_count(metric_values(raw_summary, "adv097_trade_failures"), "count", 0, "trade failures")
    exact_count(metric_values(raw_summary, "http_reqs"), "count", 101, "HTTP requests")

    failed_rate = finite_number(
        metric_values(raw_summary, "http_req_failed").get("rate"),
        "k6 http_req_failed.rate",
    )
    check_rate = finite_number(
        metric_values(raw_summary, "checks").get("rate"),
        "k6 checks.rate",
    )
    if failed_rate != 0:
        raise RuntimeError(f"k6 http_req_failed.rate must be 0, observed {failed_rate}")
    if check_rate != 1:
        raise RuntimeError(f"k6 checks.rate must be 1, observed {check_rate}")

    return {
        "summary": summary,
        "rawSummary": raw_summary,
        "throughput": throughput,
        "p95Milliseconds": p95,
        "durationSeconds": duration_seconds,
        "runFinishedAtEpochSeconds": run_finished,
    }


def wait_for_baseline():
    deadline = time.monotonic() + 45
    last_error = None
    while time.monotonic() < deadline:
        try:
            payload = query(BASELINE_QUERY)
            value = single_counter_value(payload, "baseline", allow_zero_vector=True)
            return payload, value
        except Exception as error:  # noqa: BLE001 - retry while Prometheus starts
            last_error = error
            time.sleep(2)
    raise RuntimeError(f"Unable to capture finite Prometheus baseline: {last_error}")


def write_baseline():
    payload, value = wait_for_baseline()
    with BASELINE_PATH.open("w", encoding="utf-8") as output:
        json.dump(
            {
                "capturedAt": datetime.now(timezone.utc).isoformat(),
                "query": BASELINE_QUERY,
                "value": value,
                "rawResponse": payload,
            },
            output,
            indent=2,
        )
        output.write("\n")
    print(f"ADV097 Prometheus baseline captured: {value:.0f}")


def wait_for_exact_creation_delta(baseline):
    deadline = time.monotonic() + 60
    last_error = None
    final_payload = None
    final_value = None
    while time.monotonic() < deadline:
        try:
            final_payload = query(CREATION_COUNT_QUERY)
            final_value = single_counter_value(final_payload, "final creation count")
            delta = final_value - baseline
            if delta == 100:
                return final_payload, final_value, delta
            if delta > 100:
                raise RuntimeError(
                    f"Prometheus observed more than 100 isolated HTTP 201 creations: {delta}"
                )
            last_error = RuntimeError(f"creation delta is {delta}, waiting for 100")
        except RuntimeError as error:
            last_error = error
        time.sleep(2)
    raise RuntimeError(f"Prometheus did not observe exact creation delta 100: {last_error}")


def wait_for_panel_values(at_time):
    deadline = time.monotonic() + 45
    last_error = None
    while time.monotonic() < deadline:
        try:
            request_rate_payload = query(REQUEST_RATE_QUERY, at_time=at_time)
            p95_payload = query(P95_QUERY, at_time=at_time)
            request_rates = vector_values(request_rate_payload, "request rate")
            p95_values = vector_values(p95_payload, "P95")
            trade_rate = request_rates.get(TRADE_URI)
            trade_p95 = p95_values.get(TRADE_URI)
            if trade_rate is None or trade_rate <= 0:
                raise RuntimeError("Prometheus request-rate panel has no non-zero trade series")
            if trade_p95 is None or trade_p95 <= 0:
                raise RuntimeError("Prometheus P95 panel has no non-zero trade series")
            return request_rate_payload, p95_payload, request_rates, p95_values, trade_rate, trade_p95
        except Exception as error:  # noqa: BLE001 - retry while the scrape catches up
            last_error = error
            time.sleep(2)
    raise RuntimeError(f"Prometheus panel series did not become finite and non-zero: {last_error}")


def write_evidence():
    k6 = validate_k6_artifacts()
    baseline_document = read_json(BASELINE_PATH)
    baseline = finite_number(baseline_document.get("value"), "Prometheus baseline")
    if baseline < 0:
        raise RuntimeError(f"Prometheus baseline cannot be negative: {baseline}")

    creation_payload, final_value, creation_delta = wait_for_exact_creation_delta(baseline)
    if not math.isfinite(creation_delta) or creation_delta != 100:
        raise RuntimeError(f"Prometheus creation delta must be exactly 100: {creation_delta}")

    run_finished = k6["runFinishedAtEpochSeconds"]
    (
        request_rate_payload,
        p95_payload,
        request_rates,
        p95_values,
        trade_rate,
        trade_p95,
    ) = wait_for_panel_values(run_finished)

    throughput_delta = abs(k6["throughput"] - trade_rate)
    throughput_delta_percent = throughput_delta / k6["throughput"] * 100
    within_tolerance = throughput_delta_percent <= THROUGHPUT_TOLERANCE_PERCENT

    evidence = {
        "ticket": "TICKET-ADV097",
        "observedAt": datetime.now(timezone.utc).isoformat(),
        "tradeUri": TRADE_URI,
        "runWindow": {
            "panelWindowSeconds": RATE_WINDOW_SECONDS,
            "k6MeasuredDurationSeconds": k6["durationSeconds"],
            "endEpochSeconds": run_finished,
            "rateAndP95QueriesEvaluatedAtEpochSeconds": run_finished,
            "alignment": "ADV097 queries use the same 60-second window ending at the measured k6 run end.",
        },
        "k6": {
            "vus": 10,
            "iterations": 100,
            "measuredTradeRequests": 100,
            "successfulTradeCreations": 100,
            "failedTradeRequests": 0,
            "requestsPerSecond": k6["throughput"],
            "p95Milliseconds": k6["p95Milliseconds"],
            "runDurationSeconds": k6["durationSeconds"],
        },
        "requestRate": {
            "value": trade_rate,
            "unit": "requests/second",
            "window": "1m",
            "query": REQUEST_RATE_QUERY,
            "series": request_rates,
            "tradeUriValue": trade_rate,
            "endpointWideValue": sum(request_rates.values()),
        },
        "serverP95": {
            "valueMilliseconds": trade_p95,
            "unit": "milliseconds",
            "window": "1m",
            "query": P95_QUERY,
            "series": p95_values,
            "tradeUriValueMilliseconds": trade_p95,
        },
        "throughputComparison": {
            "k6TradeRequestsPerSecond": k6["throughput"],
            "prometheusTradeUriRequestsPerSecond": trade_rate,
            "absoluteDeltaRequestsPerSecond": throughput_delta,
            "percentageDelta": throughput_delta_percent,
            "tolerancePercent": THROUGHPUT_TOLERANCE_PERCENT,
            "withinTolerance": within_tolerance,
        },
        "prometheusCreationDelta": {
            "baseline": baseline,
            "final": final_value,
            "delta": creation_delta,
            "query": CREATION_COUNT_QUERY,
            "window": "isolated run pre/post counter delta",
        },
        "rawResponses": {
            "baseline": baseline_document.get("rawResponse"),
            "creationCount": creation_payload,
            "requestRate": request_rate_payload,
            "p95": p95_payload,
        },
    }

    evidence_path = RESULTS_DIR / "prometheus-query-evidence.json"
    with evidence_path.open("w", encoding="utf-8") as output:
        json.dump(evidence, output, indent=2)
        output.write("\n")

    print("ADV097 Prometheus panel-query evidence")
    print(f"HTTP 201 creations observed: {creation_delta:.0f}/100")
    print(f"request-rate panel trade series: {trade_rate} requests/second (1m rate)")
    print(f"server P95 panel trade series: {trade_p95} milliseconds (1m histogram)")
    print(
        "throughput delta: "
        f"{throughput_delta} requests/second ({throughput_delta_percent:.2f}%, "
        f"tolerance {THROUGHPUT_TOLERANCE_PERCENT}%)"
    )
    if not within_tolerance:
        raise SystemExit(
            "Prometheus/Grafana-facing throughput differs from k6 beyond the "
            f"{THROUGHPUT_TOLERANCE_PERCENT}% tolerance"
        )


phase = os.environ.get("PROMETHEUS_PHASE", "final")
if phase == "baseline":
    write_baseline()
elif phase == "final":
    write_evidence()
else:
    raise SystemExit(f"Unsupported PROMETHEUS_PHASE: {phase}")
