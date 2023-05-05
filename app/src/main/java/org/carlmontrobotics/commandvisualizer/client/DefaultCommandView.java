package org.carlmontrobotics.commandvisualizer.client;

import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.carlmontrobotics.commandvisualizer.CommandDescriptor;

public class DefaultCommandView extends CommandView {

    protected JTable table;
    protected DefaultTableModel tableModel;

    private final Map<String, Object> data = new HashMap<>();

    public DefaultCommandView() {
        setLayout(new BorderLayout());

        add(new JScrollPane(table = new JTable(tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        }), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        tableModel.addColumn("Name");
        tableModel.addColumn("Value");
    }

    @Override
    public void update(CommandDescriptor descriptor) {
        data.clear();
        data.put("Id", descriptor.id);
        data.put("Name", descriptor.name);
        data.put("Class", descriptor.clazz);
        data.put("Describer", descriptor.describer);
        data.put("Running", descriptor.isRunning);
        data.put("Runs When Disabled", descriptor.runsWhenDisabled);
        data.put("Composed", descriptor.isComposed);
        data.put("Interruption Behavior", descriptor.interruptionBehavior);
        data.put("Requirements", Arrays.toString(descriptor.requirements));

        data.putAll(descriptor.parameters);

        while(tableModel.getRowCount() > 0) tableModel.removeRow(0);

        data.forEach((k, v) -> tableModel.addRow(new Object[] { k, v }));
    }

}
