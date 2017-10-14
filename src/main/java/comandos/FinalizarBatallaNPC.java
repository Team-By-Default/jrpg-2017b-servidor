package comandos;

import java.io.IOException;

import estados.Estado;
import mensajeria.PaqueteFinalizarBatalla;
import mensajeria.PaqueteMovimiento;
import servidor.EscuchaCliente;
import servidor.Servidor;

public class FinalizarBatallaNPC extends ComandosServer{

	@Override
	public void ejecutar() {
		
		PaqueteFinalizarBatalla paqueteFinalizarBatalla = (PaqueteFinalizarBatalla) gson.fromJson(cadenaLeida, PaqueteFinalizarBatalla.class);
		paqueteFinalizarBatalla.setComando(FINALIZARBATALLA);
		escuchaCliente.setPaqueteFinalizarBatalla(paqueteFinalizarBatalla);
		
		
		Servidor.getConector().actualizarInventario( paqueteFinalizarBatalla.getId() );
		Servidor.getPersonajesConectados().get(escuchaCliente.getPaqueteFinalizarBatalla().getId()).setEstado(Estado.estadoJuego);
		
		if( paqueteFinalizarBatalla.getGanadorBatalla() < 0){ // gano el personaje	
			Servidor.getUbicacionNPCs().remove( paqueteFinalizarBatalla.getIdEnemigo() );	
			//PaqueteDeNPC newNPC;
			//new NPC = 
					
			// PaqueteDeNPC newNPC = new PaqueteDeNPC( paqueteFinalizarBatalla.getIdEnemigo() );
			PaqueteMovimiento newPosicion = new PaqueteMovimiento( paqueteFinalizarBatalla.getIdEnemigo() , 100 + ((float) Math.random() * 500), 10 +( (float)Math.random() * 500));
					
			Servidor.getUbicacionNPCs().put( paqueteFinalizarBatalla.getIdEnemigo(), newPosicion);
			
		}
		
		
		for(EscuchaCliente conectado : Servidor.getClientesConectados()) 
		{
			if( conectado.getIdPersonaje() == escuchaCliente.getPaqueteFinalizarBatalla().getId() ){
			try {
				conectado.getSalida().writeObject(gson.toJson(escuchaCliente.getPaqueteFinalizarBatalla()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Servidor.log.append("Falló al intentar enviar finalizarBatalla a:" + conectado.getPaquetePersonaje().getId() + "\n");
			}
		}
		}

		
		synchronized(Servidor.atencionMovimientos){
			Servidor.atencionMovimientos.notify();
		}

	}
	
	
}
