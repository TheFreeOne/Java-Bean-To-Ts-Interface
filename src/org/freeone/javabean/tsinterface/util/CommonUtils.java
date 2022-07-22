package org.freeone.javabean.tsinterface.util;

import com.intellij.core.JavaCoreApplicationEnvironment;
import com.intellij.core.JavaCoreProjectEnvironment;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.FileASTNode;
import com.intellij.mock.MockProject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class CommonUtils {

    public static final List<String> numberTypes = Arrays.asList("byte", "short", "int", "long", "double", "float");


    /**
     * Determine whether the field is a numeric type
     *
     * @param field
     * @return
     */
    public static boolean isNumber(PsiField field) {
        List<PsiType> numberSuperClass = Arrays.stream(field.getType().getSuperTypes()).filter(superTypeItem -> superTypeItem.getCanonicalText().equals("java.lang.Number")).collect(Collectors.toList());
        if (!numberSuperClass.isEmpty()) {
            return true;
        }
        String canonicalText = field.getType().getCanonicalText();
        if (numberTypes.contains(canonicalText)) {
            return true;
        }
        return false;
    }

    /**
     * Check whether the field is an array
     *
     * @param field
     * @return
     */
    public static boolean isArray(PsiField field) {
        boolean contains = field.getText().contains("[]");
        if (contains) {
            return true;
        }
        PsiType[] superTypes = field.getType().getSuperTypes();
        List<PsiType> collect = Arrays.stream(superTypes).filter(superType -> superType.getCanonicalText().contains("java.util.Collection<")).collect(Collectors.toList());
        if (!collect.isEmpty()) {
            return true;
        }
        return false;
    }

    public static boolean isString(PsiField field) {
        String presentableText = field.getType().getPresentableText();
        if (presentableText.equals("String")) {
            return true;
        }
        return false;
    }

    public static boolean isBoolean(PsiField field) {
        String canonicalText = field.getType().getCanonicalText();
        if ("java.lang.Boolean".equals(canonicalText) || "boolean".equals(canonicalText)) {
            return true;
        } else {
            return false;
        }
    }

    private static ExecutorService cachedThreadPool;

//    private static final String baseDir = "/home/patrick/tmp";
//    private static final String sourceFile = "TestClass.java";


//    public static void main(String[] args) throws IOException {
//        PsiFileFactory psiFileFactory = createPsiFactory();
//        File file = new File(baseDir, sourceFile);
//        String javaSource = FileUtil.loadFile(file);
//        FileASTNode node = parseJavaSource(javaSource, psiFileFactory);
//
//    }

    /**
     * 解析java文件
     *
     * @param absolutePath
     * @return
     * @throws Exception
     */
    public static PsiJavaFile parseJavaFile(String absolutePath) throws Exception {
        PsiFileFactory psiFileFactory = createPsiFactory();
        File file = new File(absolutePath);
        String javaSource = FileUtil.loadFile(file);
        return parseJavaSource(javaSource, psiFileFactory);
    }

    private static PsiFileFactory createPsiFactory() {
        MockProject mockProject = createProject();
        return PsiFileFactory.getInstance(mockProject);
    }

    private static PsiJavaFile parseJavaSource(String JAVA_SOURCE, PsiFileFactory psiFileFactory) {
        PsiFile psiFile = psiFileFactory.createFileFromText("__dummy_file__.java", JavaFileType.INSTANCE, JAVA_SOURCE);

        if (psiFile instanceof PsiJavaFile) {
//        return psiJavaFile.getNode();
            return (PsiJavaFile) psiFile;
        } else {
            throw new RuntimeException("Target is not a valid java file");
        }
    }

    private static MockProject createProject() {
        JavaCoreProjectEnvironment environment = new JavaCoreProjectEnvironment(new Disposable() {
            @Override
            public void dispose() {
            }
        }, new JavaCoreApplicationEnvironment(new Disposable() {
            @Override
            public void dispose() {
            }
        }));

        return environment.getProject();
    }

    public static synchronized ExecutorService getCachedThreadPool() {
        if (cachedThreadPool == null) {
            cachedThreadPool = Executors.newCachedThreadPool();
        }
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100));
        return cachedThreadPool;
    }
}
