"""Offline evaluation only. Needs real, verified outcomes (NOT output from a model).
CSV columns: round_id,opponent_id,predicted_probability,observed_event
observed_event: 1 if a pre-defined event truly occurred, else 0 after confirmed opportunity.
Do NOT derive labels from simulated hidden hands or advice_proposed.
"""
from __future__ import annotations
import csv, math, json, random, collections
from pathlib import Path

MIN_ROWS=200
MIN_ROUNDS=30

def evaluate(filename,bins=10):
    observations=[]; rounds=set()
    with Path(filename).open(encoding='utf-8',newline='') as infile:
        for row in csv.DictReader(infile):
            rid=row.get('round_id','').strip()
            if not rid:raise ValueError('Missing round_id')
            p=float(row['predicted_probability']); outcome=int(row['observed_event'])
            if not math.isfinite(p) or not 0<=p<=1 or outcome not in (0,1):
                raise ValueError('Prediction / outcome out of range')
            observations.append((p,outcome,rid));rounds.add(rid)
    if len(observations)<MIN_ROWS or len(rounds)<MIN_ROUNDS:
        return {'calibrated':False,'reason':f'Insufficient independent data: rows={len(observations)}, rounds={len(rounds)}; require {MIN_ROWS}/{MIN_ROUNDS}'}
    count=[0]*bins;sumprob=[0.0]*bins;sumobs=[0.0]*bins
    for p,y,_ in observations:
        i=min(bins-1,int(p*bins));count[i]+=1;sumprob[i]+=p;sumobs[i]+=y
    per_bin=[];ece=0.0
    for i,n in enumerate(count):
        if n:
            prob=sumprob[i]/n;observed=sumobs[i]/n
            ece+=n/len(observations)*abs(prob-observed)
            per_bin.append({'bucket':i,'n':n,'predicted':round(prob,4),'observed':round(observed,4)})
    brier=sum((p-y)**2 for p,y,_ in observations)/len(observations)
    return {'calibrated':False,'sufficient_for_offline_review':True,
            'warning':'Report only. Good ECE/Brier does not prove causal effects or transfer to unseen players.',
            'rows':len(observations),'rounds':len(rounds),'brier_score':round(brier,4),
            'expected_calibration_error':round(ece,4),'reliability':per_bin}

if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('labeled_csv');a=p.parse_args()
    print(json.dumps(evaluate(a.labeled_csv),ensure_ascii=False,indent=2))
