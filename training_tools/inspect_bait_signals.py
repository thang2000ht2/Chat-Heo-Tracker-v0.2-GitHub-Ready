#!/usr/bin/env python3
"""Audit observational 'bait' evidence. NOT a predictor of hidden cards.

Reads anonymous on-device JSONL exports. Records verified high-card pressure,
confirmed player passes and observed local 2 exposure as temporal context.
It does not claim an opponent intended to bait or possesses chopping cards.
"""
import argparse
from collections import Counter, defaultdict
import json


def inspect(rows):
    counts = Counter()
    rounds = set()
    seats = defaultdict(set)
    for row in rows:
        if not isinstance(row, dict):
            continue
        rid = row.get('round_id')
        if not isinstance(rid, str):
            continue
        if row.get('event') == 'round_begin':
            rounds.add(rid)
        event = row.get('event')
        if event == 'opponent_move':
            if row.get('confidence') == 'matched_exact':
                counts['confirmed_opponent_moves'] += 1
                if row.get('seat') in (0, 1, 2):
                    seats[row['seat']].add(rid)
            else:
                counts['uncertain_opponent_moves'] += 1
        elif event == 'bait_candidate':
            if row.get('confidence') == 'weak_observable_pattern_not_hidden_intent':
                counts['weak_bait_pattern_events'] += 1
        elif event == 'opponent_pass':
            if row.get('confidence') == 'user_confirmed':
                counts['user_confirmed_passes'] += 1
                if row.get('lead_is_two') is True:
                    counts['confirmed_pass_against_two'] += 1
        elif event == 'local_play_observed':
            if row.get('played_two') is True:
                counts['local_two_exposures_observed'] += 1
        elif event == 'round_result':
            if row.get('label_source') == 'user_confirmed':
                counts['round_results_confirmed'] += 1
    return {'rounds_started': len(rounds),
            'seats_with_verified_moves': {str(k): len(v) for k, v in sorted(seats.items())},
            **counts}


def load_jsonl(path):
    with open(path, encoding='utf-8') as stream:
        for lineno, line in enumerate(stream, 1):
            try:
                yield json.loads(line)
            except json.JSONDecodeError:
                raise ValueError(f'Invalid JSON on line {lineno}')


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('dataset', help='Exported chatheo-observations.jsonl')
    p.add_argument('--min-rounds', type=int, default=30)
    a = p.parse_args()
    stats = inspect(load_jsonl(a.dataset))
    print(json.dumps(stats, ensure_ascii=False, indent=2, sort_keys=True))
    if stats['rounds_started'] < a.min_rounds:
        print(f'NOT ENOUGH DATA: {stats["rounds_started"]} rounds, need >= {a.min_rounds}.')
    print('No intention/chop probability is trained or calibrated by this audit.')


if __name__ == '__main__':
    main()
