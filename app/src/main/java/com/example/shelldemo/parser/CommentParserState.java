package com.example.shelldemo.parser;

/**
 * Tracks the state of SQL comment parsing.
 * Manages the state for string literals and comment detection.
 */
public class CommentParserState {
    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;
    boolean inLineComment = false;
    boolean inMultiLineComment = false;
    
    /**
     * Checks if the parser is currently within any type of comment.
     * 
     * @return true if inside a line or multi-line comment
     */
    public boolean inAnyComment() {
        return inLineComment || inMultiLineComment;
    }
    
    /**
     * Checks if the parser is currently within a string literal.
     * 
     * @return true if inside single or double quotes
     */
    public boolean inAnyString() {
        return inSingleQuote || inDoubleQuote;
    }
} 