from inspect_bait_signals import inspect

rows = [
    {'event': 'round_begin', 'round_id': 'a'},
    {'event': 'opponent_move', 'round_id': 'a', 'seat': 0, 'confidence': 'matched_exact'},
    {'event': 'opponent_move', 'round_id': 'a', 'seat': 1, 'confidence': 'partial_card_observation'},
    {'event': 'bait_candidate', 'round_id': 'a', 'confidence': 'weak_observable_pattern_not_hidden_intent'},
    {'event': 'opponent_pass', 'round_id': 'a', 'confidence': 'user_confirmed', 'lead_is_two': True},
    {'event': 'local_play_observed', 'round_id': 'a', 'played_two': True},
    {'event': 'round_result', 'round_id': 'a', 'label_source': 'user_confirmed'},
    {'event': 'round_begin', 'round_id': 'b'},
]
a=inspect(rows)
assert a['rounds_started']==2
assert a['confirmed_opponent_moves']==1
assert a['uncertain_opponent_moves']==1
assert a['weak_bait_pattern_events']==1
assert a['user_confirmed_passes']==1
assert a['confirmed_pass_against_two']==1
assert a['local_two_exposures_observed']==1
assert a['round_results_confirmed']==1
print('test_inspect_bait_signals OK')
