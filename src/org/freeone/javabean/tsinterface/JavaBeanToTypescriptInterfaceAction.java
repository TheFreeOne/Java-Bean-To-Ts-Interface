package org.freeone.javabean.tsinterface;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.freeone.javabean.tsinterface.util.CommonUtils;

public class JavaBeanToTypescriptInterfaceAction extends AnAction {

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

            assert project != null;
            PsiManager psiMgr = PsiManager.getInstance(project);
            PsiFile file = psiMgr.findFile(target);
            if (file instanceof PsiJavaFile ) {
                PsiJavaFile psiJavaFile = (PsiJavaFile) file;
                System.out.println(psiJavaFile);
                PsiClass[] classes = psiJavaFile.getClasses();
                System.out.println("classes length = " + classes.length);
                for (PsiClass aClass : classes) {

                    PsiField[] allFields = aClass.getAllFields();
                    for (PsiField fieldItem : allFields) {
                        StringBuilder stringBuilder = new StringBuilder("  ");
                        String name = fieldItem.getName();
                        stringBuilder.append(name);
                        boolean isArray = CommonUtils.isArray(fieldItem);
                        boolean isNumber = CommonUtils.isNumber(fieldItem);
                        boolean isString = CommonUtils.isString(fieldItem);
                        boolean isBoolean = CommonUtils.isBoolean(fieldItem);

                        if (isNumber){
                            stringBuilder.append(": number");
                        } else if (isString){
                            stringBuilder.append(": String");
                        } else if (isBoolean){
                            stringBuilder.append(": boolean");
                        }else {
                            stringBuilder.append(": any");
                        }
                        if (isArray){
                            stringBuilder.append("[]");
                        }
                        System.out.println(stringBuilder.toString());
                    }
                    System.out.println(aClass);
                }
            }

//            CommonUtils.getCachedThreadPool().execute(()-> {
//                try {
//
////                    CompilationUnit parse = JavaUtils.parse(path);
////                    System.out.println(parse);
//
//
//                } catch (Exception exception) {
//                    String errorMessage = "";
//                    if (exception instanceof NullPointerException) {
//                        errorMessage = "NullPointerException";
//                    } else {
//                        errorMessage = exception.getMessage();
//                    }
//                    exception.printStackTrace();
//                    Messages.showErrorDialog(errorMessage, "Plugin Internal Error");
//                }
//            });
        } else {
            Messages.showInfoMessage("Please choose a Java Bean", "");
        }
    }
}
