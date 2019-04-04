package com.cargill.common.xml;

import javax.xml.stream.EventFilter;

/**
 * Created by Erik Pearson
 */
public interface PeekableEventFilter extends EventFilter {
    default void startPeeking() {}
    default void stopPeeking() {}
    default boolean isPeeking() { return false; }
}
