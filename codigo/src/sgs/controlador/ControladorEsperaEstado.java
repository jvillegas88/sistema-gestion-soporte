package sgs.controlador;

import java.sql.SQLException;
import sgs.modelo.Integrante;
import sgs.modelo.catalogo.MotivoEspera;
import sgs.modelo.excepcion.ReglaNegocioException;
import sgs.persistencia.DatosMaestros;
import sgs.persistencia.SolicitudDAO;

/** CU07 Registrar espera y cambio de estado: abrir y cerrar una espera, y reabrir (alternativa 1.A). */
public class ControladorEsperaEstado extends ControladorSolicitud {

    public ControladorEsperaEstado(Integrante usuario, DatosMaestros maestros, SolicitudDAO solicitudDAO) {
        super(usuario, maestros, solicitudDAO);
    }

    public void abrirEspera(MotivoEspera motivo) throws SQLException, ReglaNegocioException {
        exigirPermiso(usuario.getRol().esAgente(), "registrar esperas");
        if (motivo == null) {
            throw new ReglaNegocioException("Seleccioná el motivo de la espera");
        }
        modificarYGuardar(s -> s.abrirEspera(motivo, usuario));
    }

    public void cerrarEspera() throws SQLException, ReglaNegocioException {
        exigirPermiso(usuario.getRol().esAgente(), "cerrar esperas");
        modificarYGuardar(s -> s.cerrarEspera(usuario));
    }

    /** Reabre la solicitud porque la respuesta del cliente trata el mismo problema (1.A.3). */
    public void reabrir(String motivo) throws SQLException, ReglaNegocioException {
        exigirPermiso(usuario.getRol().esAgente(), "reabrir solicitudes");
        String motivoLimpio = motivo == null ? null : motivo.trim();
        modificarYGuardar(s -> s.reabrir(usuario, motivoLimpio));
    }
}
