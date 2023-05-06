package org.carlmontrobotics.commandvisualizer.client;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class NetworkSetupPanel extends JPanel implements ActionListener, ItemListener {

    private final JTextField addressField = new JTextField(80);
    private final JTextField portField = new JTextField(80);
    private final JCheckBox useDefaultPort = new JCheckBox();
    private final JCheckBox useDSBox = new JCheckBox();
    private final JButton connectButton = new JButton("Connect");
    private final GUI gui;

    public NetworkSetupPanel(GUI gui) {
        this.gui = gui;

        setLayout(new BorderLayout());

        JPanel controlPanel = new JPanel(new GridLayout(4, 2));

        controlPanel.add(new JLabel("Address/Team Number:"));
        controlPanel.add(addressField);
        controlPanel.add(new JLabel("Port:"));
        controlPanel.add(portField);
        controlPanel.add(new JLabel("Use Default Port:"));
        controlPanel.add(useDefaultPort);
        controlPanel.add(new JLabel("Get Address from DS:"));
        controlPanel.add(useDSBox);

        useDefaultPort.addItemListener(this);
        useDSBox.addItemListener(this);

        add(controlPanel, BorderLayout.CENTER);

        add(connectButton, BorderLayout.SOUTH);

        connectButton.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() != connectButton)
            return;

        NetworkConfig config = new NetworkConfig();

        try {
            config.useDS = useDSBox.isSelected();
            if (config.useDS) {
                config.address = addressField.getText();
                if (config.addressIsTeamNum())
                    config.teamNum = Integer.parseInt(addressField.getText());
            }

            config.useDefaultPort = useDefaultPort.isSelected();
            if (!useDefaultPort.isSelected())
                config.port = Integer.parseInt(portField.getText());

            config.initialized = true;
        } catch (Exception ex) {
            ex.printStackTrace();

            JOptionPane.showMessageDialog(this, "Invalid Input!", "Command Visualizer", JOptionPane.ERROR_MESSAGE);

            return;
        }

        NetworkConfig.INSTANCE.copyFrom(config);
        NetworkConfig.save();

        config.apply();

        gui.showCommands();
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        portField.setEnabled(!useDefaultPort.isSelected());
        addressField.setEnabled(!useDSBox.isSelected());
    }

}
