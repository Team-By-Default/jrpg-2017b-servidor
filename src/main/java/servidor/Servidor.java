package servidor;

import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import mensajeria.PaqueteNPC;
import mensajeria.PaqueteMensaje;
import mensajeria.PaqueteMovimiento;
import mensajeria.PaquetePersonaje;
import mundo.Mundo;
import mundo.Tile;

public class Servidor extends Thread {

	//Conectamos los mounstruos aca
	private static ArrayList<EscuchaCliente> clientesConectados = new ArrayList<>();
	
	private static Map<Integer, PaqueteMovimiento> ubicacionPersonajes = new HashMap<>();
	private static Map<Integer, PaquetePersonaje> personajesConectados = new HashMap<>();
	private static Map<Integer, PaqueteMovimiento> ubicacionNPCs = new HashMap<>();
	private static Map<Integer, PaqueteNPC> NPCs = new HashMap<>();
	private Random random; //Para ubicar a los NPCs

	private static Thread server;
	
	private static ServerSocket serverSocket;
	private static Conector conexionDB;
	private int puerto = 55050;

	private final static int ANCHO = 700;
	private final static int ALTO = 640;
	private final static int ALTO_LOG = 520;
	private final static int ANCHO_LOG = ANCHO - 25;

	public static JTextArea log;
	
	public static AtencionConexiones atencionConexiones;
	public static AtencionMovimientos atencionMovimientos;

	public static void main(String[] args) {
		cargarInterfaz();	
	}

	public Servidor() {
		try { 
			Scanner sc = new Scanner(new File("config.txt"));
			this.puerto=sc.nextInt();
			sc.close();
			if(this.puerto>65535||this.puerto<0)
				this.puerto=55050;
		} catch (FileNotFoundException e1) {
			this.puerto=55050;
		}
		this.random = new Random();
	}
	
