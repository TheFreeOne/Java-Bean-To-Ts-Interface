package org.freeone.javabean.tsinterface.util;

import com.intellij.core.JavaCoreApplicationEnvironment;
import com.intellij.core.JavaCoreProjectEnvironment;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.mock.MockProject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsAnnotationImpl;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.tree.java.PsiAnnotationImpl;
import com.intellij.psi.impl.source.tree.java.PsiNameValuePairImpl;
import com.intellij.psi.search.GlobalSearchScope;
import org.freeone.javabean.tsinterface.setting.JavaBeanToTypescriptInterfaceSettingsState;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class CommonUtils {

    public static final List<String> numberTypes = Arrays.asList("byte", "short", "int", "long", "double", "float");

    public static final List<String> requireAnnotationShortNameList = Arrays.asList("NotNull", "NotEmpty", "NotBlank");


    public static boolean isNumberType(PsiType psiType) {
        return numberTypes.contains(psiType.getCanonicalText()) || "java.lang.Number".equalsIgnoreCase(psiType.getCanonicalText()) || Arrays.stream(psiType.getSuperTypes()).anyMatch(ele -> "java.lang.Number".equalsIgnoreCase(ele.getCanonicalText()));
    }

    public static boolean isStringType(PsiType psiType) {
        return "char".equalsIgnoreCase(psiType.getCanonicalText()) ||  Arrays.stream(psiType.getSuperTypes()).anyMatch(ele -> "java.lang.CharSequence".equalsIgnoreCase(ele.getCanonicalText()));
    }

    public static PsiClass findPsiClass(Project project, PsiType vType) {
        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(vType.getCanonicalText(), projectScope);
        if (psiClass == null && JavaBeanToTypescriptInterfaceSettingsState.getInstance().allowFindClassInAllScope) {
            GlobalSearchScope allScope = GlobalSearchScope.allScope(project);
            psiClass = JavaPsiFacade.getInstance(project).findClass(vType.getCanonicalText(), allScope);
        }
        return psiClass;
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
        if (isArrayType(field.getType())) {
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

    /**
     * 获取商品的类型
     *
     * @param field
     * @return
     */
    public static String getJavaBeanTypeForNormalField(PsiField field) {
        PsiType type = field.getType();
        if (type instanceof PsiArrayType) {
            // 数组 【】
            PsiArrayType psiArrayType = (PsiArrayType) type;
            PsiType deepComponentType = psiArrayType.getDeepComponentType();
            return deepComponentType.getCanonicalText();
        } else if (type instanceof PsiClassReferenceType) {
            // 集合或者自定义的泛型
            PsiClassReferenceType psiClassReferenceType = (PsiClassReferenceType) type;
            PsiType deepComponentType = psiClassReferenceType.getDeepComponentType();
            String canonicalText = deepComponentType.getCanonicalText();
            if (canonicalText.contains("<")) {
                PsiElement psiContext = psiClassReferenceType.getPsiContext();
                if (psiContext != null && psiContext.getChildren().length > 0) {
                    PsiElement firstChild = psiContext.getFirstChild();
                    return canonicalText;
                }
            }
            return canonicalText;
        } else {
            return "any";
        }
    }

    /**
     * [] list , set
     *
     * @return
     */
    public static boolean isArrayType(PsiType type) {

        boolean contains = type.getCanonicalText().endsWith("[]");
        if (contains) {
            return true;
        }
        PsiType[] superTypes = type.getSuperTypes();
        List<PsiType> collect = Arrays.stream(superTypes).filter(superType -> superType.getCanonicalText().contains("java.util.Collection<")).collect(Collectors.toList());
        if (!collect.isEmpty()) {
            return true;
        }
        return false;
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


    public static boolean isJavaUtilDateType(PsiType psiType) {
        return psiType.getCanonicalText().equals("java.util.Date");
    }


    public static boolean isBooleanType(PsiType psiType) {
        String canonicalText = psiType.getCanonicalText();
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

    public static String getJsonPropertyValue(PsiField fieldItem, PsiMethod[] allMethods) {
        String result = null;
        String name = fieldItem.getName();
        String getterMethodName = "get" + name;
        String setterMethodName = "set" + name;
        PsiAnnotation[] annotations = fieldItem.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            if (result != null) {
                break;
            }
            if (annotation instanceof PsiAnnotationImpl) {
                PsiAnnotationImpl psiAnnotationImpl = (PsiAnnotationImpl) annotation;
                String qualifiedName = psiAnnotationImpl.getQualifiedName();
                if (qualifiedName != null && qualifiedName.equals("com.fasterxml.jackson.annotation.JsonProperty")) {
                    for (JvmAnnotationAttribute attribute : psiAnnotationImpl.getAttributes()) {
                        if ("value".equals(attribute.getAttributeName()) && attribute.getAttributeValue() != null) {
                            if (attribute instanceof PsiNameValuePairImpl) {
                                PsiNameValuePairImpl psiNameValuePair = (PsiNameValuePairImpl) attribute;
                                String literalValue = psiNameValuePair.getLiteralValue();
                                if (literalValue != null && literalValue.trim().length() > 0) {
                                    result = literalValue;
                                }
                            }

                        }
                    }
                }
            } else if (annotation instanceof ClsAnnotationImpl) {
                ClsAnnotationImpl psiAnnotationImpl = (ClsAnnotationImpl) annotation;
                result = MyClsGetAnnotationValueUtils.getValue(psiAnnotationImpl);
            }
        }
        // 从方法中获取
        if (result == null) {
            for (PsiMethod method : allMethods) {
                if (method.getName().equalsIgnoreCase(getterMethodName) || method.getName().equalsIgnoreCase(setterMethodName)) {
                    PsiAnnotation[] methodAnnotations = method.getAnnotations();
                    for (PsiAnnotation annotation : methodAnnotations) {
                        if (result != null) {
                            break;
                        }
                        // annotation start
                        if (annotation instanceof PsiAnnotationImpl) {
                            PsiAnnotationImpl psiAnnotationImpl = (PsiAnnotationImpl) annotation;
                            String qualifiedName = psiAnnotationImpl.getQualifiedName();
                            if (qualifiedName != null && qualifiedName.equals("com.fasterxml.jackson.annotation.JsonProperty")) {
                                for (JvmAnnotationAttribute attribute : psiAnnotationImpl.getAttributes()) {
                                    if ("value".equals(attribute.getAttributeName()) && attribute.getAttributeValue() != null) {
                                        if (attribute instanceof PsiNameValuePairImpl) {
                                            PsiNameValuePairImpl psiNameValuePair = (PsiNameValuePairImpl) attribute;
                                            String literalValue = psiNameValuePair.getLiteralValue();
                                            if (literalValue != null && literalValue.trim().length() > 0) {
                                                result = literalValue;
                                                break;
                                            }
                                        }

                                    }
                                }
                            }
                        } else if (annotation instanceof ClsAnnotationImpl) {
                            ClsAnnotationImpl psiAnnotationImpl = (ClsAnnotationImpl) annotation;
                            result = MyClsGetAnnotationValueUtils.getValue(psiAnnotationImpl);
                        }
                        // annotation end
                    }
                }
            }
        }


        return result;
    }

    /**
     * 获取类的名称，如果有泛型，就带泛型
     *
     * @param psiClass
     * @return
     */
    public static String getClassNameWithGenerics(PsiClass psiClass) {
        if (psiClass == null) {
            return null;
        }
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        // 获取 PsiClass 的类型
        PsiClassType classType = factory.createType(psiClass);
        return getClassNameWithGenerics(psiClass, classType);
    }

    private static String getClassNameWithGenerics(PsiClass psiClass, PsiClassType classType) {
        // 获取类名（不包含泛型）
        String classNameWithoutPackage = classType.resolve().getName();

        // 获取泛型参数信息
        PsiType[] typeArgumentsInClassType = classType.getParameters(); // length maybe 0
        PsiTypeParameter[] typeParametersInClass = psiClass.getTypeParameters();

        // 如果有泛型参数，构造出类似 List<T> 形式
        if (typeParametersInClass.length > 0) {
            StringBuilder builder = new StringBuilder();
            builder.append(classNameWithoutPackage.substring(classNameWithoutPackage.lastIndexOf('.') + 1))  // 取类名部分
                    .append("<");
            for (int i = 0; i < typeParametersInClass.length; i++) {
                // 泛型占位符 T 或具体类型
                builder.append(typeParametersInClass[i].getName());
                if (i < typeParametersInClass.length - 1) {
                    builder.append(", ");
                }
            }
            builder.append(">");
            return builder.toString();
        } else {
            return classNameWithoutPackage; // 如果没有泛型参数，直接返回类名
        }
    }

    public static String getClassNameWithGenericsAndArguments(PsiClass psiClass, PsiType... genericTypes) {
        if (psiClass == null) {
            return null;
        }

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());

        // 如果有泛型参数，创建带有泛型参数的类型
        PsiClassType classType = factory.createType(psiClass, genericTypes);

        // 获取带有泛型的类名
        return classType.getCanonicalText();
    }
}
