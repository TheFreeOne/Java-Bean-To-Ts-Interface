package org.freeone.javabean.tsinterface.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.javadoc.PsiDocComment;
import org.freeone.javabean.tsinterface.setting.JavaBeanToTypescriptInterfaceSettingsState;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用于生成文件
 */
public class TypescriptContentGenerator {

    /**
     * 冒号
     */
    public static final String REQUIRE_SPLIT_TAG = ": ";
    /**
     * 问好+冒号
     */
    public static final String NOT_REQUIRE_SPLIT_TAG = "?: ";


    private static final List<String> SUCCESS_CANONICAL_TEXT = new ArrayList<>();
    /**
     * 属性类对应的interface的内容
     */
    private static final Map<String, String> classNameWithPackage_2_T_CONTENT = new HashMap<>(8);


    public static void processPsiClass(Project project, PsiClass selectedClass, boolean needDefault) {

//        PsiClass[] innerClasses = selectedClass.getInnerClasses();
        createTypescriptContentForSinglePsiClass(project, selectedClass);
//        for (PsiClass innerClass : innerClasses) {
//            createTypescriptContentForSinglePsiClass(innerClass);
//        }
    }

    /**
     * 合并成一个文件
     */
    public static String mergeContent(PsiClass selectedClass, boolean needDefault) {

        List<String> contentList = new ArrayList<>();
        String qualifiedName = selectedClass.getQualifiedName();
        for (String classNameWithPackage : SUCCESS_CANONICAL_TEXT) {
            StringBuilder stringBuilder = new StringBuilder();
            String content = classNameWithPackage_2_T_CONTENT.get(classNameWithPackage);
            if (content != null) {
                // 以后做成一个配置项
//                 stringBuilder.append(" /**\n" + "  * @packageName ").append(classNameWithPackage).append(" \n").append("  */\n");
                stringBuilder.append("export ");
                if (needDefault && classNameWithPackage.equalsIgnoreCase(qualifiedName)) {
                    stringBuilder.append("default ");
                }
                stringBuilder.append(content);
                contentList.add(stringBuilder.toString());
            }
        }

        return String.join("\n", contentList);
    }

    public static void clearCache() {
        SUCCESS_CANONICAL_TEXT.clear();
        classNameWithPackage_2_T_CONTENT.clear();
    }

    /**
     * 为单独的class创建内容
     *
     * @param psiClass
     * @return
     */
    public static String createTypescriptContentForSinglePsiClass(Project project, PsiClass psiClass) {
        if (psiClass != null) {
            StringBuilder contentBuilder = new StringBuilder();
            String psiClassName = psiClass.getName();

            String qualifiedName = psiClass.getQualifiedName();
            if (SUCCESS_CANONICAL_TEXT.contains(qualifiedName)) {
                return psiClassName;
            }
            System.out.println(psiClassName + " qualifiedName " + qualifiedName);

            PsiField[] fields = psiClass.getAllFields();
            if (JavaBeanToTypescriptInterfaceSettingsState.getInstance().ignoreParentField) {
                fields = psiClass.getFields();
            }
            PsiMethod[] allMethods = psiClass.getAllMethods();
            contentBuilder.append("interface ").append(psiClassName).append(" {\n");
            for (int i = 0; i < fields.length; i++) {
                PsiField fieldItem = fields[i];
                String documentText = "";
                // 获取注释
                PsiDocComment docComment = fieldItem.getDocComment();
                if (docComment != null && docComment.getText() != null) {
                    documentText = docComment.getText();
                }
                String fieldName = fieldItem.getName();
                //  2023-12-26 判断是或否使用JsonProperty
                if (JavaBeanToTypescriptInterfaceSettingsState.getInstance().useAnnotationJsonProperty) {
                    String jsonPropertyValue = CommonUtils.getJsonPropertyValue(fieldItem, allMethods);
                    if (jsonPropertyValue != null) {
                        fieldName = jsonPropertyValue;
                    }
                }


                // 默认将分隔标记设置成 ？：
                String fieldSplitTag = NOT_REQUIRE_SPLIT_TAG;
                if (CommonUtils.isFieldRequire(fieldItem.getAnnotations())) {
                    fieldSplitTag = REQUIRE_SPLIT_TAG;
                }

                String typeString = "any";
                PsiType fieldType = fieldItem.getType();
                typeString = getTypeString(project, fieldType);
                if (documentText.trim().length() > 0) {
                    contentBuilder.append("  ").append(documentText).append("\n");
                }
                contentBuilder.append("  ").append(fieldName).append(fieldSplitTag).append(typeString).append("\n");
                contentBuilder.append("\n");
            }
            contentBuilder.append("}\n");
            String content = contentBuilder.toString();
            SUCCESS_CANONICAL_TEXT.add(qualifiedName);
            classNameWithPackage_2_T_CONTENT.put(qualifiedName, content);
            return psiClassName;
        } else {
            return "any";
        }
    }

