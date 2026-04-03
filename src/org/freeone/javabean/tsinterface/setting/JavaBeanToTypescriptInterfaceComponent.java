package org.freeone.javabean.tsinterface.setting;

import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class JavaBeanToTypescriptInterfaceComponent {
    private JPanel jPanel;
    private JCheckBox dateToStringCheckBox;
    private JCheckBox useJsonPropertyCheckBox;
    private JCheckBox allowFindClassInAllScope;
    private JCheckBox ignoreParentField;
    private JCheckBox localDateToStringCheckbox;
    private JTable enableNameToStringListTable;
    private JScrollPane jScrollPanel;
    private JPanel tableActionsPanel;
    private JButton addButton;
    private JButton deleteButton;
    private JLabel labelUnderTable;

    public JavaBeanToTypescriptInterfaceComponent() {
        // 1. 获取配置数据
        List<String> fullNameWithPackageToStringList = JavaBeanToTypescriptInterfaceSettingsState.getInstance().getFullNameWithPackageToStringList();

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

        // 3. 设置模型给 *已经存在* 的表格 (由 GUI 设计器初始化)
        // 这里的 enableNameToStringListTable 是由 .form 文件注入的，不要重新 new
        enableNameToStringListTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        enableNameToStringListTable.setModel(new DefaultTableModel(rowData, columnNames));
        enableNameToStringListTable.setBounds(0, 0, 400, 180);
        enableNameToStringListTable.getColumnModel().getColumn(0).setPreferredWidth(400);

        addButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Vector<Vector<String>> rowData = new Vector<>();
                TableModel model = enableNameToStringListTable.getModel();
// 2. 获取行数和列数
                int rowCount = model.getRowCount();
                int colCount = model.getColumnCount();
                for (int i = 0; i < rowCount; i++) {

                    for (int j = 0; j < colCount; j++) {
                        Object value = model.getValueAt(i, j);
                        Vector<String> row = new Vector<>();
                        row.add(value.toString());
                        rowData.add(row);
                    }
                }
                Vector<String> row = new Vector<>();
                row.add("");
                rowData.add(row);

                Vector<String> columnNames = new Vector<>();
                columnNames.add(finalColumnName);
                enableNameToStringListTable.setModel(new DefaultTableModel(rowData, columnNames));
                enableNameToStringListTable.setBounds(0, 0, 400, 180);
                enableNameToStringListTable.getColumnModel().getColumn(0).setPreferredWidth(400);
            }
        });

        deleteButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int[] selectedRows = enableNameToStringListTable.getSelectedRows();
                if (selectedRows == null || selectedRows.length == 0) {
                    Messages.showErrorDialog("Please select row data", "Warning");
                    return;
                }
                List<Integer> selectedRowList = new ArrayList<>();
                for (int selectedRow : selectedRows) {
                    selectedRowList.add(selectedRow);
                }
                Vector<Vector<String>> rowData = new Vector<>();
                List<String> fullNameWithPackageToStringListNew = new ArrayList<>();
                TableModel model = enableNameToStringListTable.getModel();
// 2. 获取行数和列数
                int rowCount = model.getRowCount();
                int colCount = model.getColumnCount();
                for (int i = 0; i < rowCount; i++) {
                    if (selectedRowList.contains(i)){
                        continue;
                    }
                    for (int j = 0; j < colCount; j++) {
                        // 3. 从模型中获取数据（注意：这里使用的是模型索引，不受列拖拽影响）
                        Object value = model.getValueAt(i, j);
//                System.out.println("行: " + i + ", 列: " + j + " = " + value.toString());
                        fullNameWithPackageToStringListNew.add(value.toString());
                        Vector<String> row = new Vector<>();
                        row.add(value.toString());
                        rowData.add(row);
                    }
                }



                Vector<String> columnNames = new Vector<>();
                columnNames.add(finalColumnName);
//                JavaBeanToTypescriptInterfaceSettingsState.getInstance().setFullNameWithPackageToStringList(fullNameWithPackageToStringListNew);
                enableNameToStringListTable.setModel(new DefaultTableModel(rowData, columnNames));
                enableNameToStringListTable.setBounds(0, 0, 400, 180);
                enableNameToStringListTable.getColumnModel().getColumn(0).setPreferredWidth(400);

            }
        });

    }

    public JPanel getJPanel() {
        return jPanel;
    }

    public JCheckBox getDateToStringCheckBox() {
        return dateToStringCheckBox;
    }

    public JCheckBox getUseJsonPropertyCheckBox() {
        return useJsonPropertyCheckBox;
    }

    public JCheckBox getAllowFindClassInAllScope() {
        return allowFindClassInAllScope;
    }

    public JCheckBox getIgnoreParentField() {
        return ignoreParentField;
    }

    public JCheckBox getLocalDateToStringCheckbox() {
        return localDateToStringCheckbox;
    }

    public JTable getEnableNameToStringListTable() {
        return enableNameToStringListTable;
    }




    private void createUIComponents() {

    }
}
