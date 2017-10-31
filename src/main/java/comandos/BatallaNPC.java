package comandos;

import java.io.IOException;

import estados.Estado;
import mensajeria.PaqueteBatallaNPC;
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
		try {
			escuchaCliente.getSalida().writeObject(gson.toJson(escuchaCliente.getPaqueteBatallaNPC()));
		} catch (IOException e) {
			Servidor.log.append("Falló al intentar enviar Batalla con NPC \n");
		}
	}
	
}
