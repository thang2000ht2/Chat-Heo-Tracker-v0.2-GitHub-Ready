package vn.chatheotracker;

/** Pure Java digit-template ranking. Scores are similarity measures, NOT probabilities.
 * Uncertain glyphs are deliberately left unread rather than assigned incorrectly. */
final class DigitTemplateScorer {
    static final double MIN_SCORE=0.69;
    static final double MIN_MARGIN=0.060;
    static final class Ranked {
        final int winner,runnerUp;
        final double first,second,margin;
        final boolean accepted;
        final String reason;
        Ranked(int winner,int runnerUp,double first,double second) {
            this.winner=winner;this.runnerUp=runnerUp;this.first=first;this.second=second;
            margin=Math.max(0,first-second);
            reason=winner<0?"no_template":first<MIN_SCORE?"low_score":
                margin<MIN_MARGIN?"ambiguous_margin":"ok";
            accepted="ok".equals(reason);
        }
    }
    static Ranked rank(boolean[][] observed,boolean[][][] templates){
        int best=-1,second=-1;double first=-1,runner=-1;
        for(int d=0;d<templates.length;d++){
            if(templates[d]==null)continue;
            double value=score(observed,templates[d]);
            if(value>first){runner=first;second=best;first=value;best=d;}
            else if(value>runner){runner=value;second=d;}
        }
        return new Ranked(best,second,Math.max(0,first),Math.max(0,runner));
    }
    private static double score(boolean[][] shape,boolean[][] template){
        final int w=20,h=26;
        boolean[][] a=resize(shape,w,h),b=resize(template,w,h);
        int one=0,two=0,both=0;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            if(a[y][x])one++;
            if(b[y][x])two++;
            if(a[y][x]&&b[y][x])both++;
        }
        double ratioA=(double)shape[0].length/shape.length;
        double ratioB=(double)template[0].length/template.length;
        return 2.0*both/Math.max(1,one+two)-0.17*Math.abs(ratioA-ratioB);
    }
    private static boolean[][] resize(boolean[][] src,int w,int h){
        boolean[][] out=new boolean[h][w];
        int H=src.length,W=src[0].length;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)
            out[y][x]=src[Math.min(H-1,y*H/h)][Math.min(W-1,x*W/w)];
        return out;
    }
    private DigitTemplateScorer(){}
}
