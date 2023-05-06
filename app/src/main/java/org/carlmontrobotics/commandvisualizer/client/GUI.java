package org.carlmontrobotics.commandvisualizer.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.carlmontrobotics.commandvisualizer.CommandDescriptor;
import org.carlmontrobotics.commandvisualizer.CommandVisualizer;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.networktables.NetworkTableInstance;

public class GUI extends JFrame implements TreeSelectionListener {

    private final JTree commandTree;
    private final DefaultTreeModel commandTreeModel;
    private final DefaultMutableTreeNode rootCommandNode;
    private final JSplitPane splitPane;
    private final JPanel emptyPanel;
    private final NetworkSetupPanel networkSetupPanel = new NetworkSetupPanel(this);
    private final JMenuBar menuBar = new JMenuBar();
    private final JMenu showMenu = new JMenu("Show");
    private final JMenuItem showCommandsItem = new JMenuItem("Commands");
    private final JMenuItem showNetworkConfigItem = new JMenuItem("Network Config");
    private final CommandView defaultCommandView = new DefaultCommandView();
    private DefaultMutableTreeNode selectedNode;

    public GUI() {
        super("Command Visualizer");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Comparator<CommandDescriptor> topLevelOrderer = Comparator.comparing(
                descriptor -> descriptor.name,
                String.CASE_INSENSITIVE_ORDER);

        commandTree = new JTree(
                commandTreeModel = new DefaultTreeModel(rootCommandNode = new DefaultMutableTreeNode(null) {
                    @Override
                    public void add(MutableTreeNode newChild) {
                        super.add(newChild);
                        Collections.sort(children, Comparator.comparing(
                                node -> ((CommandDescriptor) ((DefaultMutableTreeNode) node).getUserObject()),
                                topLevelOrderer));
                    }
                }));

        commandTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        commandTree.setCellRenderer(new TreeCellRenderer());
        commandTree.setRootVisible(false);
        commandTree.addTreeSelectionListener(this);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(commandTree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                emptyPanel = new JPanel());

        showCommandsItem.addActionListener(e -> showCommands());
        showNetworkConfigItem.addActionListener(e -> showNetworkSetup());

        showMenu.add(showCommandsItem);
        showMenu.add(showNetworkConfigItem);

        menuBar.add(showMenu);

        setJMenuBar(menuBar);

        if(NetworkConfig.INSTANCE.initialized) {
            showCommands();
        } else {
            showNetworkSetup();
        }

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

    public void showCommands() {
        remove(networkSetupPanel);
        remove(splitPane);
        add(splitPane, BorderLayout.CENTER);
        revalidate();
        splitPane.setDividerLocation(.5);
    }

    public void showNetworkSetup() {
        remove(networkSetupPanel);
        remove(splitPane);
        add(networkSetupPanel, BorderLayout.CENTER);
        revalidate();
    }

    public void updateCommandTree(CommandDescriptor[] descriptors) {
        // Store the selected path to restore it after updating the tree
        // Sometimes, the tree deselects the selected path when updating
        TreePath selectedPath = commandTree.getSelectionPath();
        updateCommandTree(descriptors, rootCommandNode);
        if (validatePath(selectedPath, rootCommandNode)) {
            commandTree.expandPath(selectedPath);
            commandTree.setSelectionPath(selectedPath);
        }
    }

    public void updateCommandTree(CommandDescriptor[] descriptors, DefaultMutableTreeNode node) {
        List<Integer> descriptorIds = Arrays.asList(descriptors).stream().map(descriptor -> descriptor.id)
                .collect(Collectors.toList());
        // Wrap in ArrayList to allow removal
        List<CommandDescriptor> newDescriptors = new ArrayList<>(Arrays.asList(descriptors));
        // Temporary list to store nodes to remove to avoid concurrent modification
        List<TreeNode> toRemove = new ArrayList<>();

        if (node.getChildCount() != 0) {
            for (int nodeIdx = 0; nodeIdx < node.getChildCount(); nodeIdx++) {
                DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(nodeIdx);
                int descriptorIdx = descriptorIds.indexOf(((CommandDescriptor) childNode.getUserObject()).id);
                if (descriptorIdx == -1) {
                    toRemove.add(node.getChildAt(nodeIdx));
                } else {
                    childNode.setUserObject(
                            descriptors[descriptorIds.indexOf(((CommandDescriptor) childNode.getUserObject()).id)]);
                    updateCommandTree(descriptors[descriptorIdx].subCommands, childNode);
                    newDescriptors.remove(descriptors[descriptorIdx]);
                }
            }
            commandTreeModel.nodesChanged(node, IntStream.range(0, node.getChildCount()).toArray());
        }

        if (!toRemove.isEmpty()) {
            // Store pre-removal indices to update the tree model
            int[] removedIndices = toRemove.stream().mapToInt(child -> node.getIndex(child)).toArray();
            // We have to recalculate the indices as we are removing nodes
            toRemove.forEach(child -> node.remove(node.getIndex(child)));
            commandTreeModel.nodesWereRemoved(node, removedIndices, toRemove.toArray());
            if (toRemove.contains(selectedNode)) {
                closeCommandView();
            }
        }

        if (!newDescriptors.isEmpty()) {
            for (CommandDescriptor descriptor : newDescriptors) {
                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(descriptor);
                updateCommandTree(descriptor.subCommands, newNode);
                node.add(newNode);
            }
            commandTreeModel.nodesWereInserted(node,
                    IntStream.range(node.getChildCount() - newDescriptors.size(), node.getChildCount()).toArray());
        }

        if (!toRemove.isEmpty() || !newDescriptors.isEmpty()) {
            commandTreeModel.nodeStructureChanged(node);
        }
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

    public boolean validatePath(TreePath path, DefaultMutableTreeNode rootNode) {
        if (path == null)
            return false;
        Object[] nodes = path.getPath();
        if (nodes[0] != rootNode)
            return false;
        for (int i = 0; i < nodes.length - 1; i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes[i];
            DefaultMutableTreeNode nextNode = (DefaultMutableTreeNode) nodes[i + 1];
            if (node.getIndex(nextNode) == -1)
                return false;
        }

        return true;
    }

    public void closeCommandView() {
        selectedNode = null;
        splitPane.setRightComponent(emptyPanel);
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        TreePath newPath = e.getNewLeadSelectionPath();
        if (newPath == null) {
            closeCommandView();
            return;
        }
        DefaultMutableTreeNode newNode = (DefaultMutableTreeNode) newPath.getLastPathComponent();
        if (newNode == selectedNode) {
            return;
        }
        int dividerLocation = splitPane.getDividerLocation(); // Store divider location to restore later
        selectedNode = newNode;

        CommandDescriptor descriptor = (CommandDescriptor) selectedNode.getUserObject();
        CommandView commandView = CommandView.COMMAND_VIEWS.getOrDefault(descriptor.describer, defaultCommandView);
        commandView.update(descriptor);
        splitPane.setRightComponent(commandView);
        splitPane.setDividerLocation(dividerLocation);
    }

}
