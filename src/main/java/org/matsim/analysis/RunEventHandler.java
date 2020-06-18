package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.*;
import org.matsim.core.network.io.MatsimNetworkReader;


import java.beans.EventHandler;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.nio.file.Paths;

public class RunEventHandler{

    public static void main(String[] args) {
        String inputFile = Paths.get(".").toAbsolutePath().normalize().toString()+"/output_dm1/berlin-v5.5-1pct.output_events.xml.gz";
        System.out.println(inputFile);
//        String inputFile = Paths.get(".").toAbsolutePath().normalize().toString()+"/output_original_run/berlin-v5.5-1pct.output_events.xml.gz";

        Network network;
        Path inputNetwork = Paths.get(Paths.get(".").toAbsolutePath().normalize().toString()+"/output_dm/berlin-v5.5-1pct.output_network.xml.gz");
        network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(inputNetwork.toString());

        EventsManager eventsManager = EventsUtils.createEventsManager();

        CongestionDetectionEventHandler eventHandler = new CongestionDetectionEventHandler(network);

        eventsManager.addHandler(eventHandler);

        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
        eventsReader.readFile(inputFile);

    }

}
