# PowersortForJava

Current file organisation - Will need to change

Main holds the version of powersort that I made based off Sebastian's code for the purpose of making sure I understand how it works
some of it actually uses sebastian's code just with renamed variables to make it easier for me to read and understand. Main also holds the code for running testing

TimSortNoComparator is timsort changed to remove the use of a comparator so it can work on primitive data types. This is currently unstable and doesn't
work properly, not sure why as all I did was change the comparators to equalities signs.

TimSortAlter is me taking the origninal TimSort and altering it into a powersort while trying to change as little as possible, going with the idea that if
it's as similar as possible to the current standard then it could be good.

TimSortAlterNoComparator is the same idea as TimSortAlter but again to work with primitive data types. CURRENT WIP

sebsInputs is just the input file taken from Sebastian's java implementation, currently in use for generating the testing data 

TimSort - it's timsort

