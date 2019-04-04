package com.cargill.common.xml.util;

import com.cargill.common.xml.XMLException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

/**
 * Created by Erik Pearson
 */
public class XMLUtil {
  public static final String CORRECTION = "correction";
  public static final String QC_SUBMISSION = "qc-submission";
    public static <T> T noThrow(ThrowableSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException e) {
            throw e;
        } catch (XMLStreamException e) {
            throw new XMLException(e);
        } catch (Exception e) {
            throw new XMLException(e.getMessage(), e);
        }
    }

    public static void noThrow(ThrowableRunnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (XMLStreamException e) {
            throw new XMLException(e);
        } catch (Exception e) {
            throw new XMLException(e.getMessage(), e);
        }
    }

    public static <T> Optional<T> optionalNoThrow(ThrowableSupplier<T> eventSupplier) {
        try {
            return Optional.ofNullable(noThrow(eventSupplier));
        } catch (XMLException | NoSuchElementException e) {
            return Optional.empty();
        }
    }

    public static <T> Optional<T> optional(T t) {
        return Optional.ofNullable(t);
    }

    public static <T> Iterable<T> iterableOf(Supplier<T> supplier) {
        return new SupplierIterable<>(supplier);
    }

    public static <T> List<T> listOf(Supplier<T> supplier) {
        return StreamSupport.stream(iterableOf(supplier).spliterator(), false).collect(toList());
    }

    public static <T> Iterable<T> iterableOfOptional(Supplier<Optional<T>> supplier) {
        return new SupplierIterable<>(() -> supplier.get().orElse(null));
    }

    public static <T> List<T> listOfOptional(Supplier<Optional<T>> supplier) {
        return StreamSupport.stream(iterableOfOptional(supplier).spliterator(), false).collect(toList());
    }

    public static Map<String, String> getAttributeMap(StartElement startElement) {
        return StreamSupport.stream(Spliterators
                .spliteratorUnknownSize((Iterator<Attribute>)startElement.getAttributes(), Spliterator.ORDERED), false)
                .map(a -> new AbstractMap.SimpleEntry<>(a.getName().getLocalPart(), a.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static String getAttributeValue(StartElement e, QName qname) {
        return e.getAttributeByName(qname).getValue();
    }

    public static String getAttributeValue(StartElement e, String name) {
        return getAttributeValue(e, new QName(name));
    }

    public static Optional<String> getOptionalAttributeValue(StartElement e, QName qname) {
        return Optional.ofNullable(e.getAttributeByName(qname)).map(Attribute::getValue);
    }

    public static Optional<String> getOptionalAttributeValue(StartElement e, String name) {
        return getOptionalAttributeValue(e, new QName(name));
    }

}
