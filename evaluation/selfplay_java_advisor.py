"""Research bridge: actual Android Java policy against diverse Python reference bots.
Rules are NOT verified Pig Hunters rules; rejected Java moves are counted explicitly.
Observations exclude unknown hands, and each policy is paired on identical deals/seats.
"""
from __future__ import annotations
import argparse, csv, json, random, math, subprocess, sys
from pathlib import Path
from tienlen_engine import Rules, deal, legal_moves, card, parse
from selfplay import policy_score

ROOT=Path(__file__).resolve().parents[1]
SYMBOLS={'♠':'S','♣':'C','♦':'D','♥':'H'}
BASELINES=['lowest','max_shed','save_twos','random','strategic_pass']

class JavaAdvisor:
    def __init__(self, classes):
        self.p=subprocess.Popen(['java','-Xmx256m','-cp',str(classes),'vn.chatheotracker.AdvisorBenchmarkBridge'],
                                stdin=subprocess.PIPE,stdout=subprocess.PIPE,text=True,encoding='utf-8',bufsize=1)
        self.calls=0
    def pick(self,hand,lead,seen,counts,options):
        fields=[','.join(card(c) for c in hand),','.join(card(c) for c in lead.cards) if lead else '',
                ','.join(card(c) for c in sorted(seen)),','.join(map(str,counts))]
        self.p.stdin.write('|'.join(fields)+'\n');self.p.stdin.flush()
        line=self.p.stdout.readline().strip();self.calls+=1
        if line.startswith('ĐÁNH '):
            try:
                codes=tuple(sorted(parse(''.join(SYMBOLS.get(ch,ch) for ch in token)) for token in line[5:].split()))
                for m in options:
                    if m.cards==codes:return m,'valid'
            except (ValueError,IndexError):pass
            return None,'illegal'
        if line=='BỎ LƯỢT':return None,'pass'
        return None,'unavailable'
    def close(self):
        self.p.stdin.write('EXIT\n');self.p.stdin.flush();self.p.stdin.close();self.p.terminate();self.p.wait(timeout=3)

