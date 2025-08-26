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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.internal.org.jline.terminal.TerminalBuilder;
import jdk.internal.org.jline.utils.ExecHelper;
import jdk.internal.org.jline.utils.OSUtils;
import jdk.jfr.consumer.RecordingStream;

import toolbox.ToolBox;

public class TerminalExecTest {

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

            // remove cygpath command executed by OSUtils.cygPathToWinPath()
            if (OSUtils.IS_CYGWIN || OSUtils.IS_MSYSTEM) {
                commands.removeIf(cmd -> cmd.contains("cygpath"));
            }

            String expectedRegex;
            if (OSUtils.IS_CYGWIN) {
                expectedRegex = "cygwin.*\\\\bin\\\\test\\.exe";
            } else if (OSUtils.IS_MSYSTEM) {
                expectedRegex = "msys.*\\\\usr\\\\bin\\\\test\\.exe";
            } else {
                expectedRegex = "bin/test";
            }

            Pattern pattern = Pattern.compile(expectedRegex);

            for (String cmd : commands) {
                Matcher matcher = pattern.matcher(cmd);
                if (!matcher.find()) {
                    System.err.println("Command did not match expected pattern.");
                    System.err.println("  Expected Regex: \"" + expectedRegex + "\"");
                    System.err.println("  Actual Command:   \"" + cmd + "\"");
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
                                   TerminalExecTest.class.getName(),
                                   "run-test")
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT);
             
            String javaExe = ToolBox.isWindows() ? "java.exe" : "java";
            String testExe = ToolBox.isWindows() ? "test.exe" : "test";

            tb.copyDir(Path.of(ToolBox.testJDK), Path.of("tmp"));
            // copy java exe file as test exe file
            tb.moveFile(Path.of("tmp", "bin", javaExe), Path.of("tmp", "bin", testExe));

            // add test exe file directory to PATH
            String currentPath = System.getenv("PATH");
            String testExeDirPath = Path.of("tmp", "bin").toAbsolutePath().toString();
            String newPath = testExeDirPath + File.pathSeparator + currentPath;
            Map<String, String> env = pb.environment();
            env.put("PATH", newPath);

            Process target = pb.start();
            target.waitFor();

            delete(Path.of("tmp").toFile());

            int exitCode = target.exitValue();
            if (exitCode != 0) {
                throw new AssertionError("Incorrect exit value, expected 0, got: " + exitCode);
            }
        }
    }

    private static void delete(File f) {
        if (f != null && f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        if (!f.delete()) {
            System.err.println("WARNING: unable to delete/cleanup directory: " + f);
        }
    }
}
