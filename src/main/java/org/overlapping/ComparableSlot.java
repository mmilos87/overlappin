package org.overlapping;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public interface ComparableSlot<C extends Comparable<? super C>> {

    C slotStart();
    C slotEnd();

    default C slotEndWithOffset(){
        return slotEnd();
    }


    static <C extends Comparable<? super C>, T extends ComparableSlot<C>>
    boolean checkOverlappingGrouping(Set<T> set, Function<T, ?> groupingFunction) {
        return checkOverlappingGrouping(set, groupingFunction,
                ComparableSlot::slotStart, ComparableSlot::defaultCheckFunction);
    }

    static <C extends Comparable<? super C>, T>
    boolean checkOverlappingGrouping(Set<T> set, Function<T, ?> groupingFunction,
                                     Function<T,C> start, Function<T,C> stop, Function<T,C> offset) {
        return checkOverlappingGrouping(set, groupingFunction,start, checkFunction(start, stop, offset));
    }

    static <C extends Comparable<? super C>, T>
    boolean checkOverlappingGrouping(Set<T> set, Function<T, ?> groupingFunction,
                                     Function<T, C> comparingFunction, BiPredicate<T, T> checkFunction) {
        return checkOverlapping(grouping(set, groupingFunction), comparingFunction, checkFunction);
    }

    private static <C extends Comparable<? super C>, T> boolean checkOverlapping(
            Set<T> set, Function<T, C> comparingFunction, BiPredicate<T, T> checkFunction) {
        return checkOverlapping(List.of(set), comparingFunction, checkFunction);
    }

    static <C extends Comparable<? super C>, T extends ComparableSlot<C>>
    boolean checkOverlapping(Set<T> set) {
        return checkOverlapping(set, ComparableSlot::slotStart, ComparableSlot::defaultCheckFunction);
    }

    private static <C extends Comparable<? super C>, T>
    Map<T, Boolean> checkOverlappingAndMap(Set<T> set, Function<T, C> comparingFunction,
                                           BiPredicate<T,T>checkFunction ) {
        return checkOverlappingAndMap(List.of(set), comparingFunction, checkFunction);
    }

    static <C extends Comparable<? super C>, T extends ComparableSlot<C>>
    Map<T, Boolean> checkOverlappingAndMap(Set<T> set) {
        return checkOverlappingAndMap(set, ComparableSlot::slotStart, ComparableSlot::defaultCheckFunction);
    }

    static <C extends Comparable<? super C>, T extends ComparableSlot<C>>
    Map<T, Boolean> checkOverlappingAndMapGrouping(Set<T> set, Function<T, ?> groupingFunction) {
        return checkOverlappingAndMapGrouping(set, groupingFunction,
                ComparableSlot::slotStart, ComparableSlot::defaultCheckFunction);
    }

    static <C extends Comparable<? super C>, T>
    Map<T, Boolean>  checkOverlappingAndMap(Set<T> set,
                                            Function<T,C> start, Function<T,C> stop, Function<T,C> offset) {
        return checkOverlappingAndMap(set, start, checkFunction(start, stop, offset));
    }

    static <C extends Comparable<? super C>, T>
    Map<T, Boolean>  checkOverlappingAndMapGrouping(Set<T> set, Function<T,?> groupingFunction,
                                                    Function<T,C> start, Function<T,C> stop, Function<T,C> offset) {
        return checkOverlappingAndMapGrouping(set, groupingFunction, start, checkFunction(start, stop, offset));
    }

    static <C extends Comparable<? super C>, T>
    Map<T, Boolean> checkOverlappingAndMapGrouping(Set<T> set, Function<T, ?> groupingFunction,
                                                   Function<T, C> comparingFunction,
                                                   BiPredicate<T, T>checkFunction) {
        return checkOverlappingAndMap(grouping(set, groupingFunction),comparingFunction, checkFunction);
    }

    private static <C extends Comparable<? super C>, T>
    boolean checkOverlapping(Collection<Set<T>> collect, Function<T, C> comparingFunction,
                             BiPredicate<T, T> checkFunction) {
        return getSortedList(collect, comparingFunction)
                .stream().anyMatch(treeSet -> checkOverlapping(treeSet, checkFunction));
    }

    private static <C extends Comparable<? super C>,T>
    Boolean checkOverlapping(ArrayList<T> treeSet, BiPredicate<T,T> checkFunction) {
        return checkOverlappingLinear(treeSet, checkFunction)
                .containsValue(Boolean.TRUE);
    }

    private static <C extends Comparable<? super C>, T >
    Map<T, Boolean> checkOverlappingAndMap(Collection<Set<T>> collect, Function<T, C> comparingFunction,
                                           BiPredicate<T,T> checkFunction) {
        return getSortedList(collect, comparingFunction).stream()
                .map(treeSet->checkOverlappingLinear(treeSet, checkFunction))
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static <T> Collection<Set<T>> grouping(Set<T> set, Function<T, ?> groupingFunction) {
        return set.stream()
                .collect(groupingCollector(groupingFunction));
    }

    private static <C extends Comparable<? super C>, T>
    List<ArrayList<T>> getSortedList(Collection<Set<T>> collect, Function<T, C> comparingFunction) {
        return collect.stream()
                .map(set-> bubbleSort(new ArrayList<>(set), comparingFunction)).toList();
    }

    private static <T> Collector<T, ?, Collection<Set<T>>> groupingCollector(Function<T, ?> groupingFunction) {
        Objects.requireNonNull(groupingFunction);
        return Collectors.collectingAndThen(
                Collectors.groupingBy(groupingFunction, Collectors.toSet()), Map::values);
    }

    private static <C extends Comparable<? super C>, T> BiPredicate<T, T>
    checkFunction(Function<T, C> start, Function<T, C> stop, Function<T, C> offset) {
        return (t, t1) ->
                defaultCheckFunction(start.apply(t), stop.apply(t), offset.apply(t), start.apply(t1), stop.apply(t1));
    }


    private static <T extends ComparableSlot<C>, C extends Comparable<? super C>>
    boolean defaultCheckFunction(T t, T t1){
        return defaultCheckFunction(t.slotStart(),t.slotEnd(), t.slotEndWithOffset(), t1.slotStart(), t1.slotEnd());
    }

    private static <C extends Comparable<? super C>>
    boolean defaultCheckFunction(C c1Start, C c1End,C c1EndWithOffset,C c2Start,C c2End){
        return equals(c1Start, c1End, c2Start, c2End)
                ? test(c1Start, c1End)
                : checkNext(c1EndWithOffset, c2Start, c2End);
    }

    private static <C>boolean equals(C c1Start, C c1End, C c2Start, C c2End){
        return c1Start.equals(c2Start) && c1End.equals(c2End);
    }

    private static  <C extends Comparable<? super C>> boolean test(C start, C end){
        int i = start.compareTo(end);
        return  i>=0;
    }

    private static <C extends Comparable<? super C>>boolean checkNext(C c1EndWithOffset, C c2Start, C c2End){
        return c1EndWithOffset.compareTo(c2Start)>0 || test(c2Start,c2End);
    }

    private static<T, C extends Comparable<? super C>>
    Map<T,Boolean> checkOverlappingLinear(ArrayList<T> treeSet, BiPredicate<T,T> checkFunction){
        Map<T,Boolean> map = new HashMap<>();
        Iterator<T> iterator = treeSet.iterator();
        T t = null;
        while(iterator.hasNext()){
            T t1 = iterator.next();
            if(t == null) t = t1;
            boolean check = checkFunction.test(t,t1);
            map.put(t1, check);
            if (!check) t = t1;
        }
        return map;
    }

    private static <T,C extends Comparable<? super C>> ArrayList<T> bubbleSort(ArrayList<T> arrayList,
                                                                              Function<T, C> comparingFunction){
        boolean moved;
        do {
            moved = false;
            for (int i = 1; i < arrayList.size(); i++) {
                C c1 = comparingFunction.apply(arrayList.get(i - 1));
                C c2 = comparingFunction.apply(arrayList.get(i));
                if (c1.compareTo(c2) > 0) {
                    Collections.swap(arrayList, i - 1, i);
                    moved = true;
                }
            }
        } while (moved);
        return arrayList;
    }

}