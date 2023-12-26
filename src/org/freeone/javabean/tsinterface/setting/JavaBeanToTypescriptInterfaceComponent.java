package org.freeone.javabean.tsinterface.setting;

import javax.swing.*;

public class JavaBeanToTypescriptInterfaceComponent {
    private JPanel jPanel;
    private JCheckBox dateToStringCheckBox;
    private JCheckBox useJsonPropertyCheckBox;

    public JPanel getJPanel() {
        return jPanel;
    }

    public JCheckBox getDateToStringCheckBox() {
        return dateToStringCheckBox;
    }

    public JCheckBox getUseJsonPropertyCheckBox() {
        return useJsonPropertyCheckBox;
    }
}
