package Turing;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.StringTokenizer;


public class Client {
	private static Scanner keyboard;//prendo input da tastiera
	public final static int BLOCK_SIZE = 8024;
	private static final int LENGTH = 512;//grandezza datagramma
	private static MulticastSocket socket;//socket per il servizio di chat
	private static final int numerochemidicecheceunanotifica = 442;

	public static void main(String[] args) throws NotBoundException, IOException {
		String pathClient="./Cartella_Clients";//<path dove tengo le cartelle dei client*/
		String pathClientLoggato="";//path della cartella del client
		if(!Files.exists(Paths.get(pathClient)))
			Files.createDirectory(Paths.get(pathClient));//creo se non esiste
		Registry reg= LocateRegistry.getRegistry("localhost",6666);
		Interfaccia registratore=(Interfaccia) reg.lookup(Interfaccia.SERVICE_NAME);//RMI
		SocketChannel client = SocketChannel.open(new InetSocketAddress("127.0.0.1",9999));//mi connetto al server
		ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);
		socket = new MulticastSocket(3000);
		socket.setSoTimeout(500);//timeout per la receive
		String ip=null;//ip del gruppo di multicast
		InetAddress  address = null;//per connettermi all'ip di sopra
		String file_in_editing="";//il nome del file che l'utente attuale sta editando
		DatagramPacket packet = new DatagramPacket(
				new byte[LENGTH], LENGTH);
		keyboard = new Scanner(System.in);
		String operazione;
		String utenteloggato = null;
		boolean loggato=false;
		boolean in_editing=false;
		int documenti_in_editing=0;//numero di sezioni del documento che sto editando
		while(true) {
			System.out.println("inserisci una nuova operazione");
			operazione=keyboard.next();
			if (operazione.equals(""))//non dovrebbe succedere, sta per sicurezza
				continue;
			switch(operazione) {//vedo che operazione ho ricevuto
				case "register":
					String utente=keyboard.next();
					String password=keyboard.next();
					if(loggato) {
						System.out.println("per registrare un nuovo utente devi prima fare il logout");
						continue;
					}
					if(registratore.register(utente, password)==false) {//servizio fornito tramite RMI
						System.out.println("registrazione di "+utente+" fallita" );
					}
					else {
						String dacreare=pathClient+"/"+utente;//aggiorno il path
						if(!Files.exists(Paths.get(dacreare)))
							Files.createDirectory(Paths.get(dacreare));
						System.out.println("registrazione riuscita!");
					}
					break;
				case "login":
					if(loggato==true) {
						keyboard.next();
						keyboard.next();
						System.out.println("sei già loggato");
						continue;
					}
					utenteloggato=keyboard.next();
					String password1=keyboard.next();
					buffer.putInt(Operazioni.LOGIN.ordinal());
					buffer.put(utenteloggato.getBytes());
					buffer.put(" ".getBytes());//inserisco degli spazi per distinguere l'utente dalla password
					buffer.put(password1.getBytes());
					buffer.flip();
					while(buffer.hasRemaining()) {
						client.write(buffer);
					}
					buffer.clear();
					client.read(buffer);
					buffer.flip();
					int n=buffer.getInt();
					if(n!=Operazioni.ERRORE.ordinal()) {
						System.out.println(utenteloggato + " s'è loggato");
						loggato=true;
						pathClientLoggato=pathClient+"/"+utenteloggato;
						if(n==numerochemidicecheceunanotifica) {//c'è una notifica
							System.out.println(Charset.forName("UTF-8").decode(buffer).toString());
						}
					}
					else {
						System.out.println("login fallito, riprova");
						utenteloggato="";
					}
					buffer.clear();
					break;
				case "logout":
					if(loggato==false) {
						System.out.println("non ti puoi disconnettere se non ti sei prima connesso");
						continue;
					}
					if(in_editing==true) {
						System.out.println("esci prima dalla modalità editing");
						continue;
					}
					buffer.putInt(Operazioni.LOGOUT.ordinal());
					buffer.put(utenteloggato.getBytes());
					buffer.flip();
					while(buffer.hasRemaining()) {
						client.write(buffer);
					}
					buffer.clear();
					client.read(buffer);
					buffer.flip();
					if(buffer.getInt()!=Operazioni.ERRORE.ordinal()) {
						loggato=false;
						System.out.println("l'utente s'è sloggato");
						utenteloggato="";
						pathClientLoggato="";
					}
					else {
						System.out.println("qualcosa di strano è in atto in questo programma");
					}
					buffer.clear();
					break;
				case "create":
					String nomefile=keyboard.next();
					int num_sezioni = 0;
					try {
						num_sezioni=Integer.parseInt(keyboard.next());
					}catch(NumberFormatException e) {
						System.out.println("errore, il secondo argomento di create deve essere un intero");
						continue;
					}
					if(loggato==false) {
						System.out.println("non puoi creare un file se prima non ti sei loggato");
						continue;
					}
					if(in_editing==true) {
						System.out.println("esci prima dalla modalità editing");
						continue;
					}
					if(num_sezioni<=0) {
						System.out.println("il file deve avere almeno una sezione");
						continue;
					}
					buffer.putInt(Operazioni.CREATE.ordinal());
					buffer.putInt(num_sezioni);
					buffer.put(utenteloggato.getBytes());
					buffer.put(" ".getBytes());
					buffer.put(nomefile.getBytes());
					buffer.flip();
					while(buffer.hasRemaining()) {
						client.write(buffer);
					}
					buffer.clear();
					client.read(buffer);
					buffer.flip();
					int number=buffer.getInt();
					if(number!=Operazioni.ERRORE.ordinal()) {
						System.out.println("file creato con successo");
						if(number==numerochemidicecheceunanotifica) {//notifica
							System.out.println(Charset.forName("UTF-8").decode(buffer).toString());
						}
					}
					else {
						System.out.println("creazione fallita");
					}
					buffer.clear();
					break;
				case "list":
					if(loggato==false) {
						System.out.println("loggati prima");
						continue;
					}
					if(in_editing==true) {
						System.out.println("esci prima dalla modalità editing");
						continue;
					}
					buffer.putInt(Operazioni.LISTA.ordinal());
					buffer.put(utenteloggato.getBytes());
					buffer.flip();
					while(buffer.hasRemaining()) {
						client.write(buffer);
					}
					buffer.clear();
					System.out.println("leggo");
					client.read(buffer);
					buffer.flip();
					System.out.println(Charset.forName("UTF-8").decode(buffer).toString());//aggiungo la notifica a questa stringa se dovesse essercene una
					buffer.clear();
					break;
				case "showd":
					String nomedoc=keyboard.next();
					if(loggato==false) {
						System.out.println("loggati prima");
						continue;
					}
					if(in_editing==true) {
						System.out.println("esci prima dalla modalità editing");
						continue;
					}
					buffer.putInt(Operazioni.SHOWD.ordinal());
					buffer.put(utenteloggato.getBytes());
					buffer.put(" ".getBytes());
					buffer.put(nomedoc.getBytes());
					buffer.flip();
					while(buffer.hasRemaining()) {
						client.write(buffer);
					}
					buffer.clear();
					client.read(buffer);
					buffer.flip();
					int ris=buffer.getInt();
					if(ris!=Operazioni.ERRORE.ordinal()) {
						int k=buffer.getInt();
						String stringa1=Charset.forName("UTF-8").decode(buffer).toString();
						StringTokenizer token=new StringTokenizer(stringa1,"\0");//divido i vari dati delle sezioni del file
						for(int i=0; i<k; i++) {
							String stringa=token.nextToken();
							stringa=stringa.substring(0, stringa.length()-1);//elimino la 'a' inserita dal server per non far scartare i file vuoti
							Files.write(Paths.get(pathClientLoggato+"/"+nomedoc+i), stringa.getBytes());
							System.out.println("la sezione " +i+" di "+nomedoc+" è stata scaricata correttamente");
						}
						System.out.println(token.nextToken());//ultimo token indica quali sezioni sono in modifica
						if(ris==numerochemidicecheceunanotifica) {
							System.out.println(token.nextToken());//la notifica
						}
					}
					else {
						System.out.println("documento non disponibile");
					}
					buffer.clear();
					break;
				case "shows"://shows uguale con un int in più
					String nomedavisualizzare=keyboard.next();
					int num=-1;
					try {
						num=Integer.parseInt(keyboard.next());
					}catch(NumberFormatException e) {
						System.out.println("errore, il secondo argomento di create deve essere un intero");
						continue;
					}
					if(loggato==false) {
						System.out.println("loggati prima");
						continue;
					}
					if(in_editing==true) {
						System.out.println("esci prima dalla modalità editing");
						continue;
					}
					buffer.putInt(Operazioni.SHOWS.ordinal());
					buffer.putInt(num);
					buffer.put(utenteloggato.getBytes());
					buffer.put(" ".getBytes());
					buffer.put(nomedavisualizzare.getBytes());
					buffer.flip();
					while(buffer.hasRemaining()) {
						client.write(buffer);
					}
					buffer.clear();
					client.read(buffer);
					buffer.flip();
					int risp=buffer.getInt();
					if(risp!=Operazioni.ERRORE.ordinal()) {
						 int in_modifica=buffer.getInt();
		    			 String stringa1=Charset.forName("UTF-8").decode(buffer).toString();
		    			 StringTokenizer tokens=new StringTokenizer(stringa1,"\0");
		    			 String stringa=tokens.nextToken();
		    			 stringa=stringa.substring(0, stringa.length()-1);//elimino la a inserita per non far scartare i file vuoti
		    			 Files.write(Paths.get(pathClientLoggato+"/"+nomedavisualizzare+num), stringa.getBytes());
						 System.out.println("la sezione " +num+" di "+nomedavisualizzare+" è stata scaricata correttamente");
						 if(risp==numerochemidicecheceunanotifica)
							 System.out.println(tokens.nextToken());//notifica!
						 if(in_modifica==1)
								System.out.println("questa sezione è in modifica");
							else
								System.out.println("questa sezione non è in modifica");
					}
					else {
						System.out.println("file non disponibile");
					}
					buffer.clear();
					break;
				case "invite":
					String documento=keyboard.next();
					String dainvitare=keyboard.next();
					if(loggato==false) {
						System.out.println("loggati prima");
						continue;
					}
					if(in_editing==true) {
						System.out.println("esci prima dalla modalità editing");
						continue;
					}
					if(dainvitare.equals(utenteloggato)) {//mi posso autoinvitare? *thinking*
						System.out.println("mmmh");
						continue;
					}
					buffer.putInt(Operazioni.INVITE.ordinal());
					buffer.put(utenteloggato.getBytes());
					buffer.put(" ".getBytes());
					buffer.put(documento.getBytes());
					buffer.put(" ".getBytes());
					buffer.put(dainvitare.getBytes());
					buffer.flip();
					while(buffer.hasRemaining()) {
						client.write(buffer);
					}
					buffer.clear();
					client.read(buffer);
					buffer.flip();
					int risposta=buffer.getInt();
					if(risposta!=Operazioni.ERRORE.ordinal()) {
						System.out.println(dainvitare+" invitato!");
						if(risposta==numerochemidicecheceunanotifica)
							System.out.println(Charset.forName("UTF-8").decode(buffer).toString());//aggiungo la notifica a questa stringa se dovesse essercene una
					}
					else {
						System.out.println("impossibile eseguire l'operazione");
					}
					buffer.clear();
					break;
				case "edit":
					String nomed=keyboard.next();
					int s=-1;
					try {
						s=Integer.parseInt(keyboard.next());
					}catch(NumberFormatException e) {
						System.out.println("errore, il secondo argomento di create deve essere un intero");
						continue;
					}
					if(loggato==false) {
						System.out.println("loggati prima");
						continue;
					}
					if(in_editing==true && !nomed.equals(file_in_editing)) {//codice aggiunto per la chat, permetto di far editare più sezioni contemporaneamente da uno stesso utente ma solo dello stesso documento
						System.out.println("non è un documento che puoi editare al momento");
						continue;
					}
					buffer.putInt(Operazioni.EDIT.ordinal());
					buffer.putInt(s);
					buffer.put(utenteloggato.getBytes());
					buffer.put(" ".getBytes());
					buffer.put(nomed.getBytes());
					buffer.flip();
					while(buffer.hasRemaining()) {
						client.write(buffer);
					}
					buffer.clear();
					client.read(buffer);
					buffer.flip();
					int rispos=buffer.getInt();
					if(rispos!=Operazioni.ERRORE.ordinal()) {
		    			 String stringa1=Charset.forName("UTF-8").decode(buffer).toString();
		    			 StringTokenizer tokens=new StringTokenizer(stringa1,"\0");
		    			 String stringa=tokens.nextToken();
		    			 stringa=stringa.substring(0, stringa.length()-1);//elimino la a inserita per non far scartare i file vuoti
		    			 Files.write(Paths.get(pathClientLoggato+"/"+nomed+s+"_editVersion"), stringa.getBytes());
						 System.out.println("la sezione " +s+" di "+nomed+" è stata scaricata correttamente e puoi editarla!");
						 ip=tokens.nextToken();//invio la stringa su cui l'utente può connettersi per chattare
						 address=InetAddress.getByName(ip);
						 file_in_editing=nomed;
						 if(rispos==numerochemidicecheceunanotifica)
							 System.out.println(tokens.nextToken());//notifica (come sempre)
						 if(in_editing==false)
						 	socket.joinGroup(address);//se non ti sei già connesso prima, connettiti
						 in_editing=true;
						 documenti_in_editing++;
					}
					else {
						System.out.println("file non disponibile");
					}
					buffer.clear();
					break;
				case "end_edit":
					String nomedo=keyboard.next();
					int sez=-1;
					try {
						sez=Integer.parseInt(keyboard.next());
					}catch(NumberFormatException e) {
						System.out.println("errore, il secondo argomento di create deve essere un intero");
						continue;
					}
					if(in_editing==false) {
						System.out.println("dovresti editare qualcosa per smettere di editarla");
						continue;
					}
					String pathfiledainviare=pathClientLoggato+"/"+nomedo+sez+"_editVersion";
					Path paz = Paths.get(pathfiledainviare);
					if(!Files.exists(paz)) {
						System.out.println("non stai editando questi file");
						continue;
					}
					ByteChannel read = Files.newByteChannel(paz);
					buffer.putInt(Operazioni.END_EDIT.ordinal());
					buffer.putInt(sez);
					buffer.put(utenteloggato.getBytes());
					buffer.put(" ".getBytes());
					buffer.put(nomedo.getBytes());
					buffer.put(" ".getBytes());
					read.read(buffer);
					buffer.putChar('\0');
					buffer.flip();
					while(buffer.hasRemaining()) {
						client.write(buffer);
					}
					buffer.clear();
					client.read(buffer);
					buffer.flip();
					int ret=buffer.getInt();
					if(ret!=Operazioni.ERRORE.ordinal()) {
						System.out.println(nomedo+" è modificato con successo nel server");
						if(ret==numerochemidicecheceunanotifica)
							System.out.println(Charset.forName("UTF-8").decode(buffer).toString());//aggiungo la notifica a questa stringa se dovesse essercene una
						documenti_in_editing--;
						if(documenti_in_editing==0) {//hai smesso di editare tutte le sezioni
							in_editing=false;
							file_in_editing="";
							ip=null;
							socket.leaveGroup(address);
						}
					}
					else {
						System.out.println("impossibile eseguire l'operazione");
					}
					buffer.clear();
					break;
				case "send":
					String messaggio=keyboard.nextLine();
					if(loggato==false) {
						System.out.println("loggati prima");
						continue;
					}
					if(in_editing==false) {
						System.out.println("per utilizzare la chat devi star editando un documento");
						continue;
					}
					if(messaggio.isEmpty()) {
						System.out.println("il messaggio è vuoto");
						continue;
					}
					messaggio=utenteloggato+":"+messaggio;
					buffer.putInt(Operazioni.SEND.ordinal());
					buffer.put(messaggio.getBytes());
					buffer.putChar('\0');
					buffer.put(ip.getBytes());
					buffer.putChar('\0');
					buffer.flip();
					while(buffer.hasRemaining()) {
						client.write(buffer);
					}
					buffer.clear();
					break;
				case "receive":
					if(loggato==false) {
						System.out.println("loggati prima");
						continue;
					}
					if(in_editing==false) {
						System.out.println("per utilizzare la chat devi star editando un documento");
						continue;
					}
					while(true) {
						try {
							socket.receive(packet);
						}
						catch(SocketTimeoutException e) {
							System.out.println("non ci son più messaggi da leggere");
							break;
						}
						System.out.println(new String(
								packet.getData(),
								packet.getOffset(),
								packet.getLength(),
								"UTF-8"));
					}
					break;
				case "exit"://chiudo il client
					if(loggato==true) {
						System.out.println("fai logout prima");
						continue;
					}
					client.close();
					return;
				default:
					keyboard.nextLine();
					System.out.println("'" +operazione+"' non è un'operazione supportata, riprova");
					break;
			}
		}
	}
}
