package org.matsim.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class NetworkModifierUnterDenLiten {
    public static void main(String[] args) throws FileNotFoundException {

        //fichiers d'input et d'output
        Path inputNetwork = Paths.get("output_dm/berlin-v5.5-1pct.output_network.xml.gz");
        Path outputNetwork = Paths.get("output_dm/berlin-v5.5-1pct.output_networkmodified.xml.gz");

        //Création du network
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(inputNetwork.toString());

        //Création du set de modes à mettre
        TreeSet<String> set = new TreeSet<String>();
        set.add("walk");
        set.add("pt");



        //extraction du fichier excel et mise dans une liste
        List<String> result = new ArrayList<String>();
        String file = "scenarios/berlin-v5.5-1pct/output-berlin-v5.5-1pct/unterlinks.csv"; //nom du fichier csv à lire
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);

        try {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                result.add(line);
            }
            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        //extraction des ID des links
        for(int i =0 ; i< result.size(); i++){
            int a = result.get(i).indexOf(';');
            result.set(i, result.get(i).substring(0, a));
        }
        result.remove(0);
        System.out.println(result);


//        // liste de test pour le petit réseau d'essai
//        List<String> result1 = new ArrayList<String>();
//        result1.add("1");
//        result1.add("2");
//        result1.add("3");
//        System.out.println(result1);

        //Modification des links
        for(String str:result){
            network.getLinks().get(Id.createLinkId(str)).setAllowedModes(set);

        }


        //ecriture du fichier
        new NetworkWriter(network).write(outputNetwork.toString());


    }
}
