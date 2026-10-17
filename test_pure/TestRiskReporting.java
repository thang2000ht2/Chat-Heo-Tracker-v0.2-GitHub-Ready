package vn.chatheotracker;
public final class TestRiskReporting {
    private static void check(boolean pass,String message){if(!pass)throw new AssertionError(message);}
    public static void main(String[] args){
        String saturated=RiskReporting.describe(.98,.96,1.0,192);
        check(saturated.contains("bão hòa")&&!saturated.contains("%"),"No false 100% labels");
        String tied=RiskReporting.describe(.51,.49,.52,192);
        check(tied.contains("tương đương")&&!tied.contains("%"),"Tie guard");
        check(RiskReporting.describe(.20,.13,.76,192).contains("chưa hiệu chuẩn"),"Model bias guard");
        check(RiskReporting.describe(Double.NaN,0,1,192).contains("chưa đủ căn cứ"),"Invalid data guard");
        System.out.println("TestRiskReporting OK");
    }
}
