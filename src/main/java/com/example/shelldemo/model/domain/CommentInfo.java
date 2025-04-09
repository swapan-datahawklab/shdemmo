package com.example.shelldemo.model.domain;

/**
 * Helper class to store information about a found comment during SQL parsing.
 * Used by SqlScriptParser.
 */
// Make package-private as it's only intended for use within this package
public class CommentInfo {
    public final int position;
    public final boolean isMultiLine;
    public final boolean found; // Indicates if any comment was found

    // Constructor for a found comment
    public CommentInfo(int position, boolean isMultiLine) {
        this.position = position;
        this.isMultiLine = isMultiLine;
        this.found = true;
    }

    // Constructor for no comment found
    CommentInfo() {
        this.position = -1;
        this.isMultiLine = false;
        this.found = false;
    }

    // Singleton instance representing "no comment found"
    public static final CommentInfo NONE = new CommentInfo();
}