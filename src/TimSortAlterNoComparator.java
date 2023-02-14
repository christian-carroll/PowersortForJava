/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2009 Google Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.

 */

import java.lang.reflect.Array;

/**
 * A stable, adaptive, iterative mergesort that requires far fewer than
 * n lg(n) comparisons when running on partially sorted arrays, while
 * offering performance comparable to a traditional mergesort when run
 * on random arrays.  Like all proper mergesorts, this sort is stable and
 * runs O(n log n) time (worst case).  In the worst case, this sort requires
 * temporary storage space for n/2 object references; in the best case,
 * it requires only a small constant amount of space.
 *
 * This implementation was adapted from Tim Peters's list sort for
 * Python, which is described in detail here:
 *
 *   http://svn.python.org/projects/python/trunk/Objects/listsort.txt
 *
 * Tim's C code may be found here:
 *
 *   http://svn.python.org/projects/python/trunk/Objects/listobject.c
 *
 * The underlying techniques are described in this paper (and may have
 * even earlier origins):
 *
 *  "Optimistic Sorting and Information Theoretic Complexity"
 *  Peter McIlroy
 *  SODA (Fourth Annual ACM-SIAM Symposium on Discrete Algorithms),
 *  pp 467-474, Austin, Texas, 25-27 January 1993.
 *
 * While the API to this class consists solely of static methods, it is
 * (privately) instantiable; a TimSort instance holds the state of an ongoing
 * sort, assuming the input array is large enough to warrant the full-blown
 * TimSort. Small arrays are sorted in place, using a binary insertion sort.
 *
 * @author Josh Bloch
 */
class TimSortAlterNoComparator<T> {
    /**
     * This is the minimum sized sequence that will be merged.  Shorter
     * sequences will be lengthened by calling binarySort.  If the entire
     * array is less than this length, no merges will be performed.
     * <p>
     * This constant should be a power of two.  It was 64 in Tim Peter's C
     * implementation, but 32 was empirically determined to work better in
     * this implementation.  In the unlikely event that you set this constant
     * to be a number that's not a power of two, you'll need to change the
     * {@link #minRunLength} computation.
     * <p>
     * If you decrease this constant, you must change the stackLen
     * computation in the TimSort constructor, or you risk an
     * ArrayOutOfBounds exception.  See listsort.txt for a discussion
     * of the minimum stack length required as a function of the length
     * of the array being sorted and the minimum merge sequence length.
     */
    private static final int MIN_MERGE = 32;

    /**
     * The array being sorted.
     */
    private final int[] a;



    /**
     * A stack of pending runs yet to be merged.  Run i starts at
     * address base[i] and extends to address end[i].  It's sometimes
     * true (so long as the indices are in bounds) that:
     * <p>
     * runEnd[i] - 1 == runBase[i + 1]
     * <p>
     * so we could cut the storage for this, but it's a minor amount,
     * and keeping all the info explicit simplifies the code.
     */
    private int stackSize = 0;  // Number of pending runs on stack
    private final int[] runBase;
    private final int[] runEnd;
    private int stackTop = 0;
    static int NULL_INDEX = Integer.MIN_VALUE;

    /**
     * Creates a TimSort instance to maintain the state of an ongoing sort.
     *
     * @param a        the array to be sorted
     * @param work     a workspace array (slice)
     * @param workBase origin of usable space in work array
     * @param workLen  usable size of work array
     */
    private TimSortAlterNoComparator(int[] a, int[] work, int workBase, int workLen) {
        this.a = a;
        int len = a.length;

        /*
         * Allocate runs-to-be-merged stack (which cannot be expanded).  The
         * stack length requirements are described in listsort.txt.  The C
         * version always uses the same stack length (85), but this was
         * measured to be too expensive when sorting "mid-sized" arrays (e.g.,
         * 100 elements) in Java.  Therefore, we use smaller (but sufficiently
         * large) stack lengths for smaller arrays.  The "magic numbers" in the
         * computation below must be changed if MIN_MERGE is decreased.  See
         * the MIN_MERGE declaration above for more information.
         * The maximum value of 49 allows for an array up to length
         * Integer.MAX_VALUE-4, if array is filled by the worst case stack size
         * increasing scenario. More explanations are given in section 4 of:
         * http://envisage-project.eu/wp-content/uploads/2015/02/sorting.pdf
         */
        int stackLen = (len < 120 ? 5 :
                len < 1542 ? 10 :
                        len < 119151 ? 24 : 49);
        runBase = new int[stackLen];
        runEnd = new int[stackLen];
    }

    /*
     * The next method (package private and static) constitutes the
     * entire API of this class.
     */

