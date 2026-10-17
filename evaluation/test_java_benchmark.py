import unittest, sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parent))
from selfplay_java_advisor import report,BASELINES
class TestPairedBenchmark(unittest.TestCase):
    def test_gate_must_reject_small_unproven_result(self):
        rows=[]
        for base in BASELINES:
            for deal in range(3):
                for seat in range(4):
                    for policy in ('java','control'):
                        rows.append(dict(deal=deal,baseline=base,seat=seat,policy=policy,rank=2,win=0,fallbacks=0))
        x=report(rows,3,312,50)
        self.assertFalse(x['ANY_RELEASE_PASS'])
        self.assertTrue(all(not b['passed_release_gate'] for b in x['baseline_results'].values()))

if __name__=='__main__':unittest.main()
