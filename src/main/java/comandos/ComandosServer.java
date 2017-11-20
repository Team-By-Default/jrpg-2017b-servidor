package comandos;

import java.io.IOException;

import mensajeria.Comando;
import servidor.EscuchaCliente;
import servidor.Servidor;

public abstract class ComandosServer extends Comando{
	protected EscuchaCliente escuchaCliente;

	public void setEscuchaCliente(EscuchaCliente escuchaCliente) {
		this.escuchaCliente = escuchaCliente;
	}
	
	protected void notificarPjActualizado() {
		Servidor.getPersonajesConectados().remove(escuchaCliente.getPaquetePersonaje().getId());
		Servidor.getPersonajesConectados().put(escuchaCliente.getPaquetePersonaje().getId(), escuchaCliente.getPaquetePersonaje());

		for(EscuchaCliente conectado : Servidor.getClientesConectados()) {
			try {
				conectado.getSalida().writeObject(gson.toJson(escuchaCliente.getPaquetePersonaje()));
			} catch (IOException e) {
				Servidor.log.append("Falló al intentar enviar paquetePersonaje a:" + conectado.getPaquetePersonaje().getId() + "\n");
			}
		}
	}
	
}
