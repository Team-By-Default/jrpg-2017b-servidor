package comandos;

import java.io.IOException;

import mensajeria.PaqueteComerciar;
import mensajeria.PaquetePersonajeDominio;
import servidor.EscuchaCliente;
import servidor.Servidor;

public class ChuckNorrisEnBatalla extends ComandosServer{

	@Override
	public void ejecutar() {
		PaquetePersonajeDominio pj = (PaquetePersonajeDominio) gson.fromJson(cadenaLeida, PaquetePersonajeDominio.class);
		//BUSCO EN LAS ESCUCHAS AL QUE SE LO TENGO QUE MANDAR
		for(EscuchaCliente conectado : Servidor.getClientesConectados()) {
			if(conectado.getPaquetePersonaje().getId() == pj.getIdEnemigo()) {
				try {
					conectado.getSalida().writeObject(gson.toJson(pj));
				} catch (IOException e) {
					Servidor.log.append("Falló al intentar enviar paquete de personaje dominio a:" + conectado.getPaquetePersonaje().getId() + "\n");
				}	
			}
		}
	}
	
}
