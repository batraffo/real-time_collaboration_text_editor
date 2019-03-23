package Turing;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

public class Server {
	
	public final static int BLOCK_SIZE = 8024;
	private static MulticastSocket socket;
	private static final int numeronotifica = 442;
	
	public static void main(String[] args) throws IOException {
		ArrayList<Documento> listaDocumenti=new ArrayList<Documento>();//tutti i documenti creati in questa sessione
		HashMap<String,String> danotificare=new HashMap<String,String>();//lista utenti che devono ricevere invito, utente+mess invito
		String pathServer="./cartella_server";//dove si trovano i file del server
		if(!Files.exists(Paths.get(pathServer)))
			Files.createDirectory(Paths.get(pathServer));
		UtentiRegistrati registratore=new UtentiRegistrati();//utilizzata per far controlli su utenti registrati, alcune funzioni disponibili in RMI
		LocateRegistry.createRegistry(6666);
		Registry r =LocateRegistry.getRegistry(6666);
		r.rebind(Interfaccia.SERVICE_NAME,registratore);
		System.out.println("Registrazione attiva sulla porta 6666");
		ServerSocketChannel server = ServerSocketChannel.open();
		server.bind(new InetSocketAddress("127.0.0.1", 9999));
		System.out.println("il server è in ascolto sulla porta 9999");
		server.configureBlocking(false);
		Selector selector = Selector.open();
		server.register(selector, SelectionKey.OP_ACCEPT);
		socket = new MulticastSocket(2000);
		InetAddress  address= null;//utilizzata per inviare i messaggi al gruppo chat, quando un client fa una send
		IPAddress ip=new IPAddress("239.0.0.0");
		while(true) {
			selector.select();
			Set<SelectionKey> readyKeys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = readyKeys.iterator();
			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();
				iterator.remove();//la rimozione dal selected key set è manuale
				if ((key.isValid()) && (key.isAcceptable())) {
					server = (ServerSocketChannel) key.channel();
					SocketChannel client = server.accept();
					System.out.println("c'è una nuova connessione!");
					client.configureBlocking(false);
					client.register(selector, SelectionKey.OP_READ);
				}
				else
				if ((key.isValid()) && (key.isReadable())) {
					ByteBuffer buffer =ByteBuffer.allocate(BLOCK_SIZE);
					SocketChannel client = (SocketChannel) key.channel();
					int read = -1;
					try{
						read = client.read(buffer);
					}
					catch(java.io.IOException e) {
						client.close();
					}
					if (read < 0)
						client.close();
					else {
						Attachments attacco=new Attachments();
						buffer.flip();
						switch(buffer.getInt()) {
							case 1://login
								String stringa=Charset.forName("UTF-8").decode(buffer).toString();
								StringTokenizer token=new StringTokenizer(stringa);
								attacco.utente=token.nextToken();
								String password= token.nextToken();
								System.out.println(attacco.utente +" vuole connettersi");
								if(registratore.logger(attacco.utente, password)==true) {
									System.out.println("login avvenuto");
									attacco.attacco=42;
									if(danotificare.containsKey(attacco.utente)) {
										attacco.attacco=numeronotifica;
									}
								}
								else {
									System.out.println("pw o nome non corretti");
									attacco.attacco=Operazioni.ERRORE.ordinal();
								}
								client.register(selector, SelectionKey.OP_WRITE,attacco);
								break;
							case 2://logout
								attacco.utente=Charset.forName("UTF-8").decode(buffer).toString();
								registratore.delogger(attacco.utente);
								attacco.attacco=423;
								client.register(selector, SelectionKey.OP_WRITE,attacco);
								break;
							case 3://creazione
								int numSezioni=buffer.getInt();
								String stringa1=Charset.forName("UTF-8").decode(buffer).toString();
								StringTokenizer token1=new StringTokenizer(stringa1);
								attacco.utente=token1.nextToken();
								String nomeFile= token1.nextToken();
								String nomePath=pathServer.toString()+"/"+nomeFile+"0";//dove salverò il file creato
								if (Files.exists(Paths.get(nomePath))|| registratore.is_loggato(attacco.utente)==false || numSezioni<=0) {//controllo se il file non esiste, l'utente sia loggato e che ci sia almeno una sezione
									System.out.println("impossibile creare il file");
									attacco.attacco=Operazioni.ERRORE.ordinal();
								}
								else {
									Files.createFile(Paths.get(nomePath));
									System.out.println("creato il file "+nomePath);
									for(int i=1; i<numSezioni; i++) {//creo tutte le sezioni del file
										nomePath= nomePath.substring(0, nomePath.length()-1)+Integer.toString(i);
										Files.createFile(Paths.get(nomePath));
										System.out.println("creato il file "+nomePath);
									}
									Documento nuovo=new Documento(nomeFile,attacco.utente,numSezioni,ip.toString());
									ip=ip.next();//creo un nuovo ip da associare al documento
									listaDocumenti.add(nuovo);
									if(registratore.addDocumento(attacco.utente, nuovo)==false) {
										System.out.println("strano");//non dovrebbe succedere, perchè ho fatto un controllo prima sull'esistenza del file
									}
									attacco.attacco=42;
									if(danotificare.containsKey(attacco.utente)) {//notifica
										attacco.attacco=numeronotifica;
									}
								}
								client.register(selector, SelectionKey.OP_WRITE,attacco);
								break;
							case 4://lista
								System.out.println("invio la lista di documenti");
								attacco.utente=Charset.forName("UTF-8").decode(buffer).toString();
								attacco.stringaComunicativa=registratore.getLista(attacco.utente);//utilizzo la funzione di utenti registrati per creare la stringa
								attacco.attacco=100;
								client.register(selector, SelectionKey.OP_WRITE,attacco);
								break;
							case 5://show intero documento
								String stringa3=Charset.forName("UTF-8").decode(buffer).toString();
								StringTokenizer token3=new StringTokenizer(stringa3);
								attacco.utente=token3.nextToken();
								String nomedoc= token3.nextToken();
								Documento doc=null;
								for(Documento i: listaDocumenti) {//cerco il documento nella lista documenti
									if(i.getNome().equals(nomedoc)) {
										doc=i;
										break;
									}
								}
								if(doc==null) {
									attacco.attacco=Operazioni.ERRORE.ordinal();
									System.out.println("il documento non esiste");
								}
								else {
									if(doc.controllo(attacco.utente, 0)==false) {
										attacco.attacco=Operazioni.ERRORE.ordinal();
										System.out.println("non puoi accedere al documento");
									}
									else {
										attacco.attacco=300;
										attacco.sezioniinmodifica=doc.sezioniinmodifica();
										attacco.numsez=doc.numSezioni();
										attacco.stringaComunicativa=pathServer+"/"+nomedoc;
									}
								}
								client.register(selector, SelectionKey.OP_WRITE,attacco);
								break;
							case 6://show sezione documento
								int sezione=buffer.getInt();
								String stringa2=Charset.forName("UTF-8").decode(buffer).toString();
								StringTokenizer token2=new StringTokenizer(stringa2);
								attacco.utente=token2.nextToken();
								String nomefile= token2.nextToken();
								Documento copia=null;
								for(Documento i: listaDocumenti) {//cerco il documento
									if(i.getNome().equals(nomefile)) {
										copia=i;
										break;
									}
								}
								if(copia==null) {
									attacco.attacco=Operazioni.ERRORE.ordinal();
									System.out.println("il file non esiste");
								}
								else {
									if(copia.controllo(attacco.utente,sezione)==false) {
										attacco.attacco=Operazioni.ERRORE.ordinal();
										System.out.println("non puoi accedere al file");
									}
									else {
										if(copia.inmodifica(sezione))
											attacco.inmodifica=true;
										else
											attacco.inmodifica=false;
										attacco.attacco=200;
										attacco.stringaComunicativa=pathServer+"/"+nomefile+sezione;
									}
								}
								client.register(selector, SelectionKey.OP_WRITE,attacco);
								break;
							case 7://invito
								String stringadatokenizzare=Charset.forName("UTF-8").decode(buffer).toString();
								StringTokenizer tokenizer=new StringTokenizer(stringadatokenizzare);
								attacco.utente=tokenizer.nextToken();
								String nomedocu=tokenizer.nextToken();
								String destinatario=tokenizer.nextToken();
								Documento doc1=null;
								for(Documento i: listaDocumenti) {//cerco il documento
									if(i.getNome().equals(nomedocu)) {
										doc1=i;
										break;
									}
								}
								if(doc1==null) {
									attacco.attacco=Operazioni.ERRORE.ordinal();
									System.out.println("il file non esiste");
								}
								else {
									if(doc1.isproprietario(attacco.utente) && registratore.isregistrato(destinatario) && !doc1.controllo(destinatario,0)) {//controllo che sia il proprietario, che l'utente che invita esista e che l'utente invitato non sia già stato invitato in precedenza
										attacco.attacco=42;//devo inviare una notifica
										doc1.aggiungiScrittore(destinatario);
										registratore.addDocumento(destinatario, doc1);//aggiungo l'associazione tra documento e utente
										if(danotificare.containsKey(destinatario)) //controllo se abbia già una o più notifiche pendenti
											danotificare.put(destinatario, danotificare.get(destinatario)+"\n sei stato invitato da " + attacco.utente+" a lavorare su "+ nomedocu);
										else
											danotificare.put(destinatario," sei stato invitato da " + attacco.utente+" a lavorare su "+ nomedocu);
										if(danotificare.containsKey(attacco.utente)) {
											attacco.attacco=numeronotifica;
										}
									}
									else {
										attacco.attacco=Operazioni.ERRORE.ordinal();
										System.out.println("non puoi accedere al documento");
									}
								}
								client.register(selector, SelectionKey.OP_WRITE,attacco);
								break;
							case 8://edit
								int s=buffer.getInt();
								String datokenizzare=Charset.forName("UTF-8").decode(buffer).toString();
								StringTokenizer tokens=new StringTokenizer(datokenizzare);
								attacco.utente=tokens.nextToken();
								String nomesez= tokens.nextToken();
								Documento sez=null;
								for(Documento i: listaDocumenti) {//cerco il documento
									if(i.getNome().equals(nomesez)) {
										sez=i;
										break;
									}
								}
								if(sez==null) {
									attacco.attacco=Operazioni.ERRORE.ordinal();
									System.out.println("il file non esiste");
								}
								else {
									if(sez.faieditare(attacco.utente,s)==false) {//"blocco" la sezione
										attacco.attacco=Operazioni.ERRORE.ordinal();
										System.out.println("impossibile editare");
									}
									else {
										attacco.attacco=200;
										attacco.ipchat=sez.IPaddresschat;
										attacco.stringaComunicativa=pathServer+"/"+nomesez+s;
									}
								}
								client.register(selector, SelectionKey.OP_WRITE,attacco);
								break;
							case 9://end_edit
								int sezz=buffer.getInt();
								String datokenizzar=Charset.forName("UTF-8").decode(buffer).toString();
								StringTokenizer tokenizzatore=new StringTokenizer(datokenizzar);
								attacco.utente=tokenizzatore.nextToken();
								String nomefil=tokenizzatore.nextToken();
								String fil=tokenizzatore.nextToken("\0");
								Documento documen=null;
								for(Documento i: listaDocumenti) {//cerco il documento
									if(i.getNome().equals(nomefil)) {
										documen=i;
										break;
									}
								}
								if(documen==null) {
									attacco.attacco=Operazioni.ERRORE.ordinal();
									System.out.println("il file non esiste");
								}
								else {
									if(documen.smettidieditare(attacco.utente, sezz)==false) {//"sblocco" la sezione
										attacco.attacco=Operazioni.ERRORE.ordinal();
										System.out.println("il documento non è in editing dall'utente "+attacco.utente);
									}
									else {
										String pathfiledamodificare=pathServer+"/"+nomefil+sezz;
										fil=fil.substring(1, fil.length());//elimino lo spazio all'inizio del file
										Files.write(Paths.get(pathfiledamodificare), fil.getBytes());
										System.out.println("la sezione " +sezz+" di "+nomefil+" è stata scaricata correttamente");
										if(danotificare.containsKey(attacco.utente)) {
											attacco.attacco=numeronotifica;
										}
										else
											attacco.attacco=42;
									}
								}
								client.register(selector, SelectionKey.OP_WRITE,attacco);
								break;
							case 10://send
								String messaggioeip=Charset.forName("UTF-8").decode(buffer).toString();
								StringTokenizer divisore=new StringTokenizer(messaggioeip,"\0");
								byte [] messaggio=divisore.nextToken().getBytes();
								String addresso=divisore.nextToken();
								address=InetAddress.getByName(addresso);
								DatagramPacket datagram=new DatagramPacket(messaggio,messaggio.length ,address,3000);
								try {
									socket.send(datagram);
								} catch (IOException e) {
									e.printStackTrace();
								}
								client.register(selector, SelectionKey.OP_READ);
								break;
							default:
								System.out.println("quest'operazione non è supportata");
								attacco.attacco=Operazioni.ERRORE.ordinal();
								client.register(selector, SelectionKey.OP_WRITE,attacco);
								break;
						}
					}
				}
				else
				if ((key.isValid()) && (key.isWritable())) {
					boolean notifica=false;
					SocketChannel client = (SocketChannel) key.channel();
					Attachments dainviare=(Attachments) key.attachment();
					System.out.println("ecco l'intero "+dainviare.attacco) ;
					if(danotificare.containsKey(dainviare.utente) && dainviare.attacco!=numeronotifica && dainviare.attacco!=Operazioni.ERRORE.ordinal() &&dainviare.attacco!=423 /*scarto il logout*/ ) {
						notifica=true;
						System.out.println("l'utente "+dainviare.utente+" ha ricevuto una notifica di invito");
					}
					ByteBuffer buffer=ByteBuffer.allocate(BLOCK_SIZE);
					int num=dainviare.attacco;
					switch (num) {
						case 100://invio semplicemente un intero (di successo o fallimento) al client  (utilizzato solo dalla list)
							buffer.put(dainviare.stringaComunicativa.getBytes());
							if(notifica==true) {
								buffer.put("\n".getBytes());
								buffer.put(danotificare.get(dainviare.utente).getBytes());
								danotificare.remove(dainviare.utente);
							}
							buffer.flip();
							while (buffer.hasRemaining())
								client.write(buffer);
							System.out.println("stringa inviata "+dainviare.stringaComunicativa);
							buffer.clear();
							client.register(selector, SelectionKey.OP_READ);
							break;
						case 200 ://per inviare una singola sezione al client
							if(!notifica)
								buffer.putInt(dainviare.attacco);//per dire che è tutto ok
							else
								buffer.putInt(numeronotifica);
							if(dainviare.inmodifica!=null)//diverso da null per shows, invio un intero con cui comunico se la sezione è in modifica
								if(dainviare.inmodifica==true)
									buffer.putInt(1);
								else
									buffer.putInt(0);
							Path paths = Paths.get(dainviare.stringaComunicativa);
							ByteChannel reader = Files.newByteChannel(paths);
							reader.read(buffer);
							buffer.put("a".getBytes());//necessario per riconoscere i file vuoti, altrimenti sarebbero scartati
							buffer.putChar('\0');
							if(dainviare.ipchat!=null) {
								buffer.put(dainviare.ipchat.getBytes());
								buffer.putChar('\0');
							}
							if(notifica) {//inserisco la notifica in fondo
								buffer.put(danotificare.get(dainviare.utente).getBytes());
								danotificare.remove(dainviare.utente);
								buffer.putChar('\0');
							}
							buffer.flip();
							while (buffer.hasRemaining())
								client.write(buffer);
							buffer.clear();
							client.register(selector, SelectionKey.OP_READ);
							break;
						case 300://per show intero documento
							if(!notifica)
								buffer.putInt(dainviare.attacco);
							else
								buffer.putInt(numeronotifica);
							buffer.putInt(dainviare.numsez);
							for(int i=0; i<dainviare.numsez; i++) {
								String path=dainviare.stringaComunicativa+i;
								Path paz = Paths.get(path);
								ByteChannel read = Files.newByteChannel(paz);
								read.read(buffer);
								buffer.put("a".getBytes());//necessario per riconoscere i file vuoti
								buffer.putChar('\0');
							}
							buffer.put(dainviare.sezioniinmodifica.getBytes());//invio la stringa con segnate tutte le sezioni in modifica
							buffer.putChar('\0');
							if(notifica) {
								buffer.put(danotificare.get(dainviare.utente).getBytes());
								danotificare.remove(dainviare.utente);
								buffer.putChar('\0');
							}
							buffer.flip();
							while (buffer.hasRemaining())
								client.write(buffer);
							buffer.clear();
							client.register(selector, SelectionKey.OP_READ);
							break;
						case 442://login o altre operazioni con risposte standard con notifiche pendenti
							System.out.println("operazione con notifiche");
							buffer.putInt(dainviare.attacco);
							buffer.put(danotificare.get(dainviare.utente).getBytes());
							danotificare.remove(dainviare.utente);//segno che ho notificato l'utente e lo rimuovo dalla lista di utenti da notificare
							buffer.flip();
							while (buffer.hasRemaining())
								client.write(buffer);
							buffer.clear();
							client.register(selector, SelectionKey.OP_READ);
							break;
						default:
							System.out.println("invio un intero");
							buffer.putInt(dainviare.attacco);
							buffer.flip();
							while (buffer.hasRemaining())
								client.write(buffer);
							buffer.clear();
							client.register(selector, SelectionKey.OP_READ);
							break;
					}
				}
			}
		}
	}
	static class Attachments{
		public String sezioniinmodifica="non ci sono sezioni in modifica";
		public int attacco=0;
		public Boolean inmodifica=null;//per controllo fatto in shows
		public String ipchat=null;
		public int numsez=-1;
		public String stringaComunicativa="";
		public String utente;
		public Attachments() {

		}
	}
}
