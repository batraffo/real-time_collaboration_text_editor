package Turing;

import java.util.ArrayList;

/**
 * 
 * @author Raffaele Ariano
 * 
 * classe utile per tener traccia delle associazioni tra utenti e documenti
 *
 */

public class Utente {
	
	String nome;
	ArrayList<Documento> documenti;
	
	public Utente(String nome) {
		this.nome=nome;
		documenti=new ArrayList<Documento>();
	}
	
	public void adDocumento(Documento d) {
		documenti.add(d);
	}
	
	public String getUtente() {
		return nome;
	}
	
	/**
	 * metodo utilizzato per creare la stringa per l'operazione list
	 */
	
	public String getLista() {
		String stringa="";
		for(Documento i : documenti) {
			stringa=stringa+i.list()+"\n\n" ;
		}
		return stringa;
	}
	
}
