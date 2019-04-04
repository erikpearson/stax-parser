package com.cargill.common.xml;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.Optional;

/**
 * Created by Erik Pearson
 */
public interface XMLEventReader extends javax.xml.stream.XMLEventReader {
    Optional<XMLEvent> nextOptionalEvent();
    Optional<XMLEvent> optionalPeek();
    String getElementText() throws XMLStreamException;
    Optional<String> getOptionalElementText();
    Optional<XMLEvent> nextOptionalTag();
    Object getProperty(java.lang.String name) throws java.lang.IllegalArgumentException;
}
