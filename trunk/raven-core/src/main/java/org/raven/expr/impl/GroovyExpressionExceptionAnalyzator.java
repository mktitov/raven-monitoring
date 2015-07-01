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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang.StringUtils;
import org.raven.expr.ExpressionInfo;
import org.raven.tree.PropagatedAttributeValueError;

/**
 *
 * @author Mikhail Titov
 */
public class GroovyExpressionExceptionAnalyzator {
    private final int linesBeforeAfter;
    private final String expressionIdent;
    private final Throwable initialError;
    private final boolean analyzeFullStack;

    public GroovyExpressionExceptionAnalyzator(String expressionIdent, Throwable error, int linesBeforeAfter) 
    {
        this(expressionIdent, error, linesBeforeAfter, false);
    }
    
    public GroovyExpressionExceptionAnalyzator(String expressionIdent, Throwable error, int linesBeforeAfter,
            boolean analyzeFullStack) 
    {
        this.expressionIdent = expressionIdent;
        this.linesBeforeAfter = linesBeforeAfter;
        this.initialError = error;
        this.analyzeFullStack = analyzeFullStack;
    }
    
    public Collection<MessageConstructor> getMessageConstructors(final Map<String, ExpressionInfo> sources) 
    {
        Map<String, List<Integer>> errors = new LinkedHashMap<String, List<Integer>>();
        Throwable error = getLineNumbersWithErrors(initialError, errors, sources);
        Collection<MessageConstructor> messageConstructors = new LinkedList<MessageConstructor>();
        for (Map.Entry<String, List<Integer>> entry: errors.entrySet()) {
            List<Integer> errorLineNumbers = entry.getValue();
            if (!errorLineNumbers.isEmpty()) {
                final ExpressionInfo exprInfo = sources.get(entry.getKey());
                final String[] lines = StringUtils.splitPreserveAllTokens(exprInfo.getSource(), '\n');  
                List<SourceCodeBlock> codeBlocks = new ArrayList<SourceCodeBlock>(errorLineNumbers.size());
                int pos = 1;
                for (int lineNumber: errorLineNumbers)
                    if (lineNumber <= lines.length)
                        codeBlocks.add(new SourceCodeBlock(pos++, lineNumber-1, lines));
                if (!codeBlocks.isEmpty()) {
                    codeBlocks = mergeCodeBlocks(codeBlocks);
                    Collections.sort(codeBlocks);
                    messageConstructors.add(new MessageConstructorImpl(codeBlocks, exprInfo, error));
                }
            }
        }
        return messageConstructors.isEmpty()? Collections.EMPTY_LIST : messageConstructors;
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

    private Throwable getLineNumbersWithErrors(final Throwable ex, final Map<String, List<Integer>> lineNumbers, 
            final Map<String, ExpressionInfo> sources) 
    {
        String exprId; 
        ExpressionInfo exprInfo;
        List<Integer> numbers;
        for (StackTraceElement elem: ex.getStackTrace()) {
            if (elem.getLineNumber()>=0) {
                exprId = elem.getFileName();
                exprInfo = sources.get(exprId);
                if (exprInfo!=null) {
                    if (lineNumbers.containsKey(expressionIdent) && !exprId.equals(expressionIdent) && !analyzeFullStack)
                        break;
                    numbers = lineNumbers.get(exprId);
                    if (numbers==null) {
                        numbers = new ArrayList<Integer>(5);
                        lineNumbers.put(exprId, numbers);
                    }
                    if (!numbers.contains(elem.getLineNumber()))
                        numbers.add(elem.getLineNumber());
                }
            }
        }
        if (!lineNumbers.containsKey(expressionIdent))
            lineNumbers.clear();
        if (lineNumbers.isEmpty() && ex.getCause()!=null)
            return getLineNumbersWithErrors(ex.getCause(), lineNumbers, sources);
        return ex;
        
    }
       
    public static String aggregate(final GroovyExpressionException exception, 
            final Map<String, ExpressionInfo> sources, final AtomicInteger counter) 
    {
        LinkedList<GroovyExpressionException> errors = new LinkedList<GroovyExpressionException>();
        Throwable error = exception;
        while(error!=null) {
            if (error instanceof GroovyExpressionException) 
                errors.addFirst((GroovyExpressionException) error);
            error = error.getCause();
        }
        StringBuilder builder = new StringBuilder();
        for (GroovyExpressionException err: errors) {
            for (MessageConstructor messCons: err.getMessageConstructors(sources)) {
                builder.append("\n").append(counter.getAndIncrement()).append(". ");
                messCons.constructMessage("   ", builder);
            }
        }
        return builder.toString();
    }
    
    private static class MessageConstructorImpl implements MessageConstructor {
        private final List<SourceCodeBlock> errorCodeBlocks;
        private final ExpressionInfo expressionInfo;
        private final Throwable error;

        public MessageConstructorImpl(List<SourceCodeBlock> errorCodeBlocks, ExpressionInfo expressionInfo, 
                Throwable error) 
        {
            this.errorCodeBlocks = errorCodeBlocks;
            this.expressionInfo = expressionInfo;
            this.error = error;
        }

        public StringBuilder constructMessage(String prefix, StringBuilder builder) {
            builder.append("Exception at @").append(expressionInfo.getAttrName()).
                    append(" (").append(expressionInfo.getNode().getPath()).append(")\n");
            if (!(error instanceof GroovyExpressionException) && !(error instanceof PropagatedAttributeValueError)) {
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
