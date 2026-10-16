"""Prototype detector on the two user-supplied screenshots (offline; no OCR/cloud)."""
import cv2,numpy as np,glob,os,sys
from pathlib import Path
folder=Path(__file__).parent
ASSET=folder/'app/src/main/assets'
rank={p.stem.split('_')[1]:cv2.imread(str(p),0)>120 for p in ASSET.glob('rank_*.png')}
suit={p.stem.split('_')[1]:cv2.imread(str(p),0)>120 for p in ASSET.glob('suit_*.png')}
def ink(im):
 b,g,r=cv2.split(im)
 return (r>90)&(r>g*1.4)&(r>b*1.4) | ((b<120)&(g<120)&(r<120))
def comps(mask,xoff=0,yoff=0):
 n,lab,sts,cents=cv2.connectedComponentsWithStats(mask.astype('uint8'),8)
 result=[]
 for x,y,w,h,area in sts[1:]:
  if area<30:continue
  result.append((int(x+xoff),int(y+yoff),int(w),int(h),int(area),mask[y:y+h,x:x+w]))
 return result
def classify(c,templates,size):
 a=cv2.resize(c[-1].astype('uint8'),size,interpolation=cv2.INTER_NEAREST)>0
 scores={k:2*np.logical_and(a,v).sum()/max(1,a.sum()+v.sum()) for k,v in templates.items()}
 return sorted(scores.items(),key=lambda q:-q[1])[0],scores

def detect(im,verbose=False):
 W,H=2048,942
 if im.shape[:2]!=(H,W):im=cv2.resize(im,(W,H))
 xleft,xright,ytop,ybottom=650,1400,300,650
 ys=[y for y in range(ytop,ybottom,2) if np.all(im[y,xleft:xright:2,:]>215,axis=1).sum()>35]
 if not ys: return []
 y0=ys[0]
 xs=[x for x in range(xleft,xright) if np.all(im[y0+6:y0+64:2,x,:]>215,axis=1).sum()>=9]
 if not xs:return []
 x0,x1=xs[0],xs[-1]
 if x1-x0<65 or x1-x0>630:return []
 raw=comps(ink(im[y0+2:y0+57,x0+1:x1]),x0+1,y0+2)
 glyphs=[z for z in raw if z[4]>=65 and 19<=z[3] and 5<=z[2]<=70]
 glyphs.sort()
 if verbose:print('bounds',y0,x0,x1, 'raw glyphs', [(z[0],z[1],z[2],z[3]) for z in glyphs])
 i=0
 while i<len(glyphs)-1:
  z,n=glyphs[i],glyphs[i+1]
  if z[2]<12 and n[0]-(z[0]+z[2])<13 and abs(z[1]-n[1])<12:
   x=min(z[0],n[0]);y=min(z[1],n[1]);w=max(z[0]+z[2],n[0]+n[2])-x;h=max(z[1]+z[3],n[1]+n[3])-y
   pix=np.zeros((h,w),bool)
   for v in (z,n):pix[v[1]-y:v[1]-y+v[3],v[0]-x:v[0]-x+v[2]]|=v[-1]
   glyphs[i]=(x,y,w,h,z[4]+n[4],pix);glyphs.pop(i+1)
  i+=1
 cards=[]; firstGlyph=None
 for z in glyphs:
  if firstGlyph is not None:
   dx=z[0]-firstGlyph;nearest=round(dx/46.5)
   if nearest<1 or abs(dx-nearest*46.5)>11:continue
  (rk,rs),rks=classify(z,rank,(32,40))
  if rs<.63:continue
  rawSuit=comps(ink(im[y0+52:y0+112,max(x0,z[0]-8):min(x1,z[0]+62)]),max(x0,z[0]-8),y0+52)
  candidates=[s for s in rawSuit if s[4]>=70 and 13<=s[2]<=50 and 14<=s[3]<=45 and abs(s[0]+s[2]/2-z[0]-z[2]/2)<=24]
  scores=[]
  for s in candidates:
   (sk,ss),_=classify(s,suit,(28,28));scores.append((sk,ss))
  if verbose:print('rank',z[0],rk,round(rs,2),'suit',scores)
  if not scores:continue
  sk,ss=max(scores,key=lambda p:p[1]);
  if ss<.58:continue
  if firstGlyph is None:firstGlyph=z[0]
  card=rk+sk
  if card not in cards:cards.append(card)
 return cards
if __name__=='__main__':
 for fn in (sys.argv[1:] if len(sys.argv)>1 else sorted(glob.glob('/mnt/data/Screenshot_2026-10-16-22-13-*'))):
  image=cv2.imread(fn)
  print(os.path.basename(fn),detect(image,True))
