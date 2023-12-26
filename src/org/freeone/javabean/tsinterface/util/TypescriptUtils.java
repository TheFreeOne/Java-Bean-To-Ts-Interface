package org.freeone.javabean.tsinterface.util;

import com.intellij.lang.jvm.JvmClassKind;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue;
import com.intellij.lang.jvm.types.JvmReferenceType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassImpl;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.tree.java.PsiAnnotationImpl;
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl;
import com.intellij.psi.impl.source.tree.java.PsiNameValuePairImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;
import org.freeone.javabean.tsinterface.setting.JavaBeanToTypescriptInterfaceSettingsState;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class TypescriptUtils {

    /**
     * 记录通过canonicalText查找次数的记录
     */
    private static Map<String, Integer> canonicalText2findClassTimeMap = new HashMap<>(8);
    /**
     * 属性对应的类对应在json中的等级，属性的类的等级更大
     */
    private static Map<String, Integer> canonicalText2TreeLevel = new HashMap<>(8);

    /**
     * 属性类对应的interface的内容
     */
    private static Map<String, String> canonicalText2TInnerClassInterfaceContent = new HashMap<>(8);

    /**
     * 冒号
     */
    public static final String REQUIRE_SPLIT_TAG = ": ";
    /**
     * 问好+冒号
     */
    public static final String NOT_REQUIRE_SPLIT_TAG = "?: ";

    /**
     * 移除多余的缓存
     */
    public static void clearCache() {
        canonicalText2TreeLevel.clear();
        canonicalText2TInnerClassInterfaceContent.clear();
        canonicalText2findClassTimeMap.clear();
    }

    /**
     * 提供给action调用的主入口
     *
     * @param project
     * @param psiJavaFile
     * @return
     */
    public static String generatorInterfaceContentForPsiJavaFile(Project project, PsiJavaFile psiJavaFile, boolean isSaveToFile) {
        String interfaceContent;
        // 保存到文件中就是需要default, 不然就不是
        boolean isDefault = isSaveToFile;
        try {
            interfaceContent = generatorInterfaceContentForPsiJavaFile(project, psiJavaFile, isDefault, 1);
            StringBuilder stringBuilder = new StringBuilder(interfaceContent);
            //将Map转换成List
            List<Map.Entry<String, Integer>> list = new ArrayList<>(canonicalText2TreeLevel.entrySet());
            // 借助List的sort方法，需要重写排序规则
            Collections.sort(list, (o1, o2) -> o1.getValue() - o2.getValue());
            // 反转
//        Collections.reverse(list);
            // Collections.sort(list, Comparator.comparingInt(Map.Entry::getValue));  // IDE 提示可以写成更简便的这种形式,我还是习惯自己重新，然后lambda简化
            Map<String, Integer> map2 = new LinkedHashMap<>();  // 这里必须声明成为LinkedHashMap，否则构造新map时会打乱顺序
            for (Map.Entry<String, Integer> o : list) {  // 构造新map
                map2.put(o.getKey(), o.getValue());
            }
            for (Map.Entry<String, Integer> entry : map2.entrySet()) {  // out
                String key = entry.getKey();
                String content = canonicalText2TInnerClassInterfaceContent.getOrDefault(key, "");
                if (content != null) {
                    stringBuilder.insert(0, content + "\n");
                }
            }
            return stringBuilder.toString();
        } finally {
            clearCache();
        }


    }

    /**
     * 针对枚举获取枚举的内容
     *
     * @param project
     * @param psiJavaFile
     * @param isDefault
     * @param treeLevel
     * @return
     */
    public static String generatorTypeContent(Project project, PsiJavaFile psiJavaFile, boolean isDefault, int treeLevel) {
        StringBuilder typeContent = new StringBuilder();
        String defaultText = "";
        if (isDefault) {
            defaultText = "default ";
        }
        PsiClass[] classes = psiJavaFile.getClasses();
        for (PsiClass aClass : classes) {
            String classNameAsTypeName = aClass.getName();
            typeContent.append("export ").append(defaultText).append("type ").append(classNameAsTypeName).append(" = ");
            PsiField[] allFields = aClass.getAllFields();
            List<String> enumConstantValueList = new ArrayList<>();
            enumConstantValueList.add("string");
            for (PsiField psiField : allFields) {
                if (psiField instanceof PsiEnumConstant) {
                    String name = psiField.getName();
                    // 将字段的名字视为字符串
                    String value = "'" + name + "'";
                    enumConstantValueList.add(value);
                }
            }
            String join = String.join(" | ", enumConstantValueList);
            typeContent.append(join);
        }
        return typeContent.toString() + "\n";
    }

    /**
     * 为目标java文件生成typescript的文本内容
     *
     * @param project
     * @param psiJavaFile
     * @param isDefault
     * @param treeLevel
     * @return
     */
    public static String generatorInterfaceContentForPsiJavaFile(Project project, PsiJavaFile psiJavaFile, boolean isDefault, int treeLevel) {
        StringBuilder interfaceContent = new StringBuilder();
        String defaultText = "";
        if (isDefault) {
            defaultText = "default ";
        }
        PsiClass[] classes = psiJavaFile.getClasses();
        // 类文件只有只有一个类，查找一次就可以了
        for (PsiClass aClass : classes) {
            doClassInterfaceContentForTypeScript(project, treeLevel, interfaceContent, defaultText, aClass, "JAVA_FILE");
        }
        return interfaceContent.toString();
    }

    /**
     * 为内部类生成typescript的文本内容，这个针对内部类进行查询
     *
     * @param project
     * @param psiClassInParameters
     * @param isDefault
     * @param treeLevel
     * @return
     */
    public static String generatorInterfaceContentForPsiClass(Project project, PsiClass psiClassInParameters, PsiClass targetPsiClass, boolean isDefault, int treeLevel) {
        StringBuilder interfaceContent = new StringBuilder();
        String defaultText = "";
        if (isDefault) {
            defaultText = "default ";
        }
        // 和上面的方法的区分
        PsiClass[] classes = psiClassInParameters.getInnerClasses();
        // 内部内部类可能重新查询多次
        PsiClass innerClassByName = psiClassInParameters.findInnerClassByName(targetPsiClass.getName(), true);
        doClassInterfaceContentForTypeScript(project, treeLevel, interfaceContent, defaultText, innerClassByName, "CLASS");
//        for (PsiClass aClass : classes) {
//            doClassInterfaceContentForTypeScript(project, treeLevel, interfaceContent, defaultText, aClass,"CLASS");
//        }
        return interfaceContent.toString();
    }

    /**
     * 单个类生成
     *
     * @param project
     * @param treeLevel
     * @param interfaceContent
     * @param defaultText
     * @param aClass
     */
    private static void doClassInterfaceContentForTypeScript(Project project, int treeLevel, StringBuilder interfaceContent, String defaultText, PsiClass aClass, String enterTYpe) {
        String classNameAsInterfaceName = aClass.getName();
        interfaceContent.append("export ").append(defaultText).append("interface ").append(classNameAsInterfaceName).append(" {\n");
        PsiField[] fields = aClass.getAllFields();
        for (int i = 0; i < fields.length; i++) {
            PsiField fieldItem = fields[i];
            // 默认将分隔标记设置成 ？：
            String fieldSplitTag = NOT_REQUIRE_SPLIT_TAG;
            if (CommonUtils.isFieldRequire(fieldItem.getAnnotations())) {
                fieldSplitTag = REQUIRE_SPLIT_TAG;
            }
            // 获取注释
            processDocComment(interfaceContent, fieldItem);
            String fieldName = fieldItem.getName();

            //  2023-12-26 判断是或否使用JsonProperty
            if(JavaBeanToTypescriptInterfaceSettingsState.getInstance().useAnnotationJsonProperty) {
                String jsonPropertyValue = getJsonPropertyValue(fieldItem);
                if (jsonPropertyValue != null) {
                    fieldName = jsonPropertyValue;
                }
            }
            interfaceContent.append("  ").append(fieldName);
            boolean isArray = CommonUtils.isArray(fieldItem);
            boolean isNumber = CommonUtils.isNumber(fieldItem);
            boolean isString = CommonUtils.isString(fieldItem);
            boolean isBoolean = CommonUtils.isBoolean(fieldItem);
            boolean isJavaUtilDate = CommonUtils.isJavaUtilDate(fieldItem);
            boolean isMap = CommonUtils.isMap(fieldItem);
            if (isArray) {
                // 获取泛型
                processArray(project, treeLevel, interfaceContent, fieldItem, fieldSplitTag);

            } else if (isMap) {
                //  : 2023-12-06 针对map做处理
                processMap(project, treeLevel, interfaceContent, fieldItem, fieldSplitTag);

            } else if (isJavaUtilDate && JavaBeanToTypescriptInterfaceSettingsState.getInstance().enableDataToString) {

                interfaceContent.append(fieldSplitTag).append("string");
            } else{
                if (isNumber) {
                    interfaceContent.append(fieldSplitTag).append("number");
                } else if (isString) {
                    interfaceContent.append(fieldSplitTag).append("string");
                } else if (isBoolean) {
                    interfaceContent.append(fieldSplitTag).append("boolean");
                } else {
                    processOtherTypes(project, treeLevel, interfaceContent, fieldItem, fieldSplitTag);
                }
            }
            // end of field
            if (i != fields.length - 1) {
                // 属性之间额外空一行
                interfaceContent.append("\n");
            }
            // 每个属性后面的换行
            interfaceContent.append("\n");
        }
        // end of class
        interfaceContent.append("}\n");
    }

    private static String getJsonPropertyValue(PsiField fieldItem) {
        String result = null;
        PsiAnnotation[] annotations = fieldItem.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            if(annotation instanceof  PsiAnnotationImpl) {
                PsiAnnotationImpl psiAnnotationImpl = (PsiAnnotationImpl) annotation;
                String qualifiedName = psiAnnotationImpl.getQualifiedName();
                if (qualifiedName != null && qualifiedName.equals("com.fasterxml.jackson.annotation.JsonProperty")) {
                    for (JvmAnnotationAttribute attribute : psiAnnotationImpl.getAttributes()) {
                        if ("value".equals(attribute.getAttributeName())  && attribute.getAttributeValue() != null ) {
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
            }

        }
        return result;
    }

    private static void processDocComment(StringBuilder interfaceContent, PsiField fieldItem) {
        PsiDocComment docComment = fieldItem.getDocComment();
        if (docComment != null) {
            String docCommentText = docComment.getText();
            if (docCommentText != null) {
                String[] split = docCommentText.split("\\n");
                for (String docCommentLine : split) {
                    docCommentLine = docCommentLine.trim();
                    interfaceContent.append("  ").append(docCommentLine).append("\n");
                }
            }
        }
    }

    private static void processMap(Project project, int treeLevel, StringBuilder interfaceContent, PsiField fieldItem, String fieldSplitTag) {
        PsiType type = fieldItem.getType();
        String defaultKTYpe = "string";
        String defaultVType = "any";
        if (type instanceof PsiClassReferenceType) {
            PsiClassReferenceType psiClassReferenceType = (PsiClassReferenceType) type;
            PsiType[] parameters = psiClassReferenceType.getParameters();
            if (parameters != null && parameters.length == 2) {
                PsiType kType = parameters[0];
                PsiType vType = parameters[1];

                {
                    boolean isNumber = CommonUtils.isNumberType(kType);
                    boolean isStringType = CommonUtils.isStringType(kType);
                    if (isNumber) {
                        defaultKTYpe = "number";
                    } else if (isStringType) {
                        defaultKTYpe = "string";
                    } else {
                        defaultKTYpe = "any";
                    }
                }

                {
                    boolean isNumber = CommonUtils.isNumberType(vType);
                    boolean isStringType = CommonUtils.isStringType(vType);
                    if (isNumber) {
                        defaultVType = "number";
                    } else if (isStringType) {
                        defaultVType = "string";
                    } else {
                        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
                        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(vType.getCanonicalText(), projectScope);
                        if (psiClass ==null) {
                            defaultVType = "any";
                        } else {
                            defaultVType = vType.getPresentableText();
                            findClassToTsInterface(project, treeLevel +1, vType.getCanonicalText());
                        }


                    }

                }



            }

        }
        interfaceContent.append(fieldSplitTag).append("{[x:"+defaultKTYpe+"]: "+defaultVType+"}");
    }

    private static void processOtherTypes(Project project, int treeLevel, StringBuilder interfaceContent, PsiField fieldItem, String fieldSplitTag) {
        // 不需要设置:any
//                        boolean needSetAny = true;
        String canonicalText = CommonUtils.getJavaBeanTypeForNormalField(fieldItem);
        System.out.println("canonicalText = " + canonicalText);
        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
//                        GlobalSearchScope globalSearchScope = GlobalSearchScope.allScope(project);

        Integer findClassTime = canonicalText2findClassTimeMap.getOrDefault(canonicalText, 0);
        if (findClassTime == 0) {
            PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(canonicalText, projectScope);
            if (psiClass != null) {
                PsiClass psiClassImpl = psiClass;
                JvmClassKind classKind = psiClassImpl.getClassKind();
                //  2022-08-09  ignroe ANNOTATION and  INTERFACE
                if (classKind != JvmClassKind.ANNOTATION && classKind != JvmClassKind.INTERFACE) {
                    canonicalText2findClassTimeMap.put(canonicalText, 1);
                    JvmReferenceType superClassType = psiClass.getSuperClassType();
                    if (superClassType == null) {

                    }
                    if ("Enum".equalsIgnoreCase(superClassType.getName())) {
                        // Enum
                        PsiElement parent = psiClass.getParent();
                        if (parent instanceof PsiJavaFile) {
                            PsiJavaFile enumParentJavaFile = (PsiJavaFile) parent;
                            String findTypeContent = generatorTypeContent(project, enumParentJavaFile, false, treeLevel);
                            canonicalText2TInnerClassInterfaceContent.put(canonicalText, findTypeContent);
                        }


                    } else {
                        // class
                        canonicalText2findClassTimeMap.put(canonicalText, 1);
                        PsiElement parent = psiClass.getParent();
                        // 内部类parent instanceof PsiJavaFile  ==false
                        if (parent instanceof PsiJavaFile) {
                            PsiJavaFile classParentJavaFile = (PsiJavaFile) parent;
                            String findClassContent = generatorInterfaceContentForPsiJavaFile(project, classParentJavaFile, false, treeLevel + 1);
                            canonicalText2TInnerClassInterfaceContent.put(canonicalText, findClassContent);
                        } else if (parent instanceof PsiClass) {
                            PsiClassImpl psiClassParent = (PsiClassImpl) parent;
                            String findClassContent = generatorInterfaceContentForPsiClass(project, psiClassParent, psiClass, false, treeLevel + 1);
                            canonicalText2TInnerClassInterfaceContent.put(canonicalText, findClassContent);
                        }
                    }
                }

            }
        }
        canonicalText2TreeLevel.put(canonicalText, treeLevel);

        if (canonicalText2TInnerClassInterfaceContent.get(canonicalText) != null) {
            String shortName = StringUtil.getShortName(canonicalText);
            interfaceContent.append(fieldSplitTag).append(shortName);
        } else {
            interfaceContent.append(fieldSplitTag).append("any");
        }
    }

    /**
     * 处理集合数组一类的字段
     *
     * @param project
     * @param treeLevel
     * @param interfaceContent
     * @param fieldItem
     * @param fieldSplitTag
     */
    private static void processArray(Project project, int treeLevel, StringBuilder interfaceContent, PsiField fieldItem, String fieldSplitTag) {
        // 泛型
        String generics = getFirstGenericsForArray(project, treeLevel + 1, interfaceContent, fieldItem);
        if (fieldSplitTag != null) {
            interfaceContent.append(fieldSplitTag);
        }
        interfaceContent.append(generics);
        if (!CommonUtils.isTypescriptPrimaryType(generics)) {
            String canonicalText = CommonUtils.getJavaBeanTypeForArrayField(fieldItem);
            findClassToTsInterface(project, treeLevel + 1, canonicalText);
        }
        interfaceContent.append("[]");
    }

    /**
     * 获取数组的泛型
     * @return
     */
    public static String getFirstGenericsForArray(Project project, int treeLevel, StringBuilder interfaceContent, PsiField fieldItem) {
        PsiField field = fieldItem;
        if (CommonUtils.isArray(field)) {
            PsiType type = field.getType();
            // 数组 【】
            if (type instanceof PsiArrayType) {

                PsiArrayType psiArrayType = (PsiArrayType) type;
                PsiType deepComponentType = psiArrayType.getDeepComponentType();
                int arrayDimensions = psiArrayType.getArrayDimensions();
                String firstTsTypeForArray = getFirstTsTypeForArray(project, treeLevel + 1, deepComponentType);

                if (arrayDimensions > 1) {
                    for (int i = 0; i < arrayDimensions - 1; i++) {
                        firstTsTypeForArray += "[]";
                    }
                }
                return firstTsTypeForArray;
            } else if (type instanceof PsiClassReferenceType) {
                // 集合
                PsiClassReferenceType psiClassReferenceType = (PsiClassReferenceType) type;
                String name = psiClassReferenceType.getName();
                String className = psiClassReferenceType.getClassName();
                PsiType[] parameters = psiClassReferenceType.getParameters();
                if (parameters.length == 0) {
                    return "any";
                } else {
                    PsiType deepComponentType = parameters[0].getDeepComponentType();
                    // 判断泛型是不是number
                    String firstTsTypeForArray = getFirstTsTypeForArray(project, treeLevel + 1, deepComponentType);
                    return firstTsTypeForArray;
                }
            }
            return "any";
        } else {
            throw new RuntimeException("target field is not  array type");
        }
    }

    @NotNull
    private static String getFirstTsTypeForArray(Project project, int treeLevel, PsiType deepComponentType) {
        List<PsiType> numberSuperClass = Arrays.stream(deepComponentType.getSuperTypes()).filter(superTypeItem -> superTypeItem.getCanonicalText().equals("java.lang.Number")).collect(Collectors.toList());
        if (!numberSuperClass.isEmpty()) {
            return "number";
        }
        String canonicalText = deepComponentType.getCanonicalText();
        if ("java.lang.Boolean".equals(canonicalText)) {
            return "boolean";
        } else if ("java.lang.String".equals(canonicalText)) {
            return "string";
        } else {
  
            boolean isArrayType = CommonUtils.isArrayType(deepComponentType);
            boolean isMapType = CommonUtils.isMapType(deepComponentType);
            // 里头还是一层 集合
            if (isArrayType) {
                PsiClassReferenceType psiClassReferenceType = (PsiClassReferenceType) deepComponentType;
                PsiType[] parameters = psiClassReferenceType.getParameters();
                if (parameters.length == 0) {
                    return "any[]";
                } else {
                    // 判断泛型是不是number
                    String firstTsTypeForArray = getFirstTsTypeForArray(project,treeLevel + 1, parameters[0].getDeepComponentType());
                    return firstTsTypeForArray + "[]";
                }
            } else if (isMapType) {
                // TODO: 2023-12-06
                return deepComponentType.getPresentableText();
            } else {
                findClassToTsInterface(project ,treeLevel + 1, canonicalText);
                return deepComponentType.getPresentableText();
            }

        }
    }

    /**
     * 寻找 canonicalText的相关嘞
     *
     * @param project
     * @param treeLevel
     * @param canonicalText
     */
    private static void findClassToTsInterface(Project project, int treeLevel, String canonicalText) {
        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
        Integer findClassTimes = canonicalText2findClassTimeMap.getOrDefault(canonicalText, 0);
        if (findClassTimes == 0) {
            PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(canonicalText, projectScope);
            if (psiClass != null) {
                canonicalText2findClassTimeMap.put(canonicalText, 1);
                PsiElement parent = psiClass.getParent();
                if (parent instanceof PsiJavaFile) {
                    PsiJavaFile classParentJavaFile = (PsiJavaFile) parent;
                    String findClassContent = generatorInterfaceContentForPsiJavaFile(project, classParentJavaFile, false, treeLevel + 1);
                    canonicalText2TInnerClassInterfaceContent.put(canonicalText, findClassContent);
                }
            }
        }
        canonicalText2TreeLevel.put(canonicalText, treeLevel);
    }
}
