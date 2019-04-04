package com.cargill.common.xml;

import javax.xml.stream.EventFilter;
import javax.xml.stream.events.XMLEvent;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Erik Pearson
 */
public class ChainedEventFilter extends PeekableEventFilterImpl {

    private final List<EventFilter> filters;
    private Deque<EventFilter> queue;

    public ChainedEventFilter(EventFilter... filters) {
        this(Arrays.asList(filters));
    }

    public ChainedEventFilter(List<EventFilter> filters) {
        if (filters == null || filters.isEmpty()) { throw new IllegalArgumentException("at least one filter must be supplied"); }
        this.filters = filters;
        this.queue = new LinkedList<>(filters);
    }

    @Override
    public boolean accept(XMLEvent event) {
        EventFilter f = queue.pop();
        boolean accepted = f.accept(event);
        try {
            return accepted && queue.isEmpty();
        } finally {
            if (!accepted || (isPeeking() && queue.isEmpty())) {
                queue.push(f);
            }
            if (queue.isEmpty()) queue.addAll(filters);
        }
    }

    public ChainedEventFilter copy() {
        return new ChainedEventFilter(this.filters);
    }

    @Override
    public String toString() {
        return "chained{queue=" + queue + "," + super.toString() + "}";
    }
}