def baseline(options, hand, lead, remaining, name, rng):
    if name=='random':return rng.choice(options) if options else None
    if name=='strategic_pass':
        # Strategic optional pass if opponent has many cards, pressure low and only expensive replies.
        if lead and options and min(x for x in remaining if x>0)>4:
            cheap=[m for m in options if m.high<=9 and not any(c//4==12 for c in m.cards)]
            if not cheap:return None
            options=cheap
        name='save_twos'
    return max(options,key=lambda m:policy_score(m,hand,name,lead,remaining)) if options else None

def play_one(dealt, contestant, baseline_name, java, seed):
    rng=random.Random(seed)
    hands=[tuple(h) for h in dealt]
    alive=list(range(4));finished=[];lead=None;owner=None;seat=next(i for i,h in enumerate(hands) if 0 in h)
    first=True;passes=0;seen=set();errors={'illegal':0,'unavailable':0,'pass':0};recovery=0
    for step in range(350):
        if seat not in alive:seat=(seat+1)%4;continue
        sizes=tuple(len(h) for h in hands)
        options=legal_moves(hands[seat],lead,Rules(),first)
        if seat==contestant:
            counts=[sizes[i] for i in range(4) if i!=seat]
            # Exact game information only: private current hand, public previous moves and counters.
            decision,status=java.pick(hands[seat],lead,seen,counts,options)
            if status in ('illegal','unavailable'):
                errors[status]+=1;recovery+=1
                decision=baseline(options,hands[seat],lead,sizes,'lowest',rng)
            elif status=='pass':
                errors['pass']+=1
                # Passing on an empty table would be illegal.
                if lead is None:
                    recovery+=1;decision=baseline(options,hands[seat],lead,sizes,'lowest',rng)
        else:decision=baseline(options,hands[seat],lead,sizes,baseline_name,rng)
        if decision is None:
            if lead is None:raise RuntimeError('Illegal pass on free lead')
            passes+=1
            if passes>=len(alive)-1:
                passes=0;lead=None
                seat=owner if owner in alive else alive[0]
            else:seat=(seat+1)%4
            continue
        chosen=set(decision.cards);seen.update(chosen)
        hands[seat]=tuple(c for c in hands[seat] if c not in chosen)
        lead=decision;owner=seat;first=False;passes=0
        if not hands[seat]:
            finished.append(seat);alive.remove(seat)
            if len(alive)==1:
                finished.append(alive[0]);return finished.index(contestant)+1,errors,recovery
        seat=(seat+1)%4
    raise RuntimeError('step limit')

def paired_experiment(deals,seed,classes,out):
    rng=random.Random(seed);java=JavaAdvisor(classes)
    rows=[]
    try:
        for i in range(deals):
            hands=deal(rng)
            for base in BASELINES:
                for seat in range(4):
                    # Pair same hand / seat / opponent style; only evaluated candidate changes.
                    policies=['java','control']
                    for policy in policies:
                        # Control: use JAVA process? reference lowest chosen by policy flag in play_one.
                        if policy=='control':
                            rank,errors,recovery=play_baseline(hands,seat,base,seed+i*4+seat)
                        else:rank,errors,recovery=play_one(hands,seat,base,java,seed+i*4+seat)
                        rows.append({'deal':i,'baseline':base,'seat':seat,'policy':policy,'rank':rank,
                                     'win':int(rank==1),'illegal':errors.get('illegal',0),
                                     'unavailable':errors.get('unavailable',0),'fallbacks':recovery})
    finally:java.close()
    out=Path(out);out.mkdir(parents=True,exist_ok=True)
    with (out/'paired_trials.csv').open('w',encoding='utf8',newline='') as f:
        writer=csv.DictWriter(f,fieldnames=list(rows[0]));writer.writeheader();writer.writerows(rows)
    result=report(rows,deals,seed,java.calls)
    (out/'paired_summary.json').write_text(json.dumps(result,indent=2,ensure_ascii=False),encoding='utf8')
    return result

def play_baseline(dealt, contestant, baseline_name, seed):
    rng=random.Random(seed)
    hands=[tuple(h) for h in dealt];alive=list(range(4));finished=[];lead=None;owner=None
    seat=next(i for i,h in enumerate(hands) if 0 in h);first=True;passes=0
    for step in range(350):
        if seat not in alive:seat=(seat+1)%4;continue
        sizes=tuple(len(h) for h in hands)
        options=legal_moves(hands[seat],lead,Rules(),first)
        policy='lowest' if seat==contestant else baseline_name
        m=baseline(options,hands[seat],lead,sizes,policy,rng)
        if m is None:
            if lead is None:raise RuntimeError('pass with free lead')
            passes+=1
            if passes>=len(alive)-1:passes=0;lead=None;seat=owner if owner in alive else alive[0]
            else:seat=(seat+1)%4
            continue
        hands[seat]=tuple(c for c in hands[seat] if c not in m.cards)
        lead=m;owner=seat;first=False;passes=0
        if not hands[seat]:
            finished.append(seat);alive.remove(seat)
            if len(alive)==1:finished.append(alive[0]);return finished.index(contestant)+1,{},0
        seat=(seat+1)%4
    raise RuntimeError('step limit')

def report(rows,deals,seed,calls):
    by={(r['deal'],r['baseline'],r['seat'],r['policy']):r for r in rows}
    summary={};rng=random.Random(seed^0xAAA14)
    for base in BASELINES:
        deal_deltas=[];wins=[];rank_delta=[]
        for d in range(deals):
            j=[by[(d,base,s,'java')] for s in range(4)]
            c=[by[(d,base,s,'control')] for s in range(4)]
            deal_deltas.append(sum(a['win']-b['win'] for a,b in zip(j,c))/4)
            rank_delta.append(sum(a['rank']-b['rank'] for a,b in zip(j,c))/4)
            wins.extend(a['win'] for a in j)
        boots=[]
        for _ in range(800):
            vals=[deal_deltas[rng.randrange(deals)] for _ in range(deals)]
            boots.append(sum(vals)/deals)
        boots.sort()
        n=sum(r['fallbacks'] for r in rows if r['baseline']==base and r['policy']=='java')
        summary[base]={'java_first_rate':round(sum(wins)/len(wins),4),
                       'delta_vs_lowest':round(sum(deal_deltas)/deals,4),
                       'delta_win_95pct_cluster_bootstrap':[round(boots[20],4),round(boots[779],4)],
                       'delta_mean_rank':round(sum(rank_delta)/deals,4),
                       'fallbacks':n,'passed_release_gate':(min(boots[20],boots[779])>0 and sum(deal_deltas)/deals>=.01 and n==0)}
    return {'research_only':True,'rules':'UNVERIFIED REFERENCE, excludes off-turn chop and instant win',
            'n_deals':deals,'seats_per_deal':4,'seed':seed,'java_advisor_calls':calls,
            'release_rule':'at least +1 percentage point first-rate and cluster-bootstrap 95% CI above zero, no illegal/fallbacks',
            'baseline_results':summary,'ANY_RELEASE_PASS':all(x['passed_release_gate'] for x in summary.values())}

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--deals',type=int,default=12)
    p.add_argument('--seed',type=int,default=213419);p.add_argument('--classes',required=True)
    p.add_argument('--out',default='evaluation/results/java_bridge')
    a=p.parse_args();print(json.dumps(paired_experiment(a.deals,a.seed,a.classes,a.out),ensure_ascii=False,indent=2))
