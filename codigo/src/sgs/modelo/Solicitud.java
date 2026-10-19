package sgs.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import sgs.modelo.catalogo.Canal;
import sgs.modelo.catalogo.Estado;
import sgs.modelo.catalogo.MotivoEspera;
import sgs.modelo.catalogo.Prioridad;
import sgs.modelo.catalogo.TipoHito;
import sgs.modelo.catalogo.TipoIntervencion;
import sgs.modelo.catalogo.TipoSolicitud;
import sgs.modelo.catalogo.TipoVinculo;
import sgs.modelo.excepcion.ReglaNegocioException;

/**
 * Pedido de soporte de un cliente, con todo lo que se registra sobre él (tabla solicitudes).
 * <p>
 * El estado actual, el responsable, la fecha de cierre y el tiempo total no son atributos,
 * porque la base no los guarda: se calculan a partir de los registros. El estado solo cambia
 * por los métodos de los eventos del diagrama de estados de la sección 10, que son los únicos
 * que llaman al método privado cambiarEstado().
 * <p>
 * Cada registro nuevo entra además a una cola de pendientes. El DAO la recorre en orden de
 * llegada para guardar los registros en el mismo orden en que ocurrieron.
 */
public class Solicitud {

    public static final int LARGO_ASUNTO = 200;

    private int id;
    private final Integer numeroOrigen;
    private final Solicitante solicitante;
    private final Producto producto;
    private Modulo modulo;
    private final Canal canal;
    private TipoSolicitud tipo;
    private Prioridad prioridad;
    private Integer solicitudOrigenId;
    private TipoVinculo tipoVinculo;
    private final LocalDateTime fechaRecepcion;
    private final String asunto;
    private final String descripcion;
    private final BigDecimal tiempoHistorico;

    private final List<Intervencion> intervenciones = new ArrayList<>();
    private final List<Asignacion> asignaciones = new ArrayList<>();
    private final List<CambioEstado> cambiosEstado = new ArrayList<>();
    private final List<Espera> esperas = new ArrayList<>();
    private final List<HitoTecnico> hitos = new ArrayList<>();
    private final List<Reclasificacion> reclasificaciones = new ArrayList<>();

    private final Queue<RegistroSolicitud> pendientes = new ArrayDeque<>();
    private final List<String> faltantesCierre = new ArrayList<>();

    /** Solicitud nueva que registra soporte funcional (CU01). El número lo asigna la base. */
    public Solicitud(Solicitante solicitante, Producto producto, Canal canal, LocalDateTime fechaRecepcion,
            String asunto, String descripcion) {
        this(0, null, solicitante, producto, null, canal, null, null, null, null, fechaRecepcion, asunto,
                descripcion, null);
    }

    /** Solicitud leída de la base. Sus registros los agrega el DAO con cargarRegistroGuardado(). */
    public Solicitud(int id, Integer numeroOrigen, Solicitante solicitante, Producto producto, Modulo modulo,
            Canal canal, TipoSolicitud tipo, Prioridad prioridad, Integer solicitudOrigenId,
            TipoVinculo tipoVinculo, LocalDateTime fechaRecepcion, String asunto, String descripcion,
            BigDecimal tiempoHistorico) {
        this.id = id;
        this.numeroOrigen = numeroOrigen;
        this.solicitante = solicitante;
        this.producto = producto;
        this.modulo = modulo;
        this.canal = canal;
        this.tipo = tipo;
        this.prioridad = prioridad;
        this.solicitudOrigenId = solicitudOrigenId;
        this.tipoVinculo = tipoVinculo;
        this.fechaRecepcion = fechaRecepcion;
        this.asunto = asunto;
        this.descripcion = descripcion;
        this.tiempoHistorico = tiempoHistorico;
    }

    // ------------------------------------------------------------------
    // Datos de la solicitud
    // ------------------------------------------------------------------

    public int getId() {
        return id;
    }

    /** Lo usa el DAO para guardar el número que asignó la base. */
    public void setId(int id) {
        this.id = id;
    }

    public Integer getNumeroOrigen() {
        return numeroOrigen;
    }

    public Solicitante getSolicitante() {
        return solicitante;
    }

    /** El cliente no se guarda en la solicitud: es el del solicitante, igual que en la base. */
    public Cliente getCliente() {
        return solicitante == null ? null : solicitante.getCliente();
    }

