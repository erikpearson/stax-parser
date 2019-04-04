package com.cargill.common.xml;

import javax.xml.namespace.QName;
import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.XMLEvent;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * Created by Erik Pearson
 */
public class EventFilterSupport {

    String namespace = null;

    //
    // namespace support
    //

    public QName qname(String name) {
        return new QName(namespace, name);
    }

    public QName name(String name) {
        return new QName(name);
    }

    public EventFilterSupport withNamespaceURI(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public PeekableEventFilter element(String name) {
        return named(() -> "element{" + name + "}", element(qname(name)));
    }

    public PeekableEventFilter startElement(String name) {
        return named(() -> "startElement{" + name + "}", all(startElement(), element(name)));
    }

    public PeekableEventFilter startElementNot(String name) {
        return named(() -> "startElementNot{" + name + "}", all(startElement(), not(element(name))));
    }

    public PeekableEventFilter endElement(String name) {
        return named(() -> "endElement{" + name + "}", all(endElement(), element(name)));
    }


    //
    // static filters
    //

    public static ChainedEventFilter chain(EventFilter... filters) {
        return new ChainedEventFilter(filters);
    }

    public static SkipChildrenFilter skipChildren() {
        return new SkipChildrenFilter();
    }

    public static PeekableEventFilter not(EventFilter filter) {
        return named(() -> "not{" + filter + "}", e -> !filter.accept(e));
    }

    public static PeekableEventFilter element(QName qname) {
        return named(() -> "element{" + qname + "}", e -> {
            QName ename = null;
            if (e.isStartElement()) ename = e.asStartElement().getName();
            if (e.isEndElement()) ename = e.asEndElement().getName();
            if (ename == null) return false;
            return ename.equals(qname);
        });
    }

    public static PeekableEventFilter hasAttributes() {
        return named(() -> "hasAttributes", e -> e.isStartElement() && e.asStartElement().getAttributes().hasNext());
    }

    public static PeekableEventFilter withAttribute(String name) {
        return withAttribute(new QName(name));
    }

    public static PeekableEventFilter withAttribute(QName qname) {
        return named(() -> "withAttribute{" + qname + "}", e -> {
            if (!e.isStartElement()) return false;
            return e.asStartElement().getAttributeByName(qname) != null;
        });
    }

    public static PeekableEventFilter eventType(int type) {
        return named(() -> "eventType{" + eventTypeToString(type) + "}", e -> e.getEventType() == type);
    }

    public static String eventTypeToString(int type) {
        switch (type) {
            case XMLStreamConstants.START_ELEMENT: return "START_ELEMENT";
            case XMLStreamConstants.END_ELEMENT: return "END_ELEMENT";
            case XMLStreamConstants.PROCESSING_INSTRUCTION: return "PROCESSING_INSTRUCTION";
            case XMLStreamConstants.CHARACTERS: return "CHARACTERS";
            case XMLStreamConstants.COMMENT: return "COMMENT";
            case XMLStreamConstants.SPACE: return "SPACE";
            case XMLStreamConstants.START_DOCUMENT: return "START_DOCUMENT";
            case XMLStreamConstants.END_DOCUMENT: return "END_DOCUMENT";
            case XMLStreamConstants.ENTITY_REFERENCE: return "ENTITY_REFERENCE";
            case XMLStreamConstants.ATTRIBUTE: return "ATTRIBUTE";
            case XMLStreamConstants.DTD: return "DTD";
            case XMLStreamConstants.CDATA: return "CDATA";
            case XMLStreamConstants.NAMESPACE: return "NAMESPACE";
            case XMLStreamConstants.NOTATION_DECLARATION: return "NOTATION_DECLARATION";
            case XMLStreamConstants.ENTITY_DECLARATION: return "ENTITY_DECLARATION";
            default: throw new IllegalArgumentException("unknown event type: " + type);
        }
    }

    public static PeekableEventFilter startElement() {
        return named(() -> "startElement", XMLEvent::isStartElement);
    }

    public static PeekableEventFilter characters() {
        return named(() -> "characters", XMLEvent::isCharacters);
    }

    public static PeekableEventFilter whitespace() {
        return named(() -> "whitespace", all(characters(), e -> e.asCharacters().isWhiteSpace()));
    }

    public static PeekableEventFilter endElement() {
        return named(() -> "endElement", XMLEvent::isEndElement);
    }

    public static PeekableEventFilter all(EventFilter... filters) {
        return all(asList(filters));

    }

    public static PeekableEventFilter all(Collection<EventFilter> filters) {
        return aggregate("all", filters, (b1, b2) -> b1 && b2);
    }

    public static PeekableEventFilter any(EventFilter... filters) {
        return any(asList(filters));
    }

    public static PeekableEventFilter any(Collection<EventFilter> filters) {
        return aggregate("any", filters, (b1, b2) -> b1 || b2);
    }

    public static NamedEventFilter named(Supplier<String> name, EventFilter filter) {
        if (filter instanceof NamedEventFilter) {
            ((NamedEventFilter)filter).name = name;
            return (NamedEventFilter) filter;
        }
        return new NamedEventFilter(name, filter);
    }

    private static PeekableEventFilter aggregate(String name, Collection<EventFilter> filters, BinaryOperator<Boolean> accumulator) {
        if (filters.isEmpty()) return named(() -> "true", e -> true);
        if (filters.size() == 1) return peekable(filters.iterator().next());
        return named(() -> (name + filters), new AggregateFilter(filters, accumulator));
    }

    public static PeekableEventFilter peekable(EventFilter filter) {
        if (filter instanceof PeekableEventFilter) return (PeekableEventFilter)filter;
        return filter::accept;
    }

    private static class NamedEventFilter implements PeekableEventFilter {
        private Supplier<String> name;
        private final PeekableEventFilter filter;
        public NamedEventFilter(Supplier<String> name, EventFilter filter) {
            this.name = name;
            if (filter instanceof PeekableEventFilter) {
                this.filter = (PeekableEventFilter) filter;
            } else {
                this.filter = peekable(filter);
            }
        }
        public String toString() { return name.get(); }
        @Override
        public boolean accept(XMLEvent event) {
            return filter.accept(event);
        }
        @Override
        public void startPeeking() { filter.startPeeking(); }
        @Override
        public void stopPeeking() { filter.stopPeeking(); }
        @Override
        public boolean isPeeking() { return filter.isPeeking(); }
    }

    private static class AggregateFilter implements PeekableEventFilter {
        private final Collection<PeekableEventFilter> filters;
        private final BinaryOperator<Boolean> accumulator;
        private boolean peeking = false;
        AggregateFilter(Collection<EventFilter> filters, BinaryOperator<Boolean> accumulator) {
            this.filters = filters.stream().filter(Objects::nonNull).map(EventFilterSupport::peekable).collect(Collectors.toList());;
            this.accumulator = accumulator;
        }
        @Override
        public void startPeeking() {
            filters.forEach(PeekableEventFilter::startPeeking);
            peeking = true;
        }
        @Override
        public void stopPeeking() {
            filters.forEach(PeekableEventFilter::stopPeeking);
            peeking = false;
        }
        @Override
        public boolean isPeeking() {
            return peeking;
        }
        @Override
        public boolean accept(XMLEvent event) {
            return filters.stream().map(f -> f.accept(event)).reduce(accumulator).orElse(true);
        }
        public String toString() {
            return filters.toString();
        }
    }

}
