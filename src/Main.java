import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    public static Boolean isSorted(Object[] array, Comparator cmp, Object[] original) {
        if (cmp == null) {
            cmp = Comparator.naturalOrder();
        };
        Arrays.sort(original, 0, original.length);
        //Ascending
        for (int i = 1; i < array.length; i++ ) {
            if (!Objects.equals(array[i].toString(), original[i].toString())) {
                System.out.println("Algorithm says " + array[i] + " timsort says "+ original[i]);
                return false;
            }
            if (cmp.compare(array[i - 1], array[i]) > 0) {
                System.out.println("The offending pair " + array[i - 1] + " " + array[i] + " index " + i + " original says " + original[i]);
                return false;
            }
        }
        return true;
    };

    public static boolean ABORT_IF_RESULT_IS_NOT_SORTED = true;
    public static boolean COUNT_COSTS = true;

    public static void main(String[] args) throws IOException {

        System.in.read();
        long seed = 42424242;
        int warmupRounds = 10_000;
        List<Integer> sizes = Arrays.asList(1_000_000);
        int reps = 100;
        double[] msTimes = new double[reps];
        int inputRunLength = 1000;

        Inputs.InputGenerator[] inputTypes = {Inputs.RANDOM_PERMUTATIONS_GENERATOR, Inputs.randomRunsGenerator(inputRunLength), Inputs.timsortDragGenerator(32)};
        Inputs.InputGenerator warmupInput = Inputs.RANDOM_PERMUTATIONS_GENERATOR;

        final String algoName = "Timsort";

        String outdirect = "/Users/ChristianCarroll/Documents/Uni_final_year/Dissertation/PowerSort/Powersort_project/Output/";
        String fileName = algoName;

        SimpleDateFormat format = new SimpleDateFormat("-yyyy-MM-dd_HH-mm-ss");
        fileName += format.format(new Date());
        fileName += "-reps" + reps;
        fileName += "-ns";
        for (int n : sizes) fileName += "-" + n;
        fileName += "-seed" + seed;
        fileName += ".csv";
        String outFileName = outdirect + fileName;

        File outFile = new File(outFileName);

        BufferedWriter out = new BufferedWriter(new FileWriter(outFile));

        System.out.println("Doing warmup (" + warmupRounds + " rounds)");
        Random random = new Random(seed);
        for (int r = 0; r < warmupRounds; ++r) {
                for (final int size : new int[]{10000, 1000, 1000}) {
                    final int[] intWarm = warmupInput.next(size, random, null);
                    final Integer[] warmup = Arrays.stream( intWarm ).boxed().toArray( Integer[]::new );
                    //ComparablePowerSort.sort(warmup,0,size, null, 0, 0);
                    Arrays.sort(warmup, 0, size);
                }
        }
        System.out.println("Warmup finished!\n");

        if (COUNT_COSTS) {
            out.write("algorithm,ms,n,input,input-num,merge-cost,comparison-cost\n");
            System.out.println("Also counting merge costs.");
        } else {
            out.write("algorithm,ms,n,input,input-num\n");
            System.out.println("Not counting merge costs.");
        }

        System.out.println("\nRuns with individual timing (skips first run):");
        random = new Random(seed);
        for (Inputs.InputGenerator input : inputTypes) {
            for (final int size : sizes) {
                int[] A = input.next(size, random, null);
                ComparInteger[] compareA = new ComparInteger[size];
                ComparInteger[] copyForCheck = new ComparInteger[size];
                for (int i = 0; i < size; i++) {
                    compareA[i] = new ComparInteger(A[i]);
                    copyForCheck[i] = new ComparInteger(A[i]);
                }
                Integer[] integerA = Arrays.stream(A).boxed().toArray(Integer[]::new);
                for (int r = 0; r < reps; ++r) {
                    if (r != 0) {
                        A = input.next(size, random, A);
                        for (int i = 0; i < size; i++) {
                            compareA[i] = new ComparInteger(A[i]);
                            copyForCheck[i] = new ComparInteger(A[i]);
                        }
                        integerA = Arrays.stream(A).boxed().toArray(Integer[]::new);
                    }
                    ComparablePowerSort.totalMergeCosts = 0;
                    ComparablePowerSort.totalComparisonCosts = 0;
                    final long startNanos = System.nanoTime();
                    Arrays.sort(compareA, 0, size);
                    //ComparablePowerSort.sort(compareA,0,size, null, 0, 0);
                    final long endNanos = System.nanoTime();
                    long comparisons = ComparablePowerSort.totalComparisonCosts; //Save them here as the isSorted checker runs array.sort which then adds to the comparison count
                    if (ABORT_IF_RESULT_IS_NOT_SORTED && !isSorted(compareA, null, copyForCheck)) {
                        System.err.println("RESULT NOT SORTED!");
                        System.exit(3);
                    }
                    final double msDiff = (endNanos - startNanos) / 1e6;
                    msTimes[r] = msDiff;
                    if (r != 0) {
                        // Skip first iteration, often slower!
                        if (COUNT_COSTS)
                            out.write(algoName + "," + msDiff + "," + size + "," + input + "," + r + "," + ComparablePowerSort.totalMergeCosts + "," + comparisons + "\n");
                        else
                            out.write(algoName + "," + msDiff + "," + size + "," + input + "," + r + "\n");
                        out.flush();
                    }
                }
                double averageMs = Arrays.stream(msTimes).average().orElse(Double.NaN);
                double minMS = Arrays.stream(msTimes).min().getAsDouble();
                double maxMS = Arrays.stream(msTimes).max().getAsDouble();
                System.out.println("Average time: " + averageMs);
                System.out.println("Fastest time: " + minMS);
                System.out.println("Slowest time: " + maxMS);
                out.write("Finished input type: " + input + " Average ms: " + averageMs + " Fastest time (ms): " + minMS + " Slowest time (ms): " + maxMS + "\n");
                //System.out.println("avg-ms=" + (float) (samples.mean()) + ",\t algo=" + algoName + ", n=" + size + "     (" + total+")\t" + samples);
            }
        }
        out.write("#finished: " + format.format(new Date()) + "\n");
        out.close();
    }
}