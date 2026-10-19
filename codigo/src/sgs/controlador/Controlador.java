package sgs.controlador;

import java.sql.Connection;
import java.sql.SQLException;
import sgs.modelo.Integrante;
import sgs.modelo.excepcion.ReglaNegocioException;
import sgs.persistencia.ConexionBD;
import sgs.persistencia.DatosMaestros;

/**
 * Base de los controladores. Conoce al integrante que inició la sesión, que es el autor de
 * todo lo que se registra, y ofrece la forma de trabajar con la base: una transacción que
 * guarda todo o nada, y las consultas de solo lectura.
 */
public abstract class Controlador {

    /** Una operación que se ejecuta dentro de una transacción. */
    @FunctionalInterface
    protected interface Accion {
        void ejecutar(Connection con) throws SQLException, ReglaNegocioException;
    }

    /** Una consulta que devuelve un resultado. */
    @FunctionalInterface
    protected interface Consulta<T> {
        T ejecutar(Connection con) throws SQLException;
    }

    protected final Integrante usuario;
    protected final DatosMaestros maestros;

    protected Controlador(Integrante usuario, DatosMaestros maestros) {
        this.usuario = usuario;
        this.maestros = maestros;
    }

    public Integrante getUsuario() {
        return usuario;
    }

    public DatosMaestros getMaestros() {
        return maestros;
    }

    /**
     * Ejecuta la acción en una transacción. Si termina bien confirma con commit(). Si algo
     * falla, deshace todo con rollback() y vuelve a lanzar la excepción, así no queda nada a
     * medias en la base (sección 10, paso 4 de CU06).
     */
    protected void enTransaccion(Accion accion) throws SQLException, ReglaNegocioException {
        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                accion.ejecutar(con);
                con.commit();
            } catch (SQLException | ReglaNegocioException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Ejecuta una consulta de solo lectura con una conexión propia. */
    protected <T> T consultar(Consulta<T> consulta) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion()) {
            return consulta.ejecutar(con);
        }
    }

    /** Corta la operación si el rol del usuario no la tiene permitida (RNF06). */
    protected void exigirPermiso(boolean permitido, String operacion) throws ReglaNegocioException {
        if (!permitido) {
            throw new ReglaNegocioException("Tu rol (" + usuario.getRol() + ") no puede " + operacion);
        }
    }
}
