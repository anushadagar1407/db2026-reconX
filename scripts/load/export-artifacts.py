import json
import math
import shutil
from pathlib import Path


RESULTS_DIR = Path("/results")
EXPORT_DIR = Path("/export")
REQUIRED_FILES = (
    "k6-summary.json",
    "k6-raw-summary.json",
    "prometheus-query-evidence.json",
)


def read_json(name):
    path = RESULTS_DIR / name
    if not path.is_file() or path.stat().st_size == 0:
        raise SystemExit(f"Missing or empty load artifact: {name}")
    try:
        with path.open(encoding="utf-8") as source:
            return json.load(source)
    except json.JSONDecodeError as error:
        raise SystemExit(f"Load artifact is not parseable JSON: {name}") from error


def finite(value, name):
    try:
        number = float(value)
    except (TypeError, ValueError) as error:
        raise SystemExit(f"{name} is not numeric") from error
    if not math.isfinite(number):
        raise SystemExit(f"{name} is not finite")
    return number


def metric_values(raw_summary, name):
    try:
        metric = raw_summary["metrics"][name]
    except (KeyError, TypeError) as error:
        raise SystemExit(f"k6 raw summary is missing metric: {name}") from error
    values = metric.get("values") if isinstance(metric, dict) else None
    if isinstance(values, dict) and values:
        return values
    if isinstance(metric, dict) and any(
        key in metric
        for key in ("count", "rate", "value", "passes", "fails", "p(95)")
    ):
        return metric
    if not isinstance(metric, dict):
        raise SystemExit(f"k6 raw summary metric has no values: {name}")
    raise SystemExit(f"k6 raw summary metric has no values: {name}")


def exact_count(raw_summary, metric_name, expected):
    observed = finite(metric_values(raw_summary, metric_name).get("count"), metric_name)
    if observed != expected:
        raise SystemExit(f"{metric_name}.count must be {expected}, observed {observed}")


summary = read_json("k6-summary.json")
raw_summary = read_json("k6-raw-summary.json")
evidence = read_json("prometheus-query-evidence.json")

if summary.get("ticket") != "TICKET-ADV097":
    raise SystemExit("k6 summary has the wrong ticket identifier")
if summary.get("vus") != 10 or summary.get("iterations") != 100:
    raise SystemExit("k6 summary does not prove 10 VUs and 100 iterations")
if summary.get("measuredTradeRequests") != 100:
    raise SystemExit("k6 summary does not prove 100 measured trade requests")
if summary.get("successfulTradeCreations") != 100:
    raise SystemExit("k6 summary does not prove 100 HTTP 201 creations")
if summary.get("failedTradeRequests") != 0:
    raise SystemExit("k6 summary reports failed trade requests")
if finite(summary.get("requestsPerSecond"), "k6 throughput") <= 0:
    raise SystemExit("k6 throughput must be positive")
if finite(summary.get("p95Milliseconds"), "k6 P95") <= 0:
    raise SystemExit("k6 P95 must be positive")

exact_count(raw_summary, "adv097_trade_requests", 100)
exact_count(raw_summary, "adv097_trade_created", 100)
exact_count(raw_summary, "adv097_trade_failures", 0)
exact_count(raw_summary, "http_reqs", 101)
raw_trade_values = metric_values(raw_summary, "adv097_trade_requests")
raw_duration_values = metric_values(raw_summary, "adv097_trade_duration")
if not math.isclose(
    finite(summary.get("requestsPerSecond"), "k6 summary throughput"),
    finite(raw_trade_values.get("rate"), "k6 raw throughput"),
    rel_tol=1e-9,
    abs_tol=1e-9,
):
    raise SystemExit("k6 summary throughput is inconsistent with the raw summary")
if not math.isclose(
    finite(summary.get("p95Milliseconds"), "k6 summary P95"),
    finite(raw_duration_values.get("p(95)"), "k6 raw P95"),
    rel_tol=1e-9,
    abs_tol=1e-9,
):
    raise SystemExit("k6 summary P95 is inconsistent with the raw summary")
failed_metric = metric_values(raw_summary, "http_req_failed")
failed_rate = finite(
    failed_metric.get("rate", failed_metric.get("value")), "k6 failure rate"
)
failed_samples = finite(failed_metric.get("passes"), "k6 failed samples")
check_metric = metric_values(raw_summary, "checks")
check_rate = finite(check_metric.get("rate", check_metric.get("value")), "k6 check rate")
check_failures = finite(check_metric.get("fails"), "k6 check failure count")
if failed_rate != 0 or failed_samples != 0:
    raise SystemExit("k6 http_req_failed must prove zero failures")
if check_rate != 1 or check_failures != 0:
    raise SystemExit("k6 checks must prove every check passed")

creation_delta = evidence.get("prometheusCreationDelta", {}).get("delta")
if finite(creation_delta, "Prometheus creation delta") != 100:
    raise SystemExit("Prometheus creation delta must be exactly 100")
business_delta = evidence.get("prometheusTradeCreatedDelta", {}).get("delta")
if finite(business_delta, "Prometheus trade_created_total delta") != 100:
    raise SystemExit("Prometheus trade_created_total delta must be exactly 100")
comparison = evidence.get("throughputComparison", {})
if comparison.get("withinTolerance") is not True:
    raise SystemExit("Prometheus throughput comparison is outside the enforced tolerance")
if finite(evidence.get("tradeCreatedRate", {}).get("value"), "ADV089 rate") <= 0:
    raise SystemExit("ADV089 trade_created_total rate must be positive")

EXPORT_DIR.mkdir(parents=True, exist_ok=True)
for name in REQUIRED_FILES:
    if name == "k6-raw-summary.json":
        sanitized_raw_summary = dict(raw_summary)
        sanitized_raw_summary.pop("setup_data", None)
        serialized_raw_summary = json.dumps(sanitized_raw_summary, indent=2) + "\n"
        if '"token"' in serialized_raw_summary or '"password"' in serialized_raw_summary:
            raise SystemExit("Refusing to export a k6 summary containing credentials")
        (EXPORT_DIR / name).write_text(serialized_raw_summary, encoding="utf-8")
    else:
        shutil.copyfile(RESULTS_DIR / name, EXPORT_DIR / name)

for name in REQUIRED_FILES:
    path = EXPORT_DIR / name
    if not path.is_file() or path.stat().st_size == 0:
        raise SystemExit(f"Exported load artifact is missing or empty: {name}")
    try:
        with path.open(encoding="utf-8") as source:
            exported = json.load(source)
    except json.JSONDecodeError as error:
        raise SystemExit(f"Exported load artifact is not parseable JSON: {name}") from error
    serialized = json.dumps(exported)
    if '"token"' in serialized or 'trader123' in serialized:
        raise SystemExit(f"Refusing credential-bearing exported artifact: {name}")

print("ADV097 load artifacts validated and exported")
