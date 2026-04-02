package org.freeone.javabean.tsinterface.util;

import com.intellij.lang.jvm.annotation.*;
import com.intellij.psi.PsiAnnotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 请求的相关信息
 */
public class RequestMappingUtils {

    public static RequestInfo parsePsiAnnotation(PsiAnnotation psiAnnotation) {
        String requestPath = "";
        List<String> requestMethods = new ArrayList<>();
        String qualifiedName = Optional.ofNullable(psiAnnotation.getQualifiedName()).orElse("");
        List<JvmAnnotationAttribute> attributes = psiAnnotation.getAttributes();
        if (qualifiedName.equals("org.springframework.web.bind.annotation.RequestMapping")) {
            Optional<JvmAnnotationAttribute> firstRequestMethods = attributes.stream().filter(r -> "method".equals(r.getAttributeName())).findFirst();
            if (firstRequestMethods.isPresent()) {
                JvmAnnotationAttribute jvmAnnotationAttribute = firstRequestMethods.get();
                JvmAnnotationAttributeValue attributeValue = jvmAnnotationAttribute.getAttributeValue();
                if (attributeValue instanceof JvmAnnotationArrayValue) {
                    // 支持多个
                    List<JvmAnnotationAttributeValue> values = ((JvmAnnotationArrayValue) attributeValue).getValues();
                    System.out.println(values);
                    for (JvmAnnotationAttributeValue value : values) {
                        if (value instanceof JvmAnnotationEnumFieldValue) {
                            // 单个
                            JvmAnnotationEnumFieldValue annotationEnumFieldValue = (JvmAnnotationEnumFieldValue) value;
                            requestMethods.add(annotationEnumFieldValue.getFieldName());
                        }
                    }

                } else if (attributeValue instanceof JvmAnnotationEnumFieldValue) {
                    // 单个
                    JvmAnnotationEnumFieldValue annotationEnumFieldValue = (JvmAnnotationEnumFieldValue) attributeValue;
                    requestMethods.add(annotationEnumFieldValue.getFieldName());
                }


            }

        } else {
            switch (qualifiedName) {
                case "org.springframework.web.bind.annotation.GetMapping":
                    requestMethods.add("GET");
                case "org.springframework.web.bind.annotation.PostMapping":
                    requestMethods.add("POST");
                case "org.springframework.web.bind.annotation.PutMapping":
                    requestMethods.add("PUT");
                case "org.springframework.web.bind.annotation.PatchMapping":
                    requestMethods.add("PATCH");
                case "org.springframework.web.bind.annotation.DeleteMapping":
                    requestMethods.add("DELETE");
            }
        }
        Optional<JvmAnnotationAttribute> firstAttr = attributes.stream().filter(r -> "value".equals(r.getAttributeName())).findFirst();
        if (firstAttr.isPresent()) {
            JvmAnnotationAttribute attribute = firstAttr.get();
            JvmAnnotationAttributeValue attributeValue = attribute.getAttributeValue();
            if (attributeValue instanceof JvmAnnotationConstantValue) {
                JvmAnnotationConstantValue jvmAnnotationConstantValue = (JvmAnnotationConstantValue) attributeValue;
                if (jvmAnnotationConstantValue.getConstantValue() != null) {
                    requestPath = jvmAnnotationConstantValue.getConstantValue().toString();
                }
            }
        }
        return new RequestInfo(requestMethods, requestPath);
    }


    public static class RequestInfo {
        private List<String> requestMethods = new ArrayList<>();
        private String requestPath = "";

        public RequestInfo() {
        }

        public RequestInfo(List<String> requestMethods, String requestPath) {
            this.requestMethods = requestMethods;
            this.requestPath = requestPath;
        }

        public List<String> getRequestMethods() {
            return requestMethods;
        }

        public void setRequestMethods(List<String> requestMethods) {
            this.requestMethods = requestMethods;
        }

        public String getRequestPath() {
            return requestPath;
        }

        public void setRequestPath(String requestPath) {
            this.requestPath = requestPath;
        }

    }
}
