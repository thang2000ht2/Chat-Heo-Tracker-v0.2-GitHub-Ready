"""Offline video/screenshot checker. Initial gray+white pile layers, card rank templates.
Not a validated 52-card recognizer: repeated symbols and animations require verification.
"""
import cv2,numpy as np
from test_on_screenshots import rank,suit,ink,comps,classify

def detected_row(im, background, verbose=False):
 xleft,xright,ymin,ymax=650,1400,300,620
 if background=='gray':
  # Gray previous-play pile is tinted RGB ~185, bounded to first card row.
  ymin,ymax=295,420
  a=im[:,:,0];b=im[:,:,1];c=im[:,:,2]
  mask=(np.minimum(np.minimum(a,b),c)>=155)&(np.maximum(np.maximum(a,b),c)<=210)&((np.maximum(np.maximum(a,b),c).astype('int16')-np.minimum(np.minimum(a,b),c).astype('int16'))<15)
 else:
  mask=np.all(im>215,axis=2)
 ys=[y for y in range(ymin,ymax,2) if mask[y,xleft:xright:2].sum()>35]
 if not ys:return []
 y0=ys[0]
 xs=[x for x in range(xleft,xright) if mask[y0+6:y0+64:2,x].sum()>=9]
 if not xs: return []
 x0,x1=xs[0],xs[-1]
 if x1-x0<65 or x1-x0>630:return []
 raw=comps(ink(im[y0+2:y0+57,x0+1:x1]),x0+1,y0+2)
 glyphs=[z for z in raw if z[4]>=65 and 19<=z[3] and 5<=z[2]<=70]
 glyphs.sort()
 i=0
 while i<len(glyphs)-1:
  z,n=glyphs[i],glyphs[i+1]
  if z[2]<=17 and n[0]-(z[0]+z[2])<13 and abs(z[1]-n[1])<12:
   x=min(z[0],n[0]);y=min(z[1],n[1]);w=max(z[0]+z[2],n[0]+n[2])-x;h=max(z[1]+z[3],n[1]+n[3])-y
   pix=np.zeros((h,w),bool)
   for v in (z,n):pix[v[1]-y:v[1]-y+v[3],v[0]-x:v[0]-x+v[2]]|=v[-1]
   glyphs[i]=(x,y,w,h,z[4]+n[4],pix);glyphs.pop(i+1)
  i+=1
 cards=[];firstGlyph=None
 for z in glyphs:
  (rk,rs),_=classify(z,rank,(32,40))
  if z[2]>=35 and (rk!='10' or rs<.78):continue
  if rs<.68:continue
  if firstGlyph is not None:
   dx=z[0]-firstGlyph;nearest=round(dx/46.5)
   if nearest<1 or abs(dx-nearest*46.5)>8:continue
  rawSuit=comps(ink(im[y0+52:y0+105,max(x0,z[0]-8):min(x1,z[0]+62)]),max(x0,z[0]-8),y0+52)
  candidates=[s for s in rawSuit if s[4]>=70 and 13<=s[2]<=50 and 14<=s[3]<=45 and abs(s[0]+s[2]/2-z[0]-z[2]/2)<=24]
  if not candidates:continue
  sk,ss=max((classify(s,suit,(28,28))[0] for s in candidates), key=lambda p:p[1]);
  if ss<.65:continue
  if firstGlyph is None:firstGlyph=z[0]
  key=rk+sk
  if key not in cards:cards.append(key)
 if verbose:print(background,'y',y0,'bounds',x0,x1,'raw',[(g[0],g[1],g[2],g[3]) for g in glyphs], 'cards',cards)
 return cards

def detect(image,verbose=False):
 im=cv2.resize(image,(2048,942)) if image.shape[:2]!=(942,2048) else image
 result=[]
 for layer in ('gray','white'):
  for card in detected_row(im,layer,verbose):
   if card not in result:result.append(card)
 return result

if __name__=='__main__':
 import sys
 v=cv2.VideoCapture(sys.argv[1]);tlist=[12,24,32,36,40,44,52,56,60,68,76,80,88,92,96,100,108,112]
 for t in tlist:
  v.set(cv2.CAP_PROP_POS_MSEC,1000*t);ok,f=v.read()
  if not ok:continue
  print('\n',t,detect(f,True))