    /**
     * Sorts the given range, using the given workspace array slice
     * for temp storage when possible. This method is designed to be
     * invoked from public methods (in class Arrays) after performing
     * any necessary array bounds checks and expanding parameters into
     * the required forms.
     *
     * @param a        the array to be sorted
     * @param lo       the index of the first element, inclusive, to be sorted
     * @param hi       the index of the last element, exclusive, to be sorted
     * @param work     a workspace array (slice)
     * @param workBase origin of usable space in work array
     * @param workLen  usable size of work array
     * @since 1.8
     */
    static void sort(int[] a, int lo, int hi, int[] work, int workBase, int workLen) {
        assert a != null && lo >= 0 && lo <= hi && hi <= a.length;

        int start = lo;
        int end = hi;

        int nRemaining = hi - lo;
        if (nRemaining < 2)
            return;  // Arrays of size 0 and 1 are always sorted

        // If array is small, do a "mini-TimSort" with no merges
        if (nRemaining < MIN_MERGE) {
            int initRunLen = countRunAndMakeAscending(a, lo, hi);
            binarySort(a, lo, hi, lo + initRunLen);
            return;
        }

        /**
         * March over the array once, left to right, finding natural runs,
         * extending short natural runs to minRun elements, and merging runs
         * to maintain stack invariant.
         */
        TimSortAlterNoComparator ts = new TimSortAlterNoComparator(a, work, workBase, workLen);
        int minRun = minRunLength(nRemaining);
        int prevRunBase = 0;
        int prevRunEnd = 0;

        do {
            // Identify next run
            // In a later iteration it will likely make sense to rewrite this method to return the end of the run instead of it's length
            // but for now i'll just minus 1
            int runLen = countRunAndMakeAscending(a, lo, hi) - 1;

            // If run is short, extend to min(minRun, nRemaining)
            if (runLen < minRun) {
                int forcedEnd = nRemaining <= minRun ? nRemaining : minRun;
                // This will also need to be rewritten to take the start as just lo + runLen
                binarySort(a, lo, lo + forcedEnd, lo + runLen + 1);
                runLen = forcedEnd;
            }

            int runPower = nodePower(start, end, prevRunBase, lo, lo + runLen);
            for (int i = ts.stackTop; i > runPower; i--) {
                if (ts.runBase[i] == NULL_INDEX) continue;

                mergeRuns(a, ts.runBase[i], ts.runBase[i + 1], prevRunBase, work);
                prevRunBase = ts.runBase[i];
                ts.runBase[i] = NULL_INDEX;
            }

            // could seperate this part /\ into a merge collapse method and this part \/ onto a push run method like was done before

            ts.runBase[runPower] = prevRunBase;
            ts.runEnd[runPower] = prevRunEnd;
            ts.stackTop = runPower;
            prevRunBase = lo;
            prevRunEnd = lo + runLen;

            // Push run onto pending-run stack, and maybe merge
//            ts.pushRun(lo, runLen);
//            ts.mergeCollapse();

            // Advance to find next run
            lo += runLen;
            nRemaining -= runLen;
        } while (nRemaining != 0);

        // Merge all remaining runs to complete sort
        for (int i = ts.stackTop; i > 0; i--) {
            if (ts.runBase[i] == NULL_INDEX) continue;
            mergeRuns(a, ts.runBase[i], ts.runEnd[i] + 1, prevRunEnd, work);
        }
    }

    /**
     * Sorts the specified portion of the specified array using a binary
     * insertion sort.  This is the best method for sorting small numbers
     * of elements.  It requires O(n log n) compares, but O(n^2) data
     * movement (worst case).
     * <p>
     * If the initial part of the specified range is already sorted,
     * this method can take advantage of it: the method assumes that the
     * elements from index {@code lo}, inclusive, to {@code start},
     * exclusive are already sorted.
     *
     * @param a     the array in which a range is to be sorted
     * @param lo    the index of the first element in the range to be sorted
     * @param hi    the index after the last element in the range to be sorted
     * @param start the index of the first element in the range that is
     *              not already known to be sorted ({@code lo <= start <= hi})
     */
    @SuppressWarnings("fallthrough")
    private static void binarySort(int[] a, int lo, int hi, int start) {
        assert lo <= start && start <= hi;
        if (start == lo)
            start++;
        for (; start < hi; start++) {
            int pivot = a[start];

            // Set left (and right) to the index where a[start] (pivot) belongs
            int left = lo;
            int right = start;
            assert left <= right;
            /*
             * Invariants:
             *   pivot >= all in [lo, left).
             *   pivot <  all in [right, start).
             */
            while (left < right) {
                int mid = (left + right) >>> 1;
                if (pivot < a[mid])
                    right = mid;
                else
                    left = mid + 1;
            }
            assert left == right;

            /*
             * The invariants still hold: pivot >= all in [lo, left) and
             * pivot < all in [left, start), so pivot belongs at left.  Note
             * that if there are elements equal to pivot, left points to the
             * first slot after them -- that's why this sort is stable.
             * Slide elements over to make room for pivot.
             */
            int n = start - left;  // The number of elements to move
            // Switch is just an optimization for arraycopy in default case
            switch (n) {
                case 2:
                    a[left + 2] = a[left + 1];
                case 1:
                    a[left + 1] = a[left];
                    break;
                default:
                    System.arraycopy(a, left, a, left + 1, n);
            }
            a[left] = pivot;
        }
    }

