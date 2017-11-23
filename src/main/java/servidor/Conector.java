package servidor;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;

import antlr.collections.List;
import dominio.Item;
import dominio.Mochila;
import mensajeria.PaquetePersonaje;
import mensajeria.PaqueteUsuario;

public class Conector {

	/**
	 * Path del archivo de la base de datos
	 */
	private String url = "primeraBase.bd";
	Connection connect;
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
			//JDBC
			connect = DriverManager.getConnection("jdbc:sqlite:" + url);
			Servidor.log.append("Conexión con la base de datos establecida con éxito." + System.lineSeparator());
			
		} catch (SQLException ex) {
			Servidor.log.append("Fallo al intentar establecer la conexión con la base de datos. " + ex.getMessage()
					+ System.lineSeparator());
		}
	}
	
	/**
	 * Termina la conexión con la base de datos
	 */
	public void close() {
		try {
			connect.close();
			factory.close();
		} catch (SQLException ex) {
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
		
		/*
		ResultSet result = null;
		try {
			
			//Busca si ya hay algún usuario con ese nombre
			PreparedStatement st1 = connect.prepareStatement("SELECT * FROM registro WHERE usuario= ? ");
			st1.setString(1, user.getUsername());
			result = st1.executeQuery();

			//Si no existía ese nombre, lo agrega
			if (!result.next()) {

				//Genera la instrucción SQL con todos los datos y la ejecuta
				PreparedStatement st = connect.prepareStatement("INSERT INTO registro (usuario, password, idPersonaje) VALUES (?,?,?)");
				st.setString(1, user.getUsername());
				st.setString(2, user.getPassword());
				st.setInt(3, user.getIdPj());
				st.execute();
				Servidor.log.append("El usuario " + user.getUsername() + " se ha registrado." + System.lineSeparator());
				return true;
			} else {
				Servidor.log.append("El usuario " + user.getUsername() + " ya se encuentra en uso." + System.lineSeparator());
				return false;
			}
		} catch (SQLException ex) {
			Servidor.log.append("Eror al intentar registrar el usuario " + user.getUsername() + System.lineSeparator());
			System.err.println(ex.getMessage());
			return false;
		}
		*/
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
		
		/*
		try {

			// Registro al personaje en la base de datos
			PreparedStatement stRegistrarPersonaje = connect.prepareStatement(
					"INSERT INTO personaje (idInventario, idMochila,casta,raza,fuerza,destreza,inteligencia,saludTope,energiaTope,nombre,experiencia,nivel,idAlianza) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
					PreparedStatement.RETURN_GENERATED_KEYS);
			stRegistrarPersonaje.setInt(1, -1);
			stRegistrarPersonaje.setInt(2, -1);
			stRegistrarPersonaje.setString(3, personaje.getCasta());
			stRegistrarPersonaje.setString(4, personaje.getRaza());
			stRegistrarPersonaje.setInt(5, personaje.getFuerza());
			stRegistrarPersonaje.setInt(6, personaje.getDestreza());
			stRegistrarPersonaje.setInt(7, personaje.getInteligencia());
			stRegistrarPersonaje.setInt(8, personaje.getSaludTope());
			stRegistrarPersonaje.setInt(9, personaje.getEnergiaTope());
			stRegistrarPersonaje.setString(10, personaje.getNombre());
			stRegistrarPersonaje.setInt(11, 0);
			stRegistrarPersonaje.setInt(12, 1);
			stRegistrarPersonaje.setInt(13, -1);
			stRegistrarPersonaje.execute();

			// Recupero la última key generada
			ResultSet rs = stRegistrarPersonaje.getGeneratedKeys();
			if (rs != null && rs.next()) {

				// Obtengo el id
				int idPersonaje = rs.getInt(1);

				// Le asigno el id al paquete personaje que voy a devolver
				personaje.setId(idPersonaje);

				// Le asigno el personaje al usuario
				PreparedStatement stAsignarPersonaje = connect.prepareStatement("UPDATE registro SET idPersonaje=? WHERE usuario=? AND password=?");
				stAsignarPersonaje.setInt(1, idPersonaje);
				stAsignarPersonaje.setString(2, user.getUsername());
				stAsignarPersonaje.setString(3, user.getPassword());
				stAsignarPersonaje.execute();

				// Por ultimo registro el inventario y la mochila
				if (this.registrarInventarioMochila(idPersonaje)) {
					Servidor.log.append("El usuario " + user.getUsername() + " ha creado el personaje "
							+ personaje.getId() + System.lineSeparator());
					return true;
				} else {
					Servidor.log.append("Error al registrar la mochila y el inventario del usuario " + user.getUsername() + " con el personaje" + personaje.getId() + System.lineSeparator());
					return false;
				}
			}
			return false;

		} catch (SQLException e) {
			Servidor.log.append(
					"Error al intentar crear el personaje " + personaje.getNombre() + System.lineSeparator());
			return false;
		}
		*/
	}
	
	/**
	 * Registra el inventario y la mochila para un personaje
	 * @param idInventarioMochila: numero id del personaje, inventario y mochila
	 * @return true si se pudo registrar, false si hubo problemas
	 */
	public boolean registrarInventarioMochila(int idInventarioMochila) {
		//--------------HIBERNATE------------------------
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
		
		/*
		try {
			// Preparo la consulta para el registro la mochila en la base de
			// datos
			PreparedStatement stRegistrarMochila = connect.prepareStatement("INSERT INTO mochila(idMochila,item1,item2,item3,item4,item5,item6,item7,item8,item9,item10,item11,item12,item13,item14,item15,item16,item17,item18,item19,item20) VALUES(?,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1)");
			stRegistrarMochila.setInt(1, idInventarioMochila);

			// Registro mochila
			stRegistrarMochila.execute();

			// Le asigno el inventario y la mochila al personaje
			PreparedStatement stAsignarPersonaje = connect
					.prepareStatement("UPDATE personaje SET idInventario=?, idMochila=? WHERE idPersonaje=?");
			stAsignarPersonaje.setInt(1, idInventarioMochila);
			stAsignarPersonaje.setInt(2, idInventarioMochila);
			stAsignarPersonaje.setInt(3, idInventarioMochila);
			stAsignarPersonaje.execute();

			Servidor.log.append("Se ha registrado el inventario de " + idInventarioMochila + System.lineSeparator());
			return true;

		} catch (SQLException e) {
			Servidor.log.append("Error al registrar el inventario de " + idInventarioMochila + System.lineSeparator());
			return false;
		}
		*/
	}
	
	/**
	 * Autentica al usuario. Este método tiene seguridad nula.
	 * @param user: paquete con los datos del usuario
	 * @return true si se autenticó correctamente, false si hubo problemas
	 */
	public boolean loguearUsuario(PaqueteUsuario user) {
		//--------------HIBERNATE------------------------
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
		
		/*
		ResultSet result = null;
		try {
			// Busco usuario y contraseña
			PreparedStatement st = connect
					.prepareStatement("SELECT * FROM registro WHERE usuario = ? AND password = ? ");
			st.setString(1, user.getUsername());
			st.setString(2, user.getPassword());
			result = st.executeQuery();

			// Si existe inicio sesion
			if (result.next()) {
				Servidor.log.append("El usuario " + user.getUsername() + " ha iniciado sesión." + System.lineSeparator());
				connect.close();
				return true;
			}

			// Si no existe informo y devuelvo false
			Servidor.log.append("El usuario " + user.getUsername() + " ha realizado un intento fallido de inicio de sesión." + System.lineSeparator());
			connect.close();
			return false;

		} catch (SQLException e) {
			Servidor.log.append("El usuario " + user.getUsername() + " fallo al iniciar sesión." + System.lineSeparator());
			try {
				connect.close();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return false;
		}
		*/
	}

	/**
	 * Actualiza datos del personaje
	 * @param paquetePersonaje: paquete con los datos del personaje
	 */
	public void actualizarPersonaje(PaquetePersonaje paquetePersonaje) {
		//--------------HIBERNATE------------------------
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
				//Pongo cada uno de los 9
				for(int i=0; i<9; i++) {
					if(mochi.getItem(i) > -1) {
						cQueryI.select(rootI).where(cBuilder.equal(rootI.get("idItem"), mochi.getItem(i)));
						if(!session.createQuery(cQueryI).getResultList().isEmpty())
							paquetePersonaje.anadirItem(session.createQuery(cQueryI).getResultList().get(0));
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
		
		/*
		try {
			int i = 2;
			int j = 1;
			//Prepara y ejecuta la instrucción SQL para actualizar las stats
			PreparedStatement stActualizarPersonaje = connect
					.prepareStatement("UPDATE personaje SET fuerza=?, destreza=?, inteligencia=?, saludTope=?, energiaTope=?, experiencia=?, nivel=? "
							+ "  WHERE idPersonaje=?");
			
			stActualizarPersonaje.setInt(1, paquetePersonaje.getFuerza());
			stActualizarPersonaje.setInt(2, paquetePersonaje.getDestreza());
			stActualizarPersonaje.setInt(3, paquetePersonaje.getInteligencia());
			stActualizarPersonaje.setInt(4, paquetePersonaje.getSaludTope());
			stActualizarPersonaje.setInt(5, paquetePersonaje.getEnergiaTope());
			stActualizarPersonaje.setInt(6, paquetePersonaje.getExperiencia());
			stActualizarPersonaje.setInt(7, paquetePersonaje.getNivel());
			stActualizarPersonaje.setInt(8, paquetePersonaje.getId());
			stActualizarPersonaje.executeUpdate();

			//Levanta la mochila y los items de la BD
			PreparedStatement stDameItemsID = connect.prepareStatement("SELECT * FROM mochila WHERE idMochila = ?");
			stDameItemsID.setInt(1, paquetePersonaje.getId());
			ResultSet resultadoItemsID = stDameItemsID.executeQuery();
			PreparedStatement stDatosItem = connect.prepareStatement("SELECT * FROM item WHERE idItem = ?");
			
			ResultSet resultadoDatoItem = null;
			//Le borra los items al personaje en memoria
			paquetePersonaje.eliminarItems();
			
			//Le pone los items al personaje en memoria según la BD
			while (j <= 9) {
				if(resultadoItemsID.getInt(i) != -1) {
					stDatosItem.setInt(1, resultadoItemsID.getInt(i));
					resultadoDatoItem = stDatosItem.executeQuery();
					
					paquetePersonaje.anadirItem(resultadoDatoItem.getInt("idItem"), resultadoDatoItem.getString("nombre"),
							resultadoDatoItem.getInt("wereable"), resultadoDatoItem.getInt("bonusSalud"),
							resultadoDatoItem.getInt("bonusEnergia"), resultadoDatoItem.getInt("bonusFuerza"),
							resultadoDatoItem.getInt("bonusDestreza"), resultadoDatoItem.getInt("bonusInteligencia"),
							resultadoDatoItem.getString("foto"), resultadoDatoItem.getString("fotoEquipado"));
				}
				i++;
				j++;
			}
			Servidor.log.append("El personaje " + paquetePersonaje.getNombre() + " se ha actualizado con éxito."  + System.lineSeparator());;
		} catch (SQLException e) {
			Servidor.log.append("Fallo al intentar actualizar el personaje " + paquetePersonaje.getNombre()  + System.lineSeparator());
		}
		*/
		
	}
	
	/**
	 * Trae el personaje de un usuario de la base de datos
	 * @param user: paquete con los datos del usuario
	 * @return paquete de personaje con los datos de la BD o datos por default si hubo algún error
	 * @throws IOException
	 */
	public PaquetePersonaje getPersonaje(PaqueteUsuario user) throws IOException {
		//--------------HIBERNATE------------------------
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
					personaje.anadirItem(session.createQuery(queryItem).getSingleResult());
				}
			}
			
			//Devuelvo el personaje listo
			return personaje;
			
		}catch(HibernateException e) {
			Servidor.log.append("Fallo al intentar recuperar el personaje " + user.getUsername() + System.lineSeparator());
		}
		//Si hubo algún error, devuelvo un personaje vacío
		return new PaquetePersonaje();
		
		/*
		try {
			connect = DriverManager.getConnection("jdbc:sqlite:" + url);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ResultSet result = null;
		ResultSet resultadoItemsID = null;
		ResultSet resultadoDatoItem = null;
		int i = 2;
		int j = 0;
		try {
			// Selecciono el personaje de ese usuario
			PreparedStatement st = connect.prepareStatement("SELECT * FROM registro WHERE usuario = ?");
			st.setString(1, user.getUsername());
			result = st.executeQuery();

			// Obtengo el id
			int idPersonaje = result.getInt("idPersonaje");

			// Selecciono los datos del personaje
			PreparedStatement stSeleccionarPersonaje = connect
					.prepareStatement("SELECT * FROM personaje WHERE idPersonaje = ?");
			stSeleccionarPersonaje.setInt(1, idPersonaje);
			result = stSeleccionarPersonaje.executeQuery();
			// Traigo los id de los items correspondientes a mi personaje
			PreparedStatement stDameItemsID = connect.prepareStatement("SELECT * FROM mochila WHERE idMochila = ?");
			stDameItemsID.setInt(1, idPersonaje);
			resultadoItemsID = stDameItemsID.executeQuery();
			// Traigo los datos del item
			PreparedStatement stDatosItem = connect.prepareStatement("SELECT * FROM item WHERE idItem = ?");
			
			
			// Obtengo los atributos del personaje
			PaquetePersonaje personaje = new PaquetePersonaje();
			personaje.setId(idPersonaje);
			personaje.setRaza(result.getString("raza"));
			personaje.setCasta(result.getString("casta"));
			personaje.setFuerza(result.getInt("fuerza"));
			personaje.setInteligencia(result.getInt("inteligencia"));
			personaje.setDestreza(result.getInt("destreza"));
			personaje.setEnergiaTope(result.getInt("energiaTope"));
			personaje.setSaludTope(result.getInt("saludTope"));
			personaje.setNombre(result.getString("nombre"));
			personaje.setExperiencia(result.getInt("experiencia"));
			personaje.setNivel(result.getInt("nivel"));
			
			//Pone los items al personaje
			while (j <= 9) {
				if(resultadoItemsID.getInt(i) != -1) {
					stDatosItem.setInt(1, resultadoItemsID.getInt(i));
					resultadoDatoItem = stDatosItem.executeQuery();
					personaje.anadirItem(resultadoDatoItem.getInt("idItem"), resultadoDatoItem.getString("nombre"),
							resultadoDatoItem.getInt("wereable"), resultadoDatoItem.getInt("bonusSalud"),
							resultadoDatoItem.getInt("bonusEnergia"), resultadoDatoItem.getInt("bonusFuerza"),
							resultadoDatoItem.getInt("bonusDestreza"), resultadoDatoItem.getInt("bonusInteligencia"),
							resultadoDatoItem.getString("foto"), resultadoDatoItem.getString("fotoEquipado"));
				}
				i++;
				j++;
			}
			

			// Devuelvo el paquete personaje con sus datos
			connect.close();
			return personaje;

		} catch (SQLException ex) {
			Servidor.log.append("Fallo al intentar recuperar el personaje " + user.getUsername() + System.lineSeparator());
			Servidor.log.append(ex.getMessage() + System.lineSeparator());
			try {
				connect.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			connect.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new PaquetePersonaje();
		*/
	}
	
	/**
	 * Trae un usuario de la base de datos a partir del nombre
	 * @param usuario: nombre del usuario
	 * @return paquete con los datos del usuario, o con datos por default si hubo algún error
	 */
	public PaqueteUsuario getUsuario(String usuario) {
		
		System.out.println("Get usuario");
		
		ResultSet result = null;
		PreparedStatement st;
		
		try {
			//Prepara y ejecuta una instrucción SQL para traer los datos del usuario
			st = connect.prepareStatement("SELECT * FROM registro WHERE usuario = ?");
			st.setString(1, usuario);
			result = st.executeQuery();

			String password = result.getString("password");
			int idPersonaje = result.getInt("idPersonaje");
			
			//Genera e inicializa el paquete de usuario
			PaqueteUsuario paqueteUsuario = new PaqueteUsuario();
			paqueteUsuario.setUsername(usuario);
			paqueteUsuario.setPassword(password);
			paqueteUsuario.setIdPj(idPersonaje);
			
			return paqueteUsuario;
		} catch (SQLException e) {
			Servidor.log.append("Fallo al intentar recuperar el usuario " + usuario + System.lineSeparator());
			Servidor.log.append(e.getMessage() + System.lineSeparator());
		}
		
		return new PaqueteUsuario();
	}

	/**
	 * Actualiza el inventario de un personaje en la base de datos
	 * @param paquetePersonaje: paquete con todos los datos del personaje
	 */
	public void actualizarInventario(PaquetePersonaje paquetePersonaje) {
		//--------------HIBERNATE------------------------
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
			session.close();
			return;
		}
		return;
		
		/*
		int i = 0;
		PreparedStatement stActualizarMochila;
		try {
			/*
			 * Prepara la instrucción SQL para cargar todos los items del personaje en la BD
			 * Carga los id de los items que tiene y pone -1 para los que no tiene
			 */
		/*
			stActualizarMochila = connect.prepareStatement(
					"UPDATE mochila SET item1=? ,item2=? ,item3=? ,item4=? ,item5=? ,item6=? ,item7=? ,item8=? ,item9=? "
							+ ",item10=? ,item11=? ,item12=? ,item13=? ,item14=? ,item15=? ,item16=? ,item17=? ,item18=? ,item19=? ,item20=? WHERE idMochila=?");
			while (i < paquetePersonaje.getCantItems()) {
				stActualizarMochila.setInt(i + 1, paquetePersonaje.getItemID(i));
				i++;
			}
			for (int j = paquetePersonaje.getCantItems(); j < 20; j++) {
				stActualizarMochila.setInt(j + 1, -1);
			}
			stActualizarMochila.setInt(21, paquetePersonaje.getId());
			stActualizarMochila.executeUpdate();
		
		} catch (SQLException e) {
		}
		*/
	}		
	
	/**
	 * Actualiza el inventario de un personaje a partir de su id
	 * @param idPersonaje: numero id del personaje
	 */
	public void agregarItemInventario(int idPersonaje) {
		//--------------HIBERNATE------------------------
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
		
		
		
		/*
		//Preparo las cosas
		int i = 0;
		PaquetePersonaje paquetePersonaje = Servidor.getPersonajesConectados().get(idPersonaje);
		PreparedStatement stActualizarMochila;
		try {
			stActualizarMochila = connect.prepareStatement(
					"UPDATE mochila SET item1=? ,item2=? ,item3=? ,item4=? ,item5=? ,item6=? ,item7=? ,item8=? ,item9=? "
							+ ",item10=? ,item11=? ,item12=? ,item13=? ,item14=? ,item15=? ,item16=? ,item17=? ,item18=? ,item19=? ,item20=? WHERE idMochila=?");
			
			//Pongo los items que ya tenia
			while (i < paquetePersonaje.getCantItems()) {
				stActualizarMochila.setInt(i + 1, paquetePersonaje.getItemID(i));
				i++;
			}
			
			//Si no tiene el inventario lleno, le doy un item mas
			if( paquetePersonaje.getCantItems() < 9) {
				int itemGanado = new Random().nextInt(29) + 1;
				stActualizarMochila.setInt(paquetePersonaje.getCantItems()+1, itemGanado);
				i = paquetePersonaje.getCantItems()+2;
			}
			else i = paquetePersonaje.getCantItems()+1;
			//Completo los espacios que quedan con -1
			for(; i < 20; i++)
				stActualizarMochila.setInt(i, -1);
			
			//Pongo el id de la mochila y ejecuto
			stActualizarMochila.setInt(21, paquetePersonaje.getId());
			stActualizarMochila.executeUpdate();

		} catch (SQLException e) {
			Servidor.log.append("Falló al intentar actualizar inventario de"+ idPersonaje + "\n");
		}
		*/
	}

	/**
	 * Actualiza stats de un personaje porque subió de nivel
	 * @param paquetePersonaje: paquete con los datos del personaje
	 */
	public void actualizarPersonajeSubioNivel(PaquetePersonaje paquetePersonaje) {
		//--------------HIBERNATE------------------------
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
		
		/*
		try {
			PreparedStatement stActualizarPersonaje = connect
					.prepareStatement("UPDATE personaje SET fuerza=?, destreza=?, inteligencia=?, saludTope=?, energiaTope=?, experiencia=?, nivel=? "
							+ "  WHERE idPersonaje=?");
			
			stActualizarPersonaje.setInt(1, paquetePersonaje.getFuerza());
			stActualizarPersonaje.setInt(2, paquetePersonaje.getDestreza());
			stActualizarPersonaje.setInt(3, paquetePersonaje.getInteligencia());
			stActualizarPersonaje.setInt(4, paquetePersonaje.getSaludTope());
			stActualizarPersonaje.setInt(5, paquetePersonaje.getEnergiaTope());
			stActualizarPersonaje.setInt(6, paquetePersonaje.getExperiencia());
			stActualizarPersonaje.setInt(7, paquetePersonaje.getNivel());
			stActualizarPersonaje.setInt(8, paquetePersonaje.getId());
			
			stActualizarPersonaje.executeUpdate();
			
			Servidor.log.append("El personaje " + paquetePersonaje.getNombre() + " se ha actualizado con éxito."  + System.lineSeparator());;
		} catch (SQLException e) {
			Servidor.log.append("Fallo al intentar actualizar el personaje " + paquetePersonaje.getNombre()  + System.lineSeparator());
		}
		*/
	}
}
