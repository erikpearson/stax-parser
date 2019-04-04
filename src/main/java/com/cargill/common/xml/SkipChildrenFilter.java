package com.cargill.common.xml;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Created by Erik Pearson
 */
public class SkipChildrenFilter extends PeekableEventFilterImpl {

    private final Deque<StartElement> stack = new LinkedList<>();

    @Override
    public boolean accept(XMLEvent event) {
        if (event.isStartElement()) {
            if (!isPeeking()) stack.push(event.asStartElement());
            return false;
        }
        if (event.isEndElement()) {
            StartElement startElement = isPeeking() ? stack.peekFirst() : stack.poll();
            if (startElement == null) {
                return true;
            }
            if (!startElement.getName().equals(event.asEndElement().getName())) {
                throw new IllegalStateException("unmatched EndElement: " + event);
            }
            return false;
        }
        return false;
    }

    @Override
    public String toString() {
        return "skipChildren{" +
                "stack=" + stack +
                "," + super.toString() + "}";
    }
}
