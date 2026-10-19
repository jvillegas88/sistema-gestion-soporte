package sgs.persistencia;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Acceso a la base de las entidades que se guardan y se buscan por su número.
 * Recibe la conexión como parámetro para que el controlador pueda agrupar varias
 * operaciones en una sola transacción. No tiene método para borrar, porque no hay
 * borrado físico (RNF07).
 *
 * @param <T> la clase del modelo que guarda
 */
public interface DAO<T> {

    /** Inserta la entidad y devuelve la clave que le asignó la base. */
    int insertar(Connection con, T entidad) throws SQLException;

    /** Devuelve la entidad con esa clave, o null si no existe. */
    T buscarPorId(Connection con, int id) throws SQLException;
}
