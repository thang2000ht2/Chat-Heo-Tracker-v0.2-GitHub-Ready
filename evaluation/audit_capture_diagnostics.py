#!/usr/bin/env python3
"""Local-only diagnosis of exported JSONL. Does not upload or log nicknames."""
import argparse
import collections
import datetime as dt
import json


def parse(path):
    events = []
    with open(path, encoding="utf-8") as handle:
        for line_no, line in enumerate(handle, 1):
            try:
                item = json.loads(line)
            except json.JSONDecodeError as err:
                raise ValueError(f"Invalid JSON line {line_no}: {err}") from err
            if isinstance(item, dict) and isinstance(item.get("time_ms"), (int, float)):
                events.append(item)
    return sorted(events, key=lambda event: event["time_ms"])


def audit(events):
    grouped = collections.Counter(event.get("event") for event in events)
    starts = [index for index, event in enumerate(events) if event.get("event") == "capture_started"]
    sessions = []
    for pos, index in enumerate(starts):
        next_index = starts[pos + 1] if pos + 1 < len(starts) else len(events)
        section = events[index:next_index]
        if not section:
            continue
        first_complete = next((e for e in section if e.get("event") == "hand_scan" and e.get("complete")), None)
        sessions.append({
            "started_utc": dt.datetime.fromtimestamp(section[0]["time_ms"] / 1000, dt.timezone.utc).isoformat(timespec="seconds"),
            "duration_seconds": round((section[-1]["time_ms"] - section[0]["time_ms"]) / 1000, 1),
            "scan_events": sum(e.get("event") == "hand_scan" for e in section),
            "first_complete_hand_seconds": None if first_complete is None else
                round((first_complete["time_ms"] - section[0]["time_ms"]) / 1000, 1),
        })
    six_drops = []
    for event in events:
        if event.get("event") != "opponent_count":
            continue
        before, after = event.get("before"), event.get("after")
        if not isinstance(before, int) or not isinstance(after, int) or before - after != 6:
            continue
        when = event["time_ms"]
        same_round = [other for other in events if other.get("round_id") == event.get("round_id")]
        complete_boards = [other for other in same_round
            if abs(other["time_ms"] - when) < 4200 and len(other.get("board") or []) == 6]
        six_drops.append({
            "when_utc": dt.datetime.fromtimestamp(when / 1000, dt.timezone.utc).isoformat(timespec="seconds"),
            "six_card_board_observed": bool(complete_boards),
            "matched": any(other.get("event") == "move_matched" and other.get("delta") == 6
                and abs(other["time_ms"] - when) < 8000 for other in same_round),
        })
    return {"total_events": len(events), "event_counts": grouped, "sessions": sessions, "six_card_drops": six_drops}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("diagnostics_jsonl")
    args = parser.parse_args()
    print(json.dumps(audit(parse(args.diagnostics_jsonl)), indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
