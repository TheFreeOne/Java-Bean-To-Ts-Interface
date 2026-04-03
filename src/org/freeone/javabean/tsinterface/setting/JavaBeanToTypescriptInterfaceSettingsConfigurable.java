package org.freeone.javabean.tsinterface.setting;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

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
        // TODO: 2026-04-03  
        modified |= settings.enableLocalDateToString != mySettingsComponent.getLocalDateToStringCheckbox().isSelected();
        JTable enableNameToStringListTable = mySettingsComponent.getEnableNameToStringListTable();
        List<String> stringsInTable = getStringsInTable(enableNameToStringListTable);
        List<String> settingStrings = settings.fullNameWithPackageToStringList.stream().filter(r -> r != null && r.trim().length() > 0).collect(Collectors.toList());
        modified |= !String.join(";", settingStrings).equals(String.join(";", stringsInTable));
        return modified;
    }

    @NotNull
    private List<String> getStringsInTable(JTable enableNameToStringListTable) {
        TableModel model = enableNameToStringListTable.getModel();
        List<String> stringsInTable = new ArrayList<>();
// 2. 获取行数和列数
        int rowCount = model.getRowCount();
        int colCount = model.getColumnCount();
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < colCount; j++) {
                // 3. 从模型中获取数据（注意：这里使用的是模型索引，不受列拖拽影响）
                Object value = model.getValueAt(i, j);
//                System.out.println("行: " + i + ", 列: " + j + " = " + value.toString());
                stringsInTable.add(value.toString());
            }
        }
        return stringsInTable;
    }

    @Override
    public void apply() {
        JavaBeanToTypescriptInterfaceSettingsState settings = JavaBeanToTypescriptInterfaceSettingsState.getInstance();
        settings.setEnableDataToString(mySettingsComponent.getDateToStringCheckBox().isSelected());
        settings.setUseAnnotationJsonProperty(mySettingsComponent.getUseJsonPropertyCheckBox().isSelected());
        settings.setAllowFindClassInAllScope(mySettingsComponent.getAllowFindClassInAllScope().isSelected());
        settings.setIgnoreParentField(mySettingsComponent.getIgnoreParentField().isSelected());
        //  : 2026-04-03
        settings.setEnableLocalDateToString(mySettingsComponent.getLocalDateToStringCheckbox().isSelected());
        JTable enableNameToStringListTable = mySettingsComponent.getEnableNameToStringListTable();
        List<String> stringsInTable = getStringsInTable(enableNameToStringListTable);
        stringsInTable = stringsInTable.stream().filter(r->r != null && r.trim().length() > 0).map(String::trim).collect(Collectors.toList());
        settings.setFullNameWithPackageToStringList(stringsInTable);
    }


    @Override
    public void reset() {
        JavaBeanToTypescriptInterfaceSettingsState settings = JavaBeanToTypescriptInterfaceSettingsState.getInstance();
        mySettingsComponent.getDateToStringCheckBox().setSelected(settings.enableDataToString);
        mySettingsComponent.getUseJsonPropertyCheckBox().setSelected(settings.useAnnotationJsonProperty);
        mySettingsComponent.getAllowFindClassInAllScope().setSelected(settings.allowFindClassInAllScope);
        mySettingsComponent.getIgnoreParentField().setSelected(settings.ignoreParentField);
        mySettingsComponent.getLocalDateToStringCheckbox().setSelected(settings.enableLocalDateToString);


        List<String> fullNameWithPackageToStringList = settings.fullNameWithPackageToStringList;
        // 2. 准备数据模型
        // 注意：DefaultTableModel 的第一个参数是行数据(Vector<Vector>)，第二个是列名(Vector)
        // 你的数据是 List<String>，需要转换一下格式，把它变成一行一行的数据
        Vector<Vector<String>> rowData = new Vector<>();
        for (String name : fullNameWithPackageToStringList) {
            Vector<String> row = new Vector<>();
            row.add(name);
            rowData.add(row);
        }
        final String finalColumnName = "class name with package to string";
        Vector<String> columnNames = new Vector<>();
        columnNames.add(finalColumnName);
        JTable enableNameToStringListTable = mySettingsComponent.getEnableNameToStringListTable();
        // 3. 设置模型给 *已经存在* 的表格 (由 GUI 设计器初始化)
        // 这里的 enableNameToStringListTable 是由 .form 文件注入的，不要重新 new
        enableNameToStringListTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        enableNameToStringListTable.setModel(new DefaultTableModel(rowData, columnNames));
        enableNameToStringListTable.setBounds(0, 0, 400, 180);
        enableNameToStringListTable.getColumnModel().getColumn(0).setPreferredWidth(400);

    }

//    @Override
//    public void disposeUIResources() {
//        mySettingsComponent = null;
//    }

}