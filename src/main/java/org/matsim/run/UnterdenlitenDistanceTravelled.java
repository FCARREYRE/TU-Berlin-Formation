package org.matsim.run;

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
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;


import java.beans.EventHandler;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnterdenlitenDistanceTravelled{
    public static void main(String[] args) throws IOException {
        String inputFile = Paths.get(".").toAbsolutePath().normalize().toString()+"/output_dm1/berlin-v5.5-1pct.output_events.xml.gz";
//        String inputFile = Paths.get(".").toAbsolutePath().normalize().toString()+"/output_original_run/berlin-v5.5-1pct.output_events.xml.gz";

        EventsManager eventsManager = EventsUtils.createEventsManager();

        BerlinEventHandler eventHandler = new BerlinEventHandler();
        eventsManager.addHandler(eventHandler);

        eventsManager.addHandler(eventHandler);

        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
        eventsReader.readFile(inputFile);

        BerlinEventHandler.fileWriter();


    }

    private static class BerlinEventHandler implements LinkEnterEventHandler {

        private List<String> vehiclesToWatch = getVehiclesToWatch();
        private Network network;

        private BerlinEventHandler() throws IOException {
            Path inputNetwork = Paths.get(Paths.get(".").toAbsolutePath().normalize().toString()+"/output_dm/berlin-v5.5-1pct.output_network.xml.gz");
            network = NetworkUtils.createNetwork();
            new MatsimNetworkReader(network).readFile(inputNetwork.toString());


        }

        public List<String> getVehiclesToWatch() throws IOException {
            //extraction des véhicules à surveiller
            String affectedVehicles = Paths.get(".").toAbsolutePath().normalize().toString()+"/output_original_run/affectedVehicles.txt";

            String file = affectedVehicles ; //nom du fichier csv à lire
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);

            List<String> vehicleID = new ArrayList<String>();

            for (String line = br.readLine(); line != null; line = br.readLine()) {
                vehicleID.add(line);
            }
            br.close();
            fr.close();

            System.out.println(vehicleID);
            return vehicleID;
        }

        //Network pour récupérer les links

        private static Path inputNetwork = Paths.get(Paths.get(".").toAbsolutePath().normalize().toString()+"/output_dm/berlin-v5.5-1pct.output_network.xml.gz");
//        Config config;
//        String strInputNetwork = inputNetwork.toString();
//        Scenario scenario = ScenarioUtils.createScenario(config);
//        Network network = NetworkUtils.createNetwork();
//        Network network = NetworkUtils.createNetwork();
//        network = returnNetwork(network, inputNetwork);
//        public Network returnNetwork(Network network, Path inputNetwork){
//            new MatsimNetworkReader(network).readFile(inputNetwork.toString());
//            return network;
//        }

//        MatsimNetworkReader reader = new MatsimNetworkReader(network);
//        reader.readFile(strInputNetwork);

//        MatsimNetworkReader(network).readFile(inputNetwork.toString());


        private static Map<Id<Vehicle>, Double> travelledDistanceByPerson = new HashMap<>();



        @Override
        public void handleEvent(LinkEnterEvent event) {
            for (String str : vehiclesToWatch) {
                if (event.getVehicleId().equals(Id.createPersonId(str))) {
                    Id<Vehicle> vehicleId = event.getVehicleId();
                    Id<Link> linkId = event.getLinkId();
                    double linkLenght = network.getLinks().get(linkId).getLength();
                    if (travelledDistanceByPerson.putIfAbsent(vehicleId, linkLenght) != null) {
                        travelledDistanceByPerson.put(vehicleId, travelledDistanceByPerson.get(vehicleId) + linkLenght);
                    }
                }
            }
        }

        public static void fileWriter(){

            for(Map.Entry<Id<Vehicle>, Double> element : travelledDistanceByPerson.entrySet()){
                try {
//                    BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(".").toAbsolutePath().normalize().toString()+"/output_original_run/travelDistanceByPerson.txt", true));
                    BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(".").toAbsolutePath().normalize().toString()+"/output_dm1/travelDistanceByPerson.txt", true));
                    writer.append(element.getKey() + ";" + element.getValue() + "\n");
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }
}

