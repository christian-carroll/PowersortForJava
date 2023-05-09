
import java.util.Random;

public class ComparInteger implements Comparable<ComparInteger> {

    public static String slowDownComparisons = "";
    private int value;
    private int seed = 4637282;
    private Random random = new Random(seed);

    public ComparInteger(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String toString() {
        return "" + this.value;
    }

    private void slowDown() {
        slowDownComparisons = "";
        int nextInt = random.nextInt(0,500);
        slowDownComparisons += nextInt;
    }

    @Override public int compareTo(ComparInteger otherint) {
        Main.totalComparisonCosts++;
        return Integer.compare(this.value(), otherint.value());
    }
}
