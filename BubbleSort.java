import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

public class BubbleSort {

    public static void sort(LinkedList list, Comparator c, boolean ascending) {
        if (list.size() < 2)
            return;

        //int comparisonCounter = 0;
        //int swapCounter = 0;
        int R = list.size() - 2;
        boolean swapped = true;

        while (R >= 0 && swapped) {
            swapped = false;
            // Start from beginning of list and set current to first element
            Iterator iterator = list.iterator();
            Object current = iterator.next();
            Object next;

            for (int i = 0; i <= R; i++) {

                next = iterator.next();

                //comparisonCounter++;

                if ((ascending && less(c, next, current)) || (!ascending && less(c, current, next))) {
                    //swapCounter++;
                    swapped = true;
                    swapCurrNext(list, i);
                    Object temp = current;
                    current = next;
                    next = temp;
                }
                current = next;
                // 3, 2, 4
                // c, n, i

            }

            R--;
        }
        //System.out.println("Swaps: " + swapCounter + ", Comparisons: " + comparisonCounter);
    }

    private static boolean less(Comparator c, Object v, Object w) {
        //System.out.println("Checking if: " + v + " is less than " + w);
        return c.compare(v, w) < 0;
    }


    // Swap data in current and next
    private static void swapCurrNext(LinkedList list, int indexCurr) {

        //System.out.println(list + " Swapped: " + list.get(indexCurr) + " with " + list.get(indexCurr+1));
        Object temp = list.get(indexCurr);
        list.set(indexCurr, list.get(indexCurr + 1));
        list.set(indexCurr + 1, temp);

    }

}
