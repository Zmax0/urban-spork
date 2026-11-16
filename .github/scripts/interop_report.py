"""Render the fixed interoperability matrix from this run's Failsafe artifacts."""

import argparse
from datetime import datetime, timezone
from html import escape
import json
import os
from pathlib import Path
import xml.etree.ElementTree as ET


MATRIX = {
    "shadowsocks": ("ShadowsocksInteroperabilityIntegrationTest", 24),
    "vmess": ("VMessInteroperabilityIntegrationTest", 32),
    "trojan": ("TrojanInteroperabilityIntegrationTest", 4),
}
COUNTS = ("tests", "passed", "failures", "errors", "skipped", "flaky")


def configuration_cases(protocol):
    if protocol == "shadowsocks":
        configurations = {
            cipher: [(cipher, traffic, traffic) for traffic in ("tcp", "udp")]
            for cipher in ("aes-128-gcm", "aes-256-gcm", "chacha20-poly1305",
                           "2022-blake3-aes-128-gcm", "2022-blake3-aes-256-gcm",
                           "2022-blake3-chacha20-poly1305")
        }
    elif protocol == "vmess":
        configurations = {
            f"{cipher}-{carrier}": [(cipher, carrier, traffic) for traffic in ("tcp", "udp")]
            for cipher in ("aes-128-gcm", "chacha20-poly1305")
            for carrier in ("tcp", "tls", "ws", "wss")
        }
    else:
        configurations = {"tls": [("password", "tls", traffic) for traffic in ("tcp", "udp")]}
    cases = {
        name: [f"{role}-{cipher}-{carrier}-{traffic}"
               for role in ("client", "server") for cipher, carrier, traffic in combinations]
        for name, combinations in configurations.items()
    }
    baseline = next(iter(cases))
    cipher, carrier, traffic = configurations[baseline][0]
    cases[baseline].extend(f"{role}-{cipher}-{carrier}-{traffic}-invalid-credentials"
                           for role in ("client", "server"))
    return cases


def case_outcome(case):
    tags = {child.tag for child in case}
    if "error" in tags:
        return "errors"
    if "failure" in tags:
        return "failures"
    if "skipped" in tags:
        return "skipped"
    if tags & {"flakyFailure", "flakyError", "rerunFailure", "rerunError"}:
        return "flaky"
    return "passed"


def result_status(result, incomplete=False):
    if incomplete or result["tests"] != result["expected"] or result["skipped"]:
        return "incomplete"
    return "failed" if result["failures"] + result["errors"] + result["flaky"] else "passed"


def protocol_result(directory, protocol, test_class, positive, commit, run_id):
    result = {key: 0 for key in COUNTS}
    selections = configuration_cases(protocol)
    configurations = {
        name: dict.fromkeys(COUNTS, 0) | {"expected": len(cases)}
        for name, cases in selections.items()
    }
    case_configuration = {case: name for name, cases in selections.items() for case in cases}
    result.update(expected=positive + 2, issues=[], peers=[], configurations=configurations)
    issues = result["issues"]
    try:
        job = json.loads((directory / "interop/job.json").read_text())
        result["job"] = job
        if job["commit"] != commit or job["run_id"] != run_id:
            issues.append("Job report belongs to a different commit or run")
        if job["docker"] != "success":
            issues.append("Docker verification did not succeed")
        if job["verify"] not in ("success", "failure"):
            issues.append("Maven verification did not run to completion")

        report = directory / "failsafe-reports" / f"TEST-com.urbanspork.test.interop.{test_class}.xml"
        suite = ET.parse(report).getroot()
        cases = suite.findall("testcase")
        result["tests"] = len(cases)
        names = [case.attrib["name"].partition(" ")[2].lower() for case in cases]
        if set(names) != set(case_configuration) or len(names) != len(set(names)):
            issues.append("Case selection does not match the positive and wrong-credential matrix")
        for case, name in zip(cases, names):
            outcome = case_outcome(case)
            result[outcome] += 1
            if name in case_configuration:
                configuration = configurations[case_configuration[name]]
                configuration["tests"] += 1
                configuration[outcome] += 1
        for key in ("tests", "failures", "errors", "skipped"):
            if int(suite.attrib[key]) != result[key]:
                issues.append(f"JUnit {key} total disagrees with its test cases")
        if result["skipped"]:
            issues.append("Some cases were skipped")
        if result["passed"] == result["expected"] and job["verify"] != "success":
            issues.append("Tests passed but the Maven build failed")

        metadata_files = sorted((directory / "interop").glob("*/*/metadata.json"))
        if len(metadata_files) != result["expected"]:
            issues.append("Case diagnostics are missing")
        peers = {}
        for path in metadata_files:
            metadata = json.loads(path.read_text())
            if metadata["commit"] != commit:
                issues.append(f"Stale diagnostics: {path.parent.name}")
            version = path.with_name("peer-version.txt")
            if not version.is_file() or not version.read_text().strip():
                issues.append(f"Reference peer did not become ready: {path.parent.name}")
                continue
            peers[metadata["image"]] = {
                "image": metadata["image"],
                "actual_version": version.read_text().strip(),
            }
        result["peers"] = [peers[image] for image in sorted(peers)]
    except (OSError, ValueError, KeyError, ET.ParseError) as error:
        issues.append(f"Missing or invalid report: {error}")

    result["status"] = result_status(result, bool(issues))
    for configuration in configurations.values():
        configuration["status"] = result_status(configuration, bool(issues))
    return result


