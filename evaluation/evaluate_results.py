"""Analyze exported JSONL without calling suggestions 'outcomes' or guessing passes.
Explicitly separate pass events, advice, confirmed moves, and user-confirmed rank.
No causal effect estimate without logged adherence and comparable conditions.
"""
from __future__ import annotations
import argparse,collections,csv,json,math,random,statistics
from pathlib import Path


def analyze(path):
    rounds=collections.defaultdict(lambda:collections.defaultdict(list))
    rejected=collections.Counter()
    for ln,line in enumerate(Path(path).read_text(encoding='utf-8').splitlines(),1):
        try:x=json.loads(line)
        except ValueError:rejected['invalid_json']+=1;continue
        if x.get('schema')!=1 or not isinstance(x.get('round_id'),str):
            rejected['invalid_schema']+=1;continue
        event=x.get('event')
        if event=='opponent_pass':
            # A pass is tactical and may occur despite having a legal reply.
            # Only accept explicit multi-frame / user confirmed observations.
            if x.get('confidence') not in ('visual_confirmed','user_confirmed') or not x.get('lead'):
                rejected['unverified_pass']+=1;continue
        if event=='opponent_move' and x.get('confidence')!='matched_exact':
            rejected['unverified_move']+=1;continue
        rounds[x['round_id']][event].append(x)
    rows=[]
    for rid,events in rounds.items():
        results=[r for r in events['round_result'] if r.get('label_source')=='user_confirmed' and r.get('placement') in (1,2,3,4)]
        result=results[-1] if results else None
        advice=events['advice_proposed']; confirmed=events['opponent_move']
        rows.append({'round_id':rid,'outcome_known':bool(result),'placement':result['placement'] if result else '',
                     'advices':len(advice),'confirmed_moves':len(confirmed),
                     'confirmed_passes':len(events['opponent_pass']),
                     'unattributed':len(events['unattributed_play']),
                     'data_complete':bool(result and not events['missing_play'] and not events['unattributed_play'])})
    return rows,rejected


def summary(rows,rejected):
    labeled=[r for r in rows if r['outcome_known']]
    p1=sum(r['placement']==1 for r in labeled)
    return {'rounds':len(rows),'labeled':len(labeled),'with_advice':sum(r['advices']>0 for r in rows),
            'fully_observed_labeled':sum(r['data_complete'] for r in labeled),
            'confirmed_passes':sum(r['confirmed_passes'] for r in rows),
            'rank_counts':dict(collections.Counter(r['placement'] for r in labeled)),
            'mean_rank':round(statistics.mean(r['placement'] for r in labeled),3) if labeled else None,
            'rejected_events':dict(rejected),
            'causal_impact':'NOT IDENTIFIED: app has no verified per-decision adherence or randomized assignment; suggestions and actual moves may differ.'}

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('jsonl');p.add_argument('--out');a=p.parse_args()
    rows,rejected=analyze(a.jsonl);report=summary(rows,rejected)
    if a.out:Path(a.out).write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(report,ensure_ascii=False,indent=2))
