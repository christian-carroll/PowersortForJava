// import jdk.incubator.foreign.SymbolLookup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    public static Boolean isSorted(Object[] array, Comparator cmp) {
        if (cmp == null) {
            cmp = Comparator.naturalOrder();
        };
        //Ascending
        for (int i = 1; i < array.length; i++ ) {
            if (cmp.compare(array[i - 1], array[i]) > 0) {
                System.out.println("The offending pair " + array[i - 1] + " " + array[i]);
                return false;
            }
        }
        return true;
    };

    public static boolean ABORT_IF_RESULT_IS_NOT_SORTED = true;
    public static boolean COUNT_MERGE_COSTS = false;

    public static void main(String[] args) throws IOException {
        //System.in.read();
        long seed = 42424242;
        int warmupRounds = 10_000;
        List<Integer> sizes = Arrays.asList(1_000_000);
        int reps = 100;
        double[] msTimes = new double[reps];
        int inputRunLength = 20;

        sebsInputs.InputGenerator[] inputTypes = {sebsInputs.RANDOM_PERMUTATIONS_GENERATOR, sebsInputs.randomRunsGenerator(inputRunLength)};
        sebsInputs.InputGenerator warmupInput = sebsInputs.RANDOM_PERMUTATIONS_GENERATOR;

        final String algoName = "Powersort";

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
                    ComparablePowerSort.sort(warmup,0,size, null, 0, 0);
                    //Arrays.sort(warmup, 0, size);
                }
        }
        System.out.println("Warmup finished!\n");

        if (COUNT_MERGE_COSTS) {
            out.write("algorithm,ms,n,input,input-num,merge-cost\n");
            System.out.println("Also counting merge cost in MergeUtil.mergeRuns");
        } else {
            out.write("algorithm,ms,n,input,input-num\n");
            System.out.println("Not counting merge costs.");
        }

        System.out.println("\nRuns with individual timing (skips first run):");
        random = new Random(seed);
        for (sebsInputs.InputGenerator input : inputTypes) {
            for (final int size : sizes) {
                int total = 0;
                int[] A = input.next(size, random, null);
                Integer[] integerA = Arrays.stream(A).boxed().toArray(Integer[]::new);
                for (int r = 0; r < reps; ++r) {
                    if (r != 0) {
                        A = input.next(size, random, A);
                        integerA = Arrays.stream(A).boxed().toArray(Integer[]::new);
                    }
                    ComparablePowerSort.totalMergeCosts = 0;
                    final long startNanos = System.nanoTime();
                    //Arrays.sort(integerA, 0, size);
                    ComparablePowerSort.sort(integerA,0,size, null, 0, 0);
                    final long endNanos = System.nanoTime();
                    total += integerA[integerA.length / 2];
                    if (ABORT_IF_RESULT_IS_NOT_SORTED && !isSorted(integerA, null)) {
                        System.err.println("RESULT NOT SORTED!");
                        System.exit(3);
                    }
                    final double msDiff = (endNanos - startNanos) / 1e6;
                    msTimes[r] = msDiff;
                    if (r != 0) {
                        // Skip first iteration, often slower!
                        if (COUNT_MERGE_COSTS)
                            out.write(algoName + "," + msDiff + "," + size + "," + input + "," + r + "," + ComparablePowerSort.totalMergeCosts + "\n");
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