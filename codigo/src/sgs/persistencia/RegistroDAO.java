package sgs.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import sgs.modelo.RegistroSolicitud;
import sgs.modelo.Solicitud;

/**
 * Base de los DAO de los registros de una solicitud. Los registros no se buscan sueltos:
 * se leen y se guardan siempre con su solicitud, así que en lugar de buscarPorId() tienen
 * listarPorSolicitud(). SolicitudDAO tiene un DAO de cada tipo de registro y, para guardar
 * uno, le pregunta a cada DAO si acepta() ese registro.
 *
 * @param <T> la subclase de RegistroSolicitud que guarda
 */
public abstract class RegistroDAO<T extends RegistroSolicitud> {

    private final Class<T> tipo;
    protected final DatosMaestros maestros;

    protected RegistroDAO(Class<T> tipo, DatosMaestros maestros) {
        this.tipo = tipo;
        this.maestros = maestros;
    }

    /** True si el registro es de la clase que guarda este DAO. */
    public boolean acepta(RegistroSolicitud registro) {
        return tipo.isInstance(registro);
    }

    /** Inserta el registro si es nuevo o lo actualiza si ya estaba guardado. */
    public void guardar(Connection con, RegistroSolicitud registro) throws SQLException {
        T propio = tipo.cast(registro);
        if (propio.estaGuardado()) {
            actualizar(con, propio);
        } else {
            insertar(con, propio);
        }
    }

    /** Inserta el registro, le asigna la clave y la fecha de la base, y devuelve la clave. */
    public abstract int insertar(Connection con, T registro) throws SQLException;

    /** Lee los registros de una solicitud, del más antiguo al más reciente. */
    public abstract List<T> listarPorSolicitud(Connection con, Solicitud solicitud) throws SQLException;

    /**
     * Los registros son un historial y no se modifican. La única excepción es la espera,
     * que se guarda abierta y después se le completa el fin, y por eso EsperaDAO lo redefine.
     */
    protected void actualizar(Connection con, T registro) throws SQLException {
        throw new UnsupportedOperationException(tipo.getSimpleName() + " no se modifica una vez guardado");
    }

    // ------------------------------------------------------------------
    // Ayudas para las subclases
    // ------------------------------------------------------------------

    /** Ejecuta un INSERT ya preparado y devuelve la clave autonumérica generada. */
    protected static int ejecutarInsert(PreparedStatement ps) throws SQLException {
        ps.executeUpdate();
        try (ResultSet claves = ps.getGeneratedKeys()) {
            if (!claves.next()) {
                throw new SQLException("La base no devolvió la clave generada");
            }
            return claves.getInt(1);
        }
    }

    /** Prepara un INSERT que devuelve la clave generada. */
    protected static PreparedStatement prepararInsert(Connection con, String sql) throws SQLException {
        return con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }

    /** Lee una fecha que completó la base, por ejemplo con su valor por defecto. */
    protected static LocalDateTime leerFecha(Connection con, String sql, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1, LocalDateTime.class) : null;
            }
        }
    }

    /** Lee un entero que puede ser NULL en la base. */
    protected static Integer enteroONulo(ResultSet rs, String columna) throws SQLException {
        int valor = rs.getInt(columna);
        return rs.wasNull() ? null : valor;
    }
}
