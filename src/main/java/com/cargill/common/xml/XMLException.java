package com.cargill.common.xml;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 * Created by Erik Pearson
 *
 * RuntimeException based on {@link javax.xml.stream.XMLStreamException}
 */
public class XMLException extends RuntimeException {

    protected Throwable nested;
    protected Location location;

    /**
     * Default constructor
     */
    public XMLException(){
        super();
    }

    /**
     * Construct an exception with the assocated message.
     *
     * @param msg the message to report
     */
    public XMLException(String msg) {
        super(msg);
    }

    /**
     * Construct an exception with the assocated exception
     *
     * @param th a nested exception
     */
    public XMLException(Throwable th) {
        super(th);
        nested = th;
    }

    /**
     * Construct an exception with the assocated message and exception
     *
     * @param th a nested exception
     * @param msg the message to report
     */
    public XMLException(String msg, Throwable th) {
        super(msg, th);
        nested = th;
    }

    /**
     * Construct an exception with the assocated message, exception and location.
     *
     * @param th a nested exception
     * @param msg the message to report
     * @param location the location of the error
     */
    public XMLException(String msg, Location location, Throwable th) {
        super("ParseError at [row,col]:["+location.getLineNumber()+","+
                location.getColumnNumber()+"]\n"+
                "Message: "+msg);
        nested = th;
        this.location = location;
    }

    /**
     * Construct an exception with the assocated message, exception and location.
     *
     * @param msg the message to report
     * @param location the location of the error
     */
    public XMLException(String msg, Location location) {
        super("ParseError at [row,col]:["+location.getLineNumber()+","+
                location.getColumnNumber()+"]\n"+
                "Message: "+msg);
        this.location = location;
    }

    public XMLException(XMLStreamException ex) {
        super(ex.getMessage(), ex.getNestedException());
        this.location = ex.getLocation();
    }

    /**
     * Gets the nested exception.
     *
     * @return Nested exception
     */
    public Throwable getNestedException() {
        return nested;
    }

    /**
     * Gets the location of the exception
     *
     * @return the location of the exception, may be null if none is available
     */
    public Location getLocation() {
        return location;
    }


}
