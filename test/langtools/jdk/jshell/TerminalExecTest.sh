#!/bin/sh

#
# Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

# @test
# @bug 8334433
# @summary Verify that unintended executable file 'test' is not executed
# @library /tools/lib
# @modules jdk.internal.le/jdk.internal.org.jline.terminal
#          jdk.internal.le/jdk.internal.org.jline.utils
# @build toolbox.ToolBox toolbox.JavaTask
# @compile TerminalExecTest.java
# @run shell TerminalExecTest.sh

# In jtreg, environment variable PWD cannot be retrieved unless the test is run from a shell script.
# This causes the value of IS_CYGWIN in OSUtils to be set incorrectly. So run the tests from this shell script.

case `uname -s` in
  MSYS*)
    # When running tests using jtreg, the environment variables MSYSTEM is not available,
    # so set it here to allow OSUtils to set IS_MSYSTEM correctly.
    if [ -z "$MSYSTEM" ]; then
      export MSYSTEM="MSYS"
    fi
    ;;
esac

${TESTJAVA}/bin/java ${TESTVMOPTS} -Dtest.jdk=${TESTJAVA} -classpath ${TESTCLASSPATH} TerminalExecTest
if [ $? != 0 ]
then
  echo "Unintended executable file is executed. Failed."
  exit 1
fi

exit 0
