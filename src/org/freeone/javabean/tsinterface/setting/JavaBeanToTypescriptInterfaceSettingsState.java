package org.freeone.javabean.tsinterface.setting;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 持久化
 * Supports storing the application settings in a persistent way.
 * The {@link State} and {@link Storage} annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 * https://plugins.jetbrains.com/docs/intellij/settings-tutorial.html#the-appsettingsstate-class
 */
@State(
        name = "JavaBeanToTypescriptInterfaceSetting",
        storages = @Storage("JavaBeanToTypescriptInterfaceSettingsPlugin.xml")
)
 public final class JavaBeanToTypescriptInterfaceSettingsState implements PersistentStateComponent<JavaBeanToTypescriptInterfaceSettingsState> {

    public String userName = "TheFreeOne";

    public boolean enableDataToString = false;

    public boolean useAnnotationJsonProperty = false;

    public boolean allowFindClassInAllScope = true;

    public boolean ignoreParentField = false;



    public static JavaBeanToTypescriptInterfaceSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(JavaBeanToTypescriptInterfaceSettingsState.class);
    }

    @NotNull
    @Override
    public JavaBeanToTypescriptInterfaceSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull JavaBeanToTypescriptInterfaceSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public boolean getUseAnnotationJsonProperty() {
        return useAnnotationJsonProperty;
    }

    public void setUseAnnotationJsonProperty(boolean useAnnotationJsonProperty) {
        this.useAnnotationJsonProperty = useAnnotationJsonProperty;
    }

    public boolean getEnableDataToString() {
        return enableDataToString;
    }

    public void setEnableDataToString(boolean enableDataToString) {
        this.enableDataToString = enableDataToString;
    }

    public boolean isAllowFindClassInAllScope() {
        return allowFindClassInAllScope;
    }

    public void setAllowFindClassInAllScope(boolean allowFindClassInAllScope) {
        this.allowFindClassInAllScope = allowFindClassInAllScope;
    }

    public boolean isIgnoreParentField() {
        return ignoreParentField;
    }

    public void setIgnoreParentField(boolean ignoreParentField) {
        this.ignoreParentField = ignoreParentField;
    }
}