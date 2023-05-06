package org.carlmontrobotics.commandvisualizer.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.wpi.first.networktables.NetworkTableInstance;

public class NetworkConfig {

    public static final String saveLoc = System.getProperty("user.home") + "/.command-visualizer-network-config.json";
    public static final NetworkConfig INSTANCE = new NetworkConfig();

    public String address = "";
    public int teamNum = 0;
    public int port = 0;
    public boolean useDefaultPort = false;
    public boolean useDS = false;
    public boolean initialized = false;

    public void copyFrom(NetworkConfig other) {
        address = other.address;
        port = other.port;
        useDefaultPort = other.useDefaultPort;
        useDS = other.useDS;
        initialized = other.initialized;
    }

    public static void save() {
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(saveLoc))) {
            writer.write(new ObjectMapper().writeValueAsString(INSTANCE));
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        if(!new File(saveLoc).exists()) return;
        try(BufferedReader reader = new BufferedReader(new FileReader(saveLoc))) {
            INSTANCE.copyFrom(new ObjectMapper().readValue(reader, NetworkConfig.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean addressIsTeamNum() {
        return address.matches("\\d+");
    }

    public void apply() {
        NetworkTableInstance.getDefault().stopClient();
        NetworkTableInstance.getDefault().stopDSClient();

        if(useDS) {
            if(useDefaultPort) {
                NetworkTableInstance.getDefault().startDSClient();
            } else {
                NetworkTableInstance.getDefault().startDSClient(port);
            }
        } else {
            if(addressIsTeamNum()) {
                if(useDefaultPort)
                    NetworkTableInstance.getDefault().setServerTeam(teamNum);
                else
                    NetworkTableInstance.getDefault().setServerTeam(teamNum, port);
            } else {
                if(useDefaultPort) {
                    NetworkTableInstance.getDefault().setServer(address);
                } else {
                    NetworkTableInstance.getDefault().setServer(address, port);
                }
            }
        }

        NetworkTableInstance.getDefault().startClient4("CommmandVisualizer");
    }


}
