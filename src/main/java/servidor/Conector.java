package servidor;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import dominio.Item;
import dominio.Mochila;
import mensajeria.PaquetePersonaje;
import mensajeria.PaqueteUsuario;

public class Conector {
	Configuration cfg;
	SessionFactory factory;
	Session session;

	/**
	 * Establece la conexión con la base de datos
	 */
	public void connect() {
		try {
			Servidor.log.append("Estableciendo conexión con la base de datos..." + System.lineSeparator());
			//Hibernate
			cfg = new Configuration();
			cfg.configure("hibernate.cfg.xml");
			factory = cfg.buildSessionFactory();
			Servidor.log.append("Conexión con la base de datos establecida con éxito." + System.lineSeparator());
			
		} catch (HibernateException ex) {
			Servidor.log.append("Fallo al intentar establecer la conexión con la base de datos. " + ex.getMessage()
					+ System.lineSeparator());
		}
	}
	
	/**
	 * Termina la conexión con la base de datos
	 */
	public void close() {
		try {
			factory.close();
		} catch (HibernateException ex) {
			Servidor.log.append("Error al intentar cerrar la conexión con la base de datos." + System.lineSeparator());
			Logger.getLogger(Conector.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	
	
	/**
	 * Guarda un usuario nuevo en la base de datos
	 * @param user: paquete de usuario con los datos del nuevo usuario
	 * @return True si pudo registrarlo correctamente, false si ocurrió algún error
	 */
	public boolean registrarUsuario(PaqueteUsuario user) {
		//--------------HIBERNATE ANDANDO------------------------
		System.out.println("Registrar Usuario");
		
		// Preparo sesion de hibernate
		Session session = factory.openSession();

		// Preparo el criteria
		CriteriaBuilder cBuilder = session.getCriteriaBuilder();
		CriteriaQuery<PaqueteUsuario> cQuery = cBuilder.createQuery(PaqueteUsuario.class);
		Root<PaqueteUsuario> root = cQuery.from(PaqueteUsuario.class);

		// Ejecuto la query buscando usuarios con ese nombre
		cQuery.select(root).where(cBuilder.equal(root.get("username"), user.getUsername()));

		// Si no existen usuarios con ese nombre
		if (session.createQuery(cQuery).getResultList().isEmpty()) {

			// Registro el usuario
			Transaction transaccion = session.beginTransaction();
			try {
				session.save(user);
				session.flush();
				transaccion.commit();
			} catch (HibernateException e) {
				// Si falló, hago un rollback de la transaccion, cierro sesion, escribo el log y
				// me voy
				if (transaccion != null)
					transaccion.rollback();
				e.printStackTrace();

				Servidor.log.append("Eror al intentar registrar el usuario " + user.getUsername() + System.lineSeparator());
				session.close();
				return false;
			}
		} else {
			// Si ya existe un usuario con ese nombre, cierro sesion, escribo el log y me voy
			Servidor.log
					.append("El usuario " + user.getUsername() + " ya se encuentra en uso." + System.lineSeparator());
			session.close();
			return false;
		}
		Servidor.log.append("El usuario " + user.getUsername() + " se ha registrado." + System.lineSeparator());
		session.close();
		return true;
	}

	/**
	 * Regista un personaje nuevo para un usuario
	 * @param paquetePersonaje: paquete con los datos del personaje
	 * @param paqueteUsuario: paquete con los datos del usuario
	 * @return true si se pudo registrar, false si hubo problemas
	 */
	public boolean registrarPersonaje(PaquetePersonaje personaje, PaqueteUsuario user) {
		//--------------HIBERNATE ANDANDO------------------------
		System.out.println("Registrar personaje");
		
		// Preparo sesion de hibernate
		Session session = factory.openSession();
		
		int personajeId;
		
		//Registro el personaje
		Transaction transaccion = session.beginTransaction();
		try {
			personajeId = (Integer) session.save(personaje);
			session.flush();
			transaccion.commit();
		}catch (HibernateException e) {
			// Si falló, hago un rollback de la transaccion, cierro sesion, escribo el log y me voy
			if (transaccion != null)
				transaccion.rollback();
			e.printStackTrace();

			Servidor.log.append("Eror al intentar registrar el personaje del "
					+ "usuario " + user.getUsername() + System.lineSeparator());
			session.close();
			return false;
		}
		
		//Asigno el id al personaje, lo asocio al usuario e inicializo la mochila
		personaje.setId(personajeId);
		personaje.setMochila(personajeId);
		personaje.setInventario(personajeId);
		
		user.setIdPj(personajeId);

		personaje.setBackPack(new Mochila());
		personaje.getBackPack().setMochila(personajeId);
		
		//Guardo usuario con el nuevo personaje asociado y el personaje con los ids y la mochila
		transaccion = session.beginTransaction();
		try {
			session.saveOrUpdate(user);
			session.flush();
			session.saveOrUpdate(personaje);
			session.flush();
			session.saveOrUpdate(personaje.getBackPack());
			session.flush();
			transaccion.commit();
		} catch (HibernateException e) {
			// Si falló, hago un rollback de la transaccion, cierro sesion, escribo el log y me voy
			if (transaccion != null)
				transaccion.rollback();
			e.printStackTrace();

			Servidor.log.append("Eror al intentar asociar el personaje al "
					+ "usuario " + user.getUsername() + System.lineSeparator());
			session.close();
			return false;
		}
		
		Servidor.log.append("El usuario " + user.getUsername() + " ha creado el personaje "
				+ personaje.getId() + System.lineSeparator());
		session.close();
		return true;
	}
	
	/**
	 * OBSOLETO
	 * Registra el inventario y la mochila para un personaje
	 * @param idInventarioMochila: numero id del personaje, inventario y mochila
	 * @return true si se pudo registrar, false si hubo problemas
	 */
	public boolean registrarInventarioMochila(int idInventarioMochila) {
		//--------------HIBERNATE OBSOLETO------------------------
		System.out.println("Registrar Inventario Mochila");
		
		//Preparo la sesion
		Session session = factory.openSession();

		//Preparo la mochila
		Mochila mochila = new Mochila();
		mochila.setMochila(idInventarioMochila);
		
		//Registro la mochila
		Transaction transaccion = session.beginTransaction();
		try {
			session.save(mochila);
			session.flush();
			transaccion.commit();
		} catch (HibernateException e) {
			// Si falló, hago un rollback de la transaccion, cierro sesion, escribo el log y me voy
			if (transaccion != null)
				transaccion.rollback();
			e.printStackTrace();
			session.close();
			Servidor.log.append("Error al registrar el inventario de " + idInventarioMochila + System.lineSeparator());
			return false;
		}
		Servidor.log.append("Se ha registrado el inventario de " + idInventarioMochila + System.lineSeparator());
		session.close();
		return true;
	}
	
	/**
	 * Autentica al usuario. Este método tiene seguridad nula.
	 * @param user: paquete con los datos del usuario
	 * @return true si se autenticó correctamente, false si hubo problemas
	 */
	public boolean loguearUsuario(PaqueteUsuario user) {
		//--------------HIBERNATE ANDANDO------------------------
		System.out.println("Loguear usuario");
		
		//Preparo la sesion y el criteria
		Session session = factory.openSession();
		CriteriaBuilder cBuilder = session.getCriteriaBuilder();
		CriteriaQuery<PaqueteUsuario> cQuery = cBuilder.createQuery(PaqueteUsuario.class);
		Root<PaqueteUsuario> root = cQuery.from(PaqueteUsuario.class);

		//Busco el par usuario y contrasenia
		cQuery.select(root).where(cBuilder.equal(root.get("username"), user.getUsername()),
				cBuilder.equal(root.get("password"), user.getPassword()));

		// Si existe, inicia sesion
		if (!session.createQuery(cQuery).getResultList().isEmpty()) {
			Servidor.log.append("El usuario " + user.getUsername() + " ha iniciado sesión." + System.lineSeparator());
			session.close();
			return true;
		}
		//Si no existe, informo y devuelvo false
		Servidor.log.append("El usuario " + user.getUsername() + " ha realizado un intento fallido de inicio de sesión." + System.lineSeparator());
		session.close();
		return false;
	}

	/**
	 * Actualiza datos del personaje
	 * @param paquetePersonaje: paquete con los datos del personaje
	 */
	public void actualizarPersonaje(PaquetePersonaje paquetePersonaje) {
		//--------------HIBERNATE ANDANDO------------------------
		System.out.println("Actualizar persoanje");
		
		//Preparo la sesion
		Session session = factory.openSession();
		Transaction transaccion = session.beginTransaction();
		
		//Actualizo el personaje en la BD
		try {
			session.update(paquetePersonaje);
			session.flush();
			transaccion.commit();
		} catch (HibernateException e) {
			if(transaccion != null)
				transaccion.rollback();
			e.printStackTrace();
			
			Servidor.log.append("Fallo al intentar actualizar el personaje " + paquetePersonaje.getNombre()  + System.lineSeparator());
			session.close();
			return;
		}
		
		//Actualizo los items en memoria...
		// Preparo el criteria
		CriteriaBuilder cBuilder = session.getCriteriaBuilder();
		CriteriaQuery<Mochila> cQueryM = cBuilder.createQuery(Mochila.class);
		Root<Mochila> rootM = cQueryM.from(Mochila.class);

		try {
			// Busco la mochila de este personaje
			cQueryM.select(rootM).where(cBuilder.equal(rootM.get("mochila"), paquetePersonaje.getMochila()));
	
			// Si existe la mochila, la seteo
			LinkedList<Mochila> resultMochila = new LinkedList<Mochila>(session.createQuery(cQueryM).getResultList());
			if (!resultMochila.isEmpty()) {
				paquetePersonaje.setBackPack(resultMochila.getFirst());
				
				//Pongo todos los items
				//Preparo el criteria para conseguir los datos de cada item
				CriteriaQuery<Item> cQueryI = cBuilder.createQuery(Item.class);
				Root<Item> rootI = cQueryI.from(Item.class);
				Mochila mochi = paquetePersonaje.getBackPack();
				
				//Le borro los items al personaje en memoria
				paquetePersonaje.eliminarItems();
				
				//Pongo cada uno de los 9
				for(int i=0; i<9; i++) {
					if(mochi.getItem(i) > -1) {
						cQueryI.select(rootI).where(cBuilder.equal(rootI.get("idItem"), mochi.getItem(i)));
						if(!session.createQuery(cQueryI).getResultList().isEmpty())
							paquetePersonaje.anadirItemYBonus(session.createQuery(cQueryI).getResultList().get(0));
					}
				}
			}
		} catch(HibernateException e) {
			Servidor.log.append("Fallo al intentar recuperar el inventario del personaje " + paquetePersonaje.getNombre()  + System.lineSeparator());
			session.close();
			return;
		}
		
		Servidor.log.append("El personaje " + paquetePersonaje.getNombre() + " se ha actualizado con éxito."  + System.lineSeparator());
		session.close();
	}
	
	/**
	 * Trae el personaje de un usuario de la base de datos
	 * @param user: paquete con los datos del usuario
	 * @return paquete de personaje con los datos de la BD o datos por default si hubo algún error
	 * @throws IOException
	 */
	public PaquetePersonaje getPersonaje(PaqueteUsuario user) throws IOException {
		//--------------HIBERNATE ANDANDO------------------------
		System.out.println("Get personaje");
		
		//Preparo hibernate y criteria
		Session session = factory.openSession();
		CriteriaBuilder cBuilder = session.getCriteriaBuilder();
		
		CriteriaQuery<PaqueteUsuario> queryUser = cBuilder.createQuery(PaqueteUsuario.class);
		CriteriaQuery<PaquetePersonaje> queryPj = cBuilder.createQuery(PaquetePersonaje.class);
		CriteriaQuery<Mochila> queryMochi = cBuilder.createQuery(Mochila.class);
		CriteriaQuery<Item> queryItem = cBuilder.createQuery(Item.class);
		
		Root<PaqueteUsuario> rootUser = queryUser.from(PaqueteUsuario.class);
		Root<PaquetePersonaje> rootPj = queryPj.from(PaquetePersonaje.class);
		Root<Mochila> rootMochi = queryMochi.from(Mochila.class);
		Root<Item> rootItem = queryItem.from(Item.class);
		
		try {
			//Busco el usuario para levantar el id del personaje asociado
			queryUser.select(rootUser).where(cBuilder.equal(rootUser.get("username"), user.getUsername()));
			int pjId = session.createQuery(queryUser).getSingleResult().getIdPj();
			
			//Busco el personaje
			queryPj.select(rootPj).where(cBuilder.equal(rootPj.get("id"), pjId));
			PaquetePersonaje personaje = session.createQuery(queryPj).getSingleResult();
			
			//Busco la mochila y se la pongo al personaje
			queryMochi.select(rootMochi).where(cBuilder.equal(rootMochi.get("mochila"), pjId));
			Mochila mochi = session.createQuery(queryMochi).getSingleResult();
			personaje.setBackPack(mochi);
			
			//Busco los items y se los asigno al personaje
			for(int i=0; i<9; i++) {
				if(mochi.getItem(i) > -1) {
					queryItem.select(rootItem).where(cBuilder.equal(rootItem.get("idItem"), mochi.getItem(i)));
					personaje.anadirItemYBonus(session.createQuery(queryItem).getSingleResult());
				}
			}
			
			//Devuelvo el personaje listo
			session.close();
			return personaje;
			
		}catch(HibernateException e) {
			Servidor.log.append("Fallo al intentar recuperar el personaje " + user.getUsername() + System.lineSeparator());
			session.close();
		}
		//Si hubo algún error, devuelvo un personaje vacío
		return new PaquetePersonaje();
	}
	
	/**
	 * Trae un usuario de la base de datos a partir del nombre
	 * @param usuario: nombre del usuario
	 * @return paquete con los datos del usuario, o con datos por default si hubo algún error
	 */
	public PaqueteUsuario getUsuario(String usuario) {
		
		System.out.println("Get usuario");
		
		//Preparo hibernate y criteria
		Session session = factory.openSession();
		CriteriaBuilder cBuilder = session.getCriteriaBuilder();
		CriteriaQuery<PaqueteUsuario> cQuery = cBuilder.createQuery(PaqueteUsuario.class);
		Root<PaqueteUsuario> root = cQuery.from(PaqueteUsuario.class);
		
		//Busco el usuario y lo devuelvo
		try {
			cQuery.select(root).where(cBuilder.equal(root.get("username"), usuario));
			PaqueteUsuario user = session.createQuery(cQuery).getSingleResult();
			session.close();
			return user;
		} catch (HibernateException e) {
			Servidor.log.append("Fallo al intentar recuperar el usuario " + usuario + System.lineSeparator());
			session.close();
		}
		//Si algo falla, devuelvo un paquete de usuario vacio
		return new PaqueteUsuario();
	}

	/**
	 * Actualiza el inventario de un personaje en la base de datos
	 * @param paquetePersonaje: paquete con todos los datos del personaje
	 */
	public void actualizarInventario(PaquetePersonaje paquetePersonaje) {
		//--------------HIBERNATE ANDANDO------------------------
		System.out.println("Actualizar inventario");
		
		//Preparo la sesion
		Session session = factory.openSession();
		Transaction transaccion = session.beginTransaction();
		
		//Actualizo la mochila en memoria
		paquetePersonaje.getBackPack().setItems(paquetePersonaje.getItems());
		
		//Actualizo la mochila en la BD
		try {
			session.update(paquetePersonaje.getBackPack());
			session.flush();
			transaccion.commit();
		} catch (HibernateException e) {
			if(transaccion != null)
				transaccion.rollback();
			e.printStackTrace();
			session.close();
			Servidor.log.append("Fallo al intentar actualizar el inventario del personaje " + paquetePersonaje.getNombre()  + System.lineSeparator());
			return;
		}
		session.close();
		return;
	}		
	
	/**
	 * Actualiza el inventario de un personaje a partir de su id
	 * @param idPersonaje: numero id del personaje
	 */
	public void agregarItemInventario(int idPersonaje) {
		//--------------HIBERNATE ANDANDO------------------------
		System.out.println("Agregar item inventario");
		
		
		Session session = factory.openSession();
		PaquetePersonaje paquetePersonaje = Servidor.getPersonajesConectados().get(idPersonaje);
		
		//Si no tiene el inventario lleno, le doy un item mas
		if(paquetePersonaje.getCantItems() < 9)
			paquetePersonaje.anadirItem(new Random().nextInt(29) + 1);
		
		//Actualizo la mochila en memoria
		paquetePersonaje.getBackPack().setItems(paquetePersonaje.getItems());
		
		//Actualizo la mochila en la BD
		Transaction transaccion = session.beginTransaction();
		try {
			session.update(paquetePersonaje.getBackPack());
			session.flush();
			transaccion.commit();
		} catch (HibernateException e) {
			if(transaccion != null)
				transaccion.rollback();
			e.printStackTrace();
			session.close();
			Servidor.log.append("Fallo al intentar agregar un item al personaje " + paquetePersonaje.getNombre()  + System.lineSeparator());
		}
		session.close();
	}

	/**
	 * Actualiza stats de un personaje porque subió de nivel
	 * @param paquetePersonaje: paquete con los datos del personaje
	 */
	public void actualizarPersonajeSubioNivel(PaquetePersonaje paquetePersonaje) {
		//--------------HIBERNATE ANDANDO------------------------
		System.out.println("Actualizar persoanje subio nivel");
		
		Session session = factory.openSession();
		Transaction transaccion = session.beginTransaction();
		try {
			session.update(paquetePersonaje);
			session.flush();
			transaccion.commit();
		}catch (HibernateException e) {
			if(transaccion != null)
				transaccion.rollback();
			e.printStackTrace();
			session.close();
			Servidor.log.append("Fallo al intentar actualizar el personaje " + paquetePersonaje.getNombre()  + System.lineSeparator());
			return;
		}
		Servidor.log.append("El personaje " + paquetePersonaje.getNombre() + " se ha actualizado con éxito."  + System.lineSeparator());
		session.close();
	}
}
