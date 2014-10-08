/*
 * Copyright the original author or authors.
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

import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.lang.Writable;
import groovy.text.Template;
import groovy.text.TemplateEngine;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 *
 * @author sam
 * @author Christian Stein
 * @author Paul King
 * @author Alex Tkachman
 * @author Mikhail Titov
 */
public class RavenScriptTemplateEngine extends TemplateEngine {
    private boolean verbose;
    private final static AtomicLong counter = new AtomicLong();

    private final GroovyShell groovyShell;

    public RavenScriptTemplateEngine() {
        this(GroovyShell.class.getClassLoader());
    }

    public RavenScriptTemplateEngine(boolean verbose) {
        this(GroovyShell.class.getClassLoader());
        setVerbose(verbose);
    }

    public RavenScriptTemplateEngine(ClassLoader parentLoader) {
        this(new GroovyShell(parentLoader));
    }

    public RavenScriptTemplateEngine(GroovyShell groovyShell) {
        this.groovyShell = groovyShell;
    }

    public Template createTemplate(Reader reader) throws CompilationFailedException, IOException {
        try {
            return new RavenTemplate("RavenTemplateScript" + counter.incrementAndGet() + ".groovy", reader, groovyShell);
        } catch (Exception e) {
            throw new GroovyRuntimeException("Failed to parse template script (your template may contain an error or be trying to use expressions not currently supported): " + e.getMessage());
        }
    }
    
    public RavenTemplate createRavenTemplate(Reader reader) throws CompilationFailedException, IOException {
        return (RavenTemplate) createTemplate(reader);
    }

    /**
     * @param verbose true if you want the engine to display the template source file for debugging purposes
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public static final class RavenTemplate implements Template {

        private final Script script;
        private final String classFileName;

        public RavenTemplate(String classFileName, Reader source, GroovyShell groovyShell) throws IOException {
            this.classFileName = classFileName;
            this.script = groovyShell.parse(parse(source), classFileName);
        }

        public String getClassFileName() {
            return classFileName;
        }
        

        public Writable make() {
            return make(null);
        }

        public Writable make(final Map map) {
            return new Writable() {
                /**
                 * Write the template document with the set binding applied to the writer.
                 *
                 * @see groovy.lang.Writable#writeTo(java.io.Writer)
                 */
                public Writer writeTo(Writer writer) {
                    Binding binding;
                    if (map == null)
                        binding = new Binding();
                    else
                        binding = new Binding(map);
                    Script scriptObject = InvokerHelper.createScript(script.getClass(), binding);
                    PrintWriter pw = new PrintWriter(writer);
                    scriptObject.setProperty("out", pw);
                    scriptObject.run();
                    pw.flush();
                    return writer;
                }

                /**
                 * Convert the template and binding into a result String.
                 *
                 * @see java.lang.Object#toString()
                 */
                public String toString() {
                    StringWriter sw = new StringWriter();
                    writeTo(sw);
                    return sw.toString();
                }
            };
        }

        /**
         * Parse the text document looking for <% or <%= and then call out to the appropriate handler, otherwise copy the text directly
         * into the script while escaping quotes.
         *
         * @param reader a reader for the template text
         * @return the parsed text
         * @throws IOException if something goes wrong
         */
        protected String parse(Reader reader) throws IOException {
            if (!reader.markSupported()) {
                reader = new BufferedReader(reader);
            }
            StringWriter sw = new StringWriter();
            startScript(sw);
            int c;
            while ((c = reader.read()) != -1) {
                if (c == '<') {
                    reader.mark(1);
                    c = reader.read();
                    if (c != '%') {
                        sw.write('<');
                        reader.reset();
                    } else {
                        reader.mark(1);
                        c = reader.read();
                        if (c == '=') {
                            groovyExpression(reader, sw);
                        } else {
                            reader.reset();
                            groovySection(reader, sw);
                        }
                    }
                    continue; // at least '<' is consumed ... read next chars.
                }
                if (c == '$') {
                    reader.mark(1);
                    c = reader.read();
                    if (c != '{') {
                        sw.write('$');
                        reader.reset();
                    } else {
                        reader.mark(1);
                        sw.write("${");
                        processGSstring(reader, sw);
                    }
                    continue; // at least '$' is consumed ... read next chars.
                }
                if (c == '\"') {
                    sw.write('\\');
                }
                /*
                 * Handle raw new line characters.
                 */
                if (c == '\n' || c == '\r') {
                    if (c == '\r') { // on Windows, "\r\n" is a new line.
                        reader.mark(1);
                        c = reader.read();
                        if (c != '\n') {
                            reader.reset();
                        }
                    }
                    sw.write("\n");
                    continue;
                }
                sw.write(c);
            }
            endScript(sw);
            return sw.toString();
        }

        private void startScript(StringWriter sw) {
            sw.write("out.print(\"\"\"");
        }

        private void endScript(StringWriter sw) {
            sw.write("\"\"\");\n");
            sw.write("\n/* Generated by RavenTemplateEngine */");
        }

        private void processGSstring(Reader reader, StringWriter sw) throws IOException {
            int c;
            while ((c = reader.read()) != -1) {
                if (c != '\n' && c != '\r') {
                    sw.write(c);
                }
                if (c == '}') {
                    break;
                }
            }
        }

        /**
         * Closes the currently open write and writes out the following text as a GString expression until it reaches an end %>.
         *
         * @param reader a reader for the template text
         * @param sw     a StringWriter to write expression content
         * @throws IOException if something goes wrong
         */
        private void groovyExpression(Reader reader, StringWriter sw) throws IOException {
            sw.write("${");
            int c;
            while ((c = reader.read()) != -1) {
                if (c == '%') {
                    c = reader.read();
                    if (c != '>') {
                        sw.write('%');
                    } else {
                        break;
                    }
                }
                if (c != '\n' && c != '\r') {
                    sw.write(c);
                }
            }
            sw.write("}");
        }

        /**
         * Closes the currently open write and writes the following text as normal Groovy script code until it reaches an end %>.
         *
         * @param reader a reader for the template text
         * @param sw     a StringWriter to write expression content
         * @throws IOException if something goes wrong
         */
        private void groovySection(Reader reader, StringWriter sw) throws IOException {
            sw.write("\"\"\");");
            int c;
            while ((c = reader.read()) != -1) {
                if (c == '%') {
                    c = reader.read();
                    if (c != '>') {
                        sw.write('%');
                    } else {
                        break;
                    }
                }
                /* Don't eat EOL chars in sections - as they are valid instruction separators.
                 * See http://jira.codehaus.org/browse/GROOVY-980
                 */
                // if (c != '\n' && c != '\r') {
                sw.write(c);
                //}
            }
            sw.write(";\nout.print(\"\"\"");
        }

    }    
}
