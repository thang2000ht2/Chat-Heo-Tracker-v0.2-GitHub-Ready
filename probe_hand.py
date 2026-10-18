from PIL import Image
import numpy as np, cv2
from pathlib import Path
A=Path('/mnt/data/tracker_v28/app/src/main/assets')
R='3456789' # later
ranks=['3','4','5','6','7','8','9','10','J','Q','K','A','2']; suits=['S','C','D','H']
def tems(pref,ls):
 return {k:(np.array(Image.open(A/f'hand_{pref}_{k}.png').convert('RGB'))[:,:,0]>120) for k in ls}
RT=tems('rank',ranks);ST=tems('suit',suits)
def extract(arr,x0,y0,x1,y1,outw,outh,isrank):
 if min(x0,y0)<0 or x1>arr.shape[1] or y1>arr.shape[0]:return None
 b=arr[y0:y1,x0:x1]; r,g,bl=b[:,:,0],b[:,:,1],b[:,:,2]
 ink=((r.astype(float)>90)&(r>1.4*g)&(r>1.4*bl))|((r<120)&(g<120)&(bl<120))
 count,labels,stats,_=cv2.connectedComponentsWithStats(ink.astype('uint8'),8)
 mask=np.zeros(ink.shape,dtype=np.uint8)
 for i in range(1,count):
  xx,yy,w,h,n=stats[i]
  if n<35 or h<8 or w<3:continue
  if xx>(47 if isrank else 41) or yy>(43 if isrank else 44):continue
  mask[labels==i]=1
 coords=np.argwhere(mask>0)
 if len(coords)==0:return None
 y0,x0=coords.min(axis=0);y1,x1=coords.max(axis=0)+1
 return cv2.resize(mask[y0:y1,x0:x1],(outw,outh),interpolation=cv2.INTER_NEAREST)>0
def classify(mask,templates):
 if mask is None:return ('-',0,0)
 vals={}
 for k,t in templates.items():
  val=np.logical_and(mask,t).sum()*2/max(1,mask.sum()+t.sum())
  vals[k]=val
 vals=sorted(vals.items(),key=lambda x:x[1],reverse=True)
 return vals[0][0],round(float(vals[0][1]),3),round(float(vals[0][1]-vals[1][1]),3)
for num,n in [(1,5),(3,10)]:
 arr=np.array(Image.open(f'/mnt/data/tracker_v28/DEBUG_ANH{num}_2048.png').convert('RGB'))
 best=[]
 for y in range(668,761,2):
  white=(arr[y,350:1821,:].min(axis=1)>202)
  edges=np.diff(np.pad(white.astype('int'),(1,1)))
  for start,stop in zip(np.where(edges==1)[0],np.where(edges==-1)[0]):
   length=stop-start; count=round((length-160)/94.6)+1
   err=abs(length-(160+(count-1)*94.6))
   if count>=1 and count<=13 and length>=140 and err<=min(38,10+count*2.3):best.append((count,err,y,start+350,length))
 best.sort(key=lambda f:(-f[0],f[1]))
 print('\nIMAGE',num,'best',best[:8]);_,_,y,left,_=best[0]
 for i in range(n):
  x=left-3+round(i*94.6)
  options=[]
  for dx in [0,-3,3,-6,6,-10,10]:
   for dy in [0,-2,2,-8,-16,-24,-32,8,16]:
    top=y-5+dy
    rank=extract(arr,x+dx+9,top+11,x+dx+73,top+67,32,40,True)
    suit=extract(arr,x+dx+8,top+71,x+dx+69,top+129,28,28,False)
    rr=classify(rank,RT);ss=classify(suit,ST)
    valid=rr[1]>=.79 and rr[2]>=.022 and ss[1]>=.84 and ss[2]>=.014
    options.append((valid,round(rr[1]*ss[1],3),rr,ss,dx,dy))
  options.sort(reverse=True)
  print('slot',i,'x',x, 'best',options[:2])
