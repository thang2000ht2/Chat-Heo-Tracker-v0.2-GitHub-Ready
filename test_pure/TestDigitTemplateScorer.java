package vn.chatheotracker;
public final class TestDigitTemplateScorer {
    private static void ok(boolean v,String m){if(!v)throw new AssertionError(m);}
    private static boolean[][] glyph(int kind){
        boolean[][] a=new boolean[22][18];
        for(int y=1;y<21;y++)for(int x=2;x<16;x++)
            a[y][x]=kind==0?(x==2||x==15||y==1||y==20):
                kind==1?(x==2||x==15||y==1||y==20||y==10):
                (x==4||x==13||y==1||y==20);
        return a;
    }
    public static void main(String[] args){
        boolean[][][] templates={glyph(0),glyph(1),glyph(2)};
        DigitTemplateScorer.Ranked correct=DigitTemplateScorer.rank(glyph(0),templates);
        ok(correct.accepted&&correct.winner==0,"distinct exact glyph accepted");
        boolean[][][] duplicate={glyph(0),glyph(0),glyph(2)};
        DigitTemplateScorer.Ranked uncertain=DigitTemplateScorer.rank(glyph(0),duplicate);
        ok(!uncertain.accepted&&"ambiguous_margin".equals(uncertain.reason),
            "equal candidates cause abstention instead of guessing");
        System.out.println("TestDigitTemplateScorer OK");
    }
}
