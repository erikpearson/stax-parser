package com.cargill.common.xml.util;

/**
 * Created by Erik Pearson
 */
public interface ThrowableSupplier<T> {
    T get() throws Exception;
}
