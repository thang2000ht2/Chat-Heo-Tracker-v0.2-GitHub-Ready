from pathlib import Path
p=Path('/mnt/data/tracker_v28/app/src/main/java/vn/chatheotracker/HandRecognizer.java')
s=p.read_text()
a='''        if(best==null)return new Result(Collections.emptyList(),false,"KHÔNG THẤY QUẠT");
        // White strip is measured from the fan top. The rank/suit corner boxes'''
b='''        // A raised 9/10/J can break the white strip into separate card tops.
        // Infer card count from several white left edges, but do not classify
        // any card until its rank AND suit templates pass the confidence gates.
        HandFanGeometry.Fan raised=findRaisedFan(b);
        if(raised!=null && (best==null || raised.count>best.count))best=raised;
        if(best==null)return new Result(Collections.emptyList(),false,"KHÔNG THẤY QUẠT");
        // White strip is measured from the fan top. The rank/suit corner boxes'''
assert a in s;s=s.replace(a,b)
a='''        return partial;
    }
    private static boolean ink(int a) {'''
b='''        // The global y-offset is not enough when individual cards are selected
        // (raised) while their neighbours remain at the bottom of the screen.
        // Use measured white-top anchors and independently classify each slot.
        Result raisedResult=recognizeRaisedCorners(b,best);
        if(raisedResult.complete || raisedResult.cards.size()>partial.cards.size())return raisedResult;
        return partial;
    }

    /** Detect a fan from top-edge segments on multiple rows; selected cards may
     * have a 20-40px higher top edge than the adjacent cards.
     */
    private static HandFanGeometry.Fan findRaisedFan(Bitmap bitmap){
        ArrayList<Integer> edges=new ArrayList<>();
        for(int y=678;y<=751;y+=3){
            int start=-1;
            for(int x=350;x<=1821;x++){
                boolean white=false;
                if(x<=1820){
                    int c=bitmap.getPixel(x,y);
                    white=Color.red(c)>202 && Color.green(c)>202 && Color.blue(c)>202;
                }
                if(white && start<0)start=x;
                else if(!white && start>=0){
                    int len=x-start;
                    if(len>=74 && len<=210)edges.add(start);
                    start=-1;
                }
            }
        }
        return HandFanGeometry.fromWhiteEdges(edges,714);
    }

    /** Earliest bright rectangle top for this specific slot. A selected card
     * will rise without moving the other slots, so this must be per-card.
     */
    private static int firstWhiteTop(Bitmap b,int x){
        for(int y=672;y<=755;y++){
            int white=0;
            for(int xx=x+12;xx<=x+79;xx+=2){
                if(xx<0||xx>=b.getWidth())continue;
                int c=b.getPixel(xx,y);
                if(Color.red(c)>202 && Color.green(c)>202 && Color.blue(c)>202)white++;
            }
            if(white>=19)return y;
        }
        return -1;
    }
    private Result recognizeRaisedCorners(Bitmap b,HandFanGeometry.Fan fan){
        if(fan==null || fan.count<=0)return new Result(Collections.emptyList(),false,"KHÔNG THẤY QUẠT");
        ArrayList<String> visible=new ArrayList<>(),slots=new ArrayList<>();
        for(int i=0;i<fan.count;i++){
            int x=fan.left-3+(int)Math.round(i*94.6);
            int whiteTop=firstWhiteTop(b,x);
            String bestCard=null;
            double bestScore=-1;
            if(whiteTop>=0){
                // Include +30px: a lower card can be partly covered by an
                // earlier raised one (e.g. the rightmost K in screenshot 1).
                for(int dy:new int[]{0,-5,5,10,15,20,25,30,35,40}){
                    int top=whiteTop-5+dy;
                    for(int dx:new int[]{-5,0,3,6}){
                        boolean[][] rank=extract(b,x+dx+9,top+11,x+dx+73,top+67,32,40,true);
                        boolean[][] suit=extract(b,x+dx+8,top+71,x+dx+69,top+129,28,28,false);
                        if(rank==null || suit==null)continue;
                        Match r=classify(rank,rankTemplates),st=classify(suit,suitTemplates);
                        if(r.score>=.79 && r.margin>=.022 && st.score>=.84 && st.margin>=.014){
                            double quality=r.score*st.score;
                            if(quality>bestScore){bestScore=quality;bestCard=r.label+st.label;}
                        }
                    }
                }
            }
            if(bestCard!=null && visible.contains(bestCard))bestCard=null;
            slots.add(bestCard);
            if(bestCard!=null)visible.add(bestCard);
        }
        return new Result(visible,visible.size()==fan.count,
            visible.size()==fan.count?"GÓC BÀI LINH HOẠT":"GÓC "+visible.size()+"/"+fan.count,
            slots,fan.count,fan.left);
    }
    private static boolean ink(int a) {'''
assert a in s;s=s.replace(a,b)
p.write_text(s)
