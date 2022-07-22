package org.freeone.javabean.tsinterface;

import com.github.javaparser.ast.CompilationUnit;
import com.intellij.lang.FileASTNode;
import com.intellij.lang.impl.PsiBuilderImpl;
import com.intellij.lang.java.parser.FileParser;
import com.intellij.lang.java.parser.JavaParser;
import com.intellij.lang.java.parser.JavaParserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.file.PsiFileImplUtil;
import com.intellij.psi.impl.source.PsiFileImpl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
                        List<PsiType> numberSuperClass = Arrays.stream(fieldItem.getType().getSuperTypes()).filter(superTypeItem -> superTypeItem.getCanonicalText().equals("java.lang.Number")).collect(Collectors.toList());
                        if (!numberSuperClass.isEmpty()){
                            // number
                        }

                        String name = fieldItem.getName();
                        String presentableText = fieldItem.getType().getPresentableText();
                        if (presentableText.equals("String")) {
                            // :string
                        }
                        String canonicalText = fieldItem.getType().getCanonicalText();
                        System.out.println(name + ": " +presentableText + " - " + canonicalText);

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
