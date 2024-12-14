package org.freeone.javabean.tsinterface;

import com.intellij.lang.jvm.JvmClassKind;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiExtensibleClass;
import org.freeone.javabean.tsinterface.swing.SampleDialogWrapper;
import org.freeone.javabean.tsinterface.swing.TypescriptInterfaceShowerWrapper;
import org.freeone.javabean.tsinterface.util.CommonUtils;
import org.freeone.javabean.tsinterface.util.TypescriptContentGenerator;
import org.freeone.javabean.tsinterface.util.TypescriptUtils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 原来的保存到文件
 */
public class JavaBeanToTypescriptInterfaceAction extends AnAction {

    private final NotificationGroup notificationGroup = new NotificationGroup("JavaBeanToTypescriptInterface", NotificationDisplayType.STICKY_BALLOON, true);

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
        try {
            if (!menuText.toLowerCase().startsWith("save")) {
                needSaveToFile = false;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        VirtualFile[] virtualFiles = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
        if (virtualFiles != null && virtualFiles.length == 1) {
            VirtualFile target = virtualFiles[0];
            if (target.isDirectory()) {
                Messages.showInfoMessage("Please choose a Java Bean file !", "");
                return;
            }
            PsiManager psiMgr = PsiManager.getInstance(project);
            PsiFile file = psiMgr.findFile(target);
            if (!(file instanceof PsiJavaFile)) {
                Messages.showInfoMessage("Unsupported source file!", "");
                return;
            }
            PsiElement psiElement = e.getData(PlatformDataKeys.PSI_ELEMENT);
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
                    Messages.showInfoMessage("Can not find a class", "");
                    return;
                }
            }


            if (menuText.contains("2") || description.contains("2.0")) {
                if (psiElement == null) {
                    Messages.showInfoMessage("Can not find a class", "");
                    return;
                }
                if (psiElement instanceof PsiClass) {
                    PsiClass psiClass = (PsiClass) psiElement;
                    TypescriptContentGenerator.processPsiClass(project, psiClass, needSaveToFile);
                    String content = TypescriptContentGenerator.mergeContent(psiClass, needSaveToFile);
                    TypescriptContentGenerator.clearCache();
                    generateTypescriptContent(e, project, needSaveToFile, psiClass.getName(), content);
                }
            } else {
                // 1.0
                if (file instanceof PsiJavaFile) {
                    PsiJavaFile psiJavaFile = (PsiJavaFile) file;
                    if (psiElement instanceof PsiClass) {
                        PsiClass psiClass = (PsiClass) psiElement;
                        boolean innerPublicClass = CommonUtils.isInnerPublicClass(psiJavaFile, psiClass);
                        if (innerPublicClass) {
                            SampleDialogWrapper sampleDialogWrapper = new SampleDialogWrapper();
                            boolean b = sampleDialogWrapper.showAndGet();
                            if (b) {
                                // 只有内部public static class 会执行这一步psiClass
                                String interfaceContent = TypescriptUtils.generatorInterfaceContentForPsiClassElement(project, psiClass, needSaveToFile);
                                generateTypescriptContent(e, project, needSaveToFile, psiClass.getName(), interfaceContent);
                                return;
                            }
                        }
                    }

                    // 正常情况下
                    // 声明文件的主要内容 || content of *.d.ts
                    String interfaceContent = TypescriptUtils.generatorInterfaceContentForPsiJavaFile(project, psiJavaFile, needSaveToFile);
                    generateTypescriptContent(e, project, needSaveToFile, psiJavaFile.getVirtualFile().getNameWithoutExtension(), interfaceContent);

                }
            }


        } else {
            Messages.showInfoMessage("Please choose a Java Bean", "");
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
            fileNameToSave, String interfaceContent) {
        if (saveToFile) {
            FileChooserDescriptor chooserDescriptor = CommonUtils.createFileChooserDescriptor("Choose a folder", "The declaration file end with '.d.ts' will be saved in this folder");
            VirtualFile savePathFile = FileChooser.chooseFile(chooserDescriptor, null, null);
            if (savePathFile != null && savePathFile.isDirectory()) {
                String savePath = savePathFile.getPath();
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
