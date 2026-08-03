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
MAX_SAMPLE_AGE_SECONDS = 15
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
BUSINESS_COUNTER_QUERY = "trade_created_total"
BUSINESS_RATE_QUERY = "sum(rate(trade_created_total[1m]))"
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


def metric_value(result, name, evaluated_at=None, allow_non_finite=False):
    values = result.get("value")
    if not isinstance(values, list) or len(values) != 2:
        raise RuntimeError(f"Prometheus result has no scalar value for {name}")
    sample_time = finite_number(values[0], f"{name} sample timestamp")
    reference_time = time.time() if evaluated_at is None else evaluated_at
    sample_age = reference_time - sample_time
    if sample_age < -2 or sample_age > MAX_SAMPLE_AGE_SECONDS:
        raise RuntimeError(
            f"Prometheus result is stale for {name}: sample age {sample_age:.3f}s"
        )
    try:
        return finite_number(values[1], name)
    except RuntimeError:
        if allow_non_finite:
            return None
        raise


def single_http_counter_value(payload, name, allow_zero_vector=False):
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


def single_business_counter_value(payload, name):
    results = payload.get("data", {}).get("result", [])
    if len(results) != 1:
        raise RuntimeError(f"Expected exactly one trade_created_total series for {name}")
    metric = results[0].get("metric", {})
    if not isinstance(metric, dict) or metric.get("__name__") != "trade_created_total":
        raise RuntimeError(f"Unexpected Prometheus business-counter labels for {name}")
    return metric_value(results[0], name)


def single_expression_value(payload, name, evaluated_at):
    results = payload.get("data", {}).get("result", [])
    if len(results) != 1 or results[0].get("metric") != {}:
        raise RuntimeError(f"Expected exactly one unlabeled Prometheus series for {name}")
    return metric_value(results[0], name, evaluated_at)


def vector_values(payload, name, evaluated_at, allow_non_finite=False):
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
        values[uri] = metric_value(
            result,
            f"{name}[{uri}]",
            evaluated_at,
            allow_non_finite=allow_non_finite,
        )
    return values


def metric_values(summary, metric_name):
    metrics = summary.get("metrics")
    if not isinstance(metrics, dict) or metric_name not in metrics:
        raise RuntimeError(f"k6 raw summary is missing metric: {metric_name}")
    metric = metrics[metric_name]
    values = metric.get("values") if isinstance(metric, dict) else None
    if isinstance(values, dict) and values:
        return values
    if isinstance(metric, dict) and any(
        key in metric
        for key in ("count", "rate", "value", "passes", "fails", "p(95)")
    ):
        return metric
    if not isinstance(metric, dict):
        raise RuntimeError(f"k6 raw summary metric has no values: {metric_name}")
    raise RuntimeError(f"k6 raw summary metric has no values: {metric_name}")


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

    failed_metric = metric_values(raw_summary, "http_req_failed")
    failed_rate = finite_number(
        failed_metric.get("rate", failed_metric.get("value")),
        "k6 http_req_failed rate",
    )
    failed_samples = finite_number(failed_metric.get("passes"), "k6 failed samples")
    check_metric = metric_values(raw_summary, "checks")
    check_rate = finite_number(
        check_metric.get("rate", check_metric.get("value")),
        "k6 checks rate",
    )
    check_failures = finite_number(check_metric.get("fails"), "k6 checks fails")
    if failed_rate != 0 or failed_samples != 0:
        raise RuntimeError(
            "k6 http_req_failed must prove zero failures "
            f"(rate={failed_rate}, failedSamples={failed_samples})"
        )
    if check_rate != 1 or check_failures != 0:
        raise RuntimeError(
            "k6 checks must prove every check passed "
            f"(rate={check_rate}, fails={check_failures})"
        )

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
            http_payload = query(BASELINE_QUERY)
            business_payload = query(BUSINESS_COUNTER_QUERY)
            http_value = single_http_counter_value(
                http_payload, "HTTP 201 baseline", allow_zero_vector=True
            )
            business_value = single_business_counter_value(
                business_payload, "trade_created_total baseline"
            )
            return http_payload, http_value, business_payload, business_value
        except Exception as error:  # noqa: BLE001 - retry while Prometheus starts
            last_error = error
            time.sleep(2)
    raise RuntimeError(f"Unable to capture finite Prometheus baseline: {last_error}")


def write_baseline():
    http_payload, http_value, business_payload, business_value = wait_for_baseline()
    with BASELINE_PATH.open("w", encoding="utf-8") as output:
        json.dump(
            {
                "capturedAt": datetime.now(timezone.utc).isoformat(),
                "http201Creation": {
                    "query": BASELINE_QUERY,
                    "value": http_value,
                    "rawResponse": http_payload,
                },
                "tradeCreated": {
                    "query": BUSINESS_COUNTER_QUERY,
                    "value": business_value,
                    "rawResponse": business_payload,
                },
            },
            output,
            indent=2,
        )
        output.write("\n")
    print(
        "ADV097 Prometheus baselines captured: "
        f"HTTP 201={http_value:.0f}, trade_created_total={business_value:.0f}"
    )


def wait_for_exact_creation_delta(baseline):
    deadline = time.monotonic() + 60
    last_error = None
    final_payload = None
    final_value = None
    while time.monotonic() < deadline:
        try:
            final_payload = query(CREATION_COUNT_QUERY)
            final_value = single_http_counter_value(final_payload, "final HTTP 201 count")
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


