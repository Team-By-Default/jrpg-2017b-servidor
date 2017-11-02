package comandos;

import java.io.IOException;
import java.util.Random;

import estados.Estado;
import mensajeria.Comando;
import mensajeria.PaqueteDePersonajes;
import mensajeria.PaqueteFinalizarBatalla;
import mensajeria.PaqueteMovimiento;
import mensajeria.PaqueteNPCs;
import servidor.EscuchaCliente;
import servidor.Servidor;

public class FinalizarBatallaNPC extends ComandosServer{

	@Override
	public void ejecutar() {
		//Recibo el paquete de finalizar batalla
		PaqueteFinalizarBatalla paqueteFinalizarBatalla = (PaqueteFinalizarBatalla) gson.fromJson(cadenaLeida, PaqueteFinalizarBatalla.class);
		paqueteFinalizarBatalla.setComando(FINALIZARBATALLA);
		escuchaCliente.setPaqueteFinalizarBatalla(paqueteFinalizarBatalla);
		
		//El personaje ya no está peleando
		Servidor.getConector().actualizarInventario( paqueteFinalizarBatalla.getId() );
		Servidor.getPersonajesConectados().get(escuchaCliente.getPaqueteFinalizarBatalla().getId()).setEstado(Estado.estadoJuego);
		
		//El NPC ya no está peleando
		Servidor.getNPCs().get(paqueteFinalizarBatalla.getIdEnemigo()).setPeleando(false);
		
		//Si ganó el NPC, lo reubico
		if( paqueteFinalizarBatalla.getGanadorBatalla() < 0){
			Servidor.getUbicacionNPCs().remove( paqueteFinalizarBatalla.getIdEnemigo() );	
			float[] newPos = Servidor.generarPosIso(new Random());
			PaqueteMovimiento newPosicion = new PaqueteMovimiento( paqueteFinalizarBatalla.getIdEnemigo() , newPos[0], newPos[1]);
					
			Servidor.getUbicacionNPCs().put( paqueteFinalizarBatalla.getIdEnemigo(), newPosicion);
		}
		
		
		//Preparo paquetes y envío
		PaqueteNPCs pNPCs = new PaqueteNPCs(Servidor.getNPCs());
		pNPCs.setComando(Comando.ACTUALIZARNPCS);
		PaqueteDePersonajes personajes = new PaqueteDePersonajes(Servidor.getPersonajesConectados());
		
		for(EscuchaCliente conectado : Servidor.getClientesConectados()) 
		{
			//Al personaje de la batalla le envío Finalizar Batalla, a los demás les actualizo los estados de los NPC y personaje
			if( conectado.getIdPersonaje() == escuchaCliente.getPaqueteFinalizarBatalla().getId() ){
				try {
					conectado.getSalida().writeObject(gson.toJson(escuchaCliente.getPaqueteFinalizarBatalla()));
				} catch (IOException e) {
					Servidor.log.append("Falló al intentar enviar finalizarBatalla a:" + conectado.getPaquetePersonaje().getId() + "\n");
				}
			}
			else {
				try {
					conectado.getSalida().writeObject(gson.toJson(pNPCs));
					conectado.getSalida().writeObject(gson.toJson(personajes));
				} catch (IOException e) {
					Servidor.log.append("Falló al intentar enviar actualización de NPCs a " + conectado.getId() + System.lineSeparator());
				}
				
			}
		}

		
		synchronized(Servidor.atencionMovimientos){
			Servidor.atencionMovimientos.notify();
		}

	}
	
	
}
