package org.matsim.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.vehicles.Vehicle;
import scala.math.Ordering;


import java.beans.EventHandler;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UnterdenlitenAlternativeAxis{
    public static void main(String[] args) throws IOException {
//        String inputFile = Paths.get(".").toAbsolutePath().normalize().toString()+"output_dm3/berlin-v5.5-1pct.output_events.xml.gz";
        String inputFile = Paths.get(".").toAbsolutePath().normalize().toString()+"output_dm1n/berlin-v5.5-1pct.output_events.xml.gz";

        EventsManager eventsManager =  EventsUtils.createEventsManager();

        BerlinEventHandler eventHandler = new BerlinEventHandler();
        eventsManager.addHandler(eventHandler);

        eventsManager.addHandler(eventHandler);

        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
        eventsReader.readFile(inputFile);

        BerlinEventHandler.fileWriter();


    }

    private static class BerlinEventHandler implements LinkEnterEventHandler {

        private ArrayList<String> linksToWatch = linksList();
        private static int[][] enterLinkCounter = new int[8][48];

        private ArrayList<String> linksList() {

            ArrayList<String> list = new ArrayList<>();
            list.add("37616");
            list.add("137065");
            list.add("37625");
            list.add("37713");
            list.add("52000");
            list.add("67581");
            list.add("13443");
            list.add("87225");
            return(list);
        }





        private BerlinEventHandler() throws IOException {
        }


        @Override
        public void handleEvent(LinkEnterEvent event) {
            for(String str:linksToWatch){
                if(event.getLinkId().equals(Id.createLinkId(str))){
                    int linkNumber = linksToWatch.indexOf(str);
                    int time = (int)Math.floor(event.getTime()/3600);
                    enterLinkCounter[linkNumber][time]+=1;
//                    System.out.println(enterLinkCounter[0][1]);

                }

            }

        }

        public static void fileWriter(){

            for(int i=0; i< 48; i++ ){
                String strEntries = "";
                for(int j=0; j<8; j++){
                    strEntries += (enterLinkCounter[j][i]+";");
                }
                System.out.println(strEntries+ "\n");
                try {
//
                    BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(".").toAbsolutePath().normalize().toString()+"output_dm1/number_of_entries.txt", true));
                    writer.append((strEntries + "\n"));
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }


    }

}

