package sgs.controlador;

import java.sql.SQLException;
import sgs.modelo.HitoTecnico;
import sgs.modelo.Integrante;
import sgs.modelo.catalogo.TipoHito;
import sgs.modelo.excepcion.ReglaNegocioException;
import sgs.persistencia.DatosMaestros;
import sgs.persistencia.SolicitudDAO;

/** CU08 Registrar hito técnico: corrección, publicación en testing o producción, o intervención en el servidor. */
public class ControladorRegistrarHito extends ControladorSolicitud {

    public ControladorRegistrarHito(Integrante usuario, DatosMaestros maestros, SolicitudDAO solicitudDAO) {
        super(usuario, maestros, solicitudDAO);
    }

    /**
     * CU08, alternativa 6.A: true si se va a registrar la publicación en producción y la
     * solicitud todavía no tiene la publicación en testing. Es una advertencia: el actor puede
     * seguir igual, porque hay correcciones que se publican directo en producción.
     */
    public boolean faltaPublicacionEnTesting(TipoHito tipo) {
        if (tipo != TipoHito.PUBLICACION_PRODUCCION) {
            return false;
        }
        for (HitoTecnico hito : solicitud.getHitos()) {
            if (hito.getTipo() == TipoHito.PUBLICACION_TESTING) {
                return false;
            }
        }
        return true;
    }

    /**
     * Registra el hito. Si indica la solicitud del deploy, verifica que exista.
     *
     * @param solicitudDeployId número de la solicitud del deploy, o null
     */
    public void registrar(TipoHito tipo, String referencia, Integer solicitudDeployId)
            throws SQLException, ReglaNegocioException {
        exigirPermiso(usuario.getRol().puedeRegistrarHitos(), "registrar hitos técnicos");
        if (solicitudDeployId != null && consultar(con -> solicitudDAO.buscarPorId(con, solicitudDeployId)) == null) {
            throw new ReglaNegocioException("No existe la solicitud " + solicitudDeployId + " del deploy");
        }
        String referenciaLimpia = referencia == null || referencia.isBlank() ? null : referencia.trim();
        modificarYGuardar(s -> s.registrarHito(new HitoTecnico(s, usuario, tipo, referenciaLimpia, solicitudDeployId)));
    }
}
