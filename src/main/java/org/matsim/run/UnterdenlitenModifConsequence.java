package org.matsim.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.vehicles.Vehicle;


import java.beans.EventHandler;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnterdenlitenModifConsequence {
    public static void main(String[] args) throws IOException {
        String inputFile = Paths.get(".").toAbsolutePath().normalize().toString()+"output_dm1/berlin-v5.5-1pct.output_events.xml.gz";
//        String inputFile = Paths.get(".").toAbsolutePath().normalize().toString()+"output_original_run/berlin-v5.5-1pct.output_events.xml.gz";

        EventsManager eventsManager = EventsUtils.createEventsManager();

        BerlinEventHandler eventHandler = new BerlinEventHandler();
        eventsManager.addHandler(eventHandler);

        eventsManager.addHandler(eventHandler);

        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
        eventsReader.readFile(inputFile);

        BerlinEventHandler.fileWriter();


    }

    private static class BerlinEventHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {

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


        private static Map<Id<Person>, Double> departureTimeByPersonMap = new HashMap<>();
        private static Map<Id<Person>, Double> travelTimeByPersonMap = new HashMap<>();

        @Override
        public void handleEvent(PersonDepartureEvent event) {
            //give all the vehicles entering the watched links, i.e vehicles affected by the implementation of the no-car zone
            //and put it in a text file
            for (String str : personsToWatch) {
                if (event.getPersonId().equals(Id.createPersonId(str))) {
                    Id<Person> personId = event.getPersonId();
                    double departureTime = event.getTime();
                    departureTimeByPersonMap.put(personId, departureTime);

                }
            }
        }

        @Override
        public void handleEvent(PersonArrivalEvent event) {
            for (String str : personsToWatch) {
                if (event.getPersonId().equals(Id.createPersonId(str))) {
                    Id<Person> personId = event.getPersonId();
                    double arrivalTime = event.getTime();
                    double travelTime = arrivalTime - departureTimeByPersonMap.get(personId);
                    if (travelTimeByPersonMap.putIfAbsent(personId, travelTime) != null) {
                        travelTimeByPersonMap.put(personId, travelTimeByPersonMap.get(personId) + travelTime);
                    }
                }
            }
        }

        public static void fileWriter(){

            for(Map.Entry<Id<Person>, Double> element : travelTimeByPersonMap.entrySet()){
                try {
//                    BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(".").toAbsolutePath().normalize().toString()+"output_original_run/travelTimeByPerson.txt", true));
                    BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(".").toAbsolutePath().normalize().toString()+"output_dm1/travelTimeByPerson.txt", true));
                    writer.append(element.getKey() + ";" + element.getValue() + "\n");
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }
}

