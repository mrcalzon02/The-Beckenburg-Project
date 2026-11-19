"""Regression checks for diagnostic benchmark comparison."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

SPEC = importlib.util.spec_from_file_location('compare_frame_budget', Path(__file__).with_name('compare_frame_budget.py'))
MOD = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MOD)


class CompareTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.a = Path(self.temp.name) / 'a.json'
        self.b = Path(self.temp.name) / 'b.json'

    def save(self, path, groups, **overrides):
        data = {'schema': 2, 'unit': 'ms', 'percentile': 'nearest-rank', 'release_gate': False,
                'per_trial': False, 'groups': groups}
        data.update(overrides)
        path.write_text(json.dumps(data), encoding='utf-8')

    def row(self, phase='walking', p95=31.5):
        return {'mode': 'solo', 'phase': phase, 'intervals': 100, 'p50_ms': 20,
                'p95_ms': p95, 'p99_ms': 55, 'max_ms': 127, 'over_100_ms': 1}

    def test_signed_delta_and_samples(self):
        self.save(self.a, [self.row(p95=40)])
        self.save(self.b, [self.row(p95=30)])
        result = MOD.compare(self.a, self.b)[0]
        self.assertEqual(result['delta_p95_ms'], -10)
        self.assertEqual(result['candidate_n'], 100)
        self.assertEqual(result['delta_over_100_ms'], 0)

    def test_rejects_mismatched_routes(self):
        self.save(self.a, [self.row()])
        self.save(self.b, [self.row(phase='idle')])
        with self.assertRaisesRegex(ValueError, 'mismatched groups'):
            MOD.compare(self.a, self.b)

    def test_rejects_per_trial_and_bad_values(self):
        self.save(self.a, [self.row()])
        self.save(self.b, [self.row()], per_trial=True)
        with self.assertRaisesRegex(ValueError, 'pooled reports'):
            MOD.compare(self.a, self.b)
        self.save(self.b, [dict(self.row(), p95_ms=float('nan'))])
        with self.assertRaisesRegex(ValueError, 'invalid p95'):
            MOD.compare(self.a, self.b)

    def test_rejects_duplicate_groups(self):
        self.save(self.a, [self.row(), self.row()])
        self.save(self.b, [self.row()])
        with self.assertRaisesRegex(ValueError, 'duplicate'):
            MOD.compare(self.a, self.b)


if __name__ == '__main__':
    unittest.main()
