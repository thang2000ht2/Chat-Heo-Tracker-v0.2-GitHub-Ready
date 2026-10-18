#!/usr/bin/env python3
"""Offline scan metrics from diagnostic and independently labeled events.

Without ground truth, report only *observed* matched/missing/unknown counts,
NEVER mislabel these as recognition accuracy.

Optional ground truth JSONL events:
  {"event":"ground_truth_move", "round_id":"r", "time_ms":12345,
   "seat":1, "cards":["8S","8H",...]}
  {"event":"ground_truth_counter", "round_id":"r", "time_ms":12345,
   "seat":1, "count":6}

Labels must be produced by manual review from video, not automatically inferred
from app logs. Matches use same round and closest timestamp within 5 seconds.
"""
import argparse
import collections
import json


def read_jsonl(filename):
    if filename is None:
        return []
    data = []
    with open(filename, encoding="utf-8") as handle:
        for lineno, line in enumerate(handle, 1):
            if not line.strip():
                continue
            try:
                event = json.loads(line)
            except json.JSONDecodeError as error:
                raise ValueError(f"{filename}:{lineno}: {error}") from error
            if isinstance(event, dict):
                data.append(event)
    return data


def group_kind(event):
    return event.get("event", "unknown")


def summarize(logs, labels=()):
    named = collections.Counter(group_kind(row) for row in logs)
    per_seat = collections.defaultdict(collections.Counter)
    per_length = collections.defaultdict(collections.Counter)
    for e in logs:
        kind = group_kind(e)
        if kind in ("move_evidence_fused", "move_matched", "move_pending", "incomplete_move", "missing_play"):
            seat = str(e.get("seat", "?"))
            delta = str(e.get("delta", e.get("count_decrease", "?")))
            per_seat[seat][kind] += 1
            per_length[delta][kind] += 1
        if kind in ("counter_rejected", "counter_ocr"):
            if kind == "counter_rejected":
                per_seat[str(e.get("seat", "?"))][kind] += 1
            else:
                for idx, reason in enumerate(e.get("ocr_reasons", [])):
                    per_seat[str(idx)]["ocr_" + reason] += 1
    result = {
        "total_events": len(logs),
        "observed_events": dict(named),
        "observed_by_seat": {k: dict(v) for k, v in sorted(per_seat.items())},
        "observed_by_move_size": {k: dict(v) for k, v in sorted(per_length.items())},
        "ground_truth_available": False,
        "note": "Observed events cannot measure accuracy without independent manual labels.",
    }
    labels = list(labels)
    if not labels:
        return result

    def closest(truth, names, max_gap=5000, match_seat=True):
        matches = [e for e in logs if e.get("event") in names
                   and e.get("round_id") == truth.get("round_id")
                   and (not match_seat or truth.get("seat") is None or
                        e.get("seat") == truth.get("seat"))
                   and isinstance(e.get("time_ms"), (int, float))
                   and abs(e["time_ms"]-truth.get("time_ms", -100000))<=max_gap]
        return min(matches, key=lambda e: abs(e["time_ms"]-truth["time_ms"])) if matches else None

    move_results = collections.defaultdict(collections.Counter)
    count_results = collections.defaultdict(collections.Counter)
    for truth in labels:
        if truth.get("event") == "ground_truth_move":
            cards = set(truth.get("cards", []))
            if not cards:
                continue
            match = closest(truth, {"move_evidence_fused", "move_matched"})
            outcome = "missed" if match is None else (
                "correct" if set(match.get("cards", [])) == cards else "incorrect")
            move_results[str(len(cards))][outcome] += 1
            move_results["seat_" + str(truth.get("seat", "?"))][outcome] += 1
        if truth.get("event") == "ground_truth_counter":
            same = closest(truth, {"counter_ocr"}, match_seat=False)
            seat = truth.get("seat")
            if not isinstance(seat, int) or seat not in (0, 1, 2):
                continue
            count = (same.get("counts", []) if same else [])
            prediction = count[seat] if seat < len(count) else -1
            outcome = "correct" if prediction == truth.get("count") else (
                "abstain" if prediction < 0 else "incorrect")
            count_results[str(seat)][outcome] += 1
    result["ground_truth_available"] = True
    result["ground_truth_moves"] = {k: dict(v) for k, v in sorted(move_results.items())}
    result["ground_truth_counters"] = {k: dict(v) for k, v in sorted(count_results.items())}
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("diagnostics_jsonl")
    parser.add_argument("--labels", help="separate manually labeled JSONL; not app predictions")
    parser.add_argument("--out", help="save report as JSON")
    args = parser.parse_args()
    report = summarize(read_jsonl(args.diagnostics_jsonl), read_jsonl(args.labels))
    content = json.dumps(report, ensure_ascii=False, indent=2)
    if args.out:
        with open(args.out, "w", encoding="utf-8") as writer:
            writer.write(content + "\n")
    else:
        print(content)


if __name__ == "__main__":
    main()
