package vn.chatheotracker;
import java.util.Arrays;
import java.util.Collections;
public final class TestResidualHandQuality {
    public static void main(String[] args){
        double connected=ResidualHandQuality.penalty(Arrays.asList("3S","3H","4S","5C"));
        double scattered=ResidualHandQuality.penalty(Arrays.asList("3S","7H","9D","JS"));
        if(!(connected<scattered))throw new AssertionError("Pairs/straights must be less fragmented");
        if(ResidualHandQuality.penalty(Collections.emptyList())!=0)throw new AssertionError("Finish is best");
        System.out.println("TestResidualHandQuality OK");
    }
}
