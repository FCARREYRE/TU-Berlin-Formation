/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.run;

import static org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.analysis.RunPersonTripAnalysis;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.run.drt.OpenBerlinIntermodalPtDrtRouterModeIdentifier;
import org.matsim.run.drt.RunDrtOpenBerlinScenario;
import org.matsim.run.singleTripStrategies.ChangeSingleTripModeAndRoute;
import org.matsim.run.singleTripStrategies.RandomSingleTripReRoute;

import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

/**
* @author ikaddoura
*/

public final class RunBerlinScenario {

	private static final Logger log = Logger.getLogger(RunBerlinScenario.class );

	public static void main(String[] args) throws FileNotFoundException {
		
		for (String arg : args) {
			log.info( arg );
		}
		
		if ( args.length==0 ) {
			args = new String[] {"scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml"}  ;
		}

		Config config = prepareConfig( args ) ;
		config.controler().setLastIteration(1);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory("output_original_run");

		//modification du réseau :
		String inputNetworkName = "output_dm/berlin-v5.5-1pct.output_network.xml.gz";
		String outputNetworkName = "output_dm/berlin-v5.5-1pct.output_networkmodified.xml.gz";
		String linkToModify = "scenarios/berlin-v5.5-1pct/output-berlin-v5.5-1pct/unterlinks.csv";
		NetworkModifier.networkModifier(inputNetworkName, outputNetworkName, linkToModify );
		config.network().setInputFile("output_dm/berlin-v5.5-1pct.output_networkmodified.xml.gz");

		Scenario scenario = prepareScenario( config ) ;
		Controler controler = prepareControler( scenario ) ;










		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						((Activity) pe).setLinkId(null);
					} else if (pe instanceof Leg) {
						((Leg) pe).setRoute(null);
//                        ((Leg) pe).setMode("walk"); //marche pas, il y a pas d'agents
					} else {
						throw new RuntimeException("Plan element can either be activity or leg.");
					}
				}
			}
		}








		controler.run() ;

	}

	private static class NetworkModifier{
		public static void networkModifier(String inputNetworkName, String outputNetworkName, String linkToModify ) throws FileNotFoundException {
			//fichiers d'input et d'output. Attention, il faut faire tourner une fois le code sans la fonction pour avoir des fichiers d'output (j'ai pas réussi à les récupérer du svn)
			Path inputNetwork = Paths.get(inputNetworkName);
			Path outputNetwork = Paths.get(outputNetworkName);
			//Création du network
			Network network = NetworkUtils.createNetwork();
			new MatsimNetworkReader(network).readFile(inputNetwork.toString());
			//Création du set de modes à mettre
			TreeSet<String> set = new TreeSet<String>();
			set.add("walk");
//			set.add("pt");

			//extraction du fichier csv et mise dans une liste
			List<String> result = new ArrayList<String>();
			String file = linkToModify ; //nom du fichier csv à lire
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

			//Modification des links
			for(String str:result){
				network.getLinks().get(Id.createLinkId(str)).setAllowedModes(set);
			}
			//ecriture du fichier
			new NetworkWriter(network).write(outputNetwork.toString());
		}
	}

	public static Controler prepareControler( Scenario scenario ) {
		// note that for something like signals, and presumably drt, one needs the controler object
		
		Gbl.assertNotNull(scenario);
		
		final Controler controler = new Controler( scenario );
		
		if (controler.getConfig().transit().isUseTransit()) {
			// use the sbb pt raptor router
			controler.addOverridingModule( new AbstractModule() {
				@Override
				public void install() {
					install( new SwissRailRaptorModule() );
				}
			} );
		} else {
			log.warn("Public transit will be teleported and not simulated in the mobsim! "
					+ "This will have a significant effect on pt-related parameters (travel times, modal split, and so on). "
					+ "Should only be used for testing or car-focused studies with a fixed modal split.  ");
		}
		
		
		
		// use the (congested) car travel time for the teleported ride mode
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding( TransportMode.ride ).to( networkTravelTime() );
				addTravelDisutilityFactoryBinding( TransportMode.ride ).to( carTravelDisutilityFactoryKey() );
				bind(AnalysisMainModeIdentifier.class).to(OpenBerlinIntermodalPtDrtRouterModeIdentifier.class);
				
				addPlanStrategyBinding("RandomSingleTripReRoute").toProvider(RandomSingleTripReRoute.class);
				addPlanStrategyBinding("ChangeSingleTripModeAndRoute").toProvider(ChangeSingleTripModeAndRoute.class);

				bind(RaptorIntermodalAccessEgress.class).to(BerlinRaptorIntermodalAccessEgress.class);
			}
		} );

		return controler;
	}
	
	public static Scenario prepareScenario( Config config ) {
		Gbl.assertNotNull( config );
		
		// note that the path for this is different when run from GUI (path of original config) vs.
		// when run from command line/IDE (java root).  :-(    See comment in method.  kai, jul'18
		// yy Does this comment still apply?  kai, jul'19

		/*
		 * We need to set the DrtRouteFactory before loading the scenario. Otherwise DrtRoutes in input plans are loaded
		 * as GenericRouteImpls and will later cause exceptions in DrtRequestCreator. So we do this here, although this
		 * class is also used for runs without drt.
		 */
		final Scenario scenario = ScenarioUtils.createScenario( config );

		RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
		routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
		
		ScenarioUtils.loadScenario(scenario);

		BerlinExperimentalConfigGroup berlinCfg = ConfigUtils.addOrGetModule(config, BerlinExperimentalConfigGroup.class);
		if (berlinCfg.getPopulationDownsampleFactor() != 1.0) {
			downsample(scenario.getPopulation().getPersons(), berlinCfg.getPopulationDownsampleFactor());
		}
		
		return scenario;
	}

	public static Config prepareConfig( String [] args, ConfigGroup... customModules ){
		return prepareConfig( RunDrtOpenBerlinScenario.AdditionalInformation.none, args, customModules ) ;
	}
	public static Config prepareConfig( RunDrtOpenBerlinScenario.AdditionalInformation additionalInformation, String [] args,
					    ConfigGroup... customModules ) {
		OutputDirectoryLogging.catchLogEntries();
		
		String[] typedArgs = Arrays.copyOfRange( args, 1, args.length );
		
		ConfigGroup[] customModulesToAdd = null ;
		if ( additionalInformation== RunDrtOpenBerlinScenario.AdditionalInformation.acceptUnknownParamsBerlinConfig ) {
			customModulesToAdd = new ConfigGroup[]{ new BerlinExperimentalConfigGroup(true) };
		} else {
			customModulesToAdd = new ConfigGroup[]{ new BerlinExperimentalConfigGroup(false) };
		}
		ConfigGroup[] customModulesAll = new ConfigGroup[customModules.length + customModulesToAdd.length];
		
		int counter = 0;
		for (ConfigGroup customModule : customModules) {
			customModulesAll[counter] = customModule;
			counter++;
		}
		
		for (ConfigGroup customModule : customModulesToAdd) {
			customModulesAll[counter] = customModule;
			counter++;
		}
		
		final Config config = ConfigUtils.loadConfig( args[ 0 ], customModulesAll );
		
		config.controler().setRoutingAlgorithmType( FastAStarLandmarks );
		
		config.subtourModeChoice().setProbaForRandomSingleTripMode( 0.5 );
		
		config.plansCalcRoute().setRoutingRandomness( 3. );
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.ride);
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.pt);
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.bike);
		config.plansCalcRoute().removeModeRoutingParams("undefined");
		
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles( true );
				
		// vsp defaults
		config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.VspDefaultsCheckingLevel.info );
		config.plansCalcRoute().setInsertingAccessEgressWalk( true );
		config.qsim().setUsingTravelTimeCheckInTeleportation( true );
		config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );
				
		// activities:
		for ( long ii = 600 ; ii <= 97200; ii+=600 ) {
			config.planCalcScore().addActivityParams( new ActivityParams( "home_" + ii + ".0" ).setTypicalDuration( ii ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "work_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(6. * 3600. ).setClosingTime(20. * 3600. ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "leisure_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(9. * 3600. ).setClosingTime(27. * 3600. ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "shopping_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(8. * 3600. ).setClosingTime(20. * 3600. ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "other_" + ii + ".0" ).setTypicalDuration( ii ) );
		}
		config.planCalcScore().addActivityParams( new ActivityParams( "freight" ).setTypicalDuration( 12.*3600. ) );

		ConfigUtils.applyCommandline( config, typedArgs ) ;

		return config ;
	}
	
	public static void runAnalysis(Controler controler) {
		Config config = controler.getConfig();
		
		String modesString = "";
		for (String mode: config.planCalcScore().getAllModes()) {
			modesString = modesString + mode + ",";
		}
		// remove last ","
		if (modesString.length() < 2) {
			log.error("no valid mode found");
			modesString = null;
		} else {
			modesString = modesString.substring(0, modesString.length() - 1);
		}
		
		String[] args = new String[] {
				config.controler().getOutputDirectory(),
				config.controler().getRunId(),
				"null", // TODO: reference run, hard to automate
				"null", // TODO: reference run, hard to automate
				config.global().getCoordinateSystem(),
				"https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-bezirke/bezirke_berlin.shp",
				TransformationFactory.DHDN_GK4,
				"SCHLUESSEL",
				"home",
				"10", // TODO: scaling factor, should be 10 for 10pct scenario and 100 for 1pct scenario
				"null", // visualizationScriptInputDirectory
				modesString
		};
		
		try {
			RunPersonTripAnalysis.main(args);
		} catch (IOException e) {
			log.error(e.getStackTrace());
			throw new RuntimeException(e.getMessage());
		}
	}
	
	private static void downsample( final Map<Id<Person>, ? extends Person> map, final double sample ) {
		final Random rnd = MatsimRandom.getLocalInstance();
		log.warn( "Population downsampled from " + map.size() + " agents." ) ;
		map.values().removeIf( person -> rnd.nextDouble() > sample ) ;
		log.warn( "Population downsampled to " + map.size() + " agents." ) ;
	}

}

