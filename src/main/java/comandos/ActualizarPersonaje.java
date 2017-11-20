package comandos;

import mensajeria.PaquetePersonaje;
import servidor.Servidor;

public class ActualizarPersonaje extends ComandosServer {

	@Override
	public void ejecutar() {
		escuchaCliente.setPaquetePersonaje((PaquetePersonaje) gson.fromJson(cadenaLeida, PaquetePersonaje.class));
		
		Servidor.getConector().actualizarPersonaje(escuchaCliente.getPaquetePersonaje());
		
		super.notificarPjActualizado();

	}

}
