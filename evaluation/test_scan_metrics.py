#!/usr/bin/env python3
from scan_metrics import summarize

events = [
    {"event":"counter_ocr","round_id":"r1","time_ms":1000,"counts":[6,8,-1],
     "ocr_reasons":["ok","ok","ambiguous_margin"]},
    {"event":"move_evidence_fused","round_id":"r1","time_ms":2000,"seat":1,
     "cards":["8S","8H","9C","9D","10S","10H"],"delta":6},
    {"event":"incomplete_move","round_id":"r1","time_ms":3000,"seat":2,"delta":8},
]
labels = [
    {"event":"ground_truth_counter","round_id":"r1","time_ms":1000,"seat":0,"count":6},
    {"event":"ground_truth_counter","round_id":"r1","time_ms":1000,"seat":2,"count":5},
    {"event":"ground_truth_move","round_id":"r1","time_ms":2000,"seat":1,
     "cards":["8S","8H","9C","9D","10S","10H"]},
    {"event":"ground_truth_move","round_id":"r1","time_ms":3000,"seat":2,
     "cards":["7S","7H","8S","8H","9S","9H","10S","10H"]},
]
r = summarize(events)
assert r["ground_truth_available"] is False
assert r["observed_by_move_size"]["8"]["incomplete_move"] == 1
r = summarize(events, labels)
assert r["ground_truth_counters"]["0"]["correct"] == 1
assert r["ground_truth_counters"]["2"]["abstain"] == 1
assert r["ground_truth_moves"]["6"]["correct"] == 1
assert r["ground_truth_moves"]["8"]["missed"] == 1
print("test_scan_metrics OK")
