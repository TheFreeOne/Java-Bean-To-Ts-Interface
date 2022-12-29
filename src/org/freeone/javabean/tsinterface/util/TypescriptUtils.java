package org.freeone.javabean.tsinterface.util;

import com.intellij.lang.jvm.JvmClassKind;
import com.intellij.lang.jvm.types.JvmReferenceType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.*;

public class TypescriptUtils {
    private static Map<String, Integer> canonicalText2findClassTimeMap = new HashMap<>(8);
    /**
     * 属性对应的类对应在json中的等级，属性的类的等级更大
     */
    private static Map<String, Integer> canonicalText2TreeLevel = new HashMap<>(8);

    /**
     * 属性类对应的interface的内容
     */
    private static Map<String, String> canonicalText2TInnerClassInterfaceContent = new HashMap<>(8);


    public static final String requireSplitTag = ": ";
    public static final String notRequireSplitTag = "?: ";

    public static String generatorInterfaceContent(Project project, PsiJavaFile psiJavaFile) {

        String interfaceContent = generatorInterfaceContent(project, psiJavaFile, true, 1);
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
            String content = canonicalText2TInnerClassInterfaceContent.get(key);
            if (content != null) {
                stringBuilder.insert(0, content + "\n");
            }
        }
        return stringBuilder.toString();
    }

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
                    String value = "'" + name + "'";
                    enumConstantValueList.add(value);
                }
            }
            String join = String.join(" | ", enumConstantValueList);
            typeContent.append(join);
        }
        return typeContent.toString() + "\n";
    }

    public static String generatorInterfaceContent(Project project, PsiJavaFile psiJavaFile, boolean isDefault, int treeLevel) {
        StringBuilder interfaceContent = new StringBuilder();
        String defaultText = "";
        if (isDefault) {
            defaultText = "default ";
        }
        PsiClass[] classes = psiJavaFile.getClasses();

        for (PsiClass aClass : classes) {
            String classNameAsInterfaceName = aClass.getName();
            interfaceContent.append("export ").append(defaultText).append("interface ").append(classNameAsInterfaceName).append("{\n");
            PsiField[] allFields = aClass.getAllFields();
            for (int i = 0; i < allFields.length; i++) {
                PsiField fieldItem = allFields[i];
                String fieldSplitTag = notRequireSplitTag;
                PsiAnnotation[] annotations = fieldItem.getAnnotations();
                boolean fieldRequire = CommonUtils.isFieldRequire(annotations);
                if (fieldRequire) {
                    fieldSplitTag = requireSplitTag;
                }
                // 获取注释
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

                String name = fieldItem.getName();
                interfaceContent.append("  ").append(name);
                boolean isArray = CommonUtils.isArray(fieldItem);
                boolean isNumber = CommonUtils.isNumber(fieldItem);
                boolean isString = CommonUtils.isString(fieldItem);
                boolean isBoolean = CommonUtils.isBoolean(fieldItem);
                if (isArray) {
                    // get generics
                    String generics = CommonUtils.getGenericsForArray(fieldItem);
                    interfaceContent.append(fieldSplitTag).append(generics);

                    if (!CommonUtils.isTypescriptPrimaryType(generics)) {

                        String canonicalText = CommonUtils.getJavaBeanTypeForArrayField(fieldItem);
//                        GlobalSearchScope globalSearchScope = GlobalSearchScope.allScope(project);
                        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
                        Integer findClassTime = canonicalText2findClassTimeMap.getOrDefault(canonicalText, 0);
                        if (findClassTime == 0) {
                            PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(canonicalText, projectScope);
                            if (psiClass != null) {
                                canonicalText2findClassTimeMap.put(canonicalText, 1);
                                PsiElement parent = psiClass.getParent();
                                if (parent instanceof PsiJavaFile) {
                                    PsiJavaFile classParentJavaFile = (PsiJavaFile) parent;
                                    String findClassContent = generatorInterfaceContent(project, classParentJavaFile, false, treeLevel + 1);
                                    canonicalText2TInnerClassInterfaceContent.put(canonicalText, findClassContent);
                                }
                            }
                        }
                        canonicalText2TreeLevel.put(canonicalText, treeLevel);

                    }
                } else {
                    if (isNumber) {
                        interfaceContent.append(fieldSplitTag).append("number");
                    } else if (isString) {
                        interfaceContent.append(fieldSplitTag).append("string");
                    } else if (isBoolean) {
                        interfaceContent.append(fieldSplitTag).append("boolean");
                    } else {
                        // 不需要设置:any
                        boolean needSetAny = true;
                        String canonicalText = CommonUtils.getJavaBeanTypeForNormalField(fieldItem);
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
                                        if (parent instanceof PsiJavaFile) {
                                            PsiJavaFile classParentJavaFile = (PsiJavaFile) parent;
                                            String findClassContent = generatorInterfaceContent(project, classParentJavaFile, false, treeLevel + 1);
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
                }

                // end of field
                if (isArray) {
                    interfaceContent.append("[]");
                }
                if (i != allFields.length - 1) {
                    // 属性之间额外空一行
                    interfaceContent.append("\n");
                }
                // 每个属性后面的换行
                interfaceContent.append("\n");
            }
            // end of class
            interfaceContent.append("}\n");
        }
        return interfaceContent.toString();
    }
}
