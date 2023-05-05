package org.carlmontrobotics.commandvisualizer.client;

import javax.swing.JPanel;

import org.carlmontrobotics.commandvisualizer.CommandDescriptor;

public abstract class CommandView extends JPanel {

    public abstract void update(CommandDescriptor descriptor);

}
