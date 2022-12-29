package org.freeone.javabean.tsinterface;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import org.freeone.javabean.tsinterface.util.CommonUtils;
import org.freeone.javabean.tsinterface.util.TypescriptUtils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class JavaBeanToTypescriptInterfaceAction extends AnAction {

    public static final String requireSplitTag = ": ";
    public static final String notRequireSplitTag = "?: ";

    private NotificationGroup notificationGroup = new NotificationGroup("JavaBeanToTypescriptInterface", NotificationDisplayType.STICKY_BALLOON, true);

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile[] virtualFiles = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
        if (virtualFiles != null && virtualFiles.length == 1) {
            VirtualFile target = virtualFiles[0];
            if (target.isDirectory()) {
                Messages.showInfoMessage("Please choose a Java Bean file !", "");
                return;
            }
            String fileTypeName = target.getFileType().getName();
            if (!"JAVA".equalsIgnoreCase(fileTypeName)) {
                Messages.showInfoMessage("The file is not a java file!", "");
            }
            if (project == null) {
                return;
            }
            final String path = target.getPath();
            PsiManager psiMgr = PsiManager.getInstance(project);
            PsiFile file = psiMgr.findFile(target);
            if (file instanceof PsiJavaFile ) {
                PsiJavaFile psiJavaFile = (PsiJavaFile) file;
                String interfaceContent = TypescriptUtils.generatorInterfaceContent(project, psiJavaFile);
                FileChooserDescriptor chooserDescriptor = CommonUtils.createFileChooserDescriptor("Choose a folder", "The declaration file end with '.d.ts' will be saved in this folder");
                VirtualFile savePathFile = FileChooser.chooseFile(chooserDescriptor, null, null);
                if (savePathFile != null && savePathFile.isDirectory()){
                    String savePath = savePathFile.getPath();
                    String nameWithoutExtension = psiJavaFile.getVirtualFile().getNameWithoutExtension();
                    String interfaceFileSavePath = savePath + "/" + nameWithoutExtension+".d.ts";
                    try {
                        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(interfaceFileSavePath,false), StandardCharsets.UTF_8));
                        bufferedWriter.write(interfaceContent);
                        bufferedWriter.close();
                        Notification notification = notificationGroup.createNotification("The target file was saved to:  " + interfaceFileSavePath, NotificationType.INFORMATION);
                        notification.setImportant(true).notify(project);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }


        } else {
            Messages.showInfoMessage("Please choose a Java Bean", "");
        }
    }
}
