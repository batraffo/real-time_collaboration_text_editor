package Turing;

import java.util.ArrayList;
import java.util.Hashtable;
/**
 * 
 * @author Raffaele Ariano
 * 
 * Classe che serve a identificare i documenti e mantere alcune informazioni su di loro
 *
 */

public class Documento {
	
	private String nome;
	private String proprietario;
	private int sezioni;//numero sezioni
	private ArrayList<String> collaboratori;
	private Hashtable<Integer,Boolean> is_usata=new Hashtable<Integer,Boolean>();//quale sezione di questo file è usata
	private Hashtable<Integer,String> da_chi=new Hashtable<Integer,String>();//e da chi
	String IPaddresschat;//ip address della chatroom
	
	public Documento(String nome, String proprietario, int sezioni, String Ipadddress) {
		this.nome=nome;
		this.proprietario=proprietario;
		this.sezioni=sezioni;
		collaboratori=new ArrayList<String>();
		for(int i=0; i<sezioni;i++)
			is_usata.put(i, false);
		IPaddresschat=Ipadddress;
	}
	
	/**
	 * metodo usato per costruire la stringa che invio con list
	 */
	
	public String list() {
		String stringa=nome+":\n"+"Creatore: "+proprietario+"\n"+"Collaboratori: ";
		for(String i:collaboratori) {
			stringa=stringa+i+" ";
		}
		return stringa;
	}
	
	public void aggiungiScrittore(String utente) {
		collaboratori.add(utente);
	}
	
	public String getNome() {
		return nome;
	}
	
	public int numSezioni() {
		return sezioni;
	}
	
	/**
	 * effettuo controlli sul file del tipo:
	 * "posso editare questo file?"
	 * "questa sezione per questo file esiste?"
	 */
	
	public boolean controllo(String utentee, int sezione) {
		if(sezione<0 || sezione>=sezioni)
			return false;
		if(proprietario.equals(utentee))
			return true;
		if(collaboratori.contains(utentee))
			return true;
		return false;
	}
	
	public boolean isproprietario(String utente) {
		return proprietario.equals(utente);
	}
	
	/**
	 * metodo che permette ad un utente di "bloccare" una certa sezione
	 * ed editarla (se possibile)
	 */
	
	public boolean faieditare(String utente, int sezione) {
		boolean controllo=this.controllo(utente,sezione);
		if (controllo==false)
			return controllo;
		controllo=is_usata.get(sezione);
		if(controllo==true)
			return false;
		else {
			is_usata.put(sezione, true);
			da_chi.put(sezione, utente);
			return true;
		}
	}
	
	public boolean smettidieditare(String utente, int sezione) {
		try {
			if(da_chi.get(sezione).equals(utente)) {
				is_usata.put(sezione, false);
				da_chi.remove(sezione);
				return true;
			}
			else
				return false;
		}
		catch(NullPointerException e) {
			return false;
		}
	}
	
	/**
	 * chiedo se una certa sezione è in modifica
	 */
	
	public boolean inmodifica( int numsez) {
		Boolean b=is_usata.get(numsez);
		if(b==null || b==false)
			return false;
		return true;
	}

	public String sezioniinmodifica() {
		String ritorn="le sezioni in modifica sono: ";
		for(int i=0; i<sezioni; i++) {
			if(is_usata.get(i)==true)
				ritorn=ritorn+i+",";
		}
		ritorn=ritorn.substring(0, ritorn.length()-1);//elimino l'ultima virgola
		if(!ritorn.equals("le sezioni in modifica sono:"))
			return ritorn;
		return "non ci sono sezioni in modifica";
	}
	
	public String IpAddress() {
		return IPaddresschat;
	}
	
}
