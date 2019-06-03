package it.polito.tdp.emergency.model;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import it.polito.tdp.emergency.model.Evento.TipoEvento;
import it.polito.tdp.emergency.model.Paziente.StatoPaziente;

public class Simulatore {
	
	public class PrioritaPaziente implements Comparator<Paziente> {
		@Override
		public int compare(Paziente p1, Paziente p2) {
			if(p1.getStato() == StatoPaziente.WAITING_RED && p2.getStato() != StatoPaziente.WAITING_RED)
				return -1;
			else if(p1.getStato() != StatoPaziente.WAITING_RED && p2.getStato() == StatoPaziente.WAITING_RED)
				return 1;
			
			else if(p1.getStato() == StatoPaziente.WAITING_YELLOW && p2.getStato() != StatoPaziente.WAITING_YELLOW)
				return -1;
			else if(p1.getStato() != StatoPaziente.WAITING_YELLOW && p2.getStato() == StatoPaziente.WAITING_YELLOW)
				return 1;
			
			return p1.getOraArrivo().compareTo(p2.getOraArrivo());
		}
	}

	// Coda degli eventi
	private PriorityQueue<Evento> queue = new PriorityQueue<>();

	// Modello del Mondo
	private List<Paziente> pazienti;
	private PriorityQueue<Paziente> salaAttesa;
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
	private Duration intervalloPolling = Duration.ofMinutes(5);

	public Simulatore() {
		this.pazienti = new ArrayList<Paziente>();
	}

	public void init() {
		// Creare i pazienti
		LocalTime oraArrivo = T_inizio;
		pazienti.clear();
		for (int i = 0; i < NP; i++) {
			Paziente p = new Paziente(i + 1, oraArrivo);
			pazienti.add(p);

			oraArrivo = oraArrivo.plus(T_ARRIVAL);
		}
		
		// Inizializzare sala d'attesa vuota
		salaAttesa = new PriorityQueue<>(new PrioritaPaziente());

		// Creare gli studi medici
		studiLiberi = NS;
		
		//
		nuovoStatoPaziente = StatoPaziente.WAITING_WHITE;

		// Creare gli eventi iniziali
		queue.clear();
		for (Paziente p : pazienti) {
			queue.add(new Evento(p.getOraArrivo(), TipoEvento.ARRIVO, p));
		}
		
		// Lanciare osservatore in polling
		queue.add(new Evento(T_inizio.plus(intervalloPolling), TipoEvento.POLLING, null));

		// Resettare le statistiche
		numDimessi = 0;
		numAbbandoni = 0;
		numMorti = 0;
	}

	public void run() {

		while(!queue.isEmpty()) {
			Evento ev = queue.poll();
			System.out.println(ev) ;
			Paziente p = ev.getPaziente();

			switch (ev.getTipo()) {

				case ARRIVO:
					queue.add(new Evento(ev.getOra().plusMinutes(DURATION_TRIAGE), TipoEvento.TRIAGE, p));
					
					break;
	
				case TRIAGE:
					p.setStato(nuovoStatoPaziente);
	
					if (p.getStato() == StatoPaziente.WAITING_WHITE)
						queue.add(new Evento(ev.getOra().plusMinutes(TIMEOUT_WHITE), TipoEvento.TIMEOUT, p));
					else if (p.getStato() == StatoPaziente.WAITING_YELLOW)
						queue.add(new Evento(ev.getOra().plusMinutes(TIMEOUT_YELLOW), TipoEvento.TIMEOUT, p));
					else if (p.getStato() == StatoPaziente.WAITING_RED)
						queue.add(new Evento(ev.getOra().plusMinutes(TIMEOUT_RED), TipoEvento.TIMEOUT, p));
	
					salaAttesa.add(p);
					ruotaNuovoStatoPaziente();
	
					break;
	
				case VISITA:
					Paziente pazChiamato = salaAttesa.poll();
					if(pazChiamato == null)
						break;
					
					StatoPaziente vecchioStato = pazChiamato.getStato();
					pazChiamato.setStato(StatoPaziente.TREATING);
					
					studiLiberi--;
					
					if(vecchioStato == StatoPaziente.WAITING_RED)
						queue.add(new Evento(ev.getOra().plusMinutes(DURATION_RED), TipoEvento.CURATO, pazChiamato));
					else if(vecchioStato == StatoPaziente.WAITING_YELLOW)
						queue.add(new Evento(ev.getOra().plusMinutes(DURATION_YELLOW), TipoEvento.CURATO, pazChiamato));
					else if(vecchioStato == StatoPaziente.WAITING_WHITE)
						queue.add(new Evento(ev.getOra().plusMinutes(DURATION_WHITE), TipoEvento.CURATO, pazChiamato));

					break;
	
				case CURATO:
					p.setStato(StatoPaziente.OUT);
					
					numDimessi++;
					
					studiLiberi++;
					queue.add(new Evento(ev.getOra(), TipoEvento.VISITA, null));
					
					break;
	
				case TIMEOUT:
					salaAttesa.remove(p);
					
					if (p.getStato() == StatoPaziente.WAITING_WHITE) {
						p.setStato(StatoPaziente.OUT);
						numAbbandoni++ ;
					} else if (p.getStato() == StatoPaziente.WAITING_YELLOW) {
						p.setStato(StatoPaziente.WAITING_RED);
						queue.add(new Evento(ev.getOra().plusMinutes(TIMEOUT_RED), TipoEvento.TIMEOUT, p));
						salaAttesa.add(p);
					} else if (p.getStato() == StatoPaziente.WAITING_RED) {
						p.setStato(StatoPaziente.BLACK);
						numMorti++ ;
					} else
						System.out.println("Timeout anomalo nello stato " + p.getStato());
	
					break;
					
				case POLLING:
					if(!salaAttesa.isEmpty() && studiLiberi > 0)
						queue.add(new Evento(ev.getOra(), TipoEvento.VISITA, null));
					
					if(ev.getOra().isBefore(T_fine))
						queue.add(new Evento(ev.getOra().plus(intervalloPolling), TipoEvento.POLLING, null));
					
					break;
			}

		}

	}

	private void ruotaNuovoStatoPaziente() {
		if(nuovoStatoPaziente == StatoPaziente.WAITING_WHITE)
			nuovoStatoPaziente = StatoPaziente.WAITING_YELLOW;
		else if(nuovoStatoPaziente == StatoPaziente.WAITING_YELLOW)
			nuovoStatoPaziente = StatoPaziente.WAITING_RED;
		else if(nuovoStatoPaziente == StatoPaziente.WAITING_RED)
			nuovoStatoPaziente = StatoPaziente.WAITING_WHITE;
	}

	public void setNS(int nS) {
		NS = nS;
	}

	public void setNP(int nP) {
		NP = nP;
	}

	public void setT_ARRIVAL(Duration t_ARRIVAL) {
		T_ARRIVAL = t_ARRIVAL;
	}

	public int getNumDimessi() {
		return numDimessi;
	}

	public int getNumAbbandoni() {
		return numAbbandoni;
	}

	public int getNumMorti() {
		return numMorti;
	}
	
}
