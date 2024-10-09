package org.freeone.javabean.tsinterface.util;

import com.intellij.core.JavaCoreApplicationEnvironment;
import com.intellij.core.JavaCoreProjectEnvironment;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.mock.MockProject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassImpl;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.tree.java.PsiAnnotationImpl;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class CommonUtils {

    public static final List<String> numberTypes = Arrays.asList("byte", "short", "int", "long", "double", "float");

    public static final List<String> requireAnnotationShortNameList = Arrays.asList("NotNull", "NotEmpty", "NotBlank");


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

    public static boolean isNumberType(PsiType psiType) {
        return Arrays.stream(psiType.getSuperTypes()).anyMatch(ele -> ele.getCanonicalText().contains("java.lang.Number"));
    }

    public static boolean isStringType(PsiType psiType) {
        return Arrays.stream(psiType.getSuperTypes()).anyMatch(ele -> ele.getCanonicalText().contains("java.lang.CharSequence"));
    }


    /**
     * 判断是否是 基类
     *
     * @param type
     * @return
     */
    public static boolean isTypescriptPrimaryType(String type) {
        if ("number".equals(type) || "string".equals(type) || "boolean".equals(type)) {
            return true;
        }
        return false;
    }

    public static String getJavaBeanTypeForArrayField(PsiField field) {
        if (isArray(field)) {
            PsiType type = field.getType();

            if (type instanceof PsiArrayType) {
                // 数组 【】
                PsiArrayType psiArrayType = (PsiArrayType) type;
                PsiType deepComponentType = psiArrayType.getDeepComponentType();
                String canonicalText = deepComponentType.getCanonicalText();
                return canonicalText;
            } else if (type instanceof PsiClassReferenceType) {
                // 集合
                PsiClassReferenceType psiClassReferenceType = (PsiClassReferenceType) type;
                String name = psiClassReferenceType.getName();
                String className = psiClassReferenceType.getClassName();
                PsiType[] parameters = psiClassReferenceType.getParameters();
                PsiType deepComponentType = parameters[0].getDeepComponentType();
                String canonicalText = deepComponentType.getCanonicalText();
                return canonicalText;
            } else {

                return "any";
            }
        } else {
            throw new RuntimeException("target field is not  array type");
        }
    }

    public static String getJavaBeanTypeForNormalField(PsiField field) {
        PsiType type = field.getType();
        if (type instanceof PsiArrayType) {
            // 数组 【】
            PsiArrayType psiArrayType = (PsiArrayType) type;
            PsiType deepComponentType = psiArrayType.getDeepComponentType();
            return deepComponentType.getCanonicalText();
        } else if (type instanceof PsiClassReferenceType) {
            // 集合
            PsiClassReferenceType psiClassReferenceType = (PsiClassReferenceType) type;
            PsiType deepComponentType = psiClassReferenceType.getDeepComponentType();
            return deepComponentType.getCanonicalText();
        } else {
            return "any";
        }
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

    /**
     * psiType是否是数组
     *
     * @param psiType
     * @return
     */
    public static boolean isArrayType(PsiType psiType) {
        return psiType.getCanonicalText().contains("java.util.List") || Arrays.stream(psiType.getSuperTypes()).anyMatch(superType -> superType.getCanonicalText().contains("java.util.Collection<"));
    }


    public static boolean isMap(PsiField field) {
        String canonicalText = field.getType().getCanonicalText();
        if (canonicalText.contains("java.util.Map<")) {
            return true;
        } else {
            PsiType[] superTypes = field.getType().getSuperTypes();
            List<PsiType> collect = Arrays.stream(superTypes).filter(superType -> superType.getCanonicalText().contains("java.util.Map<")).collect(Collectors.toList());
            if (!collect.isEmpty()) {
                return true;
            }
            return false;
        }
    }

    public static boolean isMapType(PsiType psiType) {
        return psiType.getCanonicalText().contains("java.util.Map") || Arrays.stream(psiType.getSuperTypes()).filter(superType -> superType.getCanonicalText().contains("java.util.Map")).count() > 0;
    }


    public static boolean isString(PsiField field) {
        String presentableText = field.getType().getPresentableText();
        if (presentableText.equals("String")) {
            return true;
        }
        return false;
    }

    public static boolean isJavaUtilDate(PsiField field) {
        String canonicalText = field.getType().getCanonicalText();
        if (canonicalText.equals("java.util.Date")) {
            return true;
        }
        return false;
    }


    public static boolean isJavaUtilDateType(PsiType psiType) {
        return psiType.getCanonicalText().equals("java.util.Date");
    }


    public static boolean isBoolean(PsiField field) {
        String canonicalText = field.getType().getCanonicalText();
        if ("java.lang.Boolean".equals(canonicalText) || "boolean".equals(canonicalText)) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * 判断字段是否是必须的
     *
     * @param annotations
     * @return
     */
    public static boolean isFieldRequire(PsiAnnotation[] annotations) {
        if (annotations != null) {
            for (PsiAnnotation annotation : annotations) {
                if (annotation instanceof PsiAnnotationImpl) {
                    PsiAnnotationImpl annotationImpl = (PsiAnnotationImpl) annotation;
                    String qualifiedName = annotationImpl.getQualifiedName();
                    if (qualifiedName != null) {
                        String shortName = StringUtil.getShortName(qualifiedName);
                        for (String requireAnnotationShortName : requireAnnotationShortNameList) {
                            if (requireAnnotationShortName.equalsIgnoreCase(shortName)) {
                                return true;
                            }
                        }
                    }

                }

            }
        }
        return false;
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

    /**
     * 获取一个文件选择描述器
     *
     * @param title       标题
     * @param description 描述
     * @return FileChooserDescriptor
     */
    public static FileChooserDescriptor createFileChooserDescriptor(String title, String description) {
        FileChooserDescriptor singleFolderDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        if (title != null) {
            singleFolderDescriptor.setTitle(title);
        }
        if (description != null) {
            singleFolderDescriptor.setDescription(description);
        }
        return singleFolderDescriptor;
    }

    public static boolean isInnerPublicClass(PsiJavaFile psiJavaFile, PsiClass psiClass) {
        PsiClass[] classes = psiJavaFile.getClasses();
        String targetQualifiedName = psiClass.getQualifiedName();
        // 暂时仅支持只有公共类的方式，其他的骚操作后见再说
        if (classes.length == 1) {
            PsiClass mainClass = classes[0];
            String mainClassQualifiedName = mainClass.getQualifiedName();
            // 内部的public static class和外面的public class肯定不同
            if (targetQualifiedName != null && mainClassQualifiedName != null && !targetQualifiedName.equals(mainClassQualifiedName)) {
                PsiClass[] innerClasses = mainClass.getInnerClasses();
                for (PsiClass innerClass : innerClasses) {
                    String qualifiedNameOfInnerClass = innerClass.getQualifiedName();
                    if (targetQualifiedName.equals(qualifiedNameOfInnerClass)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
