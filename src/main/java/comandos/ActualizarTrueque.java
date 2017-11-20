package comandos;

import mensajeria.PaquetePersonaje;
import servidor.Servidor;

public class ActualizarTrueque extends ComandosServer {

	@Override
	public void ejecutar() {
		escuchaCliente.setPaquetePersonaje((PaquetePersonaje) gson.fromJson(cadenaLeida, PaquetePersonaje.class));
		
		Servidor.getConector().actualizarInventario(escuchaCliente.getPaquetePersonaje());
		Servidor.getConector().actualizarPersonaje(escuchaCliente.getPaquetePersonaje());
		
		super.notificarPjActualizado();

	}

}
