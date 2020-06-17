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


import java.beans.EventHandler;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class UnterdenlitenAffectedPersons {
    public static void main(String[] args) throws IOException {
//        String inputFile = Paths.get(".").toAbsolutePath().normalize().toString()+"/output_dm3/berlin-v5.5-1pct.output_events.xml.gz";
        String inputFile = Paths.get(".").toAbsolutePath().normalize().toString()+"/output_original_run/berlin-v5.5-1pct.output_events.xml.gz";

        EventsManager eventsManager =  EventsUtils.createEventsManager();

        BerlinEventHandler eventHandler = new BerlinEventHandler();
        eventsManager.addHandler(eventHandler);

        eventsManager.addHandler(eventHandler);

        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
        eventsReader.readFile(inputFile);


    }

    private static class BerlinEventHandler implements VehicleEntersTrafficEventHandler {

        private List<String> vehiclesToWatch = getVehiclesToWatch();

        private BerlinEventHandler() throws IOException {
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



        private int eventcounter = 0;
        private ArrayList<String> affectedPersons = new ArrayList<>();
        @Override
        public void handleEvent(VehicleEntersTrafficEvent event) {
            //give all the vehicles entering the watched links, i.e vehicles affected by the implementation of the no-car zone
            //and put it in a text file
            for(String str : vehiclesToWatch ){
                if (event.getVehicleId().equals(Id.createVehicleId(str))){
                    Id<Person> personId = event.getPersonId();
                    String strPersonId = personId.toString();

                    if(!affectedPersons.contains(strPersonId)){
                        affectedPersons.add(strPersonId);
                        try {
                            BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(".").toAbsolutePath().normalize().toString()+"/output_original_run/affectedPerson.txt", true));
                            writer.append(strPersonId + "\n");
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }


                }
            }
        }





    }

}

