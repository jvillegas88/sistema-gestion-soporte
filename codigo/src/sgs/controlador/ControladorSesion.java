package sgs.controlador;

import java.sql.Connection;
import java.sql.SQLException;
import sgs.modelo.Formatos;
import sgs.modelo.Integrante;
import sgs.modelo.excepcion.ReglaNegocioException;
import sgs.persistencia.ConexionBD;
import sgs.persistencia.DatosMaestros;
import sgs.persistencia.IntegranteDAO;

/** Inicio de sesión: cada integrante entra con su usuario y su contraseña (RNF06). */
public class ControladorSesion {

    private final IntegranteDAO integranteDAO = new IntegranteDAO();

    /**
     * Verifica el usuario y la contraseña. El mensaje no dice cuál de los dos está mal, para
     * no darle pistas a quien intente adivinarlos.
     */
    public Integrante iniciarSesion(String usuario, String clave) throws SQLException, ReglaNegocioException {
        if (Formatos.estaVacio(usuario) || Formatos.estaVacio(clave)) {
            throw new ReglaNegocioException("Ingresá tu usuario y tu contraseña");
        }
        Integrante integrante;
        try (Connection con = ConexionBD.obtenerConexion()) {
            integrante = integranteDAO.buscarPorUsuario(con, usuario.trim());
        }
        if (integrante == null || !integrante.verificarClave(clave)) {
            throw new ReglaNegocioException("El usuario o la contraseña no son correctos");
        }
        if (!integrante.estaActivo()) {
            throw new ReglaNegocioException("Tu usuario no está activo. Consultá con gestión");
        }
        return integrante;
    }

    /** Lee los datos maestros que usan todas las pantallas. */
    public DatosMaestros cargarDatosMaestros() throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion()) {
            return DatosMaestros.cargar(con);
        }
    }
}
