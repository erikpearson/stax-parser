package com.cargill.common.xml;

import com.cargill.common.xml.util.XMLUtil;

import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;
import static com.cargill.common.xml.EventFilterSupport.*;

/**
 * wrapper for StAX XMLEventReader that allows filters per call
 *
 * Created by Erik Pearson
 */
public class XMLFilteredEventReader implements XMLEventReader, XMLStreamConstants {

    private javax.xml.stream.XMLEventReader reader;
    private EventFilterSupport fs;
    private Optional<PeekableEventFilter> filter;
    private Optional<PeekableEventFilter> stopFilter;
    boolean stopped = false;
    private ProcessingDirectiveAccessor processingDirective;

    /**
     * Constructs a XMLFilteredEventReader that wraps the given XMLEventReader
     *
     * @param reader the reader to wrap
     */
    public XMLFilteredEventReader(javax.xml.stream.XMLEventReader reader, ProcessingDirectiveAccessor processingDirective) throws XMLStreamException {
        this.processingDirective = processingDirective;
        filter = Optional.empty();
        stopFilter = Optional.empty();
        EventFilter nextFilter = e -> {
            final boolean accepted = filter.map(f -> f.accept(e)).orElse(true);
            stopped = stopFilter.map(sf -> sf.accept(e)).orElse(false);
            return accepted || stopped;
        };
        this.reader = XMLInputFactory.newFactory().createFilteredReader(reader, nextFilter);
        this.fs = new EventFilterSupport();
    }

    private boolean wasStopped() {
        return stopped;
    }

    //
    // AggregateEventFilter
    //

    public XMLFilteredEventReader useFilter(EventFilter filter) {
        this.filter = Optional.ofNullable(filter).map(EventFilterSupport::peekable);
        return this;
    }

    public XMLFilteredEventReader stopFilter(EventFilter filter) {
        stopFilter = Optional.ofNullable(filter).map(EventFilterSupport::peekable);
        return this;
    }

    public XMLFilteredEventReader withFilter(EventFilter filter, Runnable runnable) {
        Optional<PeekableEventFilter> saveFilter = this.filter;
        useFilter(filter);
        try {
            runnable.run();
        } finally {
            this.filter = saveFilter;
        }
        return this;
    }

    public XMLFilteredEventReader withStopFilter(EventFilter stopFilter, Runnable runnable) {
        Optional<PeekableEventFilter> saveStopFilter = this.stopFilter;
        stopFilter(stopFilter);
        try {
            runnable.run();
        } finally {
            this.stopFilter = saveStopFilter;
        }
        return this;
    }

    public XMLFilteredEventReader withFilters(EventFilter filter, EventFilter stopFilter, Runnable runnable) {
        Optional<PeekableEventFilter> saveFilter = this.filter;
        Optional<PeekableEventFilter> saveStopFilter = this.stopFilter;
        useFilter(filter);
        stopFilter(stopFilter);
        try {
            runnable.run();
        } finally {
            this.filter = saveFilter;
            this.stopFilter = saveStopFilter;
        }
        return this;
    }

    public EventFilterSupport getFilterSupport() {
        return fs;
    }

    public XMLFilteredEventReader withNamespaceURI(String uri) {
        fs.withNamespaceURI(uri);
        return this;
    }

    public XMLEvent nextEvent(EventFilter filter) throws XMLStreamException {
        return nextEvent(filter, null);
    }

    public Optional<XMLEvent> nextOptionalEvent(EventFilter filter) {
        return XMLUtil.optionalNoThrow(() -> nextEvent(filter));
    }

    public XMLEvent nextEvent(EventFilter filter, EventFilter stopFilter) throws XMLStreamException {
        this.filter = Optional.ofNullable(filter).map(EventFilterSupport::peekable);
        this.stopFilter = Optional.ofNullable(stopFilter).map(EventFilterSupport::peekable);
        try {
            return this.nextEvent();
        } finally {
            this.filter = Optional.empty();
            this.stopFilter = Optional.empty();
        }
    }

    public Optional<XMLEvent> nextOptionalEvent() {
        return XMLUtil.optionalNoThrow(this::nextEvent);
    }

    public StartElement nextStartElement(EventFilter filter) throws XMLStreamException {
        return nextStartElement(filter, null);
    }

    public StartElement nextStartElement(EventFilter filter, EventFilter stopFilter) throws XMLStreamException {
        return nextEvent(all(startElement(), filter), stopFilter).asStartElement();
    }

    public Optional<StartElement> nextOptionalStartElement(EventFilter filter) {
        return nextOptionalStartElement(filter, null);
    }

    public Optional<StartElement> nextOptionalStartElement(EventFilter filter, EventFilter stopFilter) {
        return XMLUtil.optionalNoThrow(() -> nextStartElement(filter, stopFilter)).map(XMLEvent::asStartElement);
    }

