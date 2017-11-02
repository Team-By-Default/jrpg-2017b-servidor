package comandos;

import java.io.IOException;

import estados.Estado;
import mensajeria.Comando;
import mensajeria.PaqueteBatallaNPC;
import mensajeria.PaqueteDePersonajes;
import mensajeria.PaqueteNPCs;
import servidor.EscuchaCliente;
import servidor.Servidor;

public class BatallaNPC extends ComandosServer{

	@Override
	public void ejecutar() {
		escuchaCliente.setPaqueteBatallaNPC((PaqueteBatallaNPC) gson.fromJson(cadenaLeida, PaqueteBatallaNPC.class));
		
		Servidor.log.append(escuchaCliente.getPaqueteBatallaNPC().getId() + " quiere batallar con NPC "
				+ escuchaCliente.getPaqueteBatallaNPC().getIdEnemigo() + System.lineSeparator());
		
		//Seteo los estados en batalla
		Servidor.getPersonajesConectados().get(escuchaCliente.getPaqueteBatallaNPC().getId()).setEstado(Estado.estadoBatallaNPC);
		Servidor.getNPCs().get(escuchaCliente.getPaqueteBatallaNPC().getIdEnemigo()).setPeleando(true);
		
		//Envío batalla al cliente
		try {
			escuchaCliente.getSalida().writeObject(gson.toJson(escuchaCliente.getPaqueteBatallaNPC()));
		} catch (IOException e) {
			Servidor.log.append("Falló al intentar enviar Batalla con NPC \n");
		}
		
		//Preparo y envío paquetes a todos los clientes para que desaparezcan el NPC y personaje en batalla
		PaqueteNPCs pNPCs = new PaqueteNPCs(Servidor.getNPCs());
		pNPCs.setComando(Comando.ACTUALIZARNPCS);
		PaqueteDePersonajes personajes = new PaqueteDePersonajes(Servidor.getPersonajesConectados());

		for(EscuchaCliente conectado : Servidor.getClientesConectados()) {
			try {
				conectado.getSalida().writeObject(gson.toJson(pNPCs));
				conectado.getSalida().writeObject(gson.toJson(personajes));
			} catch (IOException e) {
				Servidor.log.append("Falló al intentar enviar actualización de NPCs a " + conectado.getId() + System.lineSeparator());
			}
		}
	}
	
}
