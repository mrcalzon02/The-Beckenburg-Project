"""Regression checks for callback-to-distinct-GL-frame attribution."""
import tempfile
import unittest
from pathlib import Path

from callback_gap_audit import analyze

HEADER = "mode,trial,phase,completion_ns,composited_gl_frame_id,post_geometry_ns\n"


class CallbackGapTests(unittest.TestCase):
    def setUp(self):
        folder = tempfile.TemporaryDirectory()
        self.addCleanup(folder.cleanup)
        self.path = Path(folder.name) / "callbacks.csv"

    def check(self, data):
        self.path.write_text(HEADER + data, encoding="utf-8")
        return analyze([self.path])

    def test_repeated_id_reproduces_127_ms_gap(self):
        gaps = self.check(
            "tcp,1,walking,1000000,1420,100000\n"
            "tcp,1,walking,60000000,1420,48560000\n"
            "tcp,1,walking,128349000,1422,56679000\n")
        self.assertEqual(len(gaps), 1)
        self.assertEqual(gaps[0]["gap_ms"], 127.349)
        self.assertEqual(gaps[0]["callbacks"], 2)
        self.assertEqual(gaps[0]["post_geometry_ms"], 105.239)
        self.assertTrue(gaps[0]["over_100_ms"])

    def test_trial_and_phase_isolation(self):
        gaps = self.check(
            "solo,A,idle,1,10,3\nsolo,A,idle,30000001,11,4\n"
            "solo,A,walking,2,11,3\nsolo,A,walking,20000002,12,4\n"
            "solo,B,idle,1,10,2\nsolo,B,idle,20000001,11,3\n")
        self.assertEqual(sorted(x["gap_ms"] for x in gaps), [20.0, 20.0, 30.0])

    def test_rejects_regressions_and_bad_values(self):
        for data in (
            "solo,A,idle,1,3,2\nsolo,A,idle,2,2,2\n",
            "solo,A,idle,3,3,2\nsolo,A,idle,2,4,2\n",
            "solo,A,idle,1,3,-2\n",
            "solo,,idle,1,3,2\n",
        ):
            with self.subTest(data=data), self.assertRaises(ValueError):
                self.check(data)

    def test_negative_frame_id_not_counted(self):
        gaps = self.check(
            "solo,A,idle,1,-1,2\nsolo,A,idle,10,3,3\n"
            "solo,A,idle,20000010,4,500000\n")
        self.assertEqual(len(gaps), 1)
        self.assertEqual(gaps[0]["callbacks"], 1)
        self.assertEqual(gaps[0]["gap_ms"], 20.0)


if __name__ == "__main__":
    unittest.main()
