package sgs.modelo.consulta;

import java.time.LocalDateTime;
import sgs.modelo.catalogo.Estado;
import sgs.modelo.catalogo.Prioridad;
import sgs.modelo.catalogo.TipoSolicitud;

/**
 * Una fila de la bandeja de solicitudes. Trae solo lo que se muestra en la lista, sin los
 * registros, para no cargar cada solicitud completa.
 */
public class ResumenSolicitud {

    private final int id;
    private final LocalDateTime fechaRecepcion;
    private final String cliente;
    private final String producto;
    private final String asunto;
    private final TipoSolicitud tipo;
    private final Prioridad prioridad;
    private final Estado estado;
    private final int responsableId;
    private final String responsable;

    public ResumenSolicitud(int id, LocalDateTime fechaRecepcion, String cliente, String producto, String asunto,
            TipoSolicitud tipo, Prioridad prioridad, Estado estado, int responsableId, String responsable) {
        this.id = id;
        this.fechaRecepcion = fechaRecepcion;
        this.cliente = cliente;
        this.producto = producto;
        this.asunto = asunto;
        this.tipo = tipo;
        this.prioridad = prioridad;
        this.estado = estado;
        this.responsableId = responsableId;
        this.responsable = responsable;
    }

    public int getId() {
        return id;
    }

    public LocalDateTime getFechaRecepcion() {
        return fechaRecepcion;
    }

    public String getCliente() {
        return cliente;
    }

    public String getProducto() {
        return producto;
    }

    public String getAsunto() {
        return asunto;
    }

    public TipoSolicitud getTipo() {
        return tipo;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public Estado getEstado() {
        return estado;
    }

    public int getResponsableId() {
        return responsableId;
    }

    public String getResponsable() {
        return responsable;
    }
}
