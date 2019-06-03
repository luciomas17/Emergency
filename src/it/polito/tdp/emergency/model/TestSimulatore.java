package it.polito.tdp.emergency.model;

import java.time.Duration;

public class TestSimulatore {

	public static void main(String[] args) {

		Simulatore sim = new Simulatore();
		
		sim.setNS(2);
		// sim.setNS(50);
		sim.setT_ARRIVAL(Duration.ofMinutes(5));
		
		sim.init(); 
		sim.run();
		
		System.out.println("Numero abbandoni: " + sim.getNumAbbandoni());
		System.out.println("Numero dimessi: " + sim.getNumDimessi());
		System.out.println("Numero morti: " + sim.getNumMorti());
		
	}

}
