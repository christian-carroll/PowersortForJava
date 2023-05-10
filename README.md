# PowersortForJava

Current file organisation - 

Main holds the code for running testing. When run it will wait for an input from the keyboard, this is to allow for visualVM to be linked to the process.
Once an input is received, it will run warmup runs and then start running actual timing tests, writing the results to a CSV

Inputs is just the input file taken from Sebastian's java implementation, currently in use for generating the testing data

ComparableTimSortCost is an exact copy of the ComparableTimSort in OpenJDK, but with a few lines added to allow me to count the merge costs

ComparablePowerSort is the main version I have made for this project, it has been created by modifying ComparableTimSort to use PowerSort's merging policy

ALL FILES BELOW WERE CREATED IN THE EARLY PHASE OF MY PROJECT WHILE I WAS GETTING TO GRIPS WITH IT AND SO ARE MOSTLY INCOMPLETE OR HAVE NO REAL PURPOSE

myPowersort is the version of powersort that I made based off Sebastian's code for the purpose of making sure I understand how it works,
some of it actually uses sebastian's code just with renamed variables to make it easier for me to read and understand.

TimSortNoComparator is timsort changed to remove the use of a comparator so it can work on primitive data types. This is currently unstable and doesn't
work properly, not sure why as all I did was change the comparators to equalities signs.

TimSortAlter is me taking the origninal TimSort and altering it into a powersort while trying to change as little as possible, going with the idea that if
it's as similar as possible to the current standard then it could be good.

TimSortAlterNoComparator is the same idea as TimSortAlter but again to work with primitive data types. CURRENT WIP

TimSort - OpenJDK's int version of timsort

