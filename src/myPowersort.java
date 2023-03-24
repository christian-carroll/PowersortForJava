import java.util.Arrays;

public class myPowersort {
    private static int nodePower(int left, int right, int startA, int startB, int endB) {
        int length = (right - left + 1);
        long l = (long) startA + (long) startB - ((long) left << 1); // 2*middleA
        long r = (long) startB + (long) endB + 1 - ((long) left << 1); // 2*middleB
        int a = (int) ((l << 30) / length); // middleA / 2n
        int b = (int) ((r << 30) / length); // middleB / 2n
        return Integer.numberOfLeadingZeros(a ^ b);
    }

    public static void reverseRange(int[] array, int start, int end) {
        while (start < end) {
            int temp = array[start];
            array[start++] = array[end];
            array[end--] = temp;
        }
    }

    private static int findRun(int[] array, int startIndex, int arrayEnd) {
        // weakly ascending
        if (array[startIndex+1] >= array[startIndex]) {
            for (int i = startIndex; i <= arrayEnd; i++) {
                if (i == arrayEnd) return arrayEnd;
                if (array[i + 1] < array[i]) return i;
            }
        }
        // strongly descending
        else {
            for (int i = startIndex; i <= arrayEnd; i++) {
                if (i == arrayEnd) return arrayEnd;
                if (array[i + 1] >= array[i]) {
                    reverseRange(array, startIndex, i);
                    return i;
                }
            }
        }
        //shouldn't be reached but if so then give a run of 1
        return startIndex;
    }

    //as these runs are different from the A and B runs in the main powersort loop they will be X and Y
    private static void mergeRuns(int[] array, int startX, int startY, int endY, int[] temp) {
        // Form a bitonic sequence by putting the first run into the temp array in order, then the second run in reverse
        //so that the result is an array where it first has an increasing run then a decreasing run
        int i, j;
        for(i = startX; i < startY; i++) temp[i] = array[i];
        for (j = startY; j <= endY; j++) temp[j] = array[endY+startY-j];
        i = startX; j = endY;
        for (int k = startX; k <= endY; k++) {
            array[k] = temp[j] < temp[i] ? temp[j--] : temp[i++];
        }
        //could use an array copy but would lack the i and j for later
        //loop array backwards and copy it forward?
        // as java is pass by value, could use startX and endY in the final iteration, but it alters the values passed to it?
    }

    /**
     * Merge from Seb Wild's java implementation but with the variable renamed to be more readable for me
     * Merges runs A[l..m-1] and A[m..r] in-place into A[l..r]
     * with Sedgewick's bitonic merge (Program 8.2 in Algorithms in C++)
     * using B as temporary storage.
     * B.length must be at least r+1.
     */
    public static void sebMergeRunsRename(int[] A, int startX, int startY, int endY, int[] B) {
        --startY;// mismatch in convention with Sedgewick
        int i, j;
        assert B.length >= endY+1;
        for (i = startY+1; i > startX; --i) B[i-1] = A[i-1];
        for (j = startY; j < endY; ++j) B[endY+startY-j] = A[j+1];
        for (int k = startX; k <= endY; ++k) {
            A[k] = B[j] < B[i] ? B[j--] : B[i++];
        }
    }

    // seb's insertion sort just renamed for my ease of reading
    public static void insertionsort(int[] array, int left, int right) {
        for (int i = left + 1; i <= right; ++i) {
            int j = i-1;
            final int currentValue = array[i];
            while (currentValue < array[j]) {
                array[j+1] = array[j]; --j;
                if (j < left) break;
            }
            array[j+1] = currentValue;
        }
    }


    public static int log2(int n) {
        if(n == 0) throw new IllegalArgumentException("lg(0) undefined");
        return 31 - Integer.numberOfLeadingZeros( n );
    }
    static int NULL_INDEX = Integer.MIN_VALUE;
    static int minRunLength = 3;

    public static void powerSort(int [] input, int left, int right) {
        int length = (right - left) + 1;
        int lgnPlus2 = log2(length);
        int[] runStartStack = new int[lgnPlus2], runEndStack = new int[lgnPlus2];
        Arrays.fill(runStartStack,NULL_INDEX);
        int topOfStack = 0;
        int[] buffer = new int[length];

        int startA = left;
        int endA = findRun(input, startA, right);
        if ((endA - startA) + 1 < minRunLength) {
            endA = Math.min(startA + minRunLength, right);
            insertionsort(input, startA, endA);
        }
        while (endA < right) {
            int startB = endA + 1;
            int endB = findRun(input, startB, right);
            if ((endB - startB) + 1 < minRunLength) {
                endB = Math.min(startB + minRunLength, right);
                insertionsort(input, startB, endB);
            }
            int runPower = nodePower(left, right, startA, startB, endB);
            for (int i = topOfStack; i > runPower; i--) {
                if (runStartStack[i] == NULL_INDEX) continue;

                // For when I forget again, In this one we are passing endA as the endY parameter because we are merging run A if possible, not B
                // We couldn't merge B as we don't know its node power yet as we have to discover the next run for that
                mergeRuns(input, runStartStack[i], runEndStack[i]+1, endA, buffer);
                startA = runStartStack[i];
                runStartStack[i] = NULL_INDEX;
            }
            runStartStack[runPower] = startA;
            runEndStack[runPower] = endA;
            topOfStack = runPower;
            startA = startB;
            endA = endB;
        }
        // merge down
        for (int i = topOfStack; i > 0; i--) {
            if (runStartStack[i] == NULL_INDEX) continue;
            mergeRuns(input, runStartStack[i], runEndStack[i]+1, endA, buffer);
        }

    }
}
