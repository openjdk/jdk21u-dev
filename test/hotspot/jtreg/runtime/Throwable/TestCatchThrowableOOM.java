/*
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8267118
 * @summary Test catching Throwable doesn't trigger OOME
 * @requires vm.flagless
 * @library /test/lib
 * @run driver TestCatchThrowableOOM
 */

import java.util.HashMap;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestCatchThrowableOOM {

    private static String[] expected = new String[] {
        "Test starting ...",
        "Test complete",
        "Exception <a 'java/lang/OutOfMemoryError'", // from logging
    };

    public static void main(String[] args) throws Throwable {
        ProcessBuilder pb = ProcessTools.createLimitedTestJavaProcessBuilder("-Xmx64m",
                                                                             "-Xlog:exceptions=trace",

                                                                             "TestCatchThrowableOOM$OOM");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        for (String msg : expected) {
            output.shouldContain(msg);
        }
    }

    static class OOM {
        private static HashMap<Object, Object> store = new HashMap<>();
        public static void main(String[] args) {
            System.out.println(expected[0]);
            try {
                // Keep adding entries until we throw OOME
                while (true) {
                    Object o = new Byte[100 * 1024];
                    store.put(o, o);
                }
            } catch (Throwable oome) {
                store = null;
                System.gc(); // Just for good measure
                System.out.println(expected[1]);
            }
        }
    }
}
