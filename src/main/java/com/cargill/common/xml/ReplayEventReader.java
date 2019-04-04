package com.cargill.common.xml;

import com.cargill.common.xml.util.XMLUtil;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Created by Erik Pearson
 */
public class ReplayEventReader implements XMLEventReader {

    /**
     * Queue where the item index 1 on queue is the head
     * The index 0 item provides the previous value for replay.
     */
    private LinkedList<XMLEvent> eventQueue = new LinkedList<>();

    public ReplayEventReader(javax.xml.stream.XMLEventReader reader) {
        this(reader, Integer.MAX_VALUE);
    }

    public ReplayEventReader(javax.xml.stream.XMLEventReader reader, int maxBufferSize) {
        AtomicInteger i = new AtomicInteger();
        eventQueue.add(null);// Previous event.  null because it doesn't exist
        reader.forEachRemaining(e -> {
            if (i.getAndIncrement() > maxBufferSize) throw new IllegalStateException("max buffer size exceeded: " + maxBufferSize);
            eventQueue.add((XMLEvent)e);
        });
    }

    //
    // delegate methods
    //

    @Override
    public XMLEvent nextEvent() {
        eventQueue.pop();
        XMLEvent next = eventQueue.peek();
        if (next != null) return next;
        throw new NoSuchElementException();
    }

    @Override
    public Optional<XMLEvent> nextOptionalEvent() {
        return XMLUtil.optionalNoThrow(this::nextEvent);
    }

    @Override
    public boolean hasNext() {
        return eventQueue.size() > 1;
    }

    @Override
    public Object next() {
        return nextEvent();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forEachRemaining(Consumer action) {
        eventQueue.poll();
        eventQueue.forEach(action);
    }

    @Override
    public XMLEvent peek() {
        return eventQueue.size() > 1 ? eventQueue.get(1) : null;
    }

    @Override
    public Optional<XMLEvent> optionalPeek() {
        return Optional.ofNullable(peek());
    }

    @Override
    public String getElementText() throws XMLStreamException {
        XMLEvent previousEvent = eventQueue.peek();
        if (previousEvent == null) if (eventQueue.isEmpty()) {
          throw new XMLStreamException("empty queue");
        } else {
          throw new XMLStreamException("null element in queue");
        }
        if (!previousEvent.isStartElement()) throw new XMLStreamException("not at START_ELEMENT");
        XMLEvent e = nextEvent();
        if (!e.isCharacters()) throw new XMLStreamException("non-text element encountered");
        if (!peek().isEndElement()) throw new XMLStreamException("current event is NOT END_ELEMENT.");
        return e.asCharacters().getData();
    }

    @Override
    public Optional<String> getOptionalElementText() {
        return XMLUtil.optionalNoThrow(this::getElementText);
    }

    @Override
    public XMLEvent nextTag() throws XMLStreamException {
        while (true) {
            XMLEvent e = nextEvent();
            if (e == null) throw new NoSuchElementException();
            if (e.isStartElement() || e.isEndElement()) return e;
            if (!e.isCharacters() || !e.asCharacters().isWhiteSpace()) throw new XMLStreamException("non-whitespace encountered");
        }
    }

    @Override
    public Optional<XMLEvent> nextOptionalTag() {
        return XMLUtil.optionalNoThrow(this::nextTag);
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        throw new IllegalArgumentException("not supported");
    }

    @Override
    public void close() throws XMLStreamException {
        // no-op
    }
}
