"""Produce rank templates missing/weak in v0.1 from three manually verified frames.
Run from project root with: python train_from_video.py /path/to/user/video.mp4
Coordinates refer to video resized to 2048 x 942, not arbitrary films.
"""
from pathlib import Path
import sys,cv2,numpy as np
video=sys.argv[1]
cap=cv2.VideoCapture(video)
if not cap.isOpened(): raise SystemExit('Video not readable')
assets=Path(__file__).parent/'app/src/main/assets'
def get_frame(t):
 cap.set(cv2.CAP_PROP_POS_MSEC,t*1000)
 ok,frame=cap.read()
 if not ok:raise ValueError('Unable to read '+str(t))
 return cv2.resize(frame,(2048,942))
def sample(rank,t,rect):
 x,y,w,h=rect
 bgr=get_frame(t)[y:y+h,x:x+w,:]
 b,g,r=cv2.split(bgr)
 fg=((r>90)&(r>g*1.4)&(r>b*1.4)) | ((b<120)&(g<120)&(r<120))
 ys,xs=np.where(fg)
 if not len(xs):raise ValueError(rank)
 fg=fg[ys.min():ys.max()+1,xs.min():xs.max()+1]
 img=cv2.resize(fg.astype('uint8'),(32,40),interpolation=cv2.INTER_NEAREST)*255
 cv2.imwrite(str(assets/f'rank_{rank}.png'),img)
for key,t,rect in [('8',76,(953,434,27,36)),('K',52,(931,436,28,34)),('10',36,(927,433,40,36))]:
 sample(key,t,rect)
print('Generated rank_8, rank_K, rank_10 from video.')
