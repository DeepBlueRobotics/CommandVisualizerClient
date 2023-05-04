package org.carlmontrobotics.commandvisualizer.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

import org.carlmontrobotics.commandvisualizer.CommandDescriptor;
import org.carlmontrobotics.commandvisualizer.CommandVisualizer;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.networktables.NetworkTableInstance;

public class GUI extends JFrame {

    private final JTree commandTree;
    private final DefaultTreeModel commandTreeModel;
    private final DefaultMutableTreeNode rootCommandNode;
    private final JSplitPane splitPane;

    public GUI() {
        super("Command Visualizer");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        commandTree = new JTree(
                commandTreeModel = new DefaultTreeModel(rootCommandNode = new DefaultMutableTreeNode(null)));

        commandTree.setCellRenderer(new TreeCellRenderer());
        commandTree.setRootVisible(false);

        add(splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(commandTree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                new JPanel()), BorderLayout.CENTER);

        NetworkTableInstance.getDefault().addListener(
                NetworkTableInstance.getDefault().getTopic(CommandVisualizer.NT_KEY),
                EnumSet.of(NetworkTableEvent.Kind.kValueAll), event -> {
                    try {
                        CommandDescriptor[] descriptors = new ObjectMapper().readValue(
                                event.valueData.value.getString(),
                                CommandDescriptor[].class);
                        SwingUtilities.invokeLater(() -> updateCommandTree(descriptors));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        setVisible(true);

        splitPane.setDividerLocation(.5);
    }

    public void updateCommandTree(CommandDescriptor[] descriptors) {
        updateCommandTree(descriptors, rootCommandNode);
        updateCommandTree();
    }

    public void updateCommandTree(CommandDescriptor[] descriptors, DefaultMutableTreeNode node) {
        List<Integer> descriptorIds = Arrays.asList(descriptors).stream().map(descriptor -> descriptor.id)
                .collect(Collectors.toList());
        List<CommandDescriptor> newDescriptors = new ArrayList<>(Arrays.asList(descriptors)); // Wrap in ArrayList to
                                                                                              // allow removal
        List<Integer> toRemove = new ArrayList<>(); // Temporary list to store indices of nodes to remove to avoid
                                                    // concurrent modification
        for (int nodeIdx = 0; nodeIdx < node.getChildCount(); nodeIdx++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(nodeIdx);
            int descriptorIdx = descriptorIds.indexOf(((CommandDescriptor) childNode.getUserObject()).id);
            if (descriptorIdx == -1) {
                toRemove.add(nodeIdx);
            } else {
                childNode.setUserObject(
                        descriptors[descriptorIds.indexOf(((CommandDescriptor) childNode.getUserObject()).id)]);
                updateCommandTree(descriptors[descriptorIdx].subCommands, childNode);
                newDescriptors.remove(descriptors[descriptorIdx]);
            }
        }
        // We have to convert the indices to nodes because the indices change as we
        // remove nodes
        // Force the collection into a temporary list so that the indices are used
        // before we start removing nodes
        toRemove.stream().map(node::getChildAt).collect(Collectors.toList())
                .forEach(child -> node.remove(node.getIndex(child)));
        for (CommandDescriptor descriptor : newDescriptors) {
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(descriptor);
            updateCommandTree(descriptor.subCommands, newNode);
            node.add(newNode);
        }
    }

    public void updateCommandTree() {
        // for(int nodeIdx = 0; nodeIdx < rootCommandNode.getChildCount(); nodeIdx++) {
        // DefaultMutableTreeNode node = (DefaultMutableTreeNode)
        // rootCommandNode.getChildAt(nodeIdx);
        // CommandDescriptor descriptor = (CommandDescriptor) node.getUserObject();
        // }
        commandTreeModel.reload();
    }

    public static class TreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            CommandDescriptor descriptor = (CommandDescriptor) node.getUserObject();

            if (descriptor != null) {

                setText(descriptor.name + " (" + descriptor.clazz + ")");

                setBackground(descriptor.isRunning ? new Color(96, 255, 24, 150) : new Color(255, 77, 77, 150));

                setOpaque(true);

            } else {

                setText("Root");

                setOpaque(false);

            }

            return this;
        }
    }

}
