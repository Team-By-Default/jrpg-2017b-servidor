package comandos;

import mensajeria.Comando;
import mensajeria.PaquetePersonaje;
import servidor.Servidor;

public class Cheat extends ComandosServer {

	@Override
	public void ejecutar() {
		PaquetePersonaje pj = (PaquetePersonaje) gson.fromJson(cadenaLeida, PaquetePersonaje.class);
		pj.setComando(Comando.ACTUALIZARPERSONAJE);
		escuchaCliente.setPaquetePersonaje(pj);
		Servidor.log.append("¡El personaje " + escuchaCliente.getPaquetePersonaje().getId() + " está haciendo trampa!\n");
		super.notificarPjActualizado();
	}

}
