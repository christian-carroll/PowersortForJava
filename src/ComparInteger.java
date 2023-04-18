public class ComparInteger implements Comparable<ComparInteger> {

    private int value;

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

    @Override public int compareTo(ComparInteger otherint) {
        Main.totalComparisonCosts++;
        return Integer.compare(this.value(), otherint.value());
    }
}