def build_report(artifacts, context, matrix_result):
    protocols = {
        name: protocol_result(artifacts / f"interop-{name}", name, test_class, positive,
                              context["commit"], context["run_id"])
        for name, (test_class, positive) in MATRIX.items()
    }
    result = dict(context, protocols=protocols, expected=66, issues=[])
    result.update({key: sum(protocol[key] for protocol in protocols.values()) for key in COUNTS})
    states = {protocol["status"] for protocol in protocols.values()}
    if "incomplete" in states or (states == {"passed"} and matrix_result != "success"):
        result["status"] = "incomplete"
        if matrix_result != "success":
            result["issues"].append("Protocol jobs or report download did not all succeed")
    else:
        result["status"] = "failed" if "failed" in states else "passed"
    return result


def badge_svg(result):
    status = result["status"]
    message = "incomplete" if status == "incomplete" else f'{result["passed"]}/{result["expected"]} passing'
    color = {"passed": "#4c1", "failed": "#e05d44", "incomplete": "#fe7d37"}[status]
    scope = result.get("configuration", "all protocols")
    title = escape(f'interop {scope}: {message}; {result["branch"]}; {result["commit"][:12]}')
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="168" height="20" role="img" aria-label="{title}">
  <title>{title}</title>
  <linearGradient id="s" x2="0" y2="100%"><stop offset="0" stop-color="#bbb" stop-opacity=".1"/><stop offset="1" stop-opacity=".1"/></linearGradient>
  <clipPath id="r"><rect width="168" height="20" rx="3" fill="#fff"/></clipPath>
  <g clip-path="url(#r)"><path fill="#555" d="M0 0h52v20H0z"/><path fill="{color}" d="M52 0h116v20H52z"/><path fill="url(#s)" d="M0 0h168v20H0z"/></g>
  <g fill="#fff" text-anchor="middle" font-family="Verdana,Geneva,DejaVu Sans,sans-serif" font-size="11">
    <text x="26" y="15" fill="#010101" fill-opacity=".3">interop</text><text x="26" y="14">interop</text>
    <text x="110" y="15" fill="#010101" fill-opacity=".3">{message}</text><text x="110" y="14">{message}</text>
  </g>
</svg>
'''


def summary_markdown(result):
    lines = ["## Protocol interoperability", "",
             f'**{result["status"]}: {result["passed"]}/{result["expected"]} passing** '
             "(60 forwarding cases + 6 wrong-credential cases).", "",
             f'Branch: `{result["branch"]}` · Commit: `{result["commit"]}` · [Run]({result["run_url"]})', "",
             "| Protocol | Passed / expected | Failures | Errors | Skipped | Flaky | Status |",
             "| --- | ---: | ---: | ---: | ---: | ---: | --- |"]
    for name, protocol in result["protocols"].items():
        lines.append(f'| {name} | {protocol["passed"]}/{protocol["expected"]} | {protocol["failures"]} | '
                     f'{protocol["errors"]} | {protocol["skipped"]} | {protocol["flaky"]} | {protocol["status"]} |')
    lines.extend(["", "| Protocol | Configuration | Passed / expected | Status |",
                  "| --- | --- | ---: | --- |"])
    for name, protocol in result["protocols"].items():
        for configuration, counts in protocol["configurations"].items():
            lines.append(f'| {name} | {configuration} | {counts["passed"]}/{counts["expected"]} | {counts["status"]} |')
    lines.extend(["", "VMess reference UDP covers 64/1024 bytes and consecutive datagrams; "
                  "4096-byte VMess UDP is covered by the regular urban-spork regression tests.", ""])
    for issue in result["issues"]:
        lines.append(f"- {issue}")
    for name, protocol in result["protocols"].items():
        for issue in protocol["issues"]:
            lines.append(f"- {name}: {issue}")
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--artifacts", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    context = {
        "branch": os.environ["GITHUB_REF_NAME"],
        "commit": os.environ["GITHUB_SHA"],
        "run_id": os.environ["GITHUB_RUN_ID"],
        "run_number": int(os.environ["GITHUB_RUN_NUMBER"]),
        "run_attempt": int(os.environ["GITHUB_RUN_ATTEMPT"]),
        "run_url": f'{os.environ["GITHUB_SERVER_URL"]}/{os.environ["GITHUB_REPOSITORY"]}/actions/runs/{os.environ["GITHUB_RUN_ID"]}',
        "generated_at": datetime.now(timezone.utc).isoformat(),
    }
    result = build_report(args.artifacts, context, os.environ["MATRIX_RESULT"])
    args.output.mkdir(parents=True, exist_ok=True)
    (args.output / "result.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    (args.output / "badge.svg").write_text(badge_svg(result), encoding="utf-8")
    (args.output / "configurations").mkdir(exist_ok=True)
    for name, protocol in result["protocols"].items():
        for configuration, counts in protocol["configurations"].items():
            badge = dict(context, **counts, configuration=f"{name}/{configuration}")
            (args.output / "configurations" / f"{name}-{configuration}.svg").write_text(badge_svg(badge), encoding="utf-8")
    (args.output / "summary.md").write_text(summary_markdown(result), encoding="utf-8")
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
        output.write(f'status={result["status"]}\n')


if __name__ == "__main__":
    main()
