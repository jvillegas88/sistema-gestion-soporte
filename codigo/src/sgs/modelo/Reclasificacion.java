package sgs.modelo;

import java.time.LocalDateTime;
import sgs.modelo.catalogo.Prioridad;
import sgs.modelo.catalogo.TipoSolicitud;
import sgs.modelo.excepcion.ReglaNegocioException;

/**
 * Registro de una clasificación de la solicitud, con la clasificación anterior y el motivo
 * (RF05, CU03). La primera clasificación también se registra, con la clasificación anterior
 * vacía y el motivo "Clasificación inicial", para que quede su autor y su fecha (RNF07).
 */
public class Reclasificacion extends RegistroSolicitud {

    public static final String MOTIVO_INICIAL = "Clasificación inicial";

    private final String motivo;
    private final TipoSolicitud tipoAnterior;
    private final Prioridad prioridadAnterior;

    public Reclasificacion(Solicitud solicitud, Integrante autor, String motivo, TipoSolicitud tipoAnterior,
            Prioridad prioridadAnterior) {
        super(solicitud, autor);
        this.motivo = motivo;
        this.tipoAnterior = tipoAnterior;
        this.prioridadAnterior = prioridadAnterior;
    }

    public Reclasificacion(int id, Solicitud solicitud, Integrante autor, LocalDateTime fecha, String motivo,
            TipoSolicitud tipoAnterior, Prioridad prioridadAnterior) {
        super(id, solicitud, autor, fecha);
        this.motivo = motivo;
        this.tipoAnterior = tipoAnterior;
        this.prioridadAnterior = prioridadAnterior;
    }

    public String getMotivo() {
        return motivo;
    }

    public TipoSolicitud getTipoAnterior() {
        return tipoAnterior;
    }

    public Prioridad getPrioridadAnterior() {
        return prioridadAnterior;
    }

    public boolean esInicial() {
        return tipoAnterior == null && prioridadAnterior == null;
    }

    @Override
    public void validar() throws ReglaNegocioException {
        if (Formatos.estaVacio(motivo)) {
            throw new ReglaNegocioException("Ingresá el motivo de la reclasificación");
        }
        if (motivo.length() > CambioEstado.LARGO_MOTIVO) {
            throw new ReglaNegocioException("El motivo admite hasta " + CambioEstado.LARGO_MOTIVO + " caracteres");
        }
    }

    @Override
    public String getTipoRegistro() {
        return esInicial() ? "Clasificación" : "Reclasificación";
    }

    @Override
    public String getDetalle() {
        if (esInicial()) {
            return motivo;
        }
        return "Antes: " + valorONada(tipoAnterior) + ", prioridad " + valorONada(prioridadAnterior) + ". " + motivo;
    }

    private static String valorONada(Object valor) {
        return valor == null ? "sin definir" : valor.toString();
    }
}
