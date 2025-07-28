package com.blothera.book;

/**
 * Enum representing the result of handling a book command.
 * <p>
 * HANDLED_AND_KEEP indicates that the command was handled successfully and the book should be kept.
 * NOT_HANDLED indicates that the command was not handled and the book should work as vanilla.
 */
public enum BookResult {
    HANDLED_BOOK,
    ERROR_BOOK,
    NOT_HANDLED
}



