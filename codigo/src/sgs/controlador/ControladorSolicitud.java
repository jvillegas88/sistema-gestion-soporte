package sgs.controlador;

import java.sql.SQLException;
import sgs.modelo.Integrante;
import sgs.modelo.Solicitud;
import sgs.modelo.excepcion.ReglaNegocioException;
import sgs.persistencia.DatosMaestros;
import sgs.persistencia.SolicitudDAO;

/**
 * Base de los controladores de los casos de uso que trabajan sobre una solicitud existente:
 * todos empiezan cuando el actor "abre una solicitud". Guarda la solicitud abierta y aplica
 * los cambios en el modelo antes de guardarlos.
 */
public abstract class ControladorSolicitud extends Controlador {

    /** Un cambio sobre la solicitud, que el modelo puede rechazar. */
    @FunctionalInterface
    protected interface CambioModelo {
        void aplicar(Solicitud solicitud) throws ReglaNegocioException;
    }

    protected final SolicitudDAO solicitudDAO;
    protected Solicitud solicitud;

    protected ControladorSolicitud(Integrante usuario, DatosMaestros maestros, SolicitudDAO solicitudDAO) {
        super(usuario, maestros);
        this.solicitudDAO = solicitudDAO;
    }

    /** Lee la solicitud con todos sus registros y la deja abierta en el controlador. */
    public Solicitud abrirSolicitud(int solicitudId) throws SQLException, ReglaNegocioException {
        Solicitud leida = consultar(con -> solicitudDAO.buscarPorId(con, solicitudId));
        if (leida == null) {
            throw new ReglaNegocioException("No existe la solicitud " + solicitudId);
        }
        solicitud = leida;
        return solicitud;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    /**
     * Aplica el cambio en el modelo, que verifica las reglas, y guarda la solicitud con sus
     * registros nuevos en una sola transacción. Si el modelo rechaza el cambio o falla la base,
     * vuelve a leer la solicitud para descartar lo que había quedado a medias en memoria.
     */
    protected void modificarYGuardar(CambioModelo cambio) throws SQLException, ReglaNegocioException {
        try {
            cambio.aplicar(solicitud);
            enTransaccion(con -> solicitudDAO.guardar(con, solicitud));
        } catch (SQLException | ReglaNegocioException | RuntimeException e) {
            recargar(e);
            throw e;
        }
    }

    private void recargar(Exception causa) {
        try {
            if (solicitud.getId() != 0) {
                solicitud = consultar(con -> solicitudDAO.buscarPorId(con, solicitud.getId()));
            }
        } catch (SQLException e) {
            // Si tampoco se puede leer, se informa el problema original con este como agregado
            causa.addSuppressed(e);
        }
    }
}