    /**
     * 从fieldType中获取类型
     *
     * @param project
     * @param fieldType
     * @return
     */
    private static String getTypeString(Project project, PsiType fieldType) {
        String typeString;
        if (CommonUtils.isNumberType(fieldType)) {
            typeString = "number";
        } else if (CommonUtils.isStringType(fieldType)) {
            typeString = "string";
        } else if (CommonUtils.isBooleanType(fieldType)) {
            typeString = "boolean";
        } else if (CommonUtils.isJavaUtilDateType(fieldType) && JavaBeanToTypescriptInterfaceSettingsState.getInstance().enableDataToString) {
            typeString = "string";
        } else if (CommonUtils.isMapType(fieldType)) {
            typeString = processMap(project, fieldType);
        } else if (CommonUtils.isArrayType(fieldType)) {
            typeString = processList(project, fieldType);
        } else {
            PsiClass filedClass = CommonUtils.findPsiClass(project, fieldType);
            typeString = createTypescriptContentForSinglePsiClass(project, filedClass);
        }
        return typeString;
    }


    private static String processList(Project project, PsiType psiType) {
        return getFirstTsTypeForArray(project, 0, psiType);
    }

    /**
     * 处理map
     */
    private static String processMap(Project project, PsiType type) {
        // 默认的value的类型是any
        String defaultVType = "any";
        if (type instanceof PsiClassReferenceType) {
            PsiClassReferenceType psiClassReferenceType = (PsiClassReferenceType) type;
            PsiType[] parameters = psiClassReferenceType.getParameters();
            if (parameters.length == 2) {

                PsiType vType = parameters[1];
                boolean isNumber = CommonUtils.isNumberType(vType);
                boolean isStringType = CommonUtils.isStringType(vType);
                boolean isArrayType = CommonUtils.isArrayType(vType);

                if (isNumber) {
                    defaultVType = "number";
                } else if (isStringType) {
                    defaultVType = "string";
                } else if (isArrayType) {

                    defaultVType = getTypeString(project, vType);
                    System.out.println("vtype = " + defaultVType);
//                    if (vType instanceof PsiArrayType) {
//                        PsiType getDeepComponentType = vType.getDeepComponentType();
//                        defaultVType = processList(project, getDeepComponentType);
//                    } else if (vType instanceof PsiClassReferenceType) {
//                        PsiType getDeepComponentType = type.getDeepComponentType();
//                        defaultVType = getTypeString(project, getDeepComponentType);
//                    }

                } else {
                    PsiClass psiClass = CommonUtils.findPsiClass(project, vType);
                    if (psiClass == null) {
                        defaultVType = "any";
                    } else {
                        defaultVType = createTypescriptContentForSinglePsiClass(project, psiClass);
//                        defaultVType = vType.getPresentableText();
                    }
                }

            }

        }

        return "{[x:string]: " + defaultVType + "}";
    }


    private static String getFirstTsTypeForArray(Project project, int treeLevel, PsiType psiType) {
        if (treeLevel > 100) {
            return "any";
        }
        List<PsiType> numberSuperClass = Arrays.stream(psiType.getSuperTypes()).filter(superTypeItem -> superTypeItem.getCanonicalText().equals("java.lang.Number")).collect(Collectors.toList());
        if (!numberSuperClass.isEmpty()) {
            return "number";
        }
        String canonicalText = psiType.getCanonicalText();
        if ("java.lang.Boolean".equals(canonicalText)) {
            return "boolean";
        } else if ("java.lang.String".equals(canonicalText)) {
            return "string";
        } else {

            boolean isArrayType = CommonUtils.isArrayType(psiType);
            boolean isMapType = CommonUtils.isMapType(psiType);
            // 里头还是一层 集合
            if (isArrayType) {
                if (psiType instanceof PsiClassReferenceType || psiType instanceof PsiArrayType) {

                    PsiType deepComponentType = null;
                    if (psiType instanceof PsiClassReferenceType) {
                        PsiClassReferenceType psiClassReferenceType = (PsiClassReferenceType) psiType;
                        PsiType[] parameters = psiClassReferenceType.getParameters();
                        if (parameters.length == 0) {
                            return "any[]";
                        }
                        deepComponentType = parameters[0].getDeepComponentType();
                    } else {
                        deepComponentType = psiType.getDeepComponentType();
                    }
                    PsiClass psiClass = CommonUtils.findPsiClass(project, deepComponentType);
                    createTypescriptContentForSinglePsiClass(project, psiClass);
                    String firstTsTypeForArray = getFirstTsTypeForArray(project, treeLevel + 1, deepComponentType);
                    return firstTsTypeForArray + "[]";
                } else {
                    return "any[]";
                }

            } else if (isMapType) {
                return psiType.getPresentableText();
            } else {
                return psiType.getPresentableText();
            }

        }
    }
}
