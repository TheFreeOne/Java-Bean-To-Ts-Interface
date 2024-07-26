package org.freeone.javabean.tsinterface.swing;

import com.intellij.openapi.ui.DialogWrapper;

import javax.swing.*;
import java.awt.*;

public class SampleDialogWrapper extends DialogWrapper {

    public SampleDialogWrapper() {
        super(true); // use current window as parent
        setTitle("Tips");
        init();
    }


    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        JLabel label = new JLabel("<html><p>You Have Chosen a Inner Public Static Class</p> <br> Use It As Main Class ? </html>");
        label.setPreferredSize(new Dimension(200, 50));
        dialogPanel.add(label, BorderLayout.CENTER);
        dialogPanel.setSize(200,50);
        dialogPanel.setSize(new Dimension(200, 50));
        dialogPanel.setPreferredSize(new Dimension(200, 50));
        return dialogPanel;
    }
}