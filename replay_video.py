"""Replay recording at 1fps, simulate 2-consecutive-frame confirmation.
Mainly diagnoses false positives around transitions; not equivalent to device runtime.
Usage: python replay_video.py /path/to/video.mp4
"""
import sys,cv2
from video_detector import detect
cap=cv2.VideoCapture(sys.argv[1]); duration=cap.get(cv2.CAP_PROP_FRAME_COUNT)/cap.get(cv2.CAP_PROP_FPS)
seen=set(); pending=();stable=0
for sec in range(8,int(duration)):
 cap.set(cv2.CAP_PROP_POS_MSEC,sec*1000);ok,frame=cap.read()
 if not ok:break
 cards=tuple(detect(frame))
 if cards and cards==pending:stable+=1
 elif cards:pending=cards;stable=1
 else:pending=();stable=0
 if stable>=2:
  new=[c for c in cards if c not in seen]
  if new:print(f'{sec:3d}s ADD {" ".join(new)} (total {len(seen)+len(new)})',flush=True);seen.update(new)
print('TOTAL',len(seen),'CARDS:',','.join(sorted(seen)),flush=True)
