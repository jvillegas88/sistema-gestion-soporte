package sgs.modelo;

import java.time.Duration;
import java.time.LocalDateTime;
import sgs.modelo.catalogo.MotivoEspera;
import sgs.modelo.excepcion.ReglaNegocioException;

/**
 * Período en que la solicitud está detenida esperando algo o a alguien (RF10, CU07).
 * La fecha heredada es el inicio de la espera (columna desde). Mientras el fin está vacío,
 * la espera sigue abierta.
 */
public class Espera extends RegistroSolicitud {

    private final MotivoEspera motivo;
    private LocalDateTime hasta;

    public Espera(Solicitud solicitud, Integrante autor, MotivoEspera motivo) {
        super(solicitud, autor);
        this.motivo = motivo;
    }

    public Espera(int id, Solicitud solicitud, Integrante autor, LocalDateTime desde, MotivoEspera motivo,
            LocalDateTime hasta) {
        super(id, solicitud, autor, desde);
        this.motivo = motivo;
        this.hasta = hasta;
    }

    public MotivoEspera getMotivo() {
        return motivo;
    }

    public LocalDateTime getHasta() {
        return hasta;
    }

    public boolean estaAbierta() {
        return hasta == null;
    }

    /** Cierra el período. El DAO guarda la hora del servidor y actualiza este valor. */
    public void cerrar(LocalDateTime hasta) {
        this.hasta = hasta;
    }

    /** Duración de la espera. Si sigue abierta, hasta el momento actual. */
    public Duration calcularDuracion() {
        if (fecha == null) {
            return Duration.ZERO;
        }
        LocalDateTime fin = hasta != null ? hasta : LocalDateTime.now();
        return Duration.between(fecha, fin);
    }

    @Override
    public void validar() throws ReglaNegocioException {
        if (motivo == null) {
            throw new ReglaNegocioException("Seleccioná el motivo de la espera");
        }
        if (fecha != null && hasta != null && hasta.isBefore(fecha)) {
            throw new ReglaNegocioException("El fin de la espera no puede ser anterior a su inicio");
        }
    }

    @Override
    public String getTipoRegistro() {
        return "Espera";
    }

    @Override
    public String getDetalle() {
        if (estaAbierta()) {
            return motivo + ", abierta";
        }
        long horas = calcularDuracion().toHours();
        return motivo + ", hasta " + Formatos.fechaHora(hasta) + " (" + horas + " h)";
    }
}
