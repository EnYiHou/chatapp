package server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

class Algorithms {

    public static <T> int binarySearch(List<T> list, T target, Comparator<T> comparator) {
        if (list.isEmpty()) {
            return -1;
        }

        int start = 0;
        int end = list.size();

        int indexComp;
        while (end - start > 1) {
            indexComp = ((end - start) / 2 + start);

            T currentComp = list.get(indexComp);

            if (comparator.compare(target, list.get(indexComp)) > 0) {

                start = indexComp;

            } else if (comparator.compare(target, list.get(indexComp)) < 0) {

                end = indexComp;

            } else if (target.equals(currentComp)) {

                while (indexComp > start && list.get(indexComp - 1) == target) {

                    indexComp = indexComp - 1;

                }
                return indexComp;
            }

        }

        if (target != list.get(start) && target != list.get(end - 1)) {
            return -1;
        }

        return 0;
    }

    public static <T> List<T> bubbleSort(List<T> list, Comparator comparator) {
        if (list.isEmpty()) {
            return null;
        }

        int size = list.size();
        int counter;
        do {
            counter = 0;

            for (int i = 0; i < size - 1; i++) {
                if (comparator.compare(list.get(i), list.get(i + 1)) > 0) {
                    T temp = list.get(i);
                    list.set(i, list.get(i + 1));
                    list.set(i + 1, temp);
                    counter++;
                }
            }

            size--;

        } while (counter != 0);

        return list;
    }

    public static <T> List<T> selectionSort(List<T> list, Comparator<T> comparator) {

        if (list.size() == 0) {
            return null;
        }

        int indexSmallest = 0;
        for (int i = 0; i < list.size(); i++) {
            T smallest = list.get(i);
            for (int j = i; j < list.size(); j++) {
                if (comparator.compare(list.get(j), smallest) < 0) {
                    smallest = list.get(j);
                    indexSmallest = j;
                }

            }

            list.set(indexSmallest, list.get(i));
            list.set(i, smallest);

        }
        return list;

    }

    public static void main(String[] args) {
        System.out.println(selectionSort(Arrays.asList(3, 2, 3, 76, 51, 2, 23), new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return Integer.compare(o1, o2);
            }
        }));
    }

}
