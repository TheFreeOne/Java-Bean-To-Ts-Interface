package org.freeone.javabean.tsinterface.setting;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 主要配置文件
 * Provides controller functionality for application settings.
 */
final class JavaBeanToTypescriptInterfaceSettingsConfigurable implements Configurable {

    private JavaBeanToTypescriptInterfaceComponent mySettingsComponent;

    // A default constructor with no arguments is required because this implementation
    // is registered in an applicationConfigurable EP

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "SDK: Application Settings Example";
    }


    @Nullable
    @Override
    public JComponent createComponent() {
        mySettingsComponent = new JavaBeanToTypescriptInterfaceComponent();
        return mySettingsComponent.getJPanel();
    }

    @Override
    public boolean isModified() {
        JavaBeanToTypescriptInterfaceSettingsState settings = JavaBeanToTypescriptInterfaceSettingsState.getInstance();
        boolean modified = settings.enableDataToString != mySettingsComponent.getDateToStringCheckBox().isSelected();
        modified |= settings.useAnnotationJsonProperty != mySettingsComponent.getUseJsonPropertyCheckBox().isSelected();
        modified |= settings.allowFindClassInAllScope != mySettingsComponent.getAllowFindClassInAllScope().isSelected();
        modified |= settings.ignoreParentField != mySettingsComponent.getIgnoreParentField().isSelected();
        return modified;

    }

    @Override
    public void apply() {
        JavaBeanToTypescriptInterfaceSettingsState settings = JavaBeanToTypescriptInterfaceSettingsState.getInstance();
        settings.setEnableDataToString(mySettingsComponent.getDateToStringCheckBox().isSelected());
        settings.setUseAnnotationJsonProperty(mySettingsComponent.getUseJsonPropertyCheckBox().isSelected());
        settings.setAllowFindClassInAllScope(mySettingsComponent.getAllowFindClassInAllScope().isSelected());
        settings.setIgnoreParentField(mySettingsComponent.getIgnoreParentField().isSelected());
    }


    @Override
    public void reset() {
        JavaBeanToTypescriptInterfaceSettingsState settings = JavaBeanToTypescriptInterfaceSettingsState.getInstance();
        mySettingsComponent.getDateToStringCheckBox().setSelected(settings.enableDataToString);
        mySettingsComponent.getUseJsonPropertyCheckBox().setSelected(settings.useAnnotationJsonProperty);
        mySettingsComponent.getAllowFindClassInAllScope().setSelected(settings.allowFindClassInAllScope);
        mySettingsComponent.getIgnoreParentField().setSelected(settings.ignoreParentField);
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }

}