    public Producto getProducto() {
        return producto;
    }

    public Modulo getModulo() {
        return modulo;
    }

    public Canal getCanal() {
        return canal;
    }

    public TipoSolicitud getTipo() {
        return tipo;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public Integer getSolicitudOrigenId() {
        return solicitudOrigenId;
    }

    public TipoVinculo getTipoVinculo() {
        return tipoVinculo;
    }

    public LocalDateTime getFechaRecepcion() {
        return fechaRecepcion;
    }

    public String getAsunto() {
        return asunto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getTiempoHistorico() {
        return tiempoHistorico;
    }

    public List<Intervencion> getIntervenciones() {
        return Collections.unmodifiableList(intervenciones);
    }

    public List<Asignacion> getAsignaciones() {
        return Collections.unmodifiableList(asignaciones);
    }

    public List<CambioEstado> getCambiosEstado() {
        return Collections.unmodifiableList(cambiosEstado);
    }

    public List<Espera> getEsperas() {
        return Collections.unmodifiableList(esperas);
    }

    public List<HitoTecnico> getHitos() {
        return Collections.unmodifiableList(hitos);
    }

    public List<Reclasificacion> getReclasificaciones() {
        return Collections.unmodifiableList(reclasificaciones);
    }

    // ------------------------------------------------------------------
    // Datos derivados, que se calculan con los registros
    // ------------------------------------------------------------------

    /** El estado del último cambio de estado. Es null solo antes de registrar la solicitud. */
    public Estado getEstadoActual() {
        if (cambiosEstado.isEmpty()) {
            return null;
        }
        return cambiosEstado.get(cambiosEstado.size() - 1).getEstado();
    }

    /** Abierta es Nueva, En curso o En espera, como dicen las precondiciones de los casos de uso. */
    public boolean estaAbierta() {
        Estado estado = getEstadoActual();
        return estado != null && !estado.esCierre();
    }

    /** El integrante de la última asignación. */
    public Integrante getResponsable() {
        if (asignaciones.isEmpty()) {
            return null;
        }
        return asignaciones.get(asignaciones.size() - 1).getAsignado();
    }

    /** La fecha del cambio a un estado de cierre, o null si la solicitud está abierta. */
    public LocalDateTime getFechaCierre() {
        if (cambiosEstado.isEmpty() || estaAbierta()) {
            return null;
        }
        return cambiosEstado.get(cambiosEstado.size() - 1).getFecha();
    }

    /** Suma del tiempo de las intervenciones, más el tiempo histórico de una solicitud importada. */
    public BigDecimal calcularTiempoTotal() {
        BigDecimal total = tiempoHistorico == null ? BigDecimal.ZERO : tiempoHistorico;
        for (Intervencion i : intervenciones) {
            total = total.add(i.getTiempoHoras());
        }
        return total;
    }

    /** La espera que sigue abierta, si hay. Hay como máximo una (CU07, observación 1). */
    public Espera getEsperaAbierta() {
        for (int i = esperas.size() - 1; i >= 0; i--) {
            if (esperas.get(i).estaAbierta()) {
                return esperas.get(i);
            }
        }
        return null;
    }

    /** La estimación con ítems más reciente, la que pide el cierre de un pedido de presupuesto. */
    public Estimacion getEstimacion() {
        for (int i = intervenciones.size() - 1; i >= 0; i--) {
            Intervencion intervencion = intervenciones.get(i);
            if (intervencion.getTipo() == TipoIntervencion.ESTIMACION && intervencion.getEstimacion() != null
                    && intervencion.getEstimacion().tieneItems()) {
                return intervencion.getEstimacion();
            }
        }
        return null;
    }

    /**
     * Todos los registros de la solicitud en una sola lista ordenada por fecha.
     * <p>
     * Se juntan las seis listas y se ordenan por inserción directa. Cada lista ya viene
     * ordenada desde la base, así que la lista combinada está casi ordenada, que es el caso
     * en que la inserción tiene su mejor rendimiento, cercano a O(n). Además es un método
     * estable: dos registros que el criterio considera iguales conservan el orden en que se
     * juntaron.
     * Los registros que todavía no se guardaron no tienen fecha y quedan al final.
     */
    public List<RegistroSolicitud> getHistorial() {
        List<RegistroSolicitud> historial = new ArrayList<>();
        historial.addAll(cambiosEstado);
        historial.addAll(asignaciones);
        historial.addAll(reclasificaciones);
        historial.addAll(intervenciones);
        historial.addAll(esperas);
        historial.addAll(hitos);
        Ordenamiento.porInsercion(historial, POR_FECHA);
        return historial;
    }

    /**
     * Criterio del historial: por fecha, con los registros sin fecha (sin guardar) al final.
     * Varios registros se guardan en el mismo instante cuando uno provoca otro. En ese caso el
     * alta (pasar a Nueva) va primero, después las asignaciones y clasificaciones, después las
     * intervenciones, hitos y esperas, y al final el cambio de estado que provocaron.
     */
    private static final Comparator<RegistroSolicitud> POR_FECHA = (a, b) -> {
        if (a.getFecha() == null || b.getFecha() == null) {
            return Boolean.compare(a.getFecha() == null, b.getFecha() == null);
        }
        int porFecha = a.getFecha().compareTo(b.getFecha());
        return porFecha != 0 ? porFecha : Integer.compare(ordenEnElMismoInstante(a), ordenEnElMismoInstante(b));
    };

    private static int ordenEnElMismoInstante(RegistroSolicitud registro) {
        if (registro instanceof CambioEstado cambio) {
            return cambio.getEstado() == Estado.NUEVA ? 0 : 3;
        }
        if (registro instanceof Asignacion || registro instanceof Reclasificacion) {
            return 1;
        }
        return 2;
    }

    // ------------------------------------------------------------------
    // Eventos del diagrama de estados y operaciones de los casos de uso
    // ------------------------------------------------------------------

    /**
     * Registra la solicitud: queda Nueva, con el autor como primer responsable (CU01, pasos 11
     * y 12). Antes verifica los datos obligatorios y avisa todos los que faltan (alternativa 4.A).
     */
    public void registrar(Integrante autor) throws ReglaNegocioException {
        if (getEstadoActual() != null) {
            throw new ReglaNegocioException("La solicitud ya estaba registrada");
        }
        List<String> faltan = new ArrayList<>();
        if (solicitante == null) {
            faltan.add("el solicitante");
        }
        if (producto == null) {
            faltan.add("el producto");
        }
        if (canal == null) {
            faltan.add("el canal");
        }
        if (fechaRecepcion == null) {
            faltan.add("la fecha de recepción");
        }
        if (Formatos.estaVacio(asunto)) {
            faltan.add("el asunto");
        }
        if (!faltan.isEmpty()) {
            throw new ReglaNegocioException("Falta " + String.join(", ", faltan));
        }
        if (asunto.length() > LARGO_ASUNTO) {
            throw new ReglaNegocioException("El asunto admite hasta " + LARGO_ASUNTO + " caracteres");
        }
        if (fechaRecepcion.isAfter(LocalDateTime.now())) {
            throw new ReglaNegocioException("La fecha de recepción no puede ser posterior a hoy");
        }
        cambiarEstado(Estado.NUEVA, autor, null);
        asignar(autor, autor);
    }

    /**
     * Clasifica o reclasifica la solicitud (CU03). Siempre queda un registro con el autor y la
     * fecha: la primera vez con el motivo "Clasificación inicial" y las siguientes con la
     * clasificación anterior y el motivo que da el actor (alternativa 9.A).
     */
    public void clasificar(TipoSolicitud tipo, Prioridad prioridad, Integrante autor, String motivo)
            throws ReglaNegocioException {
        exigirAbierta("clasificarla");
        if (tipo == null || prioridad == null) {
            throw new ReglaNegocioException("Seleccioná el tipo de solicitud y la prioridad");
        }
        if (tipo == this.tipo && prioridad == this.prioridad) {
            throw new ReglaNegocioException("La solicitud ya tiene esa clasificación");
        }
        boolean yaClasificada = this.tipo != null || this.prioridad != null;
        if (yaClasificada && Formatos.estaVacio(motivo)) {
            throw new ReglaNegocioException("La solicitud ya estaba clasificada: ingresá el motivo de la reclasificación");
        }
        String motivoRegistro = yaClasificada ? motivo : Reclasificacion.MOTIVO_INICIAL;
        Reclasificacion registro = new Reclasificacion(this, autor, motivoRegistro, this.tipo, this.prioridad);
        registro.validar();
        reclasificaciones.add(registro);
        pendientes.add(registro);
        this.tipo = tipo;
        this.prioridad = prioridad;
    }

    /**
     * Asigna el módulo, o lo deja sin informar si es null (CU03, alternativa 5.A). Va aparte de
     * clasificar() porque se puede completar después del análisis.
     */
    public void asignarModulo(Modulo modulo) throws ReglaNegocioException {
        exigirAbierta("cambiar el módulo");
        if (modulo != null && !producto.tieneModulo(modulo)) {
            throw new ReglaNegocioException("El módulo " + modulo + " no corresponde al producto " + producto);
        }
        this.modulo = modulo;
    }

    /** Asigna la solicitud a un integrante, que pasa a ser el responsable (RF07). */
    public void asignar(Integrante integrante, Integrante autor) throws ReglaNegocioException {
        if (getEstadoActual() == null || getEstadoActual().esCierre()) {
            throw new ReglaNegocioException("Solo se puede asignar una solicitud abierta");
        }
        Asignacion asignacion = new Asignacion(this, autor, integrante);
        asignacion.validar();
        asignaciones.add(asignacion);
        pendientes.add(asignacion);
    }

    /**
     * Agrega una intervención (CU06). Con la primera, la solicitud pasa de Nueva a En curso
     * (alternativa 10.A).
     */
    public void registrarIntervencion(Intervencion intervencion) throws ReglaNegocioException {
        exigirAbierta("registrar una intervención");
        if (intervencion.getSolicitud() != this) {
            throw new IllegalArgumentException("La intervención es de otra solicitud");
        }
        intervencion.validar();
        intervenciones.add(intervencion);
        pendientes.add(intervencion);
        if (getEstadoActual() == Estado.NUEVA) {
            cambiarEstado(Estado.EN_CURSO, intervencion.getAutor(), null);
        }
    }

    /** Abre una espera y pasa la solicitud a En espera (CU07, pasos 2 a 7). */
    public void abrirEspera(MotivoEspera motivo, Integrante autor) throws ReglaNegocioException {
        if (getEsperaAbierta() != null) {
            throw new ReglaNegocioException("La solicitud ya tiene una espera abierta: primero hay que cerrarla");
        }
        if (getEstadoActual() != Estado.EN_CURSO) {
            throw new ReglaNegocioException("Solo se puede abrir una espera sobre una solicitud En curso");
        }
        Espera espera = new Espera(this, autor, motivo);
        espera.validar();
        esperas.add(espera);
        pendientes.add(espera);
        cambiarEstado(Estado.EN_ESPERA, autor, motivo.getNombre());
    }

    /** Cierra la espera abierta y devuelve la solicitud a En curso (CU07, pasos 8 y 9). */
    public void cerrarEspera(Integrante autor) throws ReglaNegocioException {
        if (getEstadoActual() != Estado.EN_ESPERA) {
            throw new ReglaNegocioException("La solicitud no está en espera");
        }
        terminarEsperaAbierta();
        cambiarEstado(Estado.EN_CURSO, autor, null);
    }

    /** Agrega un hito técnico (CU08). */
    public void registrarHito(HitoTecnico hito) throws ReglaNegocioException {
        exigirAbierta("registrar un hito");
        if (hito.getSolicitud() != this) {
            throw new IllegalArgumentException("El hito es de otra solicitud");
        }
        hito.validar();
        hitos.add(hito);
        pendientes.add(hito);
    }

    /**
     * Verifica la información mínima para cerrar como Resuelta o Cerrada sin confirmación
     * (CU09, alternativa 5.A y observación 1). Es el método de la prueba de caja blanca
     * CP-B-01 (sección 12.3). Los faltantes quedan guardados para que la pantalla los muestre.
     * <p>
     * La decisión D0, el tipo de solicitud, se agregó en el TP3: sin tipo no se sabe qué
     * exigir, y la solicitud quedaría mal contada en los reportes por tipo.
     */
    public boolean validarCierre() {
        faltantesCierre.clear();
        if (tipo == null) {                                                         // D0
            faltantesCierre.add("tipo de solicitud");
        }
        if (!tieneRespuestaAlCliente()) {                                           // D1
            faltantesCierre.add("respuesta al cliente");
        }
        if (!tieneIntervencionDe(getResponsable())) {                              // D2
            faltantesCierre.add("intervención del responsable");
        }
        if (tipo != null && tipo.esError()) {                                       // D3
            if (!tieneHito(TipoHito.PUBLICACION_PRODUCCION)) {                      // D4
                faltantesCierre.add("hito de publicación en producción");
            }
        } else if (tipo == TipoSolicitud.PEDIDO_PRESUPUESTO) {                      // D5
            if (getEstimacion() == null) {                                          // D6
                faltantesCierre.add("estimación con al menos un ítem");
            }
        }
        return faltantesCierre.isEmpty();
    }

    /** Lo que faltó en la última llamada a validarCierre(). */
    public List<String> getFaltantesCierre() {
        return Collections.unmodifiableList(faltantesCierre);
    }

    /**
     * Cierra la solicitud como Resuelta, Cerrada sin confirmación o Cancelada (CU09). Si estaba
     * En espera, cierra también la espera abierta. Para Cancelada alcanza con el motivo y una
     * intervención del responsable (observación 2). Para los otros dos exige validarCierre().
     */
    public void cerrar(Estado cierre, Integrante autor, String motivo) throws ReglaNegocioException {
        Estado actual = getEstadoActual();
        if (actual != Estado.EN_CURSO && actual != Estado.EN_ESPERA) {
            throw new ReglaNegocioException("Solo se puede cerrar una solicitud En curso o En espera");
        }
        if (cierre != Estado.RESUELTA && cierre != Estado.CERRADA_SIN_CONFIRMACION && cierre != Estado.CANCELADA) {
            throw new IllegalArgumentException("El tipo de cierre tiene que ser Resuelta, Cerrada sin confirmación o Cancelada");
        }
        if (!autor.equals(getResponsable())) {
            throw new ReglaNegocioException("Solo el responsable de la solicitud la puede cerrar");
        }
        if (cierre == Estado.CANCELADA) {
            if (Formatos.estaVacio(motivo)) {
                throw new ReglaNegocioException("Ingresá el motivo de la cancelación");
            }
            if (!tieneIntervencionDe(autor)) {
                throw new ReglaNegocioException("Falta la intervención del responsable");
            }
        } else if (!validarCierre()) {
            throw new ReglaNegocioException("Para cerrar la solicitud falta: " + String.join(", ", faltantesCierre));
        }
        if (actual == Estado.EN_ESPERA) {
            terminarEsperaAbierta();
        }
        cambiarEstado(cierre, autor, motivo);
    }

    /** Vuelve a En curso una solicitud resuelta o cerrada sin confirmación (CU07, alternativa 1.A). */
    public void reabrir(Integrante autor, String motivo) throws ReglaNegocioException {
        Estado actual = getEstadoActual();
        if (actual != Estado.RESUELTA && actual != Estado.CERRADA_SIN_CONFIRMACION) {
            throw new ReglaNegocioException("Solo se puede reabrir una solicitud Resuelta o Cerrada sin confirmación");
        }
        if (Formatos.estaVacio(motivo)) {
            throw new ReglaNegocioException("Ingresá el motivo de la reapertura");
        }
        cambiarEstado(Estado.EN_CURSO, autor, motivo);
    }

    /** Descarta una solicitud nueva que no corresponde a soporte, sin borrarla (CU01, alternativa 10.A). */
    public void descartar(Integrante autor, String motivo) throws ReglaNegocioException {
        if (getEstadoActual() != Estado.NUEVA) {
            throw new ReglaNegocioException("Solo se puede descartar una solicitud Nueva");
        }
        cambiarEstado(Estado.DESCARTADA, autor, motivo);
    }

    /** Marca la solicitud como duplicada de otra (alternativa de CU10, prevista en el diseño). */
    public void marcarDuplicada(Solicitud original, Integrante autor) throws ReglaNegocioException {
        Estado actual = getEstadoActual();
        if (actual != Estado.NUEVA && actual != Estado.EN_CURSO) {
            throw new ReglaNegocioException("Solo se puede marcar como duplicada una solicitud Nueva o En curso");
        }
        vincular(original, TipoVinculo.DUPLICADA);
        cambiarEstado(Estado.DUPLICADA, autor, "Duplicada de la solicitud " + original.getId());
    }

    /**
     * Vincula la solicitud con otra. Un tema nuevo sobre una solicitud cerrada se vincula como
     * continuación (CU01 alternativa 3.B, CU07 alternativa 1.A.3.A), y para eso la otra tiene
     * que estar cerrada.
     */
    public void vincular(Solicitud origen, TipoVinculo vinculo) throws ReglaNegocioException {
        if (origen == null || origen.getId() == 0 || vinculo == null) {
            throw new ReglaNegocioException("Indicá la solicitud de origen y el tipo de vínculo");
        }
        if (origen.getId() == id) {
            throw new ReglaNegocioException("Una solicitud no se puede vincular consigo misma");
        }
        if (vinculo == TipoVinculo.CONTINUACION && origen.estaAbierta()) {
            throw new ReglaNegocioException("La solicitud " + origen.getId()
                    + " sigue abierta: el tema nuevo se registra en ella con una intervención");
        }
        this.solicitudOrigenId = origen.getId();
        this.tipoVinculo = vinculo;
    }

    // ------------------------------------------------------------------
    // Registros pendientes de guardar
    // ------------------------------------------------------------------

    public boolean tienePendientes() {
        return !pendientes.isEmpty();
    }

    /** Saca de la cola el registro más antiguo que falta guardar (primero en entrar, primero en salir). */
    public RegistroSolicitud tomarPendiente() {
        return pendientes.poll();
    }

    /** Agrega un registro que ya estaba guardado en la base. Lo usa el DAO al leer la solicitud. */
    public void cargarRegistroGuardado(RegistroSolicitud registro) {
        if (registro instanceof Intervencion i) {
            intervenciones.add(i);
        } else if (registro instanceof Asignacion a) {
            asignaciones.add(a);
        } else if (registro instanceof CambioEstado c) {
            cambiosEstado.add(c);
        } else if (registro instanceof Espera e) {
            esperas.add(e);
        } else if (registro instanceof HitoTecnico h) {
            hitos.add(h);
        } else if (registro instanceof Reclasificacion r) {
            reclasificaciones.add(r);
        }
    }

    // ------------------------------------------------------------------
    // Métodos auxiliares
    // ------------------------------------------------------------------

    /**
     * Único punto donde cambia el estado. Es privado para que el estado no pueda cambiar
     * por fuera del diagrama de estados.
     */
    private void cambiarEstado(Estado nuevo, Integrante autor, String motivo) throws ReglaNegocioException {
        CambioEstado cambio = new CambioEstado(this, autor, nuevo, motivo);
        cambio.validar();
        cambiosEstado.add(cambio);
        pendientes.add(cambio);
    }

    /** Cierra la espera abierta y la pone en la cola, para que el DAO guarde su fin. */
    private void terminarEsperaAbierta() {
        Espera abierta = getEsperaAbierta();
        if (abierta != null) {
            abierta.cerrar(LocalDateTime.now());
            pendientes.add(abierta);
        }
    }

    private void exigirAbierta(String operacion) throws ReglaNegocioException {
        if (!estaAbierta()) {
            throw new ReglaNegocioException("La solicitud está " + getEstadoActual()
                    + ": hay que reabrirla para " + operacion);
        }
    }

    /** Búsqueda secuencial de una respuesta al cliente entre las intervenciones. */
    private boolean tieneRespuestaAlCliente() {
        for (Intervencion i : intervenciones) {
            if (i.tieneRespuestaAlCliente()) {
                return true;
            }
        }
        return false;
    }

    /** Búsqueda secuencial de una intervención de un integrante. */
    private boolean tieneIntervencionDe(Integrante integrante) {
        if (integrante == null) {
            return false;
        }
        for (Intervencion i : intervenciones) {
            if (integrante.equals(i.getAutor())) {
                return true;
            }
        }
        return false;
    }

    /** Búsqueda secuencial de un hito de un tipo. */
    private boolean tieneHito(TipoHito tipoHito) {
        for (HitoTecnico h : hitos) {
            if (h.getTipo() == tipoHito) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Solicitud s && s.id == id && id != 0;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return id + " - " + asunto;
    }
}
