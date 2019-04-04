package com.cargill.common.xml.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * Created by Erik Pearson
 */
public class SupplierIterable<T> implements Iterable<T> {
    protected final Iterator<T> iterator;
    public SupplierIterable(Supplier<T> supplier) {
        this.iterator = new Iterator<T>() {
            T next = supplier.get();
            public boolean hasNext() {
                return next != null;
            }
            public T next() {
                if (null == next) throw new NoSuchElementException();
                try {
                    T t = next;
                    return t;
                } finally {
                    next = supplier.get();
                }
            }
        };
    }
    @Override
    public Iterator<T> iterator() {
        return iterator;
    }
}
