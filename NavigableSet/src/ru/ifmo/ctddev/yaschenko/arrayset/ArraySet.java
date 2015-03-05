package ru.ifmo.ctddev.yaschenko.arrayset;

import java.util.*;

/**
 * @author Nikita Yaschenko (nikita.yaschenko@gmail.com)
 */
public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {

    private Comparator<? super T> mComparator;
    private List<T> mData;

    public ArraySet() {
        construct(new ArrayList<T>(), null);
    }

    public ArraySet(Comparator<? super T> comparator) {
        construct(new ArrayList<T>(), comparator);
    }

    public ArraySet(Collection<? extends T> elements) {
        construct(elements, null);
    }

    public ArraySet(Collection<? extends T> elements, Comparator<? super T> comparator) {
        this.mComparator = comparator;
        this.mData = new ArrayList<T>();
        ArrayList<T> temp = new ArrayList<T>(elements);
        Collections.sort(temp, comparator);
        T last = null;
        for (T e : temp) {
            if (last == null || compare(e, last) != 0) {
                this.mData.add(e);
                last = e;
            }
        }
    }

    private ArraySet(List<T> elements, Comparator<? super T> comparator) {
        this.mData = elements;
        this.mComparator = comparator;
    }

    public ArraySet(ArraySet<T> other) {
        this.mComparator = other.mComparator;
        this.mData = other.mData;
    }

    private void construct(Collection<? extends T> elements, Comparator<? super T> comparator) {
        this.mComparator = comparator;
        this.mData = new ArrayList<T>();
        ArrayList<T> temp = new ArrayList<T>(elements);
        Collections.sort(temp, comparator);
        T lastAdded = null;
        for (T elem : temp) {
            if (lastAdded == null || compare(elem, lastAdded) != 0) {
                this.mData.add(elem);
                lastAdded = elem;
            }
        }
    }

    private int compare(T x, T y) {
        if (mComparator == null) {
            return ((Comparable<? super T>) x).compareTo(y);
        } else {
            return mComparator.compare(x, y);
        }
    }

    private ArraySet<T> subSet(int newFromIndex, int newToIndex) {
        if (newFromIndex == 0 && newToIndex == size()) {
            return this;
        }
        if (newFromIndex < newToIndex) {
            return new ArraySet<T>(mData.subList(newFromIndex, newToIndex), mComparator);
        }
        return new ArraySet<T>();
    }

    @Override
    public boolean contains(Object o) {
        return Collections.binarySearch(mData, (T) o, comparator()) >= 0;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    private int headIndex(T toElement, boolean inclusive) {
        int pos = Collections.binarySearch(mData, toElement, comparator());
        if (pos >= 0) {
            return inclusive ? pos + 1 : pos;
        } else {
            return -pos - 1;
        }
    }

    private int tailIndex(T fromElement, boolean inclusive) {
        int pos = Collections.binarySearch(mData, fromElement, comparator());
        if (pos >= 0) {
            return inclusive ? pos : pos + 1;
        } else {
            return -pos - 1;
        }
    }

    @Override
    public T lower(T t) {
        int index = headIndex(t, false) - 1;
        return (index == -1) ? null : mData.get(index);
    }

    @Override
    public T floor(T t) {
        int index = headIndex(t, true) - 1;
        return (index == -1) ? null : mData.get(index);
    }

    @Override
    public T ceiling(T t) {
        int index = tailIndex(t, true);
        return (index == size()) ? null : mData.get(index);
    }

    @Override
    public T higher(T t) {
        int index = tailIndex(t, false);
        return (index == size()) ? null : mData.get(index);
    }

    @Override
    public ArraySet<T> descendingSet() {
        return null;
    }

    @Override
    public Iterator<T> descendingIterator() {
        return null;
    }

    @Override
    public ArraySet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        return tailSet(fromElement, fromInclusive).headSet(toElement, toInclusive);
    }

    @Override
    public ArraySet<T> headSet(T toElement, boolean inclusive) {
        return subSet(0, headIndex(toElement, inclusive));
    }

    @Override
    public ArraySet<T> tailSet(T fromElement, boolean inclusive) {
        return subSet(tailIndex(fromElement, inclusive), size());
    }

    @Override
    public Comparator<? super T> comparator() {
        return mComparator;
    }

    @Override
    public ArraySet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public ArraySet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public ArraySet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return mData.get(0);
    }

    @Override
    public T last() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return mData.get(size() - 1);
    }

    @Override
    public int size() {
        return mData.size();
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < size();
            }

            @Override
            public T next() {
                return mData.get(index++);
            }
        };
    }

}
