package org.carlmontrobotics.commandvisualizer.client;

import java.util.HashMap;

import javax.swing.JPanel;

import org.carlmontrobotics.commandvisualizer.CommandDescriptor;

public abstract class CommandView extends JPanel {

    public static final HashMap<String, CommandView> COMMAND_VIEWS = new HashMap<>();

    public abstract void update(CommandDescriptor descriptor);

}