    /**
     * Returns the length of the run beginning at the specified position in
     * the specified array and reverses the run if it is descending (ensuring
     * that the run will always be ascending when the method returns).
     * <p>
     * A run is the longest ascending sequence with:
     * <p>
     * a[lo] <= a[lo + 1] <= a[lo + 2] <= ...
     * <p>
     * or the longest descending sequence with:
     * <p>
     * a[lo] >  a[lo + 1] >  a[lo + 2] >  ...
     * <p>
     * For its intended use in a stable mergesort, the strictness of the
     * definition of "descending" is needed so that the call can safely
     * reverse a descending sequence without violating stability.
     *
     * @param a  the array in which a run is to be counted and possibly reversed
     * @param lo index of the first element in the run
     * @param hi index after the last element that may be contained in the run.
     *           It is required that {@code lo < hi}.
     * @return the length of the run beginning at the specified position in
     * the specified array
     */
    private static int countRunAndMakeAscending(int[] a, int lo, int hi) {
        assert lo < hi;
        int runHi = lo + 1;
        if (runHi == hi)
            return 1;

        // Find end of run, and reverse range if descending
        if (a[runHi++] < a[lo]) { // Descending
            while (runHi < hi && a[runHi] < a[runHi - 1])
                runHi++;
            reverseRange(a, lo, runHi);
        } else {                              // Ascending
            while (runHi < hi && a[runHi] >= a[runHi - 1])
                runHi++;
        }

        return runHi - lo;
    }

    /**
     * Reverse the specified range of the specified array.
     *
     * @param a  the array in which a range is to be reversed
     * @param lo the index of the first element in the range to be reversed
     * @param hi the index after the last element in the range to be reversed
     */
    private static void reverseRange(int[] a, int lo, int hi) {
        hi--;
        while (lo < hi) {
            int t = a[lo];
            a[lo++] = a[hi];
            a[hi--] = t;
        }
    }

    private static void mergeRuns(int[] array, int startX, int startY, int endY, int[] temp) {
        // Form a bitonic sequence by putting the first run into the temp array in order, then the second run in reverse
        //so that the result is an array where it first has an increasing run then a decreasing run
        int i, j;
        for (i = startX; i < startY; i++) temp[i] = array[i];
        for (j = startY; j <= endY; j++) temp[j] = array[endY + startY - j];
        i = startX;
        j = endY;
        for (int k = startX; k <= endY; k++) {
            array[k] = temp[j] < temp[i] ? temp[j--] : temp[i++];
        }
        //could use an array copy but would lack the i and j for later
        //loop array backwards and copy it forward?
        // as java is pass by value, could use startX and endY in the final iteration, but it alters the values passed to it?
    }

    /**
     * Returns the minimum acceptable run length for an array of the specified
     * length. Natural runs shorter than this will be extended with
     * {@link #binarySort}.
     * <p>
     * Roughly speaking, the computation is:
     * <p>
     * If n < MIN_MERGE, return n (it's too small to bother with fancy stuff).
     * Else if n is an exact power of 2, return MIN_MERGE/2.
     * Else return an int k, MIN_MERGE/2 <= k <= MIN_MERGE, such that n/k
     * is close to, but strictly less than, an exact power of 2.
     * <p>
     * For the rationale, see listsort.txt.
     *
     * @param n the length of the array to be sorted
     * @return the length of the minimum run to be merged
     */
    private static int minRunLength(int n) {
        assert n >= 0;
        int r = 0;      // Becomes 1 if any 1 bits are shifted off
        while (n >= MIN_MERGE) {
            r |= (n & 1);
            n >>= 1;
        }
        return n + r;
    }

    private static int nodePower(int left, int right, int startA, int startB, int endB) {
        int length = (right - left + 1);
        long l = (long) startA + (long) startB - ((long) left << 1); // 2*middleA
        long r = (long) startB + (long) endB + 1 - ((long) left << 1); // 2*middleB
        int a = (int) ((l << 30) / length); // middleA / 2n
        int b = (int) ((r << 30) / length); // middleB / 2n
        return Integer.numberOfLeadingZeros(a ^ b);
    }
}