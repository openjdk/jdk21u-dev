/*
 * Copyright (c) 2025, Google LLC. All rights reserved.
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
 * @bug 8359336
 * @summary javac crashes with NPE while iterating params of MethodSymbol
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.compiler/com.sun.tools.javac.file
 *          java.base/jdk.internal.classfile
 *          java.base/jdk.internal.classfile.attribute
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main ParameterAnnotationsCrash
 *
 */

import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;

import jdk.internal.classfile.ClassModel;
import jdk.internal.classfile.ClassTransform;
import jdk.internal.classfile.Classfile;
import jdk.internal.classfile.MethodTransform;
import jdk.internal.classfile.attribute.RuntimeVisibleParameterAnnotationsAttribute;

import toolbox.JavacTask;
import toolbox.TestRunner;
import toolbox.ToolBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.StandardLocation;

public class ParameterAnnotationsCrash extends TestRunner {

    ToolBox tb;

    public static void main(String... args) throws Exception {
        new ParameterAnnotationsCrash().runTests();
    }

    ParameterAnnotationsCrash() {
        super(System.err);
        tb = new ToolBox();
    }

    public void runTests() throws Exception {
        runTests(m -> new Object[] {Paths.get(m.getName())});
    }

    @Test
    public void test(Path base) throws Exception {
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        new JavacTask(tb)
                .outdir(classes)
                .sources(
                        """
                        import java.lang.annotation.ElementType;
                        import java.lang.annotation.Retention;
                        import java.lang.annotation.RetentionPolicy;
                        import java.lang.annotation.Target;
                        class T {
                          @Target({ElementType.TYPE_USE, ElementType.PARAMETER})
                          @Retention(RetentionPolicy.RUNTIME)
                          @interface A {}
                          class I {}
                          public static void f(@A int x, int y) {}
                        }
                        """)
                .run()
                .writeAll();

        // Create a parameter annotation attribute that doesn't match the number of parameter,
        // which caused an AIOOBE in ClassReader#setParameters.
        // The crash is fixed in later versions by 8334870.
        transform(classes.resolve("T.class"));

        JavacTool tool = JavacTool.create();
        JavacFileManager fileManager = tool.getStandardFileManager(null, null, null);
        fileManager.setLocationFromPaths(StandardLocation.CLASS_PATH, List.of(classes));
        var javacTask =
                tool.getTask(
                        null,
                        fileManager,
                        null,
                        List.of("-XDaddTypeAnnotationsToSymbol=true"),
                        List.of(),
                        List.of());

        // Complete the inner class, which causes the enclosing class to be completed.
        // Completing T crashes due to the AIOOBE.
        Elements elements = javacTask.getElements();
        TypeElement i = elements.getTypeElement("T.I");
        if (i != null) {
            System.err.println(i.getEnclosedElements());
        }
    }

    private static void transform(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        ClassModel classModel = Classfile.parse(bytes);
        MethodTransform methodTransform =
                (mb, me) -> {
                    if (me instanceof RuntimeVisibleParameterAnnotationsAttribute annos) {
                        mb.with(
                                RuntimeVisibleParameterAnnotationsAttribute.of(
                                        List.of(annos.parameterAnnotations().get(0))));
                    } else {
                        mb.with(me);
                    }
                };

        ClassTransform classTransform = ClassTransform.transformingMethods(methodTransform);
        bytes = classModel.transform(classTransform);
        Files.write(path, bytes);
    }
}