    private StartElement nextStartElement(String[] names, boolean skip) throws XMLStreamException {
        EventFilter nameFilter = startElement();
        EventFilter stopFilter = null;
        if (names.length > 0) {
            String nameString = Arrays.asList(names).toString();
            nameFilter = named(() -> "startElement{" + nameString + "}",
                    any(Arrays.stream(names).map(n -> fs.startElement(n)).collect(toList())));
            stopFilter = named(() -> "startElementNot{" + nameString + "}",
                    all(startElement(), not(any(Arrays.stream(names).map(n -> fs.element(n)).collect(toList())))));
        }
        return nextEvent(nameFilter, skip ? null : stopFilter).asStartElement();
    }

    public StartElement nextStartElement(String... names) throws XMLStreamException {
        return nextStartElement(names, false);
    }

    public Optional<StartElement> nextOptionalStartElement(String... names) {
        return XMLUtil.optionalNoThrow(() -> nextStartElement(names)).map(XMLEvent::asStartElement);
    }

    @Override
    public Optional<String> getOptionalElementText() {
        return XMLUtil.optionalNoThrow(this::getElementText);
    }

    @Override
    public Optional<XMLEvent> optionalPeek() {
        return XMLUtil.optionalNoThrow(this::peek);
    }

    @Override
    public Optional<XMLEvent> nextOptionalTag() {
        return XMLUtil.optionalNoThrow(this::nextTag);
    }

    public StartElement skipToStartElement(String... names) throws XMLStreamException {
        return nextStartElement(names, true);
    }

    public Optional<StartElement> skipToOptionalStartElement(String... names) {
        return XMLUtil.optionalNoThrow(() -> nextStartElement(names)).map(XMLEvent::asStartElement);
    }

    private void peekFilters() {
        filter.ifPresent(PeekableEventFilter::startPeeking);
        stopFilter.ifPresent(PeekableEventFilter::startPeeking);
    }

    private void pullFilters() {
        filter.ifPresent(PeekableEventFilter::stopPeeking);
        stopFilter.ifPresent(PeekableEventFilter::stopPeeking);
    }

    /**
     * Get next parsing event, similar to {@link #next()}, but applying the chain of filters.
     *
     * @see XMLEvent
     * @see XMLEventReader
     *
     * @throws XMLStreamException if there is an error with the underlying XML.
     * @throws NoSuchElementException iteration has no more elements.
     */
    @Override
    public XMLEvent nextEvent() throws XMLStreamException {
        if (peek() == null) throw new NoSuchElementException();
        return reader.nextEvent();
    }

    /**
     * Skips any white space (isWhiteSpace() returns true), COMMENT, any PROCESSING_INSTRUCTION,
     * until a START_ELEMENT any END_ELEMENT is reached.
     *
     * @see XMLEventReader#nextTag()
     * @throws XMLStreamException if anything other than space characters are encountered
     */
    @Override
    public XMLEvent nextTag() throws XMLStreamException {
        EventFilter tagFilter = not(any(whitespace(), e -> e.getEventType() == XMLStreamConstants.COMMENT, XMLEvent::isProcessingInstruction));
        EventFilter acceptFilter = any(startElement(), endElement());
        EventFilter savedStop = stopFilter.orElse(null);
        stopFilter(any(tagFilter, savedStop));
        try {
            XMLEvent e = nextEvent();
            if (!acceptFilter.accept(e))
                throw new XMLStreamException("non-whitespace characters encountered", e.getLocation());
            return e;
        } finally {
            stopFilter(savedStop);
        }
    }

    /**
     * Applies a consumer to each remaining event in the stream that satisfies the chain of filters.
     *
     * @see XMLEventReader#forEachRemaining(Consumer)
     */
    @Override
    public void forEachRemaining(Consumer action) {
        while (hasNext()) action.accept(next());
    }

    @Override
    public void remove() {
        reader.remove();
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return reader.getProperty(name);
    }


    @Override
    public Object next() {
        try {
            return nextEvent();
        } catch (XMLStreamException e) {
            NoSuchElementException nse = new NoSuchElementException(e.getMessage());
            nse.initCause(e.getCause());
            throw nse;
        }
    }

    @Override
    public boolean hasNext() {
        try {
            return peek() != null;
        } catch (XMLStreamException e) {
            return false;
        }
    }

    @Override
    public XMLEvent peek() throws XMLStreamException {
        try {
            peekFilters();
            XMLEvent peek = reader.peek();
            return (null == peek || wasStopped()) ? null : peek;
        } finally {
            pullFilters();
        }
    }

    @Override
    public String getElementText() throws XMLStreamException {
        return reader.getElementText();
    }

    @Override
    public void close() throws XMLStreamException {
        reader.close();
    }

    @Override
    public String toString() {
        return "XMLFilteredEventReader{" +
                "filter=" + filter.map(Object::toString).orElse("null") +
                ",stopFilter=" + stopFilter.map(Object::toString).orElse("null") +
                '}';
    }

    public Optional<String> getDirective(String directiveName) {
        return processingDirective.getDirective(directiveName);
    }
}
