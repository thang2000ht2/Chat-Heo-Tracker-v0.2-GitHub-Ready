import csv,tempfile,unittest,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parent))
from calibration_check import evaluate
class Check(unittest.TestCase):
 def test_reject_unlabeled_small(self):
  with tempfile.TemporaryDirectory() as dir:
   p=Path(dir)/'labeled.csv'
   with p.open('w',newline='') as f:
    w=csv.writer(f);w.writerow(['round_id','opponent_id','predicted_probability','observed_event'])
    w.writerow(['round1','seat1','0.2','0'])
   self.assertFalse(evaluate(p)['calibrated'])
 def test_sample_has_scores_but_cannot_self_certify(self):
  with tempfile.TemporaryDirectory() as dir:
   p=Path(dir)/'labeled.csv'
   with p.open('w',newline='') as f:
    w=csv.writer(f);w.writerow(['round_id','opponent_id','predicted_probability','observed_event'])
    for i in range(250):w.writerow([f'round{i//5}', 'seat0', '0.4', str(i%2)])
   r=evaluate(p)
   self.assertIn('brier_score',r)
   self.assertFalse(r['calibrated'])
if __name__=='__main__':unittest.main()
