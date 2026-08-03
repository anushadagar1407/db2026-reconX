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
        values = raw_summary["metrics"][name]["values"]
    except (KeyError, TypeError) as error:
        raise SystemExit(f"k6 raw summary is missing metric: {name}") from error
    if not isinstance(values, dict):
        raise SystemExit(f"k6 raw summary metric has no values: {name}")
    return values


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
finite(summary.get("requestsPerSecond"), "k6 throughput")
finite(summary.get("p95Milliseconds"), "k6 P95")

exact_count(raw_summary, "adv097_trade_requests", 100)
exact_count(raw_summary, "adv097_trade_created", 100)
exact_count(raw_summary, "adv097_trade_failures", 0)
exact_count(raw_summary, "http_reqs", 101)
if finite(metric_values(raw_summary, "http_req_failed").get("rate"), "k6 failure rate") != 0:
    raise SystemExit("k6 http_req_failed.rate must be 0")
if finite(metric_values(raw_summary, "checks").get("rate"), "k6 check rate") != 1:
    raise SystemExit("k6 checks.rate must be 1")

creation_delta = evidence.get("prometheusCreationDelta", {}).get("delta")
if finite(creation_delta, "Prometheus creation delta") != 100:
    raise SystemExit("Prometheus creation delta must be exactly 100")
comparison = evidence.get("throughputComparison", {})
if comparison.get("withinTolerance") is not True:
    raise SystemExit("Prometheus throughput comparison is outside the enforced tolerance")

EXPORT_DIR.mkdir(parents=True, exist_ok=True)
for name in REQUIRED_FILES:
    shutil.copyfile(RESULTS_DIR / name, EXPORT_DIR / name)

print("ADV097 load artifacts validated and exported")
