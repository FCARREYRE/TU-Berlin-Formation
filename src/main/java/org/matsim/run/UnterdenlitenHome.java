package org.matsim.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.scoring.PersonExperiencedActivity;
import org.matsim.vehicles.Vehicle;


import java.beans.EventHandler;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnterdenlitenHome {
    public static void main(String[] args) throws IOException {
//        String inputFile = Paths.get(".").toAbsolutePath().normalize().toString()+"output_dm1/berlin-v5.5-1pct.output_events.xml.gz";
        String inputFile = Paths.get(".").toAbsolutePath().normalize().toString()+"output_original_run/berlin-v5.5-1pct.output_events.xml.gz";

        EventsManager eventsManager = EventsUtils.createEventsManager();

        BerlinEventHandler eventHandler = new BerlinEventHandler();
        eventsManager.addHandler(eventHandler);

        eventsManager.addHandler(eventHandler);

        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
        eventsReader.readFile(inputFile);

        BerlinEventHandler.fileWriter();


    }

    private static class BerlinEventHandler implements ActivityStartEventHandler {

        private List<String> personsToWatch = getPersonsToWatch();

        private BerlinEventHandler() throws IOException {
        }

        public List<String> getPersonsToWatch() throws IOException {
            //extraction des personnes à surveiller
            String affectedPersons = Paths.get(".").toAbsolutePath().normalize().toString()+"output_original_run/affectedPerson.txt";

            String file = affectedPersons; //nom du fichier csv à lire
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);

            List<String> personID = new ArrayList<String>();

            for (String line = br.readLine(); line != null; line = br.readLine()) {
                personID.add(line);
            }
            br.close();
            fr.close();

//            System.out.println(personID);
            return personID;
        }


        private static ArrayList<Id<Link>> homeLocations = new ArrayList<>();
        @Override
        public void handleEvent(ActivityStartEvent event) {
            //give all the vehicles entering the watched links, i.e vehicles affected by the implementation of the no-car zone
            //and put it in a text file
            for (String str : personsToWatch) {
                if (event.getPersonId().equals(Id.createPersonId(str))) {
//                    System.out.println(event.getActType());
                    if(event.getActType().contains("home")){
                        if(!homeLocations.contains(event.getLinkId())) {
                            homeLocations.add(event.getLinkId());
//                            System.out.println(event.getLinkId() + " ajouté à la liste");
                        }
                    }

                }
            }
        }


        public static void fileWriter(){
            System.out.println(homeLocations);

            for(Id<Link> id:homeLocations){
                try {
//                    BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(".").toAbsolutePath().normalize().toString()+"output_original_run/travelTimeByPerson.txt", true));
                    BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(".").toAbsolutePath().normalize().toString()+"output_dm1/homelocations.txt", true));
                    writer.append(id.toString() + "\n");
                    writer.close();
                    System.out.println(id.toString() + "ajouté au fichier");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }
}

