exec(open('/mnt/data/tracker_v28/probe_hand.py').read().split('for num,n in')[0])
arr=np.array(Image.open('/mnt/data/tracker_v28/DEBUG_ANH1_2048.png').convert('RGB'))
for i,x in enumerate([844,938,1032,1127,1221]):
 y=next((yy for yy in range(666,756) if (arr[yy,x+12:x+80,:].min(axis=1)>202).sum()>37),None)
 print('\nslot',i,'x',x,'y',y)
 options=[]
 for dx in [-5,-2,0,3,6]:
  for dy in [-5,0,5,10,15,20,25,30,35,40]:
   top=y-5+dy
   rr=classify(extract(arr,x-3+dx+9,top+11,x-3+dx+73,top+67,32,40,True),RT)
   ss=classify(extract(arr,x-3+dx+8,top+71,x-3+dx+69,top+129,28,28,False),ST)
   valid=rr[1]>=.79 and rr[2]>=.022 and ss[1]>=.84 and ss[2]>=.014
   options.append((valid,round(rr[1]*ss[1],3),rr,ss,dx,dy))
 print(*sorted(options,reverse=True)[:6],sep='\n')
