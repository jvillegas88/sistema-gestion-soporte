package sgs.modelo;

import java.time.LocalDateTime;
import sgs.modelo.excepcion.ReglaNegocioException;

/**
 * Asignación de la solicitud a un integrante (RF07). El autor heredado es quien asigna y
 * el asignado es el nuevo responsable. La última asignación indica el responsable actual.
 */
public class Asignacion extends RegistroSolicitud {

    private final Integrante asignado;

    public Asignacion(Solicitud solicitud, Integrante autor, Integrante asignado) {
        super(solicitud, autor);
        this.asignado = asignado;
    }

    public Asignacion(int id, Solicitud solicitud, Integrante autor, LocalDateTime fecha, Integrante asignado) {
        super(id, solicitud, autor, fecha);
        this.asignado = asignado;
    }

    public Integrante getAsignado() {
        return asignado;
    }

    @Override
    public void validar() throws ReglaNegocioException {
        if (asignado == null) {
            throw new ReglaNegocioException("Seleccioná a quién se asigna la solicitud");
        }
        if (!asignado.estaActivo()) {
            throw new ReglaNegocioException(asignado.getNombre() + " no está activo en el equipo");
        }
    }

    @Override
    public String getTipoRegistro() {
        return "Asignación";
    }

    @Override
    public String getDetalle() {
        return "Responsable: " + asignado.getNombre();
    }
}