def wait_for_exact_business_delta(baseline):
    deadline = time.monotonic() + 60
    last_error = None
    while time.monotonic() < deadline:
        try:
            payload = query(BUSINESS_COUNTER_QUERY)
            final_value = single_business_counter_value(
                payload, "final trade_created_total count"
            )
            delta = final_value - baseline
            if delta == 100:
                return payload, final_value, delta
            if delta > 100:
                raise RuntimeError(
                    f"Prometheus observed more than 100 trade_created_total increments: {delta}"
                )
            last_error = RuntimeError(f"business-counter delta is {delta}, waiting for 100")
        except RuntimeError as error:
            last_error = error
        time.sleep(2)
    raise RuntimeError(
        f"Prometheus did not observe exact trade_created_total delta 100: {last_error}"
    )


def wait_for_panel_values(at_time):
    deadline = time.monotonic() + 45
    last_error = None
    while time.monotonic() < deadline:
        try:
            request_rate_payload = query(REQUEST_RATE_QUERY, at_time=at_time)
            p95_payload = query(P95_QUERY, at_time=at_time)
            business_rate_payload = query(BUSINESS_RATE_QUERY, at_time=at_time)
            request_rates = vector_values(request_rate_payload, "request rate", at_time)
            p95_values = vector_values(
                p95_payload, "P95", at_time, allow_non_finite=True
            )
            trade_rate = request_rates.get(TRADE_URI)
            trade_p95 = p95_values.get(TRADE_URI)
            business_rate = single_expression_value(
                business_rate_payload, "trade_created_total rate", at_time
            )
            if trade_rate is None or trade_rate <= 0:
                raise RuntimeError("Prometheus request-rate panel has no non-zero trade series")
            if trade_p95 is None or trade_p95 <= 0:
                raise RuntimeError("Prometheus P95 panel has no non-zero trade series")
            if business_rate <= 0:
                raise RuntimeError("Prometheus ADV089 panel has no non-zero trade-created rate")
            return (
                request_rate_payload,
                p95_payload,
                business_rate_payload,
                request_rates,
                p95_values,
                trade_rate,
                trade_p95,
                business_rate,
            )
        except Exception as error:  # noqa: BLE001 - retry while the scrape catches up
            last_error = error
            time.sleep(2)
    raise RuntimeError(f"Prometheus panel series did not become finite and non-zero: {last_error}")


def write_evidence():
    k6 = validate_k6_artifacts()
    baseline_document = read_json(BASELINE_PATH)
    http_baseline_document = baseline_document.get("http201Creation", {})
    business_baseline_document = baseline_document.get("tradeCreated", {})
    http_baseline = finite_number(
        http_baseline_document.get("value"), "Prometheus HTTP 201 baseline"
    )
    business_baseline = finite_number(
        business_baseline_document.get("value"), "Prometheus trade_created_total baseline"
    )
    if http_baseline < 0 or business_baseline < 0:
        raise RuntimeError("Prometheus counter baselines cannot be negative")

    creation_payload, final_value, creation_delta = wait_for_exact_creation_delta(http_baseline)
    if not math.isfinite(creation_delta) or creation_delta != 100:
        raise RuntimeError(f"Prometheus creation delta must be exactly 100: {creation_delta}")
    business_payload, business_final, business_delta = wait_for_exact_business_delta(
        business_baseline
    )
    if not math.isfinite(business_delta) or business_delta != 100:
        raise RuntimeError(
            f"Prometheus trade_created_total delta must be exactly 100: {business_delta}"
        )

    run_finished = k6["runFinishedAtEpochSeconds"]
    (
        request_rate_payload,
        p95_payload,
        business_rate_payload,
        request_rates,
        p95_values,
        trade_rate,
        trade_p95,
        business_rate,
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
            "measurement": "server-side HTTP histogram latency; distinct from k6 client latency",
        },
        "tradeCreatedRate": {
            "value": business_rate,
            "unit": "trades/second",
            "window": "1m",
            "query": BUSINESS_RATE_QUERY,
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
            "baseline": http_baseline,
            "final": final_value,
            "delta": creation_delta,
            "query": CREATION_COUNT_QUERY,
            "window": "isolated run pre/post counter delta",
        },
        "prometheusTradeCreatedDelta": {
            "baseline": business_baseline,
            "final": business_final,
            "delta": business_delta,
            "query": BUSINESS_COUNTER_QUERY,
            "window": "isolated run pre/post counter delta",
        },
        "rawResponses": {
            "httpBaseline": http_baseline_document.get("rawResponse"),
            "tradeCreatedBaseline": business_baseline_document.get("rawResponse"),
            "creationCount": creation_payload,
            "tradeCreatedCount": business_payload,
            "requestRate": request_rate_payload,
            "p95": p95_payload,
            "tradeCreatedRate": business_rate_payload,
        },
    }

    evidence_path = RESULTS_DIR / "prometheus-query-evidence.json"
    with evidence_path.open("w", encoding="utf-8") as output:
        json.dump(evidence, output, indent=2)
        output.write("\n")

    print("ADV097 Prometheus panel-query evidence")
    print(f"HTTP 201 creations observed: {creation_delta:.0f}/100")
    print(f"trade_created_total increments observed: {business_delta:.0f}/100")
    print(f"request-rate panel trade series: {trade_rate} requests/second (1m rate)")
    print(f"server P95 panel trade series: {trade_p95} milliseconds (1m histogram)")
    print(f"ADV089 trade-created rate: {business_rate} trades/second (1m rate)")
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
