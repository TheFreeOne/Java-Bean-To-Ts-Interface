package org.freeone.javabean.tsinterface;

import com.intellij.lang.jvm.JvmClassKind;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassImpl;
import com.intellij.psi.impl.source.PsiExtensibleClass;
import com.intellij.psi.impl.source.PsiMethodImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import org.freeone.javabean.tsinterface.swing.TypescriptInterfaceShowerWrapper;
import org.freeone.javabean.tsinterface.util.CommonUtils;
import org.freeone.javabean.tsinterface.util.MockAllFieldJsonUtils;
import org.freeone.javabean.tsinterface.util.RequestMappingUtils;
import org.freeone.javabean.tsinterface.util.TypescriptContentGenerator;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 原来的保存到文件
 */
public class JavaBeanToTypescriptInterfaceAction extends AnAction {

    private final NotificationGroup notificationGroup = new NotificationGroup("JavaBeanToTypescriptInterface", NotificationDisplayType.STICKY_BALLOON, true);

    @Override
    public void update(@NotNull AnActionEvent e) {
        // 获取当前上下文
        Project project = e.getProject();
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);


        e.getPresentation().setVisible(true);
        e.getPresentation().setEnabled(true); // 可选：同时控制是否可点击
        // 2025.1.7 中无法获取到文件
        if (file == null) {
            return;
        }
        // 示例：只在选中 .txt 文件时显示
        boolean visible = ("java".equals(file.getExtension()) || "class".equals(file.getExtension()));
        e.getPresentation().setVisible(visible);
        e.getPresentation().setEnabled(visible);
    }


    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        boolean needSaveToFile = true;
        Presentation presentation = e.getPresentation();
        String description = Optional.ofNullable(presentation.getDescription()).orElse("");
        String menuText = Optional.ofNullable(presentation.getText()).orElse("");

        boolean isMockJson = menuText.equalsIgnoreCase("mock all field into json");
        boolean isGenerateMappingMarkDown = menuText.contains("Generate Mapping Markdown");

        try {
            if (!menuText.toLowerCase().startsWith("save")) {
                needSaveToFile = false;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        VirtualFile[] virtualFiles = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
        if (virtualFiles == null) {
            return;
        }
        List<VirtualFile> virtualFileList = Arrays.asList(virtualFiles);
        // 不支持json
        boolean containDirectory = virtualFileList.stream().anyMatch(VirtualFile::isDirectory);
        if (containDirectory) {
            Messages.showInfoMessage("Do Not choose directory !", "");
            return;
        }
        // 需包含java文件
        virtualFileList = virtualFileList.stream().filter(file -> "java".equals(file.getExtension()) || "class".equals(file.getExtension())).collect(Collectors.toList());
        if (virtualFileList.isEmpty()) {
            Messages.showInfoMessage("Please select the Java file.", "Tips");
            return;
        }
        // 模拟json不支持多文件操作
        if (isMockJson && virtualFileList.size() > 1) {
            Messages.showInfoMessage("The current function does not support multiple files", "");
            return;
        }

        if (isGenerateMappingMarkDown && virtualFileList.size() > 1) {
            Messages.showInfoMessage("The current function does not support multiple files", "");
            return;
        }
        if (isGenerateMappingMarkDown) {
            PsiElement psiElement = e.getData(PlatformDataKeys.PSI_ELEMENT);
            if (psiElement == null) {
                Messages.showInfoMessage("Psi element is null", "");
                return;
            }

            if (!(psiElement instanceof PsiMethod)) {
                Messages.showInfoMessage("Please click on a method", "");
                return;
            }
        }

        // 其他操作
        if (!needSaveToFile && virtualFileList.size() > 1) {
            Messages.showInfoMessage("The current function does not support multiple files", "");
            return;
        }
        String savePath = null;
        if (needSaveToFile) {
            FileChooserDescriptor chooserDescriptor = CommonUtils.createFileChooserDescriptor("Choose a folder", "The declaration file end with '.d.ts' will be saved in this folder");
            VirtualFile savePathFile = FileChooser.chooseFile(chooserDescriptor, null, null);
            if (savePathFile != null && savePathFile.isDirectory()) {
                savePath = savePathFile.getPath();
            }
            if (savePath == null || savePath.trim().length() == 0) {
                return;
            }
        }

        PsiManager psiMgr = PsiManager.getInstance(project);

        for (VirtualFile target : virtualFiles) {
            PsiFile file = psiMgr.findFile(target);
            // 非PsiJavaFile的不执行
            if (!(file instanceof PsiJavaFile)) {
                continue;
            }

            PsiElement psiElement = e.getData(PlatformDataKeys.PSI_ELEMENT);
            PsiElement originPsiElement = psiElement;
            // 当在editor右键的时候psiElement 可能是null的
            if ("EditorPopup".equalsIgnoreCase(e.getPlace())) {
//                psiElement may be null
                // 在 filed 上右键选择其所属弗雷
                if (psiElement instanceof PsiField || psiElement instanceof PsiMethod) {
                    if (psiElement.getParent() != null && psiElement.getParent() instanceof PsiClass) {
                        psiElement = psiElement.getParent();
                    }
                } else if (!(psiElement instanceof PsiClass)) {
                    // psiElement 可能是null的， 这个警告是因为null也不要是psiClass的实例但是这个要知道psiElement 可能是null
                    PsiJavaFile psiJavaFile = (PsiJavaFile) file;
                    PsiClass[] classes = psiJavaFile.getClasses();
                    if (classes.length != 0) {
                        psiElement = classes[0];
                    }
                } else if (psiElement instanceof PsiExtensibleClass) {
                    PsiExtensibleClass psiExtensibleClass = (PsiExtensibleClass) psiElement;
                    JvmClassKind classKind = psiExtensibleClass.getClassKind();
                    // 注解
                    if (classKind == JvmClassKind.ANNOTATION) {
                        PsiJavaFile psiJavaFile = (PsiJavaFile) file;
                        PsiClass[] classes = psiJavaFile.getClasses();
                        if (classes.length != 0) {
                            psiElement = classes[0];
                        }
                    }
                }

                if (psiElement == null) {
                    Messages.showInfoMessage("Can not find a class!", "");
                    return;
                }
            } else {
                // ProjectViewPopup
                if (psiElement == null) {
                    if (file instanceof PsiClass) {
                        psiElement = file;
                    } else {
                        PsiJavaFile psiJavaFile = (PsiJavaFile) file;
                        if (psiJavaFile.getClasses().length == 1) {
                            psiElement = psiJavaFile.getClasses()[0];
                        }
                    }
                }
                if (psiElement == null) {
                    Messages.showInfoMessage("Can not find a class", "");
                    return;
                }
            }

            // psiElement 使我们主要操作的目标
            if (isMockJson) {
                if (psiElement instanceof PsiClass) {
                    PsiClass psiClass = (PsiClass) psiElement;
                    String json = MockAllFieldJsonUtils.generateJsonFromClass(project, psiClass);
                    System.out.println(json);
                    Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    Transferable tText = new StringSelection(json);
                    systemClipboard.setContents(tText, null);
                    Notification notification = notificationGroup.createNotification("Copy To Clipboard Completed", NotificationType.INFORMATION);
                    notification.setImportant(false).notify(project);
                    return;
                } else {
                    Messages.showInfoMessage("Can not find a class", "");
                }
                return;
            } else if (isGenerateMappingMarkDown) {

                doGenerateMappingMarkdown(originPsiElement, project);
            } else if (menuText.contains("2") || description.contains("2.0")) {
                if (psiElement instanceof PsiClass) {
                    PsiClass psiClass = (PsiClass) psiElement;
                    TypescriptContentGenerator.processPsiClass(project, psiClass, needSaveToFile);
                    String content = TypescriptContentGenerator.mergeContent(psiClass, needSaveToFile);
                    TypescriptContentGenerator.clearCache();
                    generateTypescriptContent(e, project, needSaveToFile, psiClass.getName(), content, savePath);
                }
            }


        }
    }

    private void doGenerateMappingMarkdown(PsiElement originPsiElement, Project project) {
        System.out.println("isGenerateMappingMarkDown ===> isGenerateMappingMarkDown");
        if (originPsiElement instanceof PsiMethod) {
            PsiMethodImpl psiMethod = (PsiMethodImpl) originPsiElement;
            PsiAnnotation[] annotations = psiMethod.getAnnotations();


            Optional<PsiAnnotation> first = Arrays.stream(annotations).filter(r -> Objects.equals(r.getQualifiedName(), "org.springframework.web.bind.annotation.RequestMapping")
                    || Objects.equals(r.getQualifiedName(), "org.springframework.web.bind.annotation.GetMapping")
                    || Objects.equals(r.getQualifiedName(), "org.springframework.web.bind.annotation.PostMapping")
                    || Objects.equals(r.getQualifiedName(), "org.springframework.web.bind.annotation.PutMapping")
                    || Objects.equals(r.getQualifiedName(), "org.springframework.web.bind.annotation.PatchMapping")
                    || Objects.equals(r.getQualifiedName(), "org.springframework.web.bind.annotation.DeleteMapping")
            ).findFirst();
            if (first.isPresent()) {
                String className = "";
                List<String> classDocuments = Collections.EMPTY_LIST;
                PsiAnnotation psiAnnotation = first.get();
                RequestMappingUtils.RequestInfo requestInfoOfClass = null;
                RequestMappingUtils.RequestInfo requestInfo = RequestMappingUtils.parsePsiAnnotation(psiAnnotation);
                PsiElement parent = psiMethod.getParent();
                if (parent instanceof PsiClass) {
                    PsiClassImpl psiClass = (PsiClassImpl) parent;
                    className = psiClass.getName();
                    if (psiClass.getDocComment() != null) {
                        classDocuments = Arrays.stream(psiClass.getDocComment().getDescriptionElements()).map(PsiElement::getText).filter(r -> r != null && r.trim().length() > 0).collect(Collectors.toList());
                    }
                    PsiAnnotation[] classAnnotations = psiClass.getAnnotations();
                    Optional<PsiAnnotation> firstClassAnnotation = Arrays.stream(classAnnotations).filter(r -> Objects.equals(r.getQualifiedName(), "org.springframework.web.bind.annotation.RequestMapping")
                            || Objects.equals(r.getQualifiedName(), "org.springframework.web.bind.annotation.GetMapping")
                            || Objects.equals(r.getQualifiedName(), "org.springframework.web.bind.annotation.PostMapping")
                            || Objects.equals(r.getQualifiedName(), "org.springframework.web.bind.annotation.PutMapping")
                            || Objects.equals(r.getQualifiedName(), "org.springframework.web.bind.annotation.PatchMapping")
                            || Objects.equals(r.getQualifiedName(), "org.springframework.web.bind.annotation.DeleteMapping")
                    ).findFirst();
                    if (firstClassAnnotation.isPresent()) {
                        PsiAnnotation psiAnnotation1 = firstClassAnnotation.get();
                        requestInfoOfClass = RequestMappingUtils.parsePsiAnnotation(psiAnnotation1);
                    }
                }

//                System.out.println(" class = " + className);
//                System.out.println(" method = " + psiMethod.getName());
//                System.out.println(" requestMethod = " + String.join(",", requestInfo.getRequestMethods()));
//                System.out.println(" requestPath = " + requestInfo.getRequestPath());

                StringBuilder builder = new StringBuilder();
                builder.append("# ").append(className).append(".").append(psiMethod.getName()).append("\n\n");

//                if (!classDocuments.isEmpty()) {

//                    builder.append("## class info").append("\n");
//                    for (String classDocument : classDocuments) {
//                        if (classDocument != null && classDocument.trim().length() > 0) {
//                            builder.append("> ").append(classDocument.trim()).append("\n");
//                        }
//                    }
//                    builder.append("\n");
//                }

//                builder.append("## method info").append("\n");
                PsiDocComment docComment = psiMethod.getDocComment();
                if (docComment != null) {
                    PsiElement[] descriptionElements = docComment.getDescriptionElements();
                    List<String> descriptionElementList = Arrays.stream(descriptionElements).map(PsiElement::getText).filter(r -> r != null && r.trim().length() > 0).collect(Collectors.toList());

                    if (!descriptionElementList.isEmpty()) {
                        for (String descriptionElement : descriptionElementList) {
                            builder.append("> ").append(descriptionElement.trim()).append("\n");
                        }
                        builder.append("\n");
                    }
                }

                builder.append("### request info").append("\n\n");

                builder.append("| request path | request method |").append("\n");
                builder.append("| ---- | ---- |").append("\n");
                builder.append("| " + (requestInfoOfClass != null ? requestInfoOfClass.getRequestPath() : "") + requestInfo.getRequestPath() + " | " + String.join(",", requestInfo.getRequestMethods()) + " |").append("\n");

                PsiParameterList parameterList = psiMethod.getParameterList();
                PsiParameter[] parameters = parameterList.getParameters();
                for (PsiParameter parameter : parameters) {
                    // 检查参数上是否存在 @RequestBody 注解
                    // hasAnnotation 方法使用注解的短名称或全限定名
                    if (parameter.hasAnnotation("org.springframework.web.bind.annotation.RequestBody")) {
                        PsiType parameterType = parameter.getType();

                        if (parameterType instanceof PsiClassType) {
                            // 2. 进行强制类型转换
                            PsiClassType classType = (PsiClassType) parameterType;
                            // 3. 调用 resolve() 方法获取 PsiClass
                            PsiClass resolve = classType.resolve();
                            if (resolve != null) {
                                TypescriptContentGenerator.processPsiClass(project, resolve, false);
                                String content = TypescriptContentGenerator.mergeContent(resolve, false);
                                TypescriptContentGenerator.clearCache();
                                if (content.trim().length() > 0) {
                                    builder.append("### request parameters").append("\n\n");
                                    builder.append("```typescript\n\n");
                                    builder.append(content).append("\n");
                                    builder.append("```\n\n");
                                }
                            }
                        }

                    }
                }

                // 1. 获取方法的返回类型
                PsiType returnType = psiMethod.getReturnType();
                if (returnType != null) {
                    // 获取类型的完整限定名，例如 "com.example.UserDto"
                    if (returnType instanceof PsiClassType) {
                        PsiClassType classType = (PsiClassType) returnType;
                        PsiClass resolve = classType.resolve();
                        if (resolve != null) {
                            TypescriptContentGenerator.processPsiClass(project, resolve, false);
                            String content = TypescriptContentGenerator.mergeContent(resolve, false);
                            TypescriptContentGenerator.clearCache();

                            if (content.trim().length() > 0) {
                                builder.append("### response parameters").append("\n\n");
                                builder.append("```typescript\n\n");
                                builder.append(content).append("\n");
                                builder.append("```\n\n");
                            }
                        }

                    }


                } else {
                    System.out.println("返回类型: void");
                }

//                PsiType returnType = psiMethod.getReturnType();
//                String content = TypescriptContentGenerator.mergeContent(psiClass, needSaveToFile);
//                TypescriptContentGenerator.clearCache();
                String savePath = "";
                FileChooserDescriptor chooserDescriptor = CommonUtils.createFileChooserDescriptor("Choose a folder", "The declaration file end with '.d.ts' will be saved in this folder");
                VirtualFile savePathFile = FileChooser.chooseFile(chooserDescriptor, null, null);
                if (savePathFile != null && savePathFile.isDirectory()) {
                    savePath = savePathFile.getPath();
                    String interfaceFileSavePath = savePath + "/" + className + "." + psiMethod.getName() + ".md";
                    try {
                        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(interfaceFileSavePath, false), StandardCharsets.UTF_8));
                        bufferedWriter.write(builder.toString());
                        bufferedWriter.close();
                        Notification notification = notificationGroup.createNotification("The target file was saved to:  " + interfaceFileSavePath, NotificationType.INFORMATION);
                        notification.setImportant(true).notify(project);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }

                System.out.println(builder.toString());
            }

        }
    }

    /**
     * 针对interfaceContent进行处理，生成内容生成内容
     *
     * @param e
     * @param project
     * @param saveToFile
     * @param fileNameToSave
     * @param interfaceContent
     */
    private void generateTypescriptContent(AnActionEvent e, Project project, boolean saveToFile, String
            fileNameToSave, String interfaceContent, String savePath) {
        if (saveToFile && savePath != null) {
            String interfaceFileSavePath = savePath + "/" + fileNameToSave + ".d.ts";
            try {
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(interfaceFileSavePath, false), StandardCharsets.UTF_8));
                bufferedWriter.write(interfaceContent);
                bufferedWriter.close();
                Notification notification = notificationGroup.createNotification("The target file was saved to:  " + interfaceFileSavePath, NotificationType.INFORMATION);
                notification.setImportant(true).notify(project);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } else {
            try {
                // 获取当前菜单那的文本
                String text = e.getPresentation().getText();
                // 复制到剪切板
                if (text != null && text.toLowerCase().startsWith("copy")) {
                    Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    Transferable tText = new StringSelection(interfaceContent);
                    systemClipboard.setContents(tText, null);
                    Notification notification = notificationGroup.createNotification("Copy To Clipboard Completed", NotificationType.INFORMATION);
                    notification.setImportant(false).notify(project);
                } else {
                    // 在textarea进行编辑展示
                    TypescriptInterfaceShowerWrapper typescriptInterfaceShowerWrapper = new TypescriptInterfaceShowerWrapper();
                    typescriptInterfaceShowerWrapper.setContent(interfaceContent);
                    typescriptInterfaceShowerWrapper.setOKActionEnabled(false);
                    typescriptInterfaceShowerWrapper.setSize(500, 600);
                    typescriptInterfaceShowerWrapper.show();
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

}
