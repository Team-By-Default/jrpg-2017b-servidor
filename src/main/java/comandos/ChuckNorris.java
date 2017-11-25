package comandos;

import java.io.IOException;
import mensajeria.PaqueteDios;
import servidor.EscuchaCliente;
import servidor.Servidor;

public class ChuckNorris extends ComandosServer{

	@Override
	public void ejecutar() {
		PaqueteDios pj = (PaqueteDios) gson.fromJson(cadenaLeida, PaqueteDios.class);
		//BUSCO EN LAS ESCUCHAS AL QUE SE LO TENGO QUE MANDAR
		for(EscuchaCliente conectado : Servidor.getClientesConectados()) {
			if(conectado.getPaquetePersonaje().getId() == pj.getIdEnemigo()) {
				try {
					conectado.getSalida().writeObject(gson.toJson(pj));
					if (pj.isGod())
						Servidor.log.append("El personaje " + pj.getIdPersonaje() + " se cree Chuck Norris" + System.lineSeparator());
					else
						Servidor.log.append("El personaje " + pj.getIdPersonaje() + " ya no se cree Chuck Norris" + System.lineSeparator());
				} catch (IOException e) {
					Servidor.log.append("Falló al intentar enviar paquete Chuck Norris a:" + conectado.getPaquetePersonaje().getId() + "\n");
				}	
			}
		}
	}
	
}
