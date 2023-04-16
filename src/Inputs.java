import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;



public class Inputs {

    /**
     * Methods to create (random) inputs.
     *
     * @author Sebastian Wild (wild@uwaterloo.ca)
     */

    public static void swap(int[] a, int i, int j) {
        final int tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    public static void shuffle(final int[] A, final int left, final int right, final Random random) {
        int n = right - left + 1;
        for (int i = n; i > 1; i--)
            swap(A, left + i - 1, left + random.nextInt(i));
    }
        /**
         * common abstraction to generate inputs.
         * All relevant input distributions are given as an implementation of InputGenerator:
         * <ul>
         *     <li>{@link #RANDOM_PERMUTATIONS_GENERATOR}</li>
         *     <li>{@link #randomRunsGenerator(int)}</li>
         *     <li>{@link #timsortDragGenerator(int)}</li>
         *     <li>{@link #randomIidIntsGenerator(int)} (currently not used)</li>
         * </ul>
         * */
        public interface InputGenerator {
            /**
             * Generate next (random) input. If A == null, creates a new
             * array of the given length n. If A != null
             */
            default int[] next(int n, Random random, int[] A) {
                return A == null || A.length < n ?
                        newInstance(n, random) :
                        reuseInstance(n, random, A);
            }

            int[] newInstance(int n, Random random);

            default int[] reuseInstance(int n, Random random, int[] A) {
                return newInstance(n, random);
            }
        }

        /**
         * uniformly generated random permutations of [1..n]
         * */
        public static InputGenerator RANDOM_PERMUTATIONS_GENERATOR = new InputGenerator() {
            @Override
            public int[] newInstance(final int n, final Random random) {
                return randomPermutation(n, random);
            }

            @Override
            public int[] reuseInstance(final int n, final Random random, final int[] A) {
                shuffle(A, 0, n - 1, random);
                return A;
            }

            @Override public String toString() { return "random-permutations"; }
        };

        /**
         * random runs of a fixed expected run length.
         *
         * They are generated by first shuffling the array randomly and then
         * sorting segments of random lengths, where the lengths of the
         * segments are iid Geometric(1/runLen) distributed.
         * */
        public static InputGenerator randomRunsGenerator(final int runLen) {
            return new InputGenerator() {

                @Override
                public int[] newInstance(final int n, final Random random) {
                    return randomRuns(n, runLen, random);
                }

                @Override
                public int[] reuseInstance(final int n, final Random random, final int[] A) {
                    shuffle(A, 0, n-1, random);
                    sortRandomRuns(A, 0, n-1, runLen, random);
                    return A;
                }

                @Override public String toString() { return "runs-with-exp-len-" + runLen; }
            };
        }

//        /**
//         * Arrays with run lengths given by the R_Tim (Buss and Knop 2018)
//         * sequence of run lengths that cause Timsort to do unbalanced merges.
//         *
//         * All run lengths are multiplied by minRunLen, which should be >= 32.
//         * Rationale: R_Tim contains only lengths {1,2,3}, but
//         * Timsort extends runs below a minimal length to that minimal length.
//         * JDK Timsort uses at most 32 here, see {@link Timsort#minRunLength(int)}.
//         *
//         * Even without explicit extension of runs (as in {@link TimsortStrippedDown},
//         * we always have runs of length >= 2 since descending is also allowed.
//         */
        public static InputGenerator timsortDragGenerator(final int minRunLen) {
            return new InputGenerator() {

                @Override
                public int[] newInstance(final int n, final Random random) {
                    int[] A = new int[n];
                    reuseInstance(n, random, A);
                    return A;
                }

                @Override
                public int[] reuseInstance(final int n, final Random random, final int[] A) {
                    fillWithTimsortDrag(A, minRunLen, random);
                    return A;
                }

                @Override public String toString() { return "timsort-drag-minRunLen-" + minRunLen; }
            };
        }

        /** arrays filled with random iid [1..max] ints. */
        public static InputGenerator randomIidIntsGenerator(final int max) {
            return new InputGenerator() {

                @Override
                public int[] newInstance(final int n, final Random random) {
                    return randomUaryArray(max, n, random);
                }

                @Override
                public int[] reuseInstance(final int n, final Random random, final int[] A) {
                    for (int i = 1; i < A.length; i++)
                        A[i] = random.nextInt(max) + 1;
                    return A;
                }

                @Override
                public String toString() { return "iid-max-"+max; }
            };
        }

        public static int[] randomRuns(int n, int expRunLen, Random random) {
            int[] A = randomPermutation(n, random);
            sortRandomRuns(A, 0, n-1, expRunLen, random);
            return A;
        }

        public static void sortRandomRuns(final int[] A, final int left, int right, final int expRunLen, final Random random) {
            for (int i = left; i < right;) {
                int j = 1;
                while (random.nextInt(expRunLen) != 0) ++j;
                j = Math.min(right,i+j);
                Arrays.sort(A, i, j+1);
                i = j+1;
            }
        }

        /** Recursively computes R_Tim(n) (see Buss and Knop 2018) */
        public static LinkedList<Integer> timsortDragRunlengths(int n) {
            LinkedList<Integer> res;
            if (n <= 3) {
                res = new LinkedList<>();
                res.add(n);
            } else {
                int nPrime = n/2;
                int nPrimePrime = n - nPrime - (nPrime-1);
                res = timsortDragRunlengths(nPrime);
                res.addAll(timsortDragRunlengths(nPrime-1));
                res.add(nPrimePrime);
            }
            return res;
        }

        private static LinkedList<Integer> RTimCache = null;
        private static int RTimCacheN = -1;

        /**
         * Fills the given array A with a Timsort drag input of the correct length
         * where all lengths are multiplied by minRunLen.
         * random is used for shuffling, see {@link #fillWithUpAndDownRuns(int[], List, int, Random)}.
         */
        public static void fillWithTimsortDrag(int[] A, int minRunLen, Random random) {
            int N = A.length;
            int n = N / minRunLen;
            if (RTimCacheN != n || RTimCache == null) {
                RTimCacheN = n;
                RTimCache = timsortDragRunlengths(n);
            }
            LinkedList<Integer> RTim = RTimCache;
            fillWithUpAndDownRuns(A, RTim, minRunLen, random);
        }

        /**
         * Fills the given array A with a random input that runs of the given list of run
         * lengths, alternating between ascending and descending runs.
         * More precisely, the array is first filled with a random permutation
         * of [1..n], and then for i=0..l-1 segments of runLengths.get(i) * runLenFactor
         * are sorted ascending when i mod 2 == 0 and descending otherwise
         * (where l = runLengths.size()).
         *
         * The sum of all lengths in runLengths times runLenFactor should be equal to the
         * length of A.
         */
        public static void fillWithUpAndDownRuns(final int[] A, final List<Integer> runLengths,
                                                 final int runLenFactor, final Random random) {
            int n = A.length;
            assert total(runLengths) * runLenFactor == n;
            for (int i = 0; i < n; ++i) A[i] = i+1;
            shuffle(A, 0, n-1, random);
            boolean reverse = false;
            int i = 0;
            for (int l : runLengths) {
                int L = l * runLenFactor;
                Arrays.sort(A, Math.max(0,i-1), i+L);
                if (reverse) myPowersort.reverseRange(A, Math.max(0,i-1), i+L-1);
                reverse = !reverse;
                i += L;
            }
        }

        public static int total(List<Integer> l) {
            return l.stream().mapToInt(Integer::intValue).sum();
        }

        /** return new array filled with random permutation of [1..n] */
        public static int[] randomPermutation(final int len, Random random) {
            int[] res = new int[len];
            for (int i = 1; i <= len; ++i) res[i - 1] = i;
            for (int i = len; i > 1; i--)
                swap(res, i - 1, random.nextInt(i));
            return res;
        }

        /** return new array filled with iid uniform numbers in [1..u] */
        public static int[] randomUaryArray(final int u, final int len, Random random) {
            int res[] = new int[len];
            for (int i = 0; i < res.length; i++) {
                res[i] = random.nextInt(u)+1;
            }
            return res;
        }

    }