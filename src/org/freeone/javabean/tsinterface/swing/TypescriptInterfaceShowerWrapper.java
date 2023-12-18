package org.freeone.javabean.tsinterface.swing;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;

public class TypescriptInterfaceShowerWrapper extends DialogWrapper {

    private TypescriptInterfaceContentDisplayPanel typescriptInterfaceContentDisplayPanel;

    public TypescriptInterfaceShowerWrapper() {
        super(true);
        typescriptInterfaceContentDisplayPanel = new TypescriptInterfaceContentDisplayPanel();
        init();
        setTitle("Ts Interface Content");
        this.setSize(500, 600);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return typescriptInterfaceContentDisplayPanel.mainPanel();
    }

    public void setContent(String interfaceContent) {
        if (typescriptInterfaceContentDisplayPanel != null && interfaceContent != null) {
            JTextArea textArea = typescriptInterfaceContentDisplayPanel.getTextArea();
            textArea.setText(interfaceContent);
        }
    }

    @NotNull
    protected Action[] createActions() {
        TypescriptInterfaceShowerWrapper.CustomerCloseAction customerCloseAction = new TypescriptInterfaceShowerWrapper.CustomerCloseAction(this);
        customerCloseAction.putValue(DialogWrapper.DEFAULT_ACTION, true);
        return new Action[]{customerCloseAction};

    }

    protected class CustomerCloseAction extends DialogWrapperAction {

        private final TypescriptInterfaceShowerWrapper wrapper;

        protected CustomerCloseAction(TypescriptInterfaceShowerWrapper wrapper) {
            super("copy and close");
            this.wrapper = wrapper;
        }

        @Override
        protected void doAction(ActionEvent actionEvent) {
            Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            String text = typescriptInterfaceContentDisplayPanel.getTextArea().getText();
            Transferable tText = new StringSelection(text);
            systemClipboard.setContents(tText, null);
            wrapper.doCancelAction();
        }
    }
}
