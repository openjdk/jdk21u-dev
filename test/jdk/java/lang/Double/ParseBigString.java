/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4428022
 * @summary Tests for Double.toString
 * @run main/othervm -Xmx8G ParseBigString
 */

public class ParseBigString {

    public static void main(String args[]) {
        System.out.println("Testing ParseBigString");
        String s = "9999999999e10000";

        if (!Double.isInfinite(Double.parseDouble(s))) {
            throw new RuntimeException("parseDouble(" + s + "): " + Double.parseDouble(s) + " (expected: Infinity)");
        }

        s = "9".repeat(Integer.MAX_VALUE - 100) + "e10000";
        if (!Double.isInfinite(Double.parseDouble(s))) {
            System.out.println("parseDouble(999<...>99e10000): " + Double.parseDouble(s) + " (expected: Infinity)");
        }

        int x = 1 + Integer.MAX_VALUE / 2;
        s = "9".repeat(x) + "e" + x;
        try {
            if (!Double.isInfinite(Double.parseDouble(s))) {
                throw new RuntimeException("parseDouble(" + s + "): " + Double.parseDouble(s) + " (expected: Infinity)");
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("parseDouble(" + s + "): " + Double.parseDouble(s) + " (expected: Infinity)");
        }
    }
}
