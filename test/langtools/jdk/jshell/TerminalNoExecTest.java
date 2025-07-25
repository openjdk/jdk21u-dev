/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 8334433
 * @summary Verify that unintended executable file 'test' is not executed
 * @library /tools/lib
 * @modules jdk.internal.le/jdk.internal.org.jline.terminal
 *          jdk.internal.le/jdk.internal.org.jline.utils
 * @build toolbox.ToolBox toolbox.JavaTask TerminalNoExecTest
 * @run main TerminalNoExecTest
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jdk.internal.org.jline.terminal.TerminalBuilder;
import jdk.internal.org.jline.utils.ExecHelper;
import jdk.internal.org.jline.utils.OSUtils;
import jdk.jfr.consumer.RecordingStream;

import toolbox.ToolBox;

public class TerminalNoExecTest {

    public static void main(String... args) throws Exception {
        if (args.length > 0) {
            List<String> commands = new ArrayList<>();
            try (var rs = new RecordingStream()) {
                rs.enable("jdk.ProcessStart").withoutThreshold();
                rs.onEvent(evt -> {
                    System.out.println("evt: " + evt);
                    commands.add(evt.getValue("command"));
                });
                rs.startAsync();
                TerminalBuilder.terminal();
                rs.stop();
            }

            if (OSUtils.IS_CYGWIN || OSUtils.IS_MSYSTEM) {
                commands.removeIf(cmd -> cmd.contains("cygpath"));
            }

            String expected;
            if (OSUtils.IS_CYGWIN) {
                expected = "cygwin64\\bin\\test.exe";
            } else if (OSUtils.IS_MSYSTEM) {
                expected = "msys64\\usr\\bin\\test.exe";
            } else {
                expected = "bin/test";
            }

            for (String cmd : commands) {
                if (!cmd.contains(expected)) {
                    System.err.println("Expected to contain: \"" + expected + "\"");
                    System.err.println("Actual command:      \"" + cmd + "\"");
                    System.exit(1);
                }
            }
            
            System.exit(0);
        } else {
            ToolBox tb = new ToolBox();
            ProcessBuilder pb =
                new ProcessBuilder(tb.getJDKTool("java").toString(),
                                   "-classpath", System.getProperty("java.class.path"),
                                   "--add-exports", 
                                   "jdk.internal.le/jdk.internal.org.jline.utils=ALL-UNNAMED",
                                   "--add-exports", 
                                   "jdk.internal.le/jdk.internal.org.jline.terminal=ALL-UNNAMED",
                                   Test.class.getName(),
                                   "run-test")
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT);
            
            Map<String, String> env = pb.environment();

            // When running tests using jtreg, the environment variables PWD and MSYSTEM are not available,
            // so set them here to allow OSUtils to set IS_CYGWIN and IS_MSYSTEM correctly.
            if (System.getenv("PATH").contains("cygwin64")) {
                Process p = new ProcessBuilder("pwd").start();
                String result = ExecHelper.waitAndCapture(p);
                if (p.exitValue() == 0) {
                    env.put("PWD", result.trim());
                } else {
                    throw new AssertionError("Failed to get environment variable PWD by `pwd`");
                }
            } else if (System.getenv("PATH").contains("msys64")) {
                env.put("MSYSTEM", "MSYS");
            }

            Process target = pb.start();
            target.waitFor();

            int exitCode = target.exitValue();

            if (exitCode != 0) {
                throw new AssertionError("Incorrect exit value, expected 0, got: " + exitCode);
            }
        }
    }
}
