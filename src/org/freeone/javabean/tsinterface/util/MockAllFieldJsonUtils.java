package org.freeone.javabean.tsinterface.util;

import com.google.gson.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MockAllFieldJsonUtils {
    private static final Set<String> COLLECTION_INTERFACES = new HashSet<>(
            Arrays.asList("java.util.List", "java.util.Set", "java.util.Collection")
    );

    public static String generateJsonFromClass(@NotNull Project project, @NotNull PsiClass selectedClass) {
        List<String> visited = new ArrayList<>();
        JsonObject jsonObject = buildJsonObject(selectedClass, visited);
        return new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(jsonObject);
    }

    private static JsonObject buildJsonObject(@NotNull PsiClass psiClass, @NotNull List<String> visited) {
        String fqn = psiClass.getQualifiedName();
        if (fqn == null || fqn.isEmpty()) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Class has no qualified name");
            return error;
        }

        // 防止死循环：如果已经访问过该类，返回引用占位符
        if (visited.stream().filter(r -> r.equals(fqn)).count() > 100) {
            return new JsonObject();
        }

        visited.add(fqn);
        JsonObject obj = new JsonObject();

        // 遍历所有字段（包括继承的字段）
        for (PsiField field : psiClass.getAllFields()) {
            String fieldName = field.getName();
            // 跳过枚举常量
            PsiType[] superTypes = field.getType().getSuperTypes();
            boolean isEnum = false;
            for (PsiType superType : superTypes) {
                String presentableText = superType.getPresentableText();
                // 枚举视作字符串
                if (presentableText.startsWith("Enum<")) {

                    isEnum = true;
                    break;
                }
            }

            if (isEnum) {
                obj.add(fieldName, new JsonPrimitive(""));
            } else {
                PsiType fieldType = field.getType();
                JsonElement fieldJson = resolveTypeToJson(fieldType, visited);
                obj.add(fieldName, fieldJson);
            }


        }

        // 可选：从 visited 中移除（允许其他路径再次进入，但通常不需要）
        // visited.remove(fqn);

        return obj;
    }

    private static JsonElement resolveTypeToJson(@Nullable PsiType psiType, @NotNull List<String> visited) {
        if (psiType == null) {
            return new JsonPrimitive("null");
        }

        // 处理数组类型
        if (psiType instanceof PsiArrayType) {
            PsiArrayType arrayType = (PsiArrayType) psiType;
            JsonArray arr = new JsonArray();
            arr.add(resolveTypeToJson(arrayType.getComponentType(), visited));
            return arr;
        }

        // 处理可变参数（转为数组）
        if (psiType instanceof PsiEllipsisType) {
            PsiEllipsisType ellipsis = (PsiEllipsisType) psiType;
            return resolveTypeToJson(ellipsis.toArrayType(), visited);
        }

        // 处理类类型（包括泛型）
        if (psiType instanceof PsiClassType) {
            PsiClassType classType = (PsiClassType) psiType;
            PsiClass resolvedClass = PsiTypesUtil.getPsiClass(classType);

            if (resolvedClass == null) {
                return new JsonPrimitive("unknown_type");
            }

            String fqn = resolvedClass.getQualifiedName();
            if (fqn == null) {
                return new JsonPrimitive("no_fqn");
            }
// ✅ 新增：判断是否为 List / Set 等集合类型
            if (isCollectionType(resolvedClass)) {
                // 尝试获取泛型参数（如 List<Person> 中的 Person）
                PsiType[] typeParameters = classType.getParameters();
                PsiType elementType = typeParameters.length > 0 ? typeParameters[0] : null;

                // 如果没有泛型信息（原始类型），默认用 Object
                if (elementType == null) {
                    elementType = PsiType.getJavaLangObject(resolvedClass.getManager(), resolvedClass.getResolveScope());
                }

                // 创建 JsonArray，并放入一个示例元素
                JsonArray array = new JsonArray();
                array.add(resolveTypeToJson(elementType, visited));
                return array;
            }

            // 如果是简单类型或 JDK 类型，不展开
            if (isSimpleOrJdkType(fqn)) {
                if (CommonUtils.isNumberType(psiType)) {
                    return new JsonPrimitive(0);
                } else if (CommonUtils.isStringType(psiType)) {
                    return new JsonPrimitive("");
                } else if (CommonUtils.isJavaUtilDateType(psiType)) {
                    return new JsonPrimitive("");
                } else if (isTimeType(fqn)) {
                    return new JsonPrimitive("");
                } else if (CommonUtils.isBooleanType(psiType)) {
                    return new JsonPrimitive(false);
                } else if (CommonUtils.isMapType(psiType)) {
                    return new JsonObject();
                }
                return new JsonPrimitive(fqn);
            }

            // 递归处理自定义类
            return buildJsonObject(resolvedClass, visited);
        }


        // 基本类型、void 等
        String presentableText = psiType.getPresentableText();
        return new JsonPrimitive(presentableText);
    }

    /**
     * 判断是否为 List、Set、Collection 或其子类
     */
    private static boolean isCollectionType(@NotNull PsiClass psiClass) {
        for (String collectionInterface : COLLECTION_INTERFACES) {
            PsiClass interfaceClass = JavaPsiFacade.getInstance(psiClass.getProject())
                    .findClass(collectionInterface, psiClass.getResolveScope());
            if (InheritanceUtil.isInheritorOrSelf(psiClass, interfaceClass, true)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTimeType(@NotNull String fqn) {
        // ✅ 时间类型：全部视为字符串（不展开）
        if (fqn.startsWith("java.time.")) { // 包括 LocalDateTime, ZonedDateTime, Instant 等
            return true;
        }
        if (fqn.equals("java.util.Date") ||
                fqn.equals("java.util.Calendar") ||
                fqn.equals("java.sql.Date") ||
                fqn.equals("java.sql.Time") ||
                fqn.equals("java.sql.Timestamp")) {
            return true;
        }
        return false;
    }

    private static boolean isSimpleOrJdkType(@NotNull String fqn) {
        // 基本类型包装类和常用 JDK 类型
        return fqn.startsWith("java.lang.") ||
                fqn.startsWith("java.util.") ||
                fqn.startsWith("java.time.") ||
                fqn.equals("java.math.BigDecimal") ||
                fqn.equals("java.math.BigInteger") ||
                fqn.equals("java.net.URL") ||
                fqn.equals("java.net.URI") ||
                fqn.equals("java.sql.Date") ||
                fqn.equals("java.sql.Timestamp") ||
                // 原始类型（虽然通常不会以 FQN 形式出现，但安全起见）
                fqn.equals("int") || fqn.equals("long") || fqn.equals("boolean") ||
                fqn.equals("double") || fqn.equals("float") || fqn.equals("char") ||
                fqn.equals("byte") || fqn.equals("short") || fqn.equals("void");
    }
}
