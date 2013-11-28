/*
 * Copyright 2013 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.raven.expr.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.weda.beans.ObjectUtils;

/**
 *
 * @author Mikhail Titov
 */
public class GroovyExpressionExceptionAnalyzator {
    private final int linesBeforeAfter;
    private final String expressionIdent;
    private final String source;
    private final Throwable error;
    private final List<Integer> errorLineNumbers = new ArrayList<Integer>(5);
    private final List<SourceCodeBlock> errorCodeBlocks;

    public GroovyExpressionExceptionAnalyzator(String expressionIdent, String source, Throwable error
            , int linesBeforeAfter) 
    {
        this.expressionIdent = expressionIdent;
        this.source = source;
        this.linesBeforeAfter = linesBeforeAfter;
        
        this.error = getLineNumbersWithErrors(error, errorLineNumbers);
        if (!errorLineNumbers.isEmpty()) {
            final String[] lines = StringUtils.splitPreserveAllTokens(source, '\n');
            List<SourceCodeBlock> codeBlocks = new ArrayList<SourceCodeBlock>(errorLineNumbers.size());
            int pos = 1;
            for (int lineNumber: errorLineNumbers)
                if (lineNumber <= lines.length)
                    codeBlocks.add(new SourceCodeBlock(pos++, lineNumber-1, lines));
            codeBlocks = mergeCodeBlocks(codeBlocks);
            Collections.sort(codeBlocks);
            errorCodeBlocks = codeBlocks;
        } else 
            errorCodeBlocks = Collections.EMPTY_LIST;
    }
    
    public StringBuilder addResultToBuilder(String prefix, StringBuilder builder) {
        if (!(error instanceof GroovyExpressionException)) {
            if (error.getMessage()!=null)
                builder.append(prefix).append("Message: ").append(error.getMessage()).append('\n');
            builder.append(prefix).append("Cause: ").append(error.getClass().getName()).append('\n');
        }
        int lastLine=-1;
        for (SourceCodeBlock block: errorCodeBlocks) {
            if (lastLine==-1 || lastLine+1 != block.fromLine)
                builder.append(prefix).append("...\n");
            lastLine = block.toLine;
            for (String line: block.getLines())
                builder.append(prefix).append(line).append('\n');
        }
        return builder;
    }
    
    private List<SourceCodeBlock> mergeCodeBlocks(List<SourceCodeBlock> blocks) {
        int mergedCount = 0;
        for (int i=0; i<blocks.size(); ++i)
            if (blocks.get(i)!=null)
                for (int j=i+1; j<blocks.size(); ++j)
                    if (blocks.get(i).intersectsWith(blocks.get(j))) {
                        mergedCount++;
                        blocks.set(i, blocks.get(i).mergeWith(blocks.get(j)));
                        blocks.set(j, null);
                    }
        if (mergedCount==0) return blocks;
        else {
            List<SourceCodeBlock> newBlocks = new ArrayList<SourceCodeBlock>(blocks.size()-mergedCount);
            for (SourceCodeBlock block: blocks)
                if (block!=null)
                    newBlocks.add(block);
            return newBlocks;
        }
    }

    public List<Integer> getErrorLineNumbers() {
        return errorLineNumbers;
    }
    
    private Throwable getLineNumbersWithErrors(final Throwable ex, final List<Integer> lineNumbers) {
        for (StackTraceElement elem: ex.getStackTrace()) 
            if (ObjectUtils.equals(elem.getFileName(), expressionIdent) && elem.getLineNumber()>=0)
                lineNumbers.add(elem.getLineNumber());
        if (lineNumbers.isEmpty() && ex.getCause()!=null)
            return getLineNumbersWithErrors(ex.getCause(), lineNumbers);
        return ex;
    }
    
    public static String aggregate(GroovyExpressionException exception) {
        LinkedList<GroovyExpressionException> errors = new LinkedList<GroovyExpressionException>();
        Throwable error = exception;
        while(error!=null) {
            if (error instanceof GroovyExpressionException) 
                errors.addFirst((GroovyExpressionException) error);
            error = error.getCause();
        }
        StringBuilder builder = new StringBuilder();
        int i=1;
        for (GroovyExpressionException err: errors) {
            builder.append("\n").append(i++).append(". ");
            err.constructMessage("   ", builder);
        }
        return builder.toString();
    }
    
    private class SourceCodeBlock implements Comparable<SourceCodeBlock>{
        private final static String ERROR_PREFIX = ">>>";
        private final int fromLine;
        private final int toLine;
        private final String[] lines;

        public SourceCodeBlock(int pos, int errorLineNumber, String[] sourceLines) {
            fromLine = errorLineNumber-linesBeforeAfter < 0? 0 : errorLineNumber-linesBeforeAfter;
            toLine = errorLineNumber+linesBeforeAfter < sourceLines.length? errorLineNumber+linesBeforeAfter : sourceLines.length-1;
            lines = new String[toLine-fromLine+1];
            for(int i=fromLine, j=0; i<=toLine; ++i)
                lines[j++] = errorLineNumber!=i? 
                        String.format("         (%3d) %s", i+1, sourceLines[i]) :
                        String.format("%s [%2d] (%3d) %s", ERROR_PREFIX, pos, i+1, sourceLines[i]);
        }
        
        public SourceCodeBlock(String[] lines, int fromLine, int toLine) {
            this.fromLine = fromLine;
            this.toLine = toLine;
            this.lines = lines;
        }
        
        public String getAt(int lineNum) {
            return lines[lineNum-fromLine];
        }

        public int getFromLine() {
            return fromLine;
        }

        public int getToLine() {
            return toLine;
        }

        public String[] getLines() {
            return lines;
        }
        
        public boolean intersectsWith(final SourceCodeBlock b) {
            return b!=null && (btw(b.getFromLine(), fromLine, toLine) || btw(b.getToLine(), fromLine, toLine));
        }
        
        public SourceCodeBlock mergeWith(final SourceCodeBlock b) {
            final int from = Math.min(fromLine, b.fromLine);
            final int to = Math.max(toLine, b.getToLine());
            final String[] newLines = new String[to-from+1];
            for (int i=from; i<=to; ++i) {
                if (btw(i, fromLine, toLine))
                    newLines[i-from] = getAt(i);
                if (   btw(i, b.getFromLine(), b.getToLine())
                    && (newLines[i-from]==null || !newLines[i-from].startsWith(ERROR_PREFIX)))
                {
                    newLines[i-from] = b.getAt(i);
                }
            }
            return new SourceCodeBlock(newLines, from, to);
        }
        
        private boolean btw(final int v, final int d1, final int d2) {
            return v >= d1 && v <= d2;
        }

        public int compareTo(SourceCodeBlock t) {
            return new Integer(fromLine).compareTo(t.getFromLine());
        }
    }
}