	private static void cargarInterfaz() {
		JFrame ventana = new JFrame("Servidor WOME");
		ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ventana.setSize(ANCHO, ALTO);
		ventana.setResizable(false);
		ventana.setLocationRelativeTo(null);
		ventana.setLayout(null);
		ventana.setIconImage(Toolkit.getDefaultToolkit().getImage("src/main/java/servidor/server.png"));
		JLabel titulo = new JLabel("Log del servidor...");
		titulo.setFont(new Font("Courier New", Font.BOLD, 16));
		titulo.setBounds(10, 0, 200, 30);
		ventana.add(titulo);

		log = new JTextArea();
		log.setEditable(false);
		log.setFont(new Font("Times New Roman", Font.PLAIN, 13));
		JScrollPane scroll = new JScrollPane(log, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setBounds(10, 40, ANCHO_LOG, ALTO_LOG);
		ventana.add(scroll);

		final JButton botonIniciar = new JButton();
		final JButton botonDetener = new JButton();
		botonIniciar.setText("Iniciar");
		botonIniciar.setBounds(220, ALTO - 70, 100, 30);
		botonIniciar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				server = new Thread(new Servidor());
				server.start();
				botonIniciar.setEnabled(false);
				botonDetener.setEnabled(true);
			}
		});

		ventana.add(botonIniciar);

		botonDetener.setText("Detener");
		botonDetener.setBounds(360, ALTO - 70, 100, 30);
		botonDetener.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					server.stop();
					atencionConexiones.stop();
					atencionMovimientos.stop();
					for (EscuchaCliente cliente : clientesConectados) {
						cliente.getSalida().close();
						cliente.getEntrada().close();
						cliente.getSocket().close();
					}
					serverSocket.close();
					log.append("El servidor se ha detenido." + System.lineSeparator());
				} catch (IOException e1) {
					log.append("Fallo al intentar detener el servidor." + System.lineSeparator());
				}
				if(conexionDB != null)
					conexionDB.close();
				botonDetener.setEnabled(false);
				botonIniciar.setEnabled(true);
			}
		});
		botonDetener.setEnabled(false);
		ventana.add(botonDetener);

		ventana.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		ventana.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				if (serverSocket != null) {
					try {
						server.stop();
						atencionConexiones.stop();
						atencionMovimientos.stop();
						for (EscuchaCliente cliente : clientesConectados) {
							cliente.getSalida().close();
							cliente.getEntrada().close();
							cliente.getSocket().close();
						}
						serverSocket.close();
						log.append("El servidor se ha detenido." + System.lineSeparator());
					} catch (IOException e) {
						log.append("Fallo al intentar detener el servidor." + System.lineSeparator());
						System.exit(1);
					}
				}
				if (conexionDB != null)
					conexionDB.close();
				System.exit(0);
			}
		});

		ventana.setVisible(true);
	}

	public void run() {
		try {
			
			conexionDB = new Conector();
			conexionDB.connect();
			
			log.append("Iniciando el servidor..." + System.lineSeparator());
			serverSocket = new ServerSocket(puerto);
			log.append("Servidor esperando conexiones..." + System.lineSeparator());
			String ipRemota;
			
			atencionConexiones = new AtencionConexiones();
			atencionMovimientos = new AtencionMovimientos();
			
			atencionConexiones.start();
			atencionMovimientos.start();
			
			//Donde pongo a los 10 npc
			for (int i = 0; i < 10; i++) { // crea 10 NPCs en posiciones randoms
				PaqueteNPC paqueteNPC = new PaqueteNPC(i);
				//Genero coordenadas al azar en algún lugar no visible por los personajes
				float[] coords = generarPosIso(this.random);
				PaqueteMovimiento paqueteMovimiento = new PaqueteMovimiento(i, coords[0], coords[1]);
				
				//Seteo el nuevo NPC
				NPCs.put( i, paqueteNPC);
				ubicacionNPCs.put( i, paqueteMovimiento);
				
				setNPCs( NPCs );
				setUbicacionNPCs( ubicacionNPCs );
			}
			
			
			while (true) {
				Socket cliente = serverSocket.accept();
				ipRemota = cliente.getInetAddress().getHostAddress();
				log.append(ipRemota + " se ha conectado" + System.lineSeparator());

				ObjectOutputStream salida = new ObjectOutputStream(cliente.getOutputStream());
				ObjectInputStream entrada = new ObjectInputStream(cliente.getInputStream());

				EscuchaCliente atencion = new EscuchaCliente(ipRemota, cliente, entrada, salida);
				atencion.start();
				clientesConectados.add(atencion);
			}
		} catch (Exception e) {
			log.append("Fallo la conexión." + System.lineSeparator());
		}
	}
	
	/**
	 * Genera coordenadas isométricas al azar, en lugares no visibles por ningún
	 * personaje conectado. 
	 * WARNING: en el peor de los casos, o si entre todos los personajes cubren
	 * ven todo el mapa, esto caería en un while infinito
	 * @return vector de coordenadas isométricas en dos dimensiones
	 */
	public static float[] generarPosIso(Random random) {
		//Con random genero coordenadas del mapa de (0,0) a (71,71) y las paso a isométricas
		int x = random.nextInt(72);
		int y = random.nextInt(72);
		
		float[] coordsIso = Mundo.dosDaIso(x, y);
		/*
		 * Si esas coordenadas son visibles por algún personaje o dieron sobre un lugar
		 * prohibido, las regenero hasta dar con unas que no sean visibles ni prohibidas
		 */
		while(esLugarProhibido(x,y) || esVisible(coordsIso = Mundo.dosDaIso(x, y))) {
			x = random.nextInt() % 72;
			y = random.nextInt() % 72;
		}
		
		return coordsIso;
	}
	
	/**
	 * Para un par de coordenadas isométricas define si es visible o no por algún personaje
	 * @param iso: vector de coordenadas isométricas
	 * @return true si alguien lo ve, false si no es visible por nadie
	 */
	private static boolean esVisible(float iso[]) {
		//Para cada personaje conectado
		for(PaqueteMovimiento posPersonaje : ubicacionPersonajes.values()) {
			//Si entra en su campo visible, retorno true
			if(iso[0] <= posPersonaje.getPosX() +  ANCHO/2 &&
					iso[0] >= posPersonaje.getPosX() -  ANCHO/2 &&
					iso[1] <= posPersonaje.getPosY() +  ALTO/2 &&
					iso[1] >= posPersonaje.getPosX() -  ALTO/2)
				return true;
		}
		//Si llega hasta acá es que no está en el campo visible de nadie, retorna false
		return false;
	}
	
	/**
	 * Responde si un par de coordenadas (no isométricas) del mapa Aubenor 
	 * es un lugar prohibido o no.
	 * @param x: coordenada X
	 * @param y: coordenada Y
	 * @return true si es un lugar prohibido, false si no
	 */
	private static boolean esLugarProhibido(int x, int y) {
		Scanner arch;
		try {
			arch = new Scanner(new File("../jrpg-2017b-cliente/recursos/Aubenor.txt"));
			
			//Si cae fuera del mapa, es un lugar prohibido
			if(x >= arch.nextInt() || y >= arch.nextInt()) {
				arch.close();
				return true;
			}
			
			int i;
			//Salto lineas hasta posicionarme en Y.
			//Salto la primer linea (2º linea del archivo) porque son las coordenadas de spawn
			for(i=0; i < y+1; i++)
				arch.nextLine();
			//Salto hasta antes de X
			for(i=0; i<x; i++)
				arch.nextInt();
			
			//Leo la posicion X,Y
			int tile = arch.nextInt();
			arch.close();
			return tile != Tile.aubenorBase;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			//Si algo falla, asumo que es un lugar prohibido
			return true;
		}
		
	}

	/*Quiero dejar este método obsoleto, no parece hacer falta si se modifica del proyecto Servidor
	 * comandos.Talk.ejecutar()
	 */
	/**
	 * Método obsoleto.
	 * 
	 * En el contexto donde un usuario intenta enviar un mensaje a otro, chequea que el receptor
	 * esté conectado. Si lo está, informa en el log del servidor que el mensaje se envió y retorna
	 * true. Si no está conectado, informa que no se pudo enviar el mensaje y retorna false.
	 * @param pqm es el paquete de mensaje que se intenta enviar
	 * @return si el receptor está conectado o no
	 */
	public static boolean mensajeAUsuario(PaqueteMensaje pqm) {
		//Refactoreando (Lucas, sabemos que fuiste vos)
		
		for (Map.Entry<Integer, PaquetePersonaje> personaje : personajesConectados.entrySet()) {
			if(personaje.getValue().getNombre().equals(pqm.getUserReceptor())) {
				Servidor.log.append(pqm.getUserEmisor() + " envió mensaje a " + pqm.getUserReceptor() + System.lineSeparator());
				return true;
			}
		}
		Servidor.log.append("El mensaje para " + pqm.getUserReceptor() + " no se envió, ya que se encuentra desconectado." + System.lineSeparator());
		return false;
		
		/*Desastre anterior:
		
		boolean result = true;
		boolean noEncontro = true;
		for (Map.Entry<Integer, PaquetePersonaje> personaje : personajesConectados.entrySet()) {
			if(noEncontro && (!personaje.getValue().getNombre().equals(pqm.getUserReceptor()))) {
				result = false;
			} else {
				result = true;
				noEncontro = false;
			}
		}
		// Si existe inicio sesion
		if (result) {
			Servidor.log.append(pqm.getUserEmisor() + " envió mensaje a " + pqm.getUserReceptor() + System.lineSeparator());
				return true;
		} else {
			// Si no existe informo y devuelvo false
			Servidor.log.append("El mensaje para " + pqm.getUserReceptor() + " no se envió, ya que se encuentra desconectado." + System.lineSeparator());
			return false;
		}
		*/
	}
	
	/*Quiero dejar este método obsoleto, no parece hacer falta si se modifica del proyecto Servidor
	 * comandos.Talk.ejecutar()
	 */
	/**
	 * Método obsoleto.
	 * 
	 * En el contexto donde un usuario intenta enviar un mensaje a todos, chequea que la cantidad de conectados
	 * sea igual a la esperada. Si lo es, informa en el log del servidor que el mensaje se envió a todos y 
	 * retorna true. Si la cantidad no es igual, informa que algunos usuarios se han desconectado y retorna false.
	 * @param contador es la cantidad esperada de usuarios conectados
	 * @return si se envió el mensaje a todos o no
	 */
	public static boolean mensajeAAll(int contador) {
		//Refactoreando el desastre anterior (Lucas, sabemos que fuiste vos)
		if(personajesConectados.size() == contador+1) {
			Servidor.log.append("Se ha enviado un mensaje a todos los usuarios" + System.lineSeparator());
			return true;
		}
		Servidor.log.append("Uno o más de todos los usuarios se ha desconectado, se ha mandado el mensaje a los demas." + System.lineSeparator());
		return false;
			
		/* Desastre anterior:
		
		boolean result = true;
		if(personajesConectados.size() != contador+1) {
			result = false;
		}
		// Si existe inicio sesion
		if (result) {
			Servidor.log.append("Se ha enviado un mensaje a todos los usuarios" + System.lineSeparator());
				return true;
		} else {
			// Si no existe informo y devuelvo false
			Servidor.log.append("Uno o más de todos los usuarios se ha desconectado, se ha mandado el mensaje a los demas." + System.lineSeparator());
			return false;
		}
		*/
	}
	
	public static ArrayList<EscuchaCliente> getClientesConectados() {
		return clientesConectados;
	}
	
	public static void setUbicacionPersonajes(Map<Integer, PaqueteMovimiento> ubicacionPersonajes) {
		Servidor.ubicacionPersonajes = ubicacionPersonajes;
	}

	public static Map<Integer, PaqueteMovimiento> getUbicacionPersonajes() {
		return ubicacionPersonajes;
	}
	
	public static Map<Integer, PaquetePersonaje> getPersonajesConectados() {
		return personajesConectados;
	}

	public static Conector getConector() {
		return conexionDB;
	}
	
	public static Map<Integer, PaqueteMovimiento> getUbicacionNPCs() {
		return ubicacionNPCs;
	}
	
	public static void setUbicacionNPCs(Map<Integer, PaqueteMovimiento> ubicacionNPCs) {
		Servidor.ubicacionNPCs = ubicacionNPCs;
	}

	public static Map<Integer, PaqueteNPC> getNPCs() {
		return NPCs;
	}

	public static void setNPCs(Map<Integer, PaqueteNPC> NPCs) {
		Servidor.NPCs = NPCs;
	}
}