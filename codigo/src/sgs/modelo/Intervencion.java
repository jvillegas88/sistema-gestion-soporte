package sgs.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import sgs.modelo.catalogo.TipoIntervencion;
import sgs.modelo.excepcion.ReglaNegocioException;

/**
 * Lo que hizo un integrante en una solicitud y cuánto tiempo le dedicó (CU06, RF08).
 * Cada intervención queda a nombre de quien la hizo y su tiempo no se suma a un total
 * compartido: el total de la solicitud se calcula sumando las intervenciones (P01).
 */
public class Intervencion extends RegistroSolicitud {

    /** Límites del tiempo dedicado, los mismos que controla la restricción de la base. */
    public static final BigDecimal TIEMPO_MAXIMO = new BigDecimal("24");
    public static final int DECIMALES_TIEMPO = 2;
    public static final int LARGO_DESCRIPCION = 500;

    private final TipoIntervencion tipo;
    private final String descripcion;
    private final BigDecimal tiempoHoras;
    private final List<Comunicacion> comunicaciones = new ArrayList<>();
    private Estimacion estimacion;

    /** Intervención nueva que registra el integrante que inició la sesión. */
    public Intervencion(Solicitud solicitud, Integrante autor, TipoIntervencion tipo, String descripcion,
            BigDecimal tiempoHoras) {
        super(solicitud, autor);
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.tiempoHoras = tiempoHoras;
    }

    /** Intervención leída de la base. */
    public Intervencion(int id, Solicitud solicitud, Integrante autor, LocalDateTime fecha, TipoIntervencion tipo,
            String descripcion, BigDecimal tiempoHoras) {
        super(id, solicitud, autor, fecha);
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.tiempoHoras = tiempoHoras;
    }

    public TipoIntervencion getTipo() {
        return tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getTiempoHoras() {
        return tiempoHoras;
    }

    /** Agrega una nota interna o una respuesta al cliente (CU06, alternativa 3.A). */
    public void agregar(Comunicacion comunicacion) {
        comunicaciones.add(comunicacion);
    }

    public List<Comunicacion> getComunicaciones() {
        return Collections.unmodifiableList(comunicaciones);
    }

    /** Los ítems estimados de una intervención de tipo Estimación (CU06, alternativa 3.B). */
    public void setEstimacion(Estimacion estimacion) {
        this.estimacion = estimacion;
    }

    public Estimacion getEstimacion() {
        return estimacion;
    }

    /** Busca entre las comunicaciones una respuesta al cliente, que exige el cierre (CU09). */
    public boolean tieneRespuestaAlCliente() {
        for (Comunicacion c : comunicaciones) {
            if (c.esRespuestaAlCliente()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Aplica las reglas de CU06: los tres datos obligatorios de RNF04 y el tiempo mayor que
     * cero, de hasta 24 horas y con hasta dos decimales. La última condición corrige DEF-01:
     * la base redondea a dos decimales antes de su restricción, así que el control tiene que
     * estar acá. Los mensajes son los de la tabla de CP-N-01 (sección 12.2).
     */
    @Override
    public void validar() throws ReglaNegocioException {
        if (tipo == null) {
            throw new ReglaNegocioException("Seleccioná el tipo de intervención");
        }
        if (Formatos.estaVacio(descripcion)) {
            throw new ReglaNegocioException("Escribí qué hiciste en la descripción");
        }
        if (descripcion.length() > LARGO_DESCRIPCION) {
            throw new ReglaNegocioException("La descripción admite hasta " + LARGO_DESCRIPCION + " caracteres");
        }
        if (tiempoHoras == null) {
            throw new ReglaNegocioException("Ingresá el tiempo dedicado");
        }
        if (tiempoHoras.signum() <= 0) {
            throw new ReglaNegocioException("La intervención tiene que tener un tiempo mayor que cero");
        }
        if (tiempoHoras.compareTo(TIEMPO_MAXIMO) > 0) {
            throw new ReglaNegocioException("El tiempo no puede superar las 24 horas. "
                    + "Si el trabajo siguió otro día, registrá otra intervención");
        }
        if (tiempoHoras.stripTrailingZeros().scale() > DECIMALES_TIEMPO) {
            throw new ReglaNegocioException("El tiempo admite hasta dos decimales");
        }
        for (Comunicacion c : comunicaciones) {
            if (c.getTipo() == null || Formatos.estaVacio(c.getTexto())) {
                throw new ReglaNegocioException("La comunicación tiene que tener su tipo y su texto");
            }
        }
        validarEstimacion();
    }

    /** Los ítems solo van en una estimación, y cada uno con descripción y horas mayores que cero. */
    private void validarEstimacion() throws ReglaNegocioException {
        if (estimacion == null || !estimacion.tieneItems()) {
            return;
        }
        if (tipo != TipoIntervencion.ESTIMACION) {
            throw new ReglaNegocioException("Solo las intervenciones de tipo Estimación llevan ítems");
        }
        for (ItemEstimacion item : estimacion.getItems()) {
            if (Formatos.estaVacio(item.getDescripcion())) {
                throw new ReglaNegocioException("El ítem " + item.getNumero() + " no tiene descripción");
            }
            if (item.getHoras() == null || item.getHoras().signum() <= 0) {
                throw new ReglaNegocioException("Las horas del ítem " + item.getNumero() + " tienen que ser mayores que cero");
            }
            if (item.getHoras().stripTrailingZeros().scale() > DECIMALES_TIEMPO) {
                throw new ReglaNegocioException("Las horas del ítem " + item.getNumero() + " admiten hasta dos decimales");
            }
        }
    }

    @Override
    public String getTipoRegistro() {
        return "Intervención";
    }

    @Override
    public String getDetalle() {
        return tipo + " (" + Formatos.horas(tiempoHoras) + " h): " + descripcion;
    }
}
