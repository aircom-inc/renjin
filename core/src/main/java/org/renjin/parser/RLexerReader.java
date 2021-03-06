/*
 * Renjin : JVM-based interpreter for the R language for the statistical analysis
 * Copyright © 2010-2019 BeDataDriven Groep B.V. and contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.gnu.org/licenses/gpl-2.0.txt
 */
package org.renjin.parser;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;

/**
 * A Reader-like for RLexer that supports pushback and line/column tracking
 *
 */
public class RLexerReader {
  private static final int PUSHBACK_BUFSIZE = 16;
  private int pushback[] = new int[PUSHBACK_BUFSIZE];
  private int npush = 0;

  private Reader reader;
  
  private int prevpos = 0;
  private int prevlines[] = new int[PUSHBACK_BUFSIZE];
  private int prevcols[] = new int[PUSHBACK_BUFSIZE];
  private int prevbytes[] = new int[PUSHBACK_BUFSIZE];

  private int lineNumber = 0;
  private int charIndex = -1;
  private int columnNumber = -1;

  public RLexerReader(Reader reader) {
    super();
    this.reader = new PushbackReader(reader);
  }

  public Position getPosition() {
    return new Position(lineNumber, columnNumber, charIndex);
  }

  public int read() throws IOException {
    int c;

    if (npush != 0) {
      c = pushback[--npush];
    } else {
      try {
        c = reader.read();
      } catch (IOException e) {
        throw new RLexException("IOException while reading", e);
      }
    }

    prevpos = (prevpos + 1) % PUSHBACK_BUFSIZE;
    prevcols[prevpos] = columnNumber;
    prevlines[prevpos] = lineNumber;
    prevbytes[prevpos] = charIndex;

    if(c == '\n') {
      lineNumber++;
      columnNumber = -1;
      charIndex = -1;
    } else {
      columnNumber++;
      charIndex++;
    }

    if (c == '\t') { 
      columnNumber = ((columnNumber + 7) & ~7);
    }

    return c;
  }

  public int unread(int c) {
    lineNumber = prevlines[prevpos];
    columnNumber = prevcols[prevpos];
    charIndex = prevbytes[prevpos];
    prevpos = (prevpos + PUSHBACK_BUFSIZE - 1) % PUSHBACK_BUFSIZE;

    // if ( KeepSource && GenerateCode && FunctionLevel > 0 )
    // SourcePtr--;
    //R_ParseContext[R_ParseContextLast] = '\0';
    /* precaution as to how % is implemented for < 0 numbers */
    //  R_ParseContextLast = (R_ParseContextLast + PARSE_CONTEXT_SIZE -1) % PARSE_CONTEXT_SIZE;
    if (npush >= PUSHBACK_BUFSIZE) {
      throw new RuntimeException("Pusback buffer exceeded");
    }
    pushback[npush++] = c;
    return c;
  }

  /**
   * @return The zero-based line number of the character we just read. The newline character
   * is considered to be the last character of the line it terminates.
   */
  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * @return The zero-based index of the character we just read within the current line.
   */
  public int getColumnNumber() {
    return columnNumber;
  }

  /**
   * @return The zero-based index of the character we just read within the whole character stream.
   */
  public int getCharacterIndex() {
    return charIndex;
  }

   // called during processing #line directive.
  public void setLineNumber(int lineNumber) {
    this.lineNumber = lineNumber;
    this.columnNumber = 0;
  }
}
