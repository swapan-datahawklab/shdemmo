package com.example.shelldemo.parser;

/**
 * Represents the result of processing a character in SQL parsing.
 * Contains information about whether to append the character and how to shift the index.
 */
public class ProcessResult {
    final boolean appendChar;
    final int indexShift;
    
    private ProcessResult(boolean appendChar, int indexShift) {
        this.appendChar = appendChar;
        this.indexShift = indexShift;
    }
    
    /**
     * Creates a result indicating the character should be skipped.
     * 
     * @param indexShift additional positions to skip
     * @return a ProcessResult with appendChar=false
     */
    public static ProcessResult skip(int indexShift) {
        return new ProcessResult(false, indexShift);
    }
    
    /**
     * Creates a result indicating the character should be appended.
     * 
     * @return a ProcessResult with appendChar=true and no index shift
     */
    public static ProcessResult append() {
        return new ProcessResult(true, 0);
    }
} 