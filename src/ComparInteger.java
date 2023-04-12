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

    @Override public int compareTo(ComparInteger otherint) {
        ComparablePowerSort.totalComparisonCosts++;
        return Integer.compare(this.value(), otherint.value());
    }
}
