package com.valensas.vlsync.lib;

/**
 * This class represents a custom {@link java.lang.RuntimeException}
 * for VLSync library.
 * </br></br>
 * Created on 1/19/15</br>
 * Created @ Valensas
 *
 * @see java.lang.RuntimeException
 *
 * @author Furkan Bayraktar
 * @version 1.0
 * @since 1.0
 */
public class VLSyncException extends RuntimeException{

    /**
     * Constructor with only detail message.
     *
     * @since 1.0
     *
     * @param detailMessage message String
     */
    public VLSyncException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Constructor with detail message and throwable object.
     *
     * @since 1.0
     *
     * @param detailMessage message String
     * @param throwable object to be thrown
     */
    public VLSyncException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    /**
     * Constructor with only throwable object.
     *
     * @since 1.0
     *
     * @param throwable object to be thrown
     */
    public VLSyncException(Throwable throwable) {
        super(throwable);
    }
}
