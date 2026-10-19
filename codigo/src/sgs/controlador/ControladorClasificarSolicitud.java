package sgs.controlador;

import java.sql.SQLException;
import java.util.Objects;
import sgs.modelo.Integrante;
import sgs.modelo.Modulo;
import sgs.modelo.catalogo.Prioridad;
import sgs.modelo.catalogo.TipoSolicitud;
import sgs.modelo.excepcion.ReglaNegocioException;
import sgs.persistencia.DatosMaestros;
import sgs.persistencia.SolicitudDAO;

/** CU03 Clasificar solicitud: tipo, módulo y prioridad, con el motivo si es una reclasificación. */
public class ControladorClasificarSolicitud extends ControladorSolicitud {

    public ControladorClasificarSolicitud(Integrante usuario, DatosMaestros maestros, SolicitudDAO solicitudDAO) {
        super(usuario, maestros, solicitudDAO);
    }

    /** True si la solicitud ya tiene clasificación, y por lo tanto hace falta un motivo (alternativa 9.A). */
    public boolean esReclasificacion() {
        return solicitud.getTipo() != null || solicitud.getPrioridad() != null;
    }

    /**
     * Guarda la clasificación y el módulo. Si solo cambia el módulo no se registra una
     * reclasificación, porque la tabla no guarda el módulo anterior (sección 10).
     */
    public void clasificar(TipoSolicitud tipo, Prioridad prioridad, Modulo modulo, String motivo)
            throws SQLException, ReglaNegocioException {
        exigirPermiso(usuario.getRol().puedeClasificar(), "clasificar solicitudes");
        modificarYGuardar(s -> {
            boolean cambiaClasificacion = tipo != s.getTipo() || prioridad != s.getPrioridad();
            boolean cambiaModulo = !Objects.equals(modulo, s.getModulo());
            if (!cambiaClasificacion && !cambiaModulo) {
                throw new ReglaNegocioException("No hay cambios para guardar");
            }
            if (cambiaClasificacion) {
                s.clasificar(tipo, prioridad, usuario, motivo);
            }
            if (cambiaModulo) {
                s.asignarModulo(modulo);
            }
        });
    }
}
