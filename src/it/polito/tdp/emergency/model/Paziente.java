package it.polito.tdp.emergency.model;

import java.time.LocalTime;

public class Paziente {
	
	public enum StatoPaziente {
		NEW,
		WAITING_TIME,
		WAITING_WHITE,
		WAITING_YELLOW,
		WAITING_RED,
		TREATING,
		OUT,
		BLACK, 
	}

	private int id;
	private StatoPaziente stato;
	private LocalTime oraArrivo;
	
	public Paziente(int id, LocalTime oraArrivo) {
		this.id = id;
		this.stato = StatoPaziente.NEW;
		this.oraArrivo = oraArrivo;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public StatoPaziente getStato() {
		return stato;
	}

	public void setStato(StatoPaziente stato) {
		this.stato = stato;
	}

	public LocalTime getOraArrivo() {
		return oraArrivo;
	}

	public void setOraArrivo(LocalTime oraArrivo) {
		this.oraArrivo = oraArrivo;
	}

	@Override
	public String toString() {
		return String.format("Paziente [id=%s, stato=%s, oraArrivo=%s]", id, stato, oraArrivo);
	}
	
}
