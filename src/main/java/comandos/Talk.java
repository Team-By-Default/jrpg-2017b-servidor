package comandos;

import java.io.IOException;
import java.util.Map;

import mensajeria.Comando;
import mensajeria.PaqueteMensaje;
import mensajeria.PaquetePersonaje;
import servidor.EscuchaCliente;
import servidor.Servidor;

public class Talk extends ComandosServer {

	@Override
	public void ejecutar() {
		int idUser = 0;
		int contador = 0;
		PaqueteMensaje paqueteMensaje = (PaqueteMensaje) (gson.fromJson(cadenaLeida, PaqueteMensaje.class));
		
		if (!(paqueteMensaje.getUserReceptor() == null)) {
			//Si el usuario receptor está conectado (quiero sacar el método mensajeAUsuario)
			if (Servidor.mensajeAUsuario(paqueteMensaje)) {
				
				paqueteMensaje.setComando(Comando.TALK);
				
				//Casi lo mismo que hace mensajeAUsuario
				for (Map.Entry<Integer, PaquetePersonaje> personaje : Servidor.getPersonajesConectados().entrySet()) {
					if(personaje.getValue().getNombre().equals(paqueteMensaje.getUserReceptor())) {
						idUser = personaje.getValue().getId();
					}
				}
				
				//Y obtenido el idUser pregunta contra todos los clientes... es necesario?
				//Ademas el log es casi lo mismo que mensajeAUsuario
				for (EscuchaCliente conectado : Servidor.getClientesConectados()) {
					if(conectado.getIdPersonaje() == idUser) {
						try {
							conectado.getSalida().writeObject(gson.toJson(paqueteMensaje));
						} catch (IOException e) {
							Servidor.log.append("Falló al intentar enviar mensaje a:" + conectado.getPaquetePersonaje().getId() + "\n");
						}
					}
				}
				
			} else {
				Servidor.log.append("No se envió el mensaje \n");
			}
			
		} else {
			//Si el receptor es null
			for (Map.Entry<Integer, PaquetePersonaje> personaje : Servidor.getPersonajesConectados().entrySet()) {
				if(personaje.getValue().getNombre().equals(paqueteMensaje.getUserEmisor())) {
					idUser = personaje.getValue().getId();
				}
			}
			
			//Manda el mensaje a todos menos al emisor
			for (EscuchaCliente conectado : Servidor.getClientesConectados()) {
				if(conectado.getIdPersonaje() != idUser) {
					try {
						conectado.getSalida().writeObject(gson.toJson(paqueteMensaje));
					} catch (IOException e) {
						Servidor.log.append("Falló al intentar enviar mensaje a:" + conectado.getPaquetePersonaje().getId() + "\n");
					}
				}
			}
			//Esto es un booleano, pero aparentemente solo me interesa el log que genera
			//Además contador nunca se modificó, sigue en 0
			Servidor.mensajeAAll(contador);	
		}
	}
}
