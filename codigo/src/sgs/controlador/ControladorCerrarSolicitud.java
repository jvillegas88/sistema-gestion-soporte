package sgs.controlador;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import sgs.modelo.Integrante;
import sgs.modelo.Intervencion;
import sgs.modelo.catalogo.Estado;
import sgs.modelo.catalogo.TipoIntervencion;
import sgs.modelo.excepcion.ReglaNegocioException;
import sgs.persistencia.DatosMaestros;
import sgs.persistencia.SolicitudDAO;

/**
 * CU09 Cerrar solicitud. Hereda de CU06 porque lo incluye (paso 4): el cierre siempre registra
 * la intervención del agente. La intervención y el cierre se guardan en una sola transacción:
 * si falta información para cerrar, no se guarda nada y se informa qué falta (alternativa 5.A),
 * así el agente lo completa y vuelve a intentar sin cargar dos veces su tiempo.
 */
public class ControladorCerrarSolicitud extends ControladorRegistrarIntervencion {

    public ControladorCerrarSolicitud(Integrante usuario, DatosMaestros maestros, SolicitudDAO solicitudDAO) {
        super(usuario, maestros, solicitudDAO);
    }

    /** True si el usuario es el responsable, que es quien puede cerrar (precondición de CU09). */
    public boolean puedeCerrar() {
        return usuario.equals(solicitud.getResponsable());
    }

    /**
     * Lo que le falta hoy a la solicitud para cerrarse como Resuelta o Cerrada sin confirmación,
     * sin contar la intervención que se está cargando. Sirve para guiar al agente.
     */
    public List<String> faltantesActuales() {
        solicitud.validarCierre();
        return solicitud.getFaltantesCierre();
    }

    /** Registra la intervención y cierra la solicitud con el tipo de cierre elegido. */
    public void cerrar(Estado cierre, String motivo, TipoIntervencion tipo, String descripcion, BigDecimal tiempoHoras)
            throws SQLException, ReglaNegocioException {
        Intervencion intervencion = armarIntervencion(tipo, descripcion, tiempoHoras);
        String motivoLimpio = motivo == null || motivo.isBlank() ? null : motivo.trim();
        modificarYGuardar(s -> {
            s.registrarIntervencion(intervencion);
            s.cerrar(cierre, usuario, motivoLimpio);
        });
        descartarBorrador();
    }
}
