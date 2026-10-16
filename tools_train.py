from PIL import Image
from pathlib import Path
import glob,sys
import numpy as np,cv2
out=Path(__file__).parent/'app/src/main/assets';out.mkdir(parents=True,exist_ok=True)
im=cv2.imread(sys.argv[1] if len(sys.argv)>1 else glob.glob('/mnt/data/Screenshot_2026-10-16-22-13-26*')[0])
if im is None or im.shape[:2]!=(942,2048):raise SystemExit('Need 2048x942 reference screenshot')
def mask(a):
 b,g,r=cv2.split(a)
 return (((r>90)&(r>g*1.4)&(r>b*1.4)) | ((b<120)&(g<120)&(r<120))).astype('uint8')
def dump(name,rect,dims=(32,40)):
 x,y,w,h=rect; crop=mask(im[y:y+h,x:x+w]); ys,xs=np.where(crop>0)
 if len(xs)==0: print('EMPTY',name);return
 crop=crop[ys.min():ys.max()+1,xs.min():xs.max()+1]
 crop=cv2.resize(crop,dims,interpolation=cv2.INTER_NEAREST)*255
 cv2.imwrite(str(out/name),crop)
# rank exemplars: card glyphs from user's two screenshots, not invented fonts.
rank={'3':(929,432,27,37),'4':(976,432,29,37),'5':(1022,434,27,37),
 '6':(571,726,39,52),'7':(761,729,35,50),'9':(950,726,38,52),'10':(1120,725,35,52),
 'J':(1238,726,24,51),'Q':(1327,724,43,62),'A':(1422,728,45,50),'2':(1612,725,39,52)}
for name,rect in rank.items():dump('rank_%s.png'%name,rect)
suit={'S':(930,476,25,25),'H':(976,476,27,25),'D':(1025,476,21,25),'C':(761,787,36,36)}
for name,rect in suit.items(): dump('suit_%s.png'%name,rect,(28,28))
print('generated',len(list(out.glob('*.png'))),'template files')
