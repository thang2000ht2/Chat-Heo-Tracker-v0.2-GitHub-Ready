"""Check a small set of *manually inspected* video frames.
These frames also informed tuning, so matches are calibration tests,
NOT an independent accuracy estimate.
Usage: python test_video_frames.py /path/to/video.mp4
"""
import sys,cv2
from video_detector import detect
video=sys.argv[1]
cap=cv2.VideoCapture(video)
if not cap.isOpened():raise SystemExit('Cannot open video')
# S=spades, C=clubs, H=hearts, D=diamonds.
reference={
    12:['4D','6C'],24:['AC','AD'],32:['3H','4S','5S'],
    36:['3H','4S','5S','10S','JS','QS'],
    40:['6D','6H'],44:['6D','6H','AS','AH'],
    52:['KS','KC','KH'],56:['KS','KC','KH'],
    60:['JC','JH'],68:['5C','5D','5H'],
    76:['3S','3C','8S','8C'],80:['8S','8C','9S','9C'],
    88:['3D','KD'],92:['KD','2S'],
    96:['7S','8H','9H','10D'],100:['8D'],108:['JD'],112:['9D'],
}
passed=0
for sec,expected in reference.items():
    cap.set(cv2.CAP_PROP_POS_MSEC,sec*1000)
    ok,frame=cap.read()
    if not ok:raise SystemExit('Failed at timestamp '+str(sec))
    found=detect(frame)
    match=set(found)==set(expected)
    passed+=match
    print(f'{sec:>3}s {"OK" if match else "CHECK"} expected={expected} detected={found}')
print(f'Calibration snapshots: {passed}/{len(reference)} exact matches. Not full-video accuracy.')
