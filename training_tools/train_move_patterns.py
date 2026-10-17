#!/usr/bin/env python3
"""Train an OFFLINE descriptive opponent move-type model from exported JSONL.

This is NOT a policy model and must not be represented as a calibrated win or chop
probability. It predicts visible move families from *confirmed* opponent plays.
Split by round ID to prevent nearly identical observations leaking across splits.
No third-party packages or network requests.
"""
import argparse
import collections
import hashlib
import json
import math
import pathlib
import statistics
from typing import Any

KINDS = ("single", "pair", "triple", "four", "three_pairs", "four_pairs", "straight", "other")
MIN_ROUNDS = 30
MIN_MOVES = 150
ALPHA = 2.0


def kind(cards):
    if not isinstance(cards, list) or not cards or len(set(cards)) != len(cards):
        return None
    suits = {"S", "C", "D", "H"}
    ranks = {"3": 3,"4": 4,"5": 5,"6": 6,"7": 7,"8": 8,"9": 9,"10": 10,
             "J": 11,"Q": 12,"K": 13,"A": 14,"2": 15}
    values = []
    for c in cards:
        if not isinstance(c, str) or len(c) < 2 or c[-1] not in suits or c[:-1] not in ranks:
            return None
        values.append(ranks[c[:-1]])
    if len(cards) == 1:
        return "single"
    if len(cards) in (2,3,4) and len(set(values)) == 1:
        return {2:"pair",3:"triple",4:"four"}[len(cards)]
    # Verified 3/4 consecutive pairs (rank 2 does not belong to a sequence).
    if len(cards) in (6,8) and max(values)<15:
        counts=collections.Counter(values)
        if len(counts)==len(cards)//2 and all(v==2 for v in counts.values()) and \
                max(counts)-min(counts)+1==len(counts):
            return "three_pairs" if len(cards)==6 else "four_pairs"
    if len(cards) >= 3 and max(values) < 15 and len(set(values)) == len(cards) and max(values)-min(values)+1 == len(cards):
        return "straight"
    return "other"


def stage(remaining):
    return "end" if isinstance(remaining, int) and 0 <= remaining <= 4 else (
        "middle" if isinstance(remaining, int) and 5 <= remaining <= 9 else "opening")


def load_data(path):
    records = []
    seen = set()
    ignored = 0
    with open(path, encoding="utf-8") as f:
        for line in f:
            try:
                x = json.loads(line)
                if x.get("schema") != 1 or x.get("event") != "opponent_move":
                    continue
                if x.get("confidence") != "matched_exact":
                    ignored += 1
                    continue
                rid = x.get("round_id")
                if not isinstance(rid, str) or len(rid) != 36:
                    continue
                label = kind(x.get("cards"))
                if label is None:
                    ignored += 1
                    continue
                identity = (rid, x.get("seat"), str(x.get("cards")), x.get("time_ms"))
                if identity in seen:
                    continue
                seen.add(identity)
                records.append((rid,stage(x.get("remaining_after")),label))
            except (ValueError,TypeError,KeyError,AttributeError):
                ignored += 1
    return records, ignored


def grouped_split(records):
    ids = sorted({r[0] for r in records},key=lambda s:hashlib.sha256(s.encode()).hexdigest())
    split = max(1,int(.80*len(ids)))
    train_ids = set(ids[:split])
    return ([r for r in records if r[0] in train_ids],
            [r for r in records if r[0] not in train_ids])


def train(rows):
    global_counts = collections.Counter(r[2] for r in rows)
    stage_counts = {s:collections.Counter(r[2] for r in rows if r[1] == s)
                    for s in ("opening","middle","end")}
    # The global distribution is a backoff prior for stage-conditioned predictions.
    denominator = sum(global_counts.values())+ALPHA*len(KINDS)
    global_p = {k:(global_counts[k]+ALPHA)/denominator for k in KINDS}
    by_stage = {}
    for s,ct in stage_counts.items():
        total=sum(ct.values())
        weight = total/(total+60.0)  # Strong shrinkage with few observed plays.
        local = {k:(ct[k]+ALPHA*global_p[k])/(total+ALPHA) for k in KINDS}
        by_stage[s]={k:weight*local[k]+(1-weight)*global_p[k] for k in KINDS}
    return global_p,by_stage


def logloss(rows, global_p, stages=None):
    if not rows:return None
    return sum(-math.log(max(1e-8,(stages or {}).get(s,global_p).get(k,1e-8)))
               for _,s,k in rows)/len(rows)


def main():
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("jsonl",type=pathlib.Path,help="Exported local tracker JSONL")
    cli.add_argument("--out",type=pathlib.Path,default=pathlib.Path("opponent_move_model.json"))
    args=cli.parse_args()
    if not args.jsonl.is_file():
        cli.error(f"File not found: {args.jsonl}. First export JSONL from the Android app.")
    rows,ignored=load_data(args.jsonl)
    num_rounds=len({r[0] for r in rows})
    print(f"Confirmed move samples: {len(rows)}; distinct rounds: {num_rounds}; excluded observations: {ignored}")
    if len(rows)<MIN_MOVES or num_rounds<MIN_ROUNDS:
        print(f"NOT TRAINED: need at least {MIN_MOVES} confirmed moves from {MIN_ROUNDS} distinct rounds. Collect more actual games.")
        return 0
    training, test = grouped_split(rows)
    if not training or not test:
        print("NOT TRAINED: grouped holdout split is empty.")
        return 1
    prior,stage_model=train(training)
    baseline=logloss(test,prior)
    observed=logloss(test,prior,stage_model)
    print(f"Held-out per-move logloss: baseline={baseline:.4f}, stage_model={observed:.4f}")
    if observed >= baseline:
        print("NOT SAVED: model does not beat the baseline on withheld rounds.")
        return 0
    output = {
        "schema":1,"model":"observed_move_family_stage_smoothed",
        "not_a_policy_model":True,"not_calibrated_block_or_win_probability":True,
        "observed_special_combos_only":True,
        "may_be_used_in_live_advice":False,
        "round_split":"deterministic 80/20 by SHA256(round_id)",
        "train_moves":len(training),"holdout_moves":len(test),
        "holdout_logloss":observed,"holdout_baseline_logloss":baseline,
        "global_distribution":prior,"stage_distribution":stage_model,
        "warning":"Observed, recognized plays only: nonrandom missingness and hidden cards are not modeled. Evaluate before deployment."
    }
    args.out.write_text(json.dumps(output,ensure_ascii=False,indent=2),encoding="utf-8")
    print("Model saved to",args.out,"for offline analysis only (not imported by Android app).")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
