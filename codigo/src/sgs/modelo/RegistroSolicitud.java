package sgs.modelo;

import java.time.LocalDateTime;
import sgs.modelo.excepcion.ReglaNegocioException;

/**
 * Todo lo que se va registrando sobre una solicitud: intervenciones, asignaciones, cambios
 * de estado, esperas, hitos técnicos y reclasificaciones. Los seis comparten que pertenecen
 * a una solicitud, que alguien los hizo y que tienen una fecha, y eso es lo que guarda esta
 * clase. Es abstracta porque no existe un registro "genérico": cada subclase define su
 * propia regla en validar() y su propia forma de mostrarse en el historial.
 * <p>
 * Los atributos son protected para que las subclases los usen directamente, y desde
 * afuera solo se leen con los métodos get.
 */
public abstract class RegistroSolicitud {

    protected int id;
    protected Solicitud solicitud;
    protected Integrante autor;
    protected LocalDateTime fecha;

    /** Registro nuevo: todavía no tiene id ni fecha, que los asigna la base al guardarlo (RNF07). */
    protected RegistroSolicitud(Solicitud solicitud, Integrante autor) {
        this(0, solicitud, autor, null);
    }

    /** Registro leído de la base. */
    protected RegistroSolicitud(int id, Solicitud solicitud, Integrante autor, LocalDateTime fecha) {
        this.id = id;
        this.solicitud = solicitud;
        this.autor = autor;
        this.fecha = fecha;
    }

    public int getId() {
        return id;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public Integrante getAutor() {
        return autor;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public boolean estaGuardado() {
        return id != 0;
    }

    /** Lo usa el DAO después de insertar: la base asigna la clave y la fecha con su propio reloj. */
    public void asignarIdentidad(int id, LocalDateTime fecha) {
        this.id = id;
        this.fecha = fecha;
    }

    /**
     * Verifica la regla propia del registro antes de agregarlo a la solicitud.
     *
     * @throws ReglaNegocioException con un mensaje para el usuario si la regla no se cumple
     */
    public abstract void validar() throws ReglaNegocioException;

    /** Nombre del tipo de registro para la columna "Registro" del historial. */
    public abstract String getTipoRegistro();

    /** Qué pasó, en una línea, para la columna "Detalle" del historial. */
    public abstract String getDetalle();

    @Override
    public String toString() {
        return getTipoRegistro() + ": " + getDetalle();
    }
}
