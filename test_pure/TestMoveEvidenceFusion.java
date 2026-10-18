package vn.chatheotracker;
import java.util.*;
public final class TestMoveEvidenceFusion {
    private static void ok(boolean yes,String what){if(!yes)throw new AssertionError(what);}
    private static List<String> cards(String s){return Arrays.asList(s.split(" "));}
    private static MoveEvidenceFusion.CounterChange drop(int seat,int n,long t){return new MoveEvidenceFusion.CounterChange(seat,n,t);}
    public static void main(String[] args){
        ok(PlayStructure.classify(cards("8S 8H 9C 9D 10H 10C")).kind==PlayStructure.Kind.THREE_PAIRS,"three pairs");
        ok(PlayStructure.classify(cards("8S 8H 9C 9D 10H 10C JS JD")).kind==PlayStructure.Kind.FOUR_PAIRS,"four pairs");
        ok(PlayStructure.classify(cards("4S 5H 6C 7D 8H 9S 10C")).kind==PlayStructure.Kind.STRAIGHT,"seven card straight");
        ok(PlayStructure.classify(cards("8S 8H 9C 9D JS JD QH QC")).kind==PlayStructure.Kind.OTHER,"non consecutive pairs");
        ok(PlayStructure.classify(cards("8S 8H 9C 9D 10H 10C 2S 2H")).kind==PlayStructure.Kind.OTHER,"no two in pairs");
        MoveEvidenceFusion f=new MoveEvidenceFusion();
        f.observe(cards("8S 8H 9C 9D"),2000);
        f.observe(cards("9C 9D 10H 10C JS JD"),2220);
        f.observe(cards("8S 8H 10H 10C JS JD"),2440);
        MoveEvidenceFusion.Outcome result=f.match(Collections.singletonList(drop(2,8,2300)),
            Collections.emptySet(),Collections.emptySet(),cards("10H 10C JS JD"),2580);
        ok(result!=null&&result.seat==2&&result.cards.size()==8&&"FOUR_PAIRS".equals(result.shape),"fuse eight partly visible cards");
        ok(f.match(Collections.singletonList(drop(2,8,2300)),Collections.emptySet(),
            cards("8S"),cards("10H 10C"),2580)==null,"my known card excludes opponent");
        ok(f.match(Collections.singletonList(drop(2,8,2300)),cards("9C"),
            Collections.emptySet(),cards("10H 10C"),2580)==null,"previous played excluded");
        ok(f.match(Arrays.asList(drop(1,8,2300),drop(2,8,2390)),
            Collections.emptySet(),Collections.emptySet(),cards("10H 10C"),2580)==null,"ambiguous actor");
        ok(f.match(Collections.singletonList(drop(2,7,2300)),
            Collections.emptySet(),Collections.emptySet(),cards("10H 10C"),2580)==null,"no missing card invented");
        ok(f.match(Collections.singletonList(drop(2,8,2300)),
            Collections.emptySet(),Collections.emptySet(),cards("2H"),2580)==null,"inconsistent active layer");
        ok(f.hasRecentActive(3050),"brief bright-layer grace during counter OCR delay");
        ok(!f.hasRecentActive(3700),"do not credit stale bright layer as current");
        f.clear();
        ok(f.match(Collections.singletonList(drop(2,8,2300)),
            Collections.emptySet(),Collections.emptySet(),Collections.emptyList(),2580)==null,"clear resets evidence");
        MoveEvidenceFusion g=new MoveEvidenceFusion();
        g.observe(cards("4C 5S 6H 7D 8C"),1200);
        ok(g.match(Collections.singletonList(drop(0,5,1400)),Collections.emptySet(),
            Collections.emptySet(),cards("4C 5S 6H 7D 8C"),1550)!=null,"five card straight");
        MoveEvidenceFusion conflicting=new MoveEvidenceFusion();
        conflicting.observe(cards("8S 8H 9C 9D 10H 10C JS JD"),1800);
        conflicting.observe(cards("QC"),2390);
        ok(conflicting.match(Collections.singletonList(drop(1,8,2390)),
            Collections.emptySet(),Collections.emptySet(),cards("QC"),2450)==null,
            "reject prior full play when another bright lead appears at the drop");
        MoveEvidenceFusion h=new MoveEvidenceFusion();
        h.observe(cards("8S 8H 9C 9D 10H 10C"),1200);
        ok(h.match(Collections.singletonList(drop(1,6,1400)),Collections.emptySet(),
            Collections.emptySet(),cards("8S 8H 9C 9D 10H 10C"),1550)!=null,"six cards supported");
        MoveEvidenceFusion j=new MoveEvidenceFusion();
        j.observe(cards("8S 8H 9C 9D 10H 10C JS"),2000);
        ok(j.match(Collections.singletonList(drop(1,8,2100)),Collections.emptySet(),
            Collections.emptySet(),cards("8S 8H 9C"),2400)==null,"seven of eight abstain");
        System.out.println("TestMoveEvidenceFusion OK");
    }
}
