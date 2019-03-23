package Turing;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author Raffaele Ariano
 *	
 * questa classe implementa l'interfaccia RMI
 * e contiene funzioni che permettono il controllo sugli utenti registrati 
 */

public class UtentiRegistrati extends UnicastRemoteObject implements Interfaccia {
	
	private static final long serialVersionUID = 1L;
	private ConcurrentHashMap<String,String> utenti_password;//map tra utenti e pw
	private ArrayList<String> utentiloggati;//stringa con tutti gli utenti attualmente online
	private ConcurrentHashMap<String,Utente> listaUtenti;//arraylist di classe utente
	
	public UtentiRegistrati() throws RemoteException {
		utenti_password=new ConcurrentHashMap<String,String>();
		utentiloggati=new ArrayList<String>();
		listaUtenti=new ConcurrentHashMap<String,Utente>();
	}

	/**
	 * metodo per registrare un utente al server
	 */
	
	@Override
	public Boolean register(String nome, String pw) {
		String controllo=utenti_password.putIfAbsent(nome, pw);
		if(controllo!=null) {
			return false;
		}
		listaUtenti.putIfAbsent(nome,new Utente(nome));
		System.out.println("ho inserito "+nome);
		return true;
	}
	
	public boolean isregistrato(String utente) {
		return utenti_password.containsKey(utente);
	}
	
	/**
	 * qui attuo controlli per verificare che la pw sia corretta
	 * praticamente il login mi è consentito se:
	 * mi son registrato, la password è corretta e se non sono già loggato
	 */
	
	public Boolean logger(String nome,String pw) {
		if(utenti_password.containsKey(nome) && (utenti_password.get(nome)).equals(pw) && pw !=null && !utentiloggati.contains(nome)) {
			utentiloggati.add(nome);
			return true;
		}
		return false;
	}
	
	public boolean is_loggato(String nome) {
		return utentiloggati.contains(nome);
	}
	
	/**
	 * cerco l'utente nome e ritorno la stringa per l'operazione list
	 */
	
	public String getLista(String nome) {
		String ciao="";
		Utente u;
		try {
			u=listaUtenti.get(nome);
		}
		catch(NullPointerException e) {
			System.out.println("strano");
			return "error";
		}
		ciao="ecco tutti i documenti che puoi modificare:\n" + u.getLista();
		return ciao;
	}
	
	/**
	 * aggiungo un documento associato all'utente
	 */
	
	public boolean addDocumento(String nome,Documento d) {
		boolean inserito=false;
		Utente u;
		try {
			u=listaUtenti.get(nome);
		}
		catch(NullPointerException e) {
			return inserito;
		}
		if(u.getUtente().equals(nome)) {
			u.adDocumento(d);
			inserito=true;
		}
		return inserito;
	}
	
	public void delogger(String nome) {
		utentiloggati.remove(nome);
	}
	
}
