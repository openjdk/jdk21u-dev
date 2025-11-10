/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4899022
   @summary Look for erroneous representation of drive letter
   @run junit GetCanonicalPath
 */

import java.io.*;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;

import static org.junit.jupiter.api.Assertions.*;

public class GetCanonicalPath {

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void driveLetter() throws IOException {
        String path = new File("c:/").getCanonicalPath();
        assertFalse(path.length() > 3, "Drive letter incorrectly represented");
    }

    private static Stream<Arguments> pathProviderUnix() {
        return Stream.of(
                Arguments.of("/../../../../../a/b/c", "/a/b/c"),
                Arguments.of("/../../../../../a/../b/c", "/b/c"),
                Arguments.of("/../../../../../a/../../b/c", "/b/c"),
                Arguments.of("/../../../../../a/../../../b/c", "/b/c"),
                Arguments.of("/../../../../../a/../../../../b/c", "/b/c")
        );
    }

    @ParameterizedTest
    @EnabledOnOs({OS.AIX, OS.LINUX, OS.MAC})
    @MethodSource("pathProviderUnix")
    void goodPathsUnix(String pathname, String expected) throws IOException {
        File file = new File(pathname);
        String canonicalPath = file.getCanonicalPath();
        assertEquals(expected, canonicalPath);
    }
}
