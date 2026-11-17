"""Standard-library regression checks for frame-budget diagnostics."""
import importlib.util
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("frame_budget_report.py")
SPEC = importlib.util.spec_from_file_location("frame_budget_report", SCRIPT)
REPORT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(REPORT)


class FrameBudgetReportTests(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.addCleanup(self.folder.cleanup)
        self.path = Path(self.folder.name) / "frames.csv"

    def write(self, data):
        self.path.write_text(data, encoding="utf-8")
        return self.path

    def test_nearest_rank_and_strict_hitch_thresholds(self):
        rows = REPORT.analyze([self.write(
            "mode,phase,trial,frame_ms\nsolo,walking,1,20\nsolo,walking,1,33.3\n"
            "solo,walking,1,50\nsolo,walking,1,100\nsolo,walking,1,127.349\n")])
        self.assertEqual(len(rows), 1)
        self.assertEqual(rows[0]["p50_ms"], 50)
        self.assertEqual(rows[0]["p95_ms"], 127.349)
        self.assertEqual(rows[0]["over_33_3_ms"], 3)
        self.assertEqual(rows[0]["over_50_ms"], 2)
        self.assertEqual(rows[0]["over_100_ms"], 1)

    def test_per_trial_exposes_outlier_and_preserves_pooled_mode(self):
        data = "mode,phase,trial,frame_ms\n" + (
            "solo,walk,good,20\n" * 30 + "solo,walk,bad,127\n")
        self.write(data)
        pooled = REPORT.analyze([self.path])
        individual = REPORT.analyze([self.path], per_trial=True)
        self.assertEqual(pooled[0]["p95_ms"], 20)
        self.assertEqual(len(individual), 2)
        self.assertEqual(individual[0]["p95_ms"], 127)
        self.assertEqual(individual[1]["p95_ms"], 20)

    def test_rejects_invalid_values_and_missing_trial(self):
        for value in ("0", "-1", "nan", "inf", "not-a-number", ""):
            with self.subTest(value=value), self.assertRaises(ValueError):
                REPORT.analyze([self.write("frame_ms\n" + value + "\n")])
        with self.assertRaisesRegex(ValueError, "trial column"):
            REPORT.analyze([self.write("frame_ms\n22\n")], per_trial=True)
        with self.assertRaisesRegex(ValueError, "blank trial"):
            REPORT.analyze([self.write("trial,frame_ms\n,22\n")], per_trial=True)

    def test_custom_column_and_bom(self):
        self.path.write_text("mode,phase,trial,gap_ms\nsolo,idle,A,21.5\n", encoding="utf-8-sig")
        self.assertEqual(REPORT.analyze([self.path], column="gap_ms")[0]["p50_ms"], 21.5)

    def test_same_trial_name_from_different_files_not_merged(self):
        a = self.write("mode,phase,trial,frame_ms\nsolo,idle,1,20\n")
        b = Path(self.folder.name) / "other.csv"
        b.write_text("mode,phase,trial,frame_ms\nsolo,idle,1,90\n")
        rows = REPORT.analyze([a, b], per_trial=True)
        self.assertEqual([r["p50_ms"] for r in rows], [20, 90])


if __name__ == "__main__":
    unittest.main()
