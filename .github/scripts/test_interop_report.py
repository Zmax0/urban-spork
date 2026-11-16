import json
from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET

from interop_report import MATRIX, badge_svg, build_report, configuration_cases


class InteropReportTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.context = {"commit": "abc123", "run_id": "42", "branch": "dev/test"}
        for protocol, (test_class, positive) in MATRIX.items():
            directory = self.root / f"interop-{protocol}"
            (directory / "failsafe-reports").mkdir(parents=True)
            (directory / "interop").mkdir()
            (directory / "interop/job.json").write_text(json.dumps(
                dict(self.context, docker="success", verify="success")))
            suite = ET.Element("testsuite", tests=str(positive + 2), failures="0", errors="0", skipped="0")
            scenarios = [case for cases in configuration_cases(protocol).values() for case in cases]
            for index, scenario in enumerate(scenarios):
                method = "rejectsInvalidCredentials(Role)" if scenario.endswith("invalid-credentials") else "forwards(InteropCase)"
                ET.SubElement(suite, "testcase", name=f"{method} {scenario}")
                case = directory / "interop" / protocol / str(index)
                case.mkdir(parents=True)
                (case / "metadata.json").write_text(json.dumps({
                    "commit": "abc123", "image": f"{protocol}@sha256:123"}))
                (case / "peer-version.txt").write_text("1.0\n")
            ET.ElementTree(suite).write(directory / "failsafe-reports" / f"TEST-com.urbanspork.test.interop.{test_class}.xml")

    def report(self, matrix_result="success"):
        return build_report(self.root, self.context, matrix_result)

    def change_xml(self, change, protocol="trojan"):
        path = next((self.root / f"interop-{protocol}/failsafe-reports").glob("TEST-*.xml"))
        tree = ET.parse(path)
        change(tree.getroot())
        tree.write(path)

    def test_complete_matrix_is_green(self):
        result = self.report()
        self.assertEqual((result["status"], result["passed"], result["expected"]), ("passed", 66, 66))
        self.assertIn("66/66 passing", badge_svg(result))
        self.assertIn("#4c1", badge_svg(result))

    def test_configuration_counts_partition_the_matrix(self):
        protocols = self.report()["protocols"]
        configurations = [counts for protocol in protocols.values() for counts in protocol["configurations"].values()]
        self.assertEqual(len(configurations), 15)
        self.assertEqual(sum(counts["passed"] for counts in configurations), 66)
        self.assertEqual(protocols["shadowsocks"]["configurations"]["aes-128-gcm"]["passed"], 6)
        self.assertEqual(protocols["vmess"]["configurations"]["aes-128-gcm-tcp"]["passed"], 6)
        self.assertEqual(protocols["trojan"]["configurations"]["tls"]["passed"], 6)

    def test_peer_version_changes_are_recorded_without_restricting_results(self):
        version = "upstream next-release+build.42\nBuild information"
        for path in self.root.glob("interop-*/interop/*/*/peer-version.txt"):
            path.write_text(version + "\n")
        result = self.report()
        self.assertEqual((result["status"], result["passed"]), ("passed", 66))
        for protocol, report in result["protocols"].items():
            self.assertEqual(report["peers"], [{
                "image": f"{protocol}@sha256:123", "actual_version": version}])

    def test_configuration_failure_does_not_turn_other_configurations_red(self):
        def fail(suite):
            suite.set("failures", "1")
            case = next(case for case in suite if case.get("name") == "forwards(InteropCase) server-chacha20-poly1305-wss-udp")
            ET.SubElement(case, "failure")
        self.change_xml(fail, "vmess")
        result = self.report("failure")
        counts = result["protocols"]["vmess"]["configurations"]
        self.assertEqual((counts["chacha20-poly1305-wss"]["status"], counts["chacha20-poly1305-wss"]["passed"]), ("failed", 3))
        self.assertEqual(counts["aes-128-gcm-tcp"]["status"], "passed")
        self.assertEqual(result["passed"], 65)

    def test_uppercase_role_in_credential_display_name_is_supported(self):
        self.change_xml(lambda suite: suite.findall("testcase")[-1].set("name", "rejectsInvalidCredentials(Role) SERVER-password-tls-tcp-invalid-credentials"))
        self.assertEqual(self.report()["status"], "passed")

    def test_test_failure_is_red(self):
        def fail(suite):
            suite.set("failures", "1")
            ET.SubElement(suite.find("testcase"), "failure")
        self.change_xml(fail)
        result = self.report("failure")
        self.assertEqual((result["status"], result["passed"]), ("failed", 65))
        self.assertIn("#e05d44", badge_svg(result))

    def test_skipped_case_is_incomplete(self):
        def skip(suite):
            suite.set("skipped", "1")
            ET.SubElement(suite.find("testcase"), "skipped")
        self.change_xml(skip)
        self.assertEqual(self.report()["status"], "incomplete")

    def test_flaky_pass_is_not_green(self):
        self.change_xml(lambda suite: ET.SubElement(suite.find("testcase"), "flakyFailure"))
        self.assertEqual((self.report()["status"], self.report()["passed"]), ("failed", 65))

    def test_missing_protocol_cannot_shrink_denominator(self):
        next((self.root / "interop-trojan/failsafe-reports").glob("TEST-*.xml")).unlink()
        result = self.report("failure")
        self.assertEqual((result["status"], result["passed"], result["expected"]), ("incomplete", 60, 66))

    def test_zero_tests_is_incomplete(self):
        self.change_xml(lambda suite: suite.clear())
        self.assertEqual(self.report()["status"], "incomplete")

    def test_duplicate_cases_are_incomplete(self):
        self.change_xml(lambda suite: suite.findall("testcase")[1].set("name", suite.find("testcase").get("name")))
        self.assertEqual(self.report()["status"], "incomplete")

    def test_wrong_credential_cases_are_required(self):
        self.change_xml(lambda suite: suite.findall("testcase")[-1].set("name", "forwards(InteropCase) server-password-tls-tcp-extra"))
        self.assertEqual(self.report()["status"], "incomplete")

    def test_wrong_transport_cannot_replace_a_required_case(self):
        self.change_xml(lambda suite: suite.find("testcase").set("name", "forwards(InteropCase) client-aes-128-gcm-quic-tcp"), "vmess")
        self.assertEqual(self.report()["status"], "incomplete")

    def test_stale_artifact_is_incomplete(self):
        self.context["commit"] = "different"
        self.assertEqual(self.report()["status"], "incomplete")

    def test_image_startup_failure_is_incomplete(self):
        (self.root / "interop-trojan/interop/trojan/0/peer-version.txt").unlink()
        self.assertEqual(self.report("failure")["status"], "incomplete")

    def test_failed_job_with_passing_xml_is_incomplete(self):
        self.assertEqual(self.report("failure")["status"], "incomplete")

    def test_docker_failure_is_incomplete(self):
        path = self.root / "interop-trojan/interop/job.json"
        job = json.loads(path.read_text())
        job.update(docker="failure", verify="skipped")
        path.write_text(json.dumps(job))
        self.assertEqual(self.report("failure")["status"], "incomplete")

    def test_truncated_xml_is_incomplete(self):
        next((self.root / "interop-trojan/failsafe-reports").glob("TEST-*.xml")).write_text("<testsuite")
        self.assertEqual(self.report()["status"], "incomplete")

    def test_suite_totals_must_match_cases(self):
        self.change_xml(lambda suite: suite.set("tests", "0"))
        self.assertEqual(self.report()["status"], "incomplete")

    def test_svg_escapes_branch_names(self):
        self.context["branch"] = 'dev/a&b"c'
        svg = ET.fromstring(badge_svg(self.report()))
        self.assertIn('dev/a&b"c', svg.attrib["aria-label"])


if __name__ == "__main__":
    unittest.main()
