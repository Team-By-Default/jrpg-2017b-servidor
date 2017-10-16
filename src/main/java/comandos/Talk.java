package comandos;

import java.io.IOException;
import java.util.Map;

import mensajeria.Comando;
import mensajeria.PaqueteMensaje;
import mensajeria.PaquetePersonaje;
import servidor.EscuchaCliente;
import servidor.Servidor;

public class Talk extends ComandosServer {
	
	/**
	 * Envía un mensaje de por chat de un usuario a otro por privado
	 * o de un usuario a todos por la sala general
	 */
	@Override
	public void ejecutar() {
		int idUser = 0;
		int contador = 0;
		PaqueteMensaje paqueteMensaje = (PaqueteMensaje) (gson.fromJson(cadenaLeida, PaqueteMensaje.class));
		
		//Si el mensaje es a un receptor específico
		if (!(paqueteMensaje.getUserReceptor() == null)) {
			boolean receptorConectado = false;
			paqueteMensaje.setComando(Comando.TALK);
			
			//Busco al receptor entre todos los personajes conectados y obtengo su ID
			for (Map.Entry<Integer, PaquetePersonaje> personaje : Servidor.getPersonajesConectados().entrySet()) {
				if(personaje.getValue().getNombre().equals(paqueteMensaje.getUserReceptor())) {
					receptorConectado = true;
					idUser = personaje.getValue().getId();
				}
			}
			
			//Si el receptor está conectado
			if(receptorConectado) {
				Servidor.log.append(paqueteMensaje.getUserEmisor() + " envió mensaje a " + paqueteMensaje.getUserReceptor() + System.lineSeparator());
				
				/*Busco entre todos los clientes conectados al que tenga el personaje receptor y envío el mensaje
				 * No queda otra que buscar así, no hay una conexión directa entre un personaje y el EscuchaCliente correspondiente
				 */
				for (EscuchaCliente conectado : Servidor.getClientesConectados()) {
					if(conectado.getIdPersonaje() == idUser) {
						try {
							conectado.getSalida().writeObject(gson.toJson(paqueteMensaje));
						} catch (IOException e) {
							Servidor.log.append("Falló al intentar enviar mensaje a:" + conectado.getPaquetePersonaje().getId() + "\n");
						}
					}
				}
			}
			else
				//Si no encontré al personaje receptor
				Servidor.log.append("El mensaje para " + paqueteMensaje.getUserReceptor() + " no se envió, ya que se encuentra desconectado." + System.lineSeparator());
		}
		else {
			//Si el mensaje no es para un receptor específico, es para todos
			
			//Busco al emisor entre todos los personajes conectados y obtengo su ID
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
		}
			
	}
		
		/* Version anterior
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
				
				/*Y obtenido el idUser pregunta contra todos los clientes... 
				 * Parece que no queda otra, no hay ninguna conexión directa entre un paquete de personaje y el
				 * EscuchaCliente correspondiente a su usuario... 
				 */
				//El log es casi lo mismo que mensajeAUsuario
		/*
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
	*/
}
