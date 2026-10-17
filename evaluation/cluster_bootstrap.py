"""Recalculate confidence intervals from saved seeded self-play trials, clustered by deal."""
import csv,json,random,sys
from collections import defaultdict
from pathlib import Path

def apply(results_dir:Path):
    summary_file=results_dir/'selfplay_summary.json'
    meta=json.loads(summary_file.read_text(encoding='utf-8'))
    groups=defaultdict(list)
    with (results_dir/'selfplay_deals.csv').open(encoding='utf-8',newline='') as f:
        for r in csv.DictReader(f):
            groups[r['policy']].append((int(r['deal_id']),float(r['first_rate']),float(r['mean_rank'])))
    n=meta['games_per_policy']
    rnd=random.Random(meta['seed']^0xB0057)
    draws=[[rnd.randrange(n) for _ in range(n)] for _ in range(500)]
    for policy,items in groups.items():
        items.sort()
        assert len(items)==n and [x[0] for x in items]==list(range(n))
        a,b=[],[]
        for ix in draws:
            a.append(sum(items[i][1] for i in ix)/n)
            b.append(sum(items[i][2] for i in ix)/n)
        a.sort();b.sort()
        meta['results'][policy].pop('first_wilson95',None)
        meta['results'][policy]['first_cluster_bootstrap95']=[round(a[12],4),round(a[487],4)]
        meta['results'][policy]['mean_rank_cluster_bootstrap95']=[round(b[12],4),round(b[487],4)]
    meta['note']='Reference-rule self-play only; groups are per random deal, resampling all four seat rotations together. NOT Pig Hunters or human-play performance.'
    summary_file.write_text(json.dumps(meta,ensure_ascii=False,indent=2),encoding='utf-8')
    return meta
if __name__=='__main__':
    print(json.dumps(apply(Path(sys.argv[1] if len(sys.argv)>1 else 'evaluation/results')),ensure_ascii=False,indent=2))
