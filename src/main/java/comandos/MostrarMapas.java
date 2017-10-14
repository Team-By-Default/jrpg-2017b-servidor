package comandos;

import java.io.IOException;

import mensajeria.PaqueteNPCs;
import mensajeria.PaquetePersonaje;
import servidor.Servidor;

public class MostrarMapas extends ComandosServer{

	@Override
	public void ejecutar() {
		//El cliente elige un mapa
		escuchaCliente.setPaquetePersonaje((PaquetePersonaje) gson.fromJson(cadenaLeida, PaquetePersonaje.class));
		Servidor.log.append(escuchaCliente.getSocket().getInetAddress().getHostAddress() + " ha elegido el mapa " + escuchaCliente.getPaquetePersonaje().getMapa() + System.lineSeparator());
		
		//Genera los NPC
		PaqueteNPCs paqueteNPCs = new PaqueteNPCs(Servidor.getNPCs());
		paqueteNPCs.setComando(ACTUALIZARNPCS);
		try {
			escuchaCliente.getSalida().writeObject(gson.toJson(paqueteNPCs));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
