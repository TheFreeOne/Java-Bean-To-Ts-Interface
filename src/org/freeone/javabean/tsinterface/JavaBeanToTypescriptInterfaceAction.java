package org.freeone.javabean.tsinterface;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.freeone.javabean.tsinterface.util.CommonUtils;
import org.freeone.javabean.tsinterface.util.TypescriptUtils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class JavaBeanToTypescriptInterfaceAction extends AnAction {

    public static final String requireSplitTag = ": ";
    public static final String notRequireSplitTag = "?: ";

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
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
            final String path = target.getPath();
            System.out.println("path = " + path);

            PsiManager psiMgr = PsiManager.getInstance(project);
            PsiFile file = psiMgr.findFile(target);
            if (file instanceof PsiJavaFile ) {
                PsiJavaFile psiJavaFile = (PsiJavaFile) file;

                String interfaceContent = new TypescriptUtils().generatorInterfaceContent(project, psiJavaFile);
                System.out.println(interfaceContent);

                FileChooserDescriptor chooserDescriptor = CommonUtils.createFileChooserDescriptor("选择文件夹", "文件将会保存在此目录");
                VirtualFile savePathFile = FileChooser.chooseFile(chooserDescriptor, null, null);
                if (savePathFile != null && savePathFile.isDirectory()){
                    String savePath = savePathFile.getPath();
                    String nameWithoutExtension = psiJavaFile.getVirtualFile().getNameWithoutExtension();
                    String interfaceFileSavePath = savePath + "/" + nameWithoutExtension+".d.ts";
                    try {
                        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(interfaceFileSavePath,false),"utf-8"));

                        bufferedWriter.write(interfaceContent);
                        bufferedWriter.close();
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
