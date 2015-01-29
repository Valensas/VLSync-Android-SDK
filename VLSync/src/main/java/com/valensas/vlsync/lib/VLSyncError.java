package com.valensas.vlsync.lib;

/**
 * This class represents an error object to be
 * delivered to library user in case of a failure.
 *
 * Created on 1/21/15
 * Created @ Valensas
 *
 * @author Furkan Bayraktar
 * @version 1.0
 * @since 1.0
 */
public class VLSyncError {

    /**
     * Descriptive message about the error.
     */
    private String message;

    /**
     * Error code.
     */
    private int code;

    /**
     * Getter method for {@link #message}
     *
     * @since 1.0
     *
     * @return {@link #message}
     */
    public String getMessage() {
        return message;
    }

    /**
     * Setter method for {@link #message}
     *
     * @since 1.0
     *
     * @param message message String
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Getter method for {@link #code}
     *
     * @since 1.0
     *
     * @return {@link #code}
     */
    public int getCode() {
        return code;
    }

    /**
     * Setter method for {@link #code}
     *
     * @since 1.0
     *
     * @param code code String
     */
    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "{ \"_class\":\"" + getClass().getName() + "\", \"message\":\"" + message + "\", \"code\":" + code + " }";
    }
}
