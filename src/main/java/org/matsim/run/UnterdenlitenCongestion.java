package org.matsim.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scoring.PersonExperiencedActivity;
import org.matsim.vehicles.Vehicle;


import java.beans.EventHandler;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class UnterdenlitenCongestion {
    public static void main(String[] args) throws IOException {
//        String inputFile = Paths.get(".").toAbsolutePath().normalize().toString()+"/output_dm1/berlin-v5.5-1pct.output_events.xml.gz";
        String inputFile = Paths.get(".").toAbsolutePath().normalize().toString() + "/output_original_run_30it/berlin-v5.5-1pct.output_events.xml.gz";

        //création du network
        Network network;
        Path inputNetwork = Paths.get(Paths.get(".").toAbsolutePath().normalize().toString()+"/output_dm1/berlin-v5.5-1pct.output_network.xml.gz");
        network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(inputNetwork.toString());

        EventsManager eventsManager = EventsUtils.createEventsManager();

        BerlinEventHandler eventHandler = new BerlinEventHandler(network);

        eventsManager.addHandler(eventHandler);

        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
        eventsReader.readFile(inputFile);

        BerlinEventHandler.addCongestionAttribute(network);

        Path outputNetwork = Paths.get(
                Paths.get(".").toAbsolutePath().normalize().toString()+"/output_original_run_30it/berlin-v5.5-1pct.output_networkmodified_with_congestion.xml.gz");

        new NetworkWriter(network).write(outputNetwork.toString());


        BerlinEventHandler.fileWriter();


    }



    private static class BerlinEventHandler implements LinkEnterEventHandler,
            LinkLeaveEventHandler{

        private Map<Id<Vehicle>,Double> earliestLinkExitTime = new HashMap<>();
        private Network network;
        private static Map<Id<Vehicle>, Double> timeLostByVehicle = new HashMap<>();
        private static Map<Id<Link>, Double> timeLostByLink = new HashMap<>();
        private static Map<Id<Link>, Integer> enterEventCounter = new HashMap<>();


        public BerlinEventHandler( Network network ) {
            this.network = network ;
        }

        public void handleEvent(LinkEnterEvent event){
//            System.out.println("LinkEnterEvent détecté véhicule n°" + event.getVehicleId()
//                    + "sur le link n°"+ event.getLinkId());
            Link link = network.getLinks().get( event.getLinkId() ) ;
            double linkTravelTime = link.getLength() / link.getFreespeed( event.getTime() );
            this.earliestLinkExitTime.put( event.getVehicleId(), event.getTime() + linkTravelTime );
//            System.out.println("Temps de trajet min prévu: " + earliestLinkExitTime);

            Id<Link> linkId = event.getLinkId();
            if(enterEventCounter.putIfAbsent(linkId, 1) != null){
                enterEventCounter.put(linkId, enterEventCounter.get(linkId)+1);
//                System.out.println(enterEventCounter);
            }

        }

        public void handleEvent(LinkLeaveEvent event){
//            System.out.println("LinkLeaveEvent détecté véhicule n°"+ event.getVehicleId()
//                    + "sur le link n°"+ event.getLinkId());
            double leaveTime = event.getTime();
            Id<Vehicle> vehicleId = event.getVehicleId();
            Id<Link> linkId = event.getLinkId();
            try {
                double excessTravelTime = leaveTime - this.earliestLinkExitTime.get(vehicleId);
//                System.out.println("excess travel time: " + excessTravelTime);

                if(timeLostByVehicle.putIfAbsent(vehicleId, excessTravelTime) != null){
                    timeLostByVehicle.put(vehicleId, timeLostByVehicle.get(vehicleId)+excessTravelTime);
                }
                if(timeLostByLink.putIfAbsent(linkId, excessTravelTime) != null){
                    timeLostByLink.put(linkId, timeLostByLink.get(linkId)+excessTravelTime);
                }
            }catch (Exception e){
//                System.out.println("Départ du véhicule n°" + vehicleId);
            }

        }

        public static void fileWriter() throws IOException {
//            System.out.println(timeLostByLink);
//            System.out.println(timeLostByVehicle);

//          BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(".").toAbsolutePath().normalize().toString()+"/output_original_run/travelTimeByPerson.txt", true));
            BufferedWriter writerLink = new BufferedWriter(new FileWriter(
                    Paths.get(".").toAbsolutePath().normalize().toString()+"/output_dm1/timeLostByLink.txt", true));
            for(Map.Entry<Id<Link>, Double> element : timeLostByLink.entrySet()){
                try {
                    writerLink.append(element.getKey() + ";" + element.getValue() + ";" + enterEventCounter.get(element.getKey()) + "\n");

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            writerLink.close();

//          BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(".").toAbsolutePath().normalize().toString()+"/output_original_run/travelTimeByPerson.txt", true));
            BufferedWriter writerVehicle = new BufferedWriter(new FileWriter(
                    Paths.get(".").toAbsolutePath().normalize().toString()+"/output_dm1/timeLostByVehicle.txt", true));
            for(Map.Entry<Id<Vehicle>, Double> element : timeLostByVehicle.entrySet()){
                try {
                    writerVehicle.append(element.getKey() + ";" + element.getValue() + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            writerVehicle.close();
        }

        public static void addCongestionAttribute(Network network){
            for(Map.Entry<Id<Link>, Double> element : timeLostByLink.entrySet()){
                Link linkToModify = network.getLinks().get(element.getKey());
                linkToModify.getAttributes().putAttribute("congestionTime", element.getValue());
                linkToModify.getAttributes().putAttribute("congestionTimeByPerson",
                        element.getValue()/((double)enterEventCounter.get(element.getKey())));
            }



        }




    }
}