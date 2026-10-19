package sgs.controlador;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import sgs.modelo.Comunicacion;
import sgs.modelo.Estimacion;
import sgs.modelo.Integrante;
import sgs.modelo.Intervencion;
import sgs.modelo.ItemEstimacion;
import sgs.modelo.Solicitud;
import sgs.modelo.catalogo.TipoComunicacion;
import sgs.modelo.catalogo.TipoIntervencion;
import sgs.modelo.excepcion.ReglaNegocioException;
import sgs.persistencia.DatosMaestros;
import sgs.persistencia.SolicitudDAO;

/**
 * CU06 Registrar intervención y tiempo, como en la vista de capas de la sección 10.
 * La comunicación (alternativa 3.A) y los ítems de la estimación (3.B) se agregan primero a
 * un borrador, y registrar() arma con ellos la intervención.
 */
public class ControladorRegistrarIntervencion extends ControladorSolicitud {

    private final List<Comunicacion> comunicaciones = new ArrayList<>();
    private final List<ItemEstimacion> items = new ArrayList<>();

    public ControladorRegistrarIntervencion(Integrante usuario, DatosMaestros maestros, SolicitudDAO solicitudDAO) {
        super(usuario, maestros, solicitudDAO);
    }

    /** Vacía el borrador antes de cargar una intervención nueva. */
    public void descartarBorrador() {
        comunicaciones.clear();
        items.clear();
    }

    public void agregarComunicacion(TipoComunicacion tipo, String texto) {
        comunicaciones.add(new Comunicacion(tipo, texto == null ? null : texto.trim()));
    }

    /** Agrega un ítem a la estimación, con el número siguiente. */
    public void agregarItem(String descripcion, BigDecimal horas) {
        items.add(new ItemEstimacion(items.size() + 1, descripcion == null ? null : descripcion.trim(), horas));
    }

    /**
     * Registra la intervención a nombre del usuario y devuelve el tiempo total de la solicitud
     * (pasos 9 a 11). Si es la primera, la solicitud pasa a En curso (alternativa 10.A).
     */
    public BigDecimal registrar(TipoIntervencion tipo, String descripcion, BigDecimal tiempoHoras)
            throws SQLException, ReglaNegocioException {
        exigirPermiso(usuario.getRol().esAgente(), "registrar intervenciones");
        modificarYGuardar(s -> s.registrarIntervencion(armarIntervencion(s, tipo, descripcion, tiempoHoras)));
        descartarBorrador();
        return solicitud.calcularTiempoTotal();
    }

    /**
     * Arma la intervención con el borrador, para la solicitud que se va a modificar. La usa
     * también el cierre, que incluye este caso de uso.
     */
    protected Intervencion armarIntervencion(Solicitud s, TipoIntervencion tipo, String descripcion,
            BigDecimal tiempoHoras) {
        Intervencion intervencion = new Intervencion(s, usuario, tipo,
                descripcion == null ? null : descripcion.trim(), tiempoHoras);
        for (Comunicacion c : comunicaciones) {
            intervencion.agregar(c);
        }
        if (!items.isEmpty()) {
            intervencion.setEstimacion(new Estimacion(items));
        }
        return intervencion;
    }
}
