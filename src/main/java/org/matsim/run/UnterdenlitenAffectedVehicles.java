package org.matsim.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
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

public class UnterdenlitenAffectedVehicles {
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
    
    private static class BerlinEventHandler implements LinkEnterEventHandler {

        private List<String> linksToWatch = getLinksToModify();



        private BerlinEventHandler() throws IOException {
        }



        public List<String> getLinksToModify() throws IOException {
                //extraction des liens à surveiller
                String linkToModify = "scenarios/berlin-v5.5-1pct/output-berlin-v5.5-1pct/unterlinks.csv";

                String file = linkToModify ; //nom du fichier csv à lire
                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);

                List<String> links = new ArrayList<String>();

                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    links.add(line);
                }
                br.close();
                fr.close();
                for(int i =0 ; i< links.size(); i++){
                    int a = links.get(i).indexOf(';');
                    links.set(i, links.get(i).substring(0, a));
                }
                links.remove(0);
                System.out.println(links);
                return links;
            }



        private int eventcounter = 0;
        private ArrayList<String> affectedVehicles = new ArrayList<>();
        @Override
        public void handleEvent(LinkEnterEvent event) {
            //give all the vehicles entering the watched links, i.e vehicles affected by the implementation of the no-car zone
            //and put it in a text file
            for(String str : linksToWatch ){
                if (event.getLinkId().equals(Id.createLinkId(str))){
                    eventcounter++;
                    Id<Vehicle> vehiculeID = event.getVehicleId();
                    String strVehiculeID = vehiculeID.toString();
//                    System.out.println(strVehiculeID);
//                    System.out.println("Evenement détecté n°"+eventcounter);
//                    System.out.println("LinkEnterEvent by vehicule "+ vehiculeID + " in the link n°"+str+" at "+ event.getTime());

                    if(!affectedVehicles.contains(strVehiculeID)){
                        affectedVehicles.add(strVehiculeID);
                        try {
                            BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(".").toAbsolutePath().normalize().toString()+"/output_original_run/affectedVehicles.txt", true));
                            writer.append(strVehiculeID + "\n");
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

