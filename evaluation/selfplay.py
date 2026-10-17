"""Seeded, paired self-play benchmark; reference rules NOT game-verified.
No neural network; no offline training claimed. No external dependencies.
"""
from __future__ import annotations
import argparse,csv,math,random,json,statistics,sys
from collections import Counter
from pathlib import Path
from tienlen_engine import Rules,deal,legal_moves,card

POLICIES=('lowest','max_shed','save_twos','tempo')

def policy_score(m,hand,policy,lead,remaining):
    count=m.count; high=m.high
    num_twos=sum(c//4==12 for c in m.cards)
    leftover=[c for c in hand if c not in m.cards]
    if policy=='lowest':return (-high, -m.top_suit, -count)
    if policy=='max_shed':return (count,-high,-m.top_suit)
    if policy=='save_twos':return (-4*num_twos+count*.25,-high,-m.top_suit)
    if policy=='tempo':
        # Explicit heuristic; this policy is a CANDIDATE, not a trained AI.
        pressure=min(x for x in remaining if x>0)
        urgency=pressure<=3
        pairs_left=sum(v>=2 for v in Counter(c//4 for c in leftover).values())
        singles_left=sum(v==1 and r<=6 for r,v in Counter(c//4 for c in leftover).items())
        return (count*1.8 - (5 if not urgency else .6)*num_twos - high*.20 + pairs_left*.5 - singles_left*.65,
                -high,-m.top_suit)
    raise ValueError(policy)


def run_game(hands,policies,rules=Rules(),seed=0):
    rng=random.Random(seed)
    hands=[tuple(h) for h in hands]
    seat=next(i for i,h in enumerate(hands) if 0 in h)
    finished=[];passes=0;lead=None;last_actor=None
    events=[];first=True;max_steps=260
    for t in range(max_steps):
        if seat in finished:
            seat=(seat+1)%4;continue
        remaining=tuple(len(h) for h in hands)
        options=legal_moves(hands[seat],lead,rules,first)
        if not options:
            # A pass cannot be taken for granted as an absence of cards: it is a chosen action.
            if lead is None:raise RuntimeError('No legal lead')
            events.append({'step':t,'type':'pass','seat':seat,'lead':tuple(card(c) for c in lead.cards),'remaining':remaining})
            passes+=1
            active=len([i for i in range(4) if i not in finished])
            if passes>=active-1:
                passes=0;lead=None
                seat=last_actor if last_actor not in finished else next(i for i in range(4) if i not in finished)
            else:seat=(seat+1)%4
            continue
        # Baselines always reply if legal, to keep comparison deterministic and explicit.
        best=max(options,key=lambda m:policy_score(m,hands[seat],policies[seat],lead,remaining))
        played=set(best.cards)
        hands[seat]=tuple(c for c in hands[seat] if c not in played)
        events.append({'step':t,'type':'move','seat':seat,'kind':best.kind,
                       'cards':tuple(card(c) for c in best.cards),'remaining':remaining})
        first=False;lead=best;last_actor=seat;passes=0
        if not hands[seat]:
            finished.append(seat)
            if len(finished)==3:
                finished.extend(i for i in range(4) if i not in finished)
                return finished,events
        seat=(seat+1)%4
    raise RuntimeError('Game exceeded step cap - check rule engine')


def wilson(p,n,z=1.96):
    if not n:return [0.,1.]
    den=1+z*z/n
    center=(p+z*z/(2*n))/den
    half=z*math.sqrt(p*(1-p)/n+z*z/(4*n*n))/den
    return [round(center-half,4),round(center+half,4)]


def benchmark(games=300,seeds=314159,policies=POLICIES,out=None):
    # For every randomized deal, rotate tested policy through all four seats
    # and face three 'lowest' baseline seats. Deal groups must remain together
    # for holdout/bootstrap analysis, not treated as independent observations.
    rng=random.Random(seeds)
    data=[]
    for g in range(games):
        hands=deal(rng)
        for contender in policies:
            placements=[]
            for seat in range(4):
                roster=['lowest']*4;roster[seat]=contender
                ranks,events=run_game(hands,roster,seed=g*7+seat)
                rank=ranks.index(seat)+1
                placements.append(rank)
            data.append({'deal':g,'policy':contender,'placements':placements,
                         'mean_rank':sum(placements)/4,'first_rate':placements.count(1)/4})
    results={}
    by_policy={p:[x for x in data if x['policy']==p] for p in policies}
    boot_rng=random.Random(seeds^0xB0057)
    boot_indices=[[boot_rng.randrange(games) for _ in range(games)] for _ in range(400)]
    def cluster_interval(rows,field):
        values=sorted(sum(rows[i][field] for i in indices)/games for indices in boot_indices)
        return [round(values[10],4),round(values[389],4)]
    for p in policies:
        rows=by_policy[p]
        k=sum(sum(r==1 for r in x['placements']) for x in rows)
        n=len(rows)*4
        results[p]={'mean_rank':round(statistics.mean(x['mean_rank'] for x in rows),4),
                    'first_rate':round(k/n,4),
                    'first_cluster_bootstrap95':cluster_interval(rows,'first_rate'),
                    'mean_rank_cluster_bootstrap95':cluster_interval(rows,'mean_rank'),
                    'deal_groups':len(rows),'seat_trials':n}
    report={'schema':'selfplay-reference-rules-v1','seed':seeds,'games_per_policy':games,
            'note':'Heuristic baseline comparison under unverified reference rules, not Pig Hunters. Bootstrap resamples shuffled deals (four seats as a cluster). Still NOT a real-play win rate.',
            'results':results}
    if out:
        out=Path(out);out.mkdir(parents=True,exist_ok=True)
        (out/'selfplay_summary.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
        with (out/'selfplay_deals.csv').open('w',newline='',encoding='utf-8') as f:
            wr=csv.writer(f);wr.writerow(['deal_id','policy','seat_0','seat_1','seat_2','seat_3','mean_rank','first_rate'])
            for x in data:wr.writerow([x['deal'],x['policy'],*x['placements'],x['mean_rank'],x['first_rate']])
    return report

if __name__=='__main__':
    p=argparse.ArgumentParser()
    p.add_argument('--games',type=int,default=300)
    p.add_argument('--seed',type=int,default=314159)
    p.add_argument('--output',default='evaluation/results')
    a=p.parse_args()
    if a.games<1:sys.exit('games must be >=1')
    print(json.dumps(benchmark(a.games,a.seed,out=a.output),ensure_ascii=False,indent=2))
