import json
import pathlib
import tempfile
from train_move_patterns import kind,load_data,grouped_split,train,logloss

def main():
    assert kind(["10S"])=="single"
    assert kind(["JS","JC"])=="pair"
    assert kind(["3S","4C","5D"])=="straight"
    assert kind(["2S","3D","4C"])=="other"
    assert kind(["3S","3C","4D","4H","5S","5D"])=="three_pairs"
    assert kind(["3S","3C","4D","4H","5S","5D","6S","6C"])=="four_pairs"
    assert kind(["2S","2C","3S","3C","4S","4C"])=="other"
    with tempfile.TemporaryDirectory() as d:
        path=pathlib.Path(d)/"log.jsonl"
        rows=[]
        for game in range(40):
            rid=f"00000000-0000-4000-8000-{game:012d}"
            for j in range(6):
                rows.append(json.dumps({"schema":1,"event":"opponent_move","round_id":rid,
                    "seat":j%3,"cards":["3S"] if j%3 else ["4S","4C"],
                    "remaining_after":(j%13),"confidence":"matched_exact","time_ms":game*10+j}))
        path.write_text("\n".join(rows),encoding="utf8")
        values,ignored=load_data(path)
        a,b=grouped_split(values)
        assert len(values)==240 and not ignored
        assert not ({x[0] for x in a}&{x[0] for x in b})
        priors,stages=train(a)
        assert logloss(b,priors,stages)>0
    print("Dataset grouped split / no leakage / move classification PASS")

if __name__ == "__main__":main()
