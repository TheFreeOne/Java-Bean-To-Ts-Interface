package org.freeone.javabean.tsinterface.util;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;

import java.io.File;
import java.util.Optional;

public class JavaUtils {
    public static CompilationUnit parse(String absolutePath) throws Exception {

        ParseResult<CompilationUnit> parse = new JavaParser().parse(new File(absolutePath));
        Optional<CompilationUnit> result = parse.getResult();
        return result.get();
    }
}
