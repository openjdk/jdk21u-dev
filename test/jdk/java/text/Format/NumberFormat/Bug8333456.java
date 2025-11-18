/*
 * Copyright (c) 2004, 2025, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8333456
 * @summary Make sure that CompactNumberFormat integer parsing doesn't fail
 * when there is no suffix.
 * @run junit Bug8333456
 */
import java.text.CompactNumberFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Bug8333456 {

    private static final CompactNumberFormat cmpctFmt =
            (CompactNumberFormat) NumberFormat.getCompactNumberInstance(Locale.US,
                    NumberFormat.Style.SHORT);

    // No compact suffixes
    private static final Stream<Arguments> compactValidNoSuffixParseStrings() {
        return Stream.of(
            Arguments.of("5", 5),
            Arguments.of("50", 50),
            Arguments.of("50.", 50),
            Arguments.of("5,000", 5000),
            Arguments.of("5,000.", 5000),
            Arguments.of("5,000.00", 5000)
        );
    }
                    
    // 8333456: Parse values with no compact suffix -> which allows parsing to iterate
    // position to the same value as string length which throws
    // StringIndexOutOfBoundsException upon charAt invocation
    @ParameterizedTest
    @MethodSource("compactValidNoSuffixParseStrings")
    @EnabledIfSystemProperty(named = "user.language", matches = "en")
    public void compactFmtSuccessParseIntOnlyTest(String toParse, double expectedValue) {
        cmpctFmt.setParseIntegerOnly(true);
        cmpctFmt.setGroupingUsed(true);
        assertEquals(expectedValue, successParse(cmpctFmt, toParse, toParse.length()));
        cmpctFmt.setParseIntegerOnly(false);
    }
    
    // Method is used when a String should parse successfully. This does not indicate
    // that the entire String was used, however. The index and errorIndex values
    // should be as expected.
    private double successParse(NumberFormat fmt, String toParse, int expectedIndex) {
        Number parsedValue = assertDoesNotThrow(() -> fmt.parse(toParse));
        ParsePosition pp = new ParsePosition(0);
        assertDoesNotThrow(() -> fmt.parse(toParse, pp));
        assertEquals(-1, pp.getErrorIndex(),
                "ParsePosition ErrorIndex is not in correct location");
        assertEquals(expectedIndex, pp.getIndex(),
                "ParsePosition Index is not in correct location");
        return parsedValue.doubleValue();
    }
}
