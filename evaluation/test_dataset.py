import unittest,tempfile,json,uuid,os
from evaluate_results import analyze,summary
class DataTest(unittest.TestCase):
    def test_verified_pass_and_labels(self):
        rid=str(uuid.uuid4())
        rows=[{'schema':1,'round_id':rid,'event':'opponent_pass','confidence':'inferred_from_gap','lead':['9S']},
              {'schema':1,'round_id':rid,'event':'opponent_pass','confidence':'visual_confirmed','lead':['9S']},
              {'schema':1,'round_id':rid,'event':'opponent_move','confidence':'matched_exact','cards':['4S']},
              {'schema':1,'round_id':rid,'event':'round_result','placement':2,'label_source':'user_confirmed'}]
        with tempfile.NamedTemporaryFile(mode='w',encoding='utf-8',delete=False) as f:
            for row in rows:f.write(json.dumps(row)+'\n')
            name=f.name
        try:
            result,reject=analyze(name)
            self.assertEqual(result[0]['confirmed_passes'],1)
            self.assertEqual(reject['unverified_pass'],1)
            self.assertEqual(summary(result,reject)['mean_rank'],2.)
        finally:os.unlink(name)
if __name__=='__main__':unittest.main()
