package it.polito.tdp.emergency.model;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import it.polito.tdp.emergency.model.Evento.TipoEvento;
import it.polito.tdp.emergency.model.Paziente.StatoPaziente;

public class Simulatore {

	// Coda degli eventi
	private PriorityQueue<Evento> queue = new PriorityQueue<>() ;
	
	// Modello del Mondo
	private List<Paziente> pazienti;
	private int studiLiberi;
	
	// Parametri di simulazione
	private int NS = 3; // numero di studi medici
	private int NP = 50; // numero di pazienti in arrivo
	private Duration T_ARRIVAL = Duration.ofMinutes(15); // intervallo di tempo tra i pazienti

	private LocalTime T_inizio = LocalTime.of(8, 0);
	private LocalTime T_fine = LocalTime.of(20, 0);

	private int DURATION_TRIAGE = 5;
	private int DURATION_WHITE = 10;
	private int DURATION_YELLOW = 15;
	private int DURATION_RED = 30;
	private int TIMEOUT_WHITE = 120;
	private int TIMEOUT_YELLOW = 60;
	private int TIMEOUT_RED = 90;

	
	// Statistiche da calcolare
	private int numDimessi;
	private int numAbbandoni;
	private int numMorti;
	
	// Variabili interne
	private StatoPaziente nuovoStatoPaziente;
	
	public Simulatore() {
		this.pazienti = new ArrayList<>();
	}
	
	public void init() {
		// Creare i pazienti
		pazienti.clear();
		LocalTime oraArrivo = T_inizio;
		for(int i=0; i<NP; i++) {
			int id = i+1;
			Paziente p = new Paziente(id, oraArrivo);
			pazienti.add(p);
			oraArrivo = oraArrivo.plus(T_ARRIVAL);
		}
		
		// Creare gli studi medici
		studiLiberi = NS;
		
		nuovoStatoPaziente = StatoPaziente.WAITING_WHITE;
		
		// Creare gli eventi iniziali
		queue.clear();
		for(Paziente p : pazienti) {
			queue.add(new Evento(p.getOraArrivo(), TipoEvento.ARRIVO, p));
		}
		
		// Resettare le statistiche
		numDimessi = 0;
		numAbbandoni = 0;
		numMorti = 0;
	}
	
	public void run() {
		while(queue.isEmpty()) {
			Evento ev = queue.poll();
			System.out.println(ev);
			Paziente p = ev.getPaziente();
		
			switch(ev.getTipo()) {	
				case ARRIVO:
					queue.add(new Evento(ev.getOra().plusMinutes(DURATION_TRIAGE), TipoEvento.TRIAGE, p));
					
					break;
					
				case TRIAGE:
					p.setStato(nuovoStatoPaziente);
					
					if(p.getStato() == StatoPaziente.WAITING_WHITE)
						queue.add(new Evento(ev.getOra().plusMinutes(TIMEOUT_WHITE), TipoEvento.TIMEOUT, p));
					else if(p.getStato() == StatoPaziente.WAITING_YELLOW)
						queue.add(new Evento(ev.getOra().plusMinutes(TIMEOUT_YELLOW), TipoEvento.TIMEOUT, p));
					else if(p.getStato() == StatoPaziente.WAITING_RED)
						queue.add(new Evento(ev.getOra().plusMinutes(TIMEOUT_RED), TipoEvento.TIMEOUT, p));
					
					ruotaNuovoStatoPazione();
					
					break;
				
				case VISITA:
					
					break;
				
				case CURATO:
					
					break;
					
				case TIMEOUT:
					if(p.getStato() == StatoPaziente.WAITING_WHITE) {
						p.setStato(StatoPaziente.OUT);
						numAbbandoni++;
						
					} else if(p.getStato() == StatoPaziente.WAITING_YELLOW) {
						p.setStato(StatoPaziente.WAITING_RED);
						queue.add(new Evento(ev.getOra().plusMinutes(TIMEOUT_RED), TipoEvento.TIMEOUT, p));
						
					} else if(p.getStato() == StatoPaziente.WAITING_RED) {
						p.setStato(StatoPaziente.BLACK);
						numMorti++;
						
					} else
						System.out.println("Timeout anomalo nello stato " + p.getStato());
					
					break;
			}
		}
	}

	private void ruotaNuovoStatoPazione() {
		if(nuovoStatoPaziente == StatoPaziente.WAITING_TIME)
			nuovoStatoPaziente = StatoPaziente.WAITING_YELLOW;
		else if(nuovoStatoPaziente == StatoPaziente.WAITING_YELLOW)
			nuovoStatoPaziente = StatoPaziente.WAITING_RED;
		else if(nuovoStatoPaziente == StatoPaziente.WAITING_RED)
			nuovoStatoPaziente = StatoPaziente.WAITING_WHITE;
	}
	
}
