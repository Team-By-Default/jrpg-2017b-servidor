package comandos;

import java.io.IOException;
import mensajeria.Comando;
import mensajeria.Paquete;
import mensajeria.PaqueteUsuario;
import servidor.Servidor;

public class Registro extends ComandosServer {

	@Override
	public void ejecutar() {
		Paquete paqueteSv = new Paquete(null, 0);
		paqueteSv.setComando(Comando.REGISTRO);
		
		escuchaCliente.setPaqueteUsuario((PaqueteUsuario) (gson.fromJson(cadenaLeida, PaqueteUsuario.class)).clone());

		// Si el usuario se pudo registrar le envio un msj de exito
		try {
			if (Servidor.getConector().registrarUsuario(escuchaCliente.getPaqueteUsuario())) {
				paqueteSv.setMensaje(Paquete.msjExito);
				escuchaCliente.getSalida().writeObject(gson.toJson(paqueteSv));

				// Si el usuario no se pudo registrar le envio un msj de fracaso
			} else {
				paqueteSv.setMensaje(Paquete.msjFracaso);
				escuchaCliente.getSalida().writeObject(gson.toJson(paqueteSv));
			}
		} catch (IOException e) {
			Servidor.log.append("Falló al intentar enviar registro\n");
		}

	}

}
