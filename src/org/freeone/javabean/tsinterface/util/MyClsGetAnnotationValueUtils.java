package org.freeone.javabean.tsinterface.util;

import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue;
import com.intellij.psi.impl.compiled.ClsAnnotationImpl;

/**
 * 获取。class文件中的JsonProperty的注解的值
 */
public class MyClsGetAnnotationValueUtils {

    public static String getValue(ClsAnnotationImpl clsAnnotation) {

        try {
            String qualifiedName = clsAnnotation.getQualifiedName();
            if (qualifiedName != null && qualifiedName.equals("com.fasterxml.jackson.annotation.JsonProperty")) {
                for (JvmAnnotationAttribute attribute : clsAnnotation.getAttributes()) {
                    if ("value".equals(attribute.getAttributeName()) && attribute.getAttributeValue() != null) {
                        JvmAnnotationAttributeValue attributeValue = attribute.getAttributeValue();
                        if (attributeValue instanceof JvmAnnotationConstantValue) {
                            Object constantValue = ((JvmAnnotationConstantValue) attributeValue).getConstantValue();
                            if (constantValue != null) {
                                String literalValue = constantValue.toString();
                                if (literalValue != null && literalValue.trim().length() > 0) {
                                    return literalValue;
                                }
                            }
                        }

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }
}
