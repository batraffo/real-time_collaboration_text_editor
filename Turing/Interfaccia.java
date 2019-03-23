package Turing;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Interfaccia extends Remote {
	
	String SERVICE_NAME = "Registratore";
	
	public Boolean register(String nome,String pw)throws RemoteException;
	
}
