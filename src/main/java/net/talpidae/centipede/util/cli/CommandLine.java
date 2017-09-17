/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package net.talpidae.centipede.util.cli;

import java.util.*;


// copied from apache commons-exec at:
//   https://github.com/apache/commons-exec/blob/trunk/src/main/java/org/apache/commons/exec/CommandLine.java
public class CommandLine
{
    private static final int NORMAL = 0;

    private static final int IN_QUOTE = 1;

    private static final int IN_DOUBLE_QUOTE = 2;

    /**
     * Crack a command line.
     *
     * @param commandLine the command line to process
     * @return The command line broken into a list of strings. An empty or null commandLine
     * parameter results in a zero sized array
     */
    public static List<String> split(final String commandLine)
    {
        if (commandLine == null || commandLine.length() == 0)
        {
            // no command? no string
            return Collections.emptyList();
        }

        // parse with a simple finite state machine

        final StringTokenizer tok = new StringTokenizer(commandLine, "\"\' ", true);
        final List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        int state = NORMAL;
        boolean lastTokenHasBeenQuoted = false;

        while (tok.hasMoreTokens())
        {
            final String nextTok = tok.nextToken();
            switch (state)
            {
                case IN_QUOTE:
                    if ("\'".equals(nextTok))
                    {
                        lastTokenHasBeenQuoted = true;
                        state = NORMAL;
                    }
                    else
                    {
                        current.append(nextTok);
                    }
                    break;
                case IN_DOUBLE_QUOTE:
                    if ("\"".equals(nextTok))
                    {
                        lastTokenHasBeenQuoted = true;
                        state = NORMAL;
                    }
                    else
                    {
                        current.append(nextTok);
                    }
                    break;
                default:
                    if ("\'".equals(nextTok))
                    {
                        state = IN_QUOTE;
                    }
                    else if ("\"".equals(nextTok))
                    {
                        state = IN_DOUBLE_QUOTE;
                    }
                    else if (" ".equals(nextTok))
                    {
                        if (lastTokenHasBeenQuoted || current.length() != 0)
                        {
                            args.add(current.toString());
                            current = new StringBuilder();
                        }
                    }
                    else
                    {
                        current.append(nextTok);
                    }
                    lastTokenHasBeenQuoted = false;
                    break;
            }
        }

        if (lastTokenHasBeenQuoted || current.length() != 0)
        {
            args.add(current.toString());
        }

        return args;
    }
}
