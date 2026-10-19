package sgs.modelo;

import java.time.LocalDateTime;
import sgs.modelo.catalogo.Estado;
import sgs.modelo.excepcion.ReglaNegocioException;

/** Paso de la solicitud a otro estado, con autor, fecha y, si corresponde, el motivo (RF09). */
public class CambioEstado extends RegistroSolicitud {

    public static final int LARGO_MOTIVO = 300;

    private final Estado estado;
    private final String motivo;

    public CambioEstado(Solicitud solicitud, Integrante autor, Estado estado, String motivo) {
        super(solicitud, autor);
        this.estado = estado;
        this.motivo = motivo;
    }

    public CambioEstado(int id, Solicitud solicitud, Integrante autor, LocalDateTime fecha, Estado estado,
            String motivo) {
        super(id, solicitud, autor, fecha);
        this.estado = estado;
        this.motivo = motivo;
    }

    public Estado getEstado() {
        return estado;
    }

    public String getMotivo() {
        return motivo;
    }

    /** Cancelar y descartar exigen el motivo (CU09 alternativa 3.A, CU01 alternativa 10.A). */
    @Override
    public void validar() throws ReglaNegocioException {
        if (estado == null) {
            throw new ReglaNegocioException("Falta el estado nuevo");
        }
        boolean exigeMotivo = estado == Estado.CANCELADA || estado == Estado.DESCARTADA;
        if (exigeMotivo && Formatos.estaVacio(motivo)) {
            throw new ReglaNegocioException("Ingresá el motivo para pasar la solicitud a " + estado);
        }
        if (motivo != null && motivo.length() > LARGO_MOTIVO) {
            throw new ReglaNegocioException("El motivo admite hasta " + LARGO_MOTIVO + " caracteres");
        }
    }

    @Override
    public String getTipoRegistro() {
        return "Cambio de estado";
    }

    @Override
    public String getDetalle() {
        return Formatos.estaVacio(motivo) ? "Pasa a " + estado : "Pasa a " + estado + ": " + motivo;
    }
}
