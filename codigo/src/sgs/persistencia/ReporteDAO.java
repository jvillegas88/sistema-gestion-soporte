package sgs.persistencia;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import sgs.modelo.Integrante;
import sgs.modelo.Ordenamiento;
import sgs.modelo.Producto;
import sgs.modelo.catalogo.Catalogo;
import sgs.modelo.catalogo.Estado;
import sgs.modelo.catalogo.MotivoEspera;
import sgs.modelo.catalogo.TipoSolicitud;
import sgs.modelo.consulta.FilaCargaIntegrante;
import sgs.modelo.consulta.FilaDemorada;
import sgs.modelo.consulta.FilaEsfuerzoCliente;
import sgs.modelo.consulta.FilaPorTipo;

/**
 * Consultas de los reportes de CU11 y CU12. Los totales se calculan en la base con GROUP BY y
 * por la red viajan solo los resultados, que es lo que plantea la sección 11 para cumplir RNF11.
 * Los reportes no modifican ningún dato.
 * <p>
 * Un período va del primer día al último, los dos incluidos. En las consultas se usa el día
 * siguiente al último como límite excluido, para no perder lo registrado ese día a cualquier hora.
 */
public class ReporteDAO {

    /** Nombre del cliente al que van las solicitudes importadas sin cliente (CU02, alternativa 8.A). */
    public static final String CLIENTE_NO_IDENTIFICADO = "Cliente no identificado";

    private static final String DESDE_SOLICITUD_A_CLIENTE = " JOIN solicitantes so ON so.solicitante_id = s.solicitante_id"
            + " JOIN clientes c ON c.cliente_id = so.cliente_id ";

    private final DatosMaestros maestros;

    public ReporteDAO(DatosMaestros maestros) {
        this.maestros = maestros;
    }

    // ------------------------------------------------------------------
    // CU11 Reporte de esfuerzo por cliente
    // ------------------------------------------------------------------

    /**
     * Por cliente: solicitudes recibidas en el período, horas trabajadas en el período y días que
     * sus solicitudes estuvieron en espera dentro del período (CU11, paso 4). Si se indica un
     * período de comparación, agrega sus horas (alternativa 3.A). El cliente no identificado va
     * al final (alternativa 4.A).
     *
     * @param producto el producto por el que se filtra, o null para todos
     * @param compDesde inicio del período de comparación, o null si no se compara
     */
    public List<FilaEsfuerzoCliente> esfuerzoPorCliente(Connection con, LocalDate desde, LocalDate hasta,
            Producto producto, LocalDate compDesde, LocalDate compHasta) throws SQLException {
        Map<String, FilaEsfuerzoCliente> filas = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> e : contarSolicitudes(con, desde, hasta, producto).entrySet()) {
            fila(filas, e.getKey()).setSolicitudes(e.getValue().intValue());
        }
        for (Map.Entry<String, BigDecimal> e : sumarHoras(con, desde, hasta, producto).entrySet()) {
            fila(filas, e.getKey()).setHorasTrabajadas(e.getValue());
        }
        for (Map.Entry<String, BigDecimal> e : sumarDiasEspera(con, desde, hasta, producto).entrySet()) {
            fila(filas, e.getKey()).setDiasEspera(e.getValue());
        }
        if (compDesde != null) {
            Map<String, BigDecimal> horasComparacion = sumarHoras(con, compDesde, compHasta, producto);
            for (Map.Entry<String, BigDecimal> e : horasComparacion.entrySet()) {
                fila(filas, e.getKey());
            }
            for (FilaEsfuerzoCliente f : filas.values()) {
                f.setHorasComparacion(horasComparacion.getOrDefault(f.getCliente(), BigDecimal.ZERO));
            }
        }
        List<FilaEsfuerzoCliente> lista = new ArrayList<>(filas.values());
        Ordenamiento.porInsercion(lista, (a, b) -> compararClientes(a.getCliente(), b.getCliente()));
        return lista;
    }

    /** Busca la fila del cliente y, si todavía no existe, la crea. */
    private static FilaEsfuerzoCliente fila(Map<String, FilaEsfuerzoCliente> filas, String cliente) {
        return filas.computeIfAbsent(cliente, FilaEsfuerzoCliente::new);
    }

    private Map<String, BigDecimal> contarSolicitudes(Connection con, LocalDate desde, LocalDate hasta,
            Producto producto) throws SQLException {
        String sql = "SELECT c.nombre, COUNT(*) FROM solicitudes s" + DESDE_SOLICITUD_A_CLIENTE
                + "WHERE s.fecha_recepcion >= ? AND s.fecha_recepcion < ?" + filtroProducto(producto)
                + " GROUP BY c.cliente_id, c.nombre";
        return consultarPorNombre(con, sql, producto, desde.atStartOfDay(), finExcluido(hasta));
    }

    /**
     * Horas de las intervenciones registradas en el período, más el tiempo histórico de las
     * solicitudes importadas que se recibieron en el período, porque esas no tienen intervenciones.
     */
    private Map<String, BigDecimal> sumarHoras(Connection con, LocalDate desde, LocalDate hasta, Producto producto)
            throws SQLException {
        String sql = "SELECT c.nombre, SUM(t.horas) FROM ("
                + " SELECT s.solicitante_id, i.tiempo_horas AS horas FROM intervenciones i"
                + " JOIN solicitudes s ON s.solicitud_id = i.solicitud_id"
                + " WHERE i.fecha >= ? AND i.fecha < ?" + filtroProducto(producto)
                + " UNION ALL"
                + " SELECT s.solicitante_id, s.tiempo_historico FROM solicitudes s"
                + " WHERE s.tiempo_historico IS NOT NULL AND s.fecha_recepcion >= ? AND s.fecha_recepcion < ?"
                + filtroProducto(producto)
                + ") t JOIN solicitantes so ON so.solicitante_id = t.solicitante_id"
                + " JOIN clientes c ON c.cliente_id = so.cliente_id GROUP BY c.cliente_id, c.nombre";
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = finExcluido(hasta);
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int p = 1;
            ps.setObject(p++, inicio);
            ps.setObject(p++, fin);
            if (producto != null) {
                ps.setInt(p++, producto.getId());
            }
            ps.setObject(p++, inicio);
            ps.setObject(p++, fin);
            if (producto != null) {
                ps.setInt(p++, producto.getId());
            }
            return leerPorNombre(ps);
        }
    }

    /**
     * Días en espera dentro del período: de cada espera se toma solo la parte que cae entre el
     * inicio y el fin del período. Una espera abierta cuenta hasta el momento de la consulta.
     */
    private Map<String, BigDecimal> sumarDiasEspera(Connection con, LocalDate desde, LocalDate hasta,
            Producto producto) throws SQLException {
        String sql = "SELECT c.nombre, SUM(GREATEST(0, TIMESTAMPDIFF(MINUTE, GREATEST(e.desde, ?),"
                + " LEAST(COALESCE(e.hasta, NOW()), ?)))) / 1440 FROM esperas e"
                + " JOIN solicitudes s ON s.solicitud_id = e.solicitud_id" + DESDE_SOLICITUD_A_CLIENTE
                + "WHERE e.desde < ? AND COALESCE(e.hasta, NOW()) > ?" + filtroProducto(producto)
                + " GROUP BY c.cliente_id, c.nombre";
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = finExcluido(hasta);
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, inicio);
            ps.setObject(2, fin);
            ps.setObject(3, fin);
            ps.setObject(4, inicio);
            if (producto != null) {
                ps.setInt(5, producto.getId());
            }
            return leerPorNombre(ps);
        }
    }

    /** Cantidad de solicitudes recibidas en el período, por cliente y por tipo (CU11, paso 4). */
    public List<FilaPorTipo> solicitudesPorTipo(Connection con, LocalDate desde, LocalDate hasta, Producto producto)
            throws SQLException {
        String sql = "SELECT c.nombre, s.tipo_solicitud_id, COUNT(*) FROM solicitudes s" + DESDE_SOLICITUD_A_CLIENTE
                + "WHERE s.fecha_recepcion >= ? AND s.fecha_recepcion < ?" + filtroProducto(producto)
                + " GROUP BY c.cliente_id, c.nombre, s.tipo_solicitud_id";
        Map<String, FilaPorTipo> filas = new LinkedHashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, desde.atStartOfDay());
            ps.setObject(2, finExcluido(hasta));
            if (producto != null) {
                ps.setInt(3, producto.getId());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FilaPorTipo fila = filas.computeIfAbsent(rs.getString(1), FilaPorTipo::new);
                    fila.sumar(tipoONulo(rs, 2), BigDecimal.valueOf(rs.getLong(3)));
                }
            }
        }
        List<FilaPorTipo> lista = new ArrayList<>(filas.values());
        Ordenamiento.porInsercion(lista, (a, b) -> compararClientes(a.getNombre(), b.getNombre()));
        return lista;
    }

    // ------------------------------------------------------------------
    // CU12 Reporte de carga por integrante
    // ------------------------------------------------------------------

    /**
     * Por integrante: horas registradas y cantidad de intervenciones en el período (CU12, paso 5).
     * Muestra a todo el equipo activo, también a quien no registró tiempo, como la consulta de la
     * sección 17. Quien ya no está activo aparece solo si registró tiempo en el período.
     * Se ordena por nombre y no por horas, para que no funcione como un ranking (RNF05).
     */
    public List<FilaCargaIntegrante> cargaPorIntegrante(Connection con, LocalDate desde, LocalDate hasta)
            throws SQLException {
        String sql = "SELECT i.integrante_id, SUM(i.tiempo_horas), COUNT(*) FROM intervenciones i"
                + " WHERE i.fecha >= ? AND i.fecha < ? GROUP BY i.integrante_id";
        Map<Integer, Object[]> porIntegrante = new LinkedHashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, desde.atStartOfDay());
            ps.setObject(2, finExcluido(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    porIntegrante.put(rs.getInt(1), new Object[] {rs.getBigDecimal(2), rs.getInt(3)});
                }
            }
        }
        List<FilaCargaIntegrante> filas = new ArrayList<>();
        for (Integrante integrante : maestros.getIntegrantes()) {
            Object[] datos = porIntegrante.get(integrante.getId());
            if (datos != null) {
                filas.add(new FilaCargaIntegrante(integrante.getNombre(), integrante.getRol().getNombre(),
                        (BigDecimal) datos[0], (Integer) datos[1]));
            } else if (integrante.estaActivo()) {
                filas.add(new FilaCargaIntegrante(integrante.getNombre(), integrante.getRol().getNombre(),
                        BigDecimal.ZERO, 0));
            }
        }
        return filas;
    }

    /**
     * Horas de cada integrante en el período, repartidas por el tipo de la solicitud (CU12, paso 5).
     * Incluye a los mismos integrantes que cargaPorIntegrante().
     */
    public List<FilaPorTipo> horasPorTipo(Connection con, LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT i.integrante_id, s.tipo_solicitud_id, SUM(i.tiempo_horas) FROM intervenciones i"
                + " JOIN solicitudes s ON s.solicitud_id = i.solicitud_id"
                + " WHERE i.fecha >= ? AND i.fecha < ? GROUP BY i.integrante_id, s.tipo_solicitud_id";
        Map<Integer, FilaPorTipo> porIntegrante = new LinkedHashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, desde.atStartOfDay());
            ps.setObject(2, finExcluido(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    FilaPorTipo fila = porIntegrante.computeIfAbsent(id,
                            clave -> new FilaPorTipo(maestros.integrante(clave).getNombre()));
                    fila.sumar(tipoONulo(rs, 2), rs.getBigDecimal(3));
                }
            }
        }
        List<FilaPorTipo> filas = new ArrayList<>();
        for (Integrante integrante : maestros.getIntegrantes()) {
            FilaPorTipo fila = porIntegrante.get(integrante.getId());
            if (fila != null) {
                filas.add(fila);
            } else if (integrante.estaActivo()) {
                filas.add(new FilaPorTipo(integrante.getNombre()));
            }
        }
        return filas;
    }

    /**
     * Solicitudes abiertas que llevan más de cierta cantidad de días: abiertas sin respuesta al
     * cliente o con una espera abierta prolongada (CU12, alternativa 6.A, RF23).
     */
    public List<FilaDemorada> solicitudesDemoradas(Connection con, int diasUmbral) throws SQLException {
        String sql = "SELECT * FROM (SELECT s.solicitud_id, c.nombre AS cliente, s.asunto, s.fecha_recepcion, "
                + "(SELECT ce.estado_id FROM cambios_estado ce WHERE ce.solicitud_id = s.solicitud_id "
                + " ORDER BY ce.fecha DESC, ce.cambio_estado_id DESC LIMIT 1) AS estado_id, "
                + "(SELECT a.integrante_id FROM asignaciones a WHERE a.solicitud_id = s.solicitud_id "
                + " ORDER BY a.fecha DESC, a.asignacion_id DESC LIMIT 1) AS responsable_id, "
                + "(SELECT e.desde FROM esperas e WHERE e.solicitud_id = s.solicitud_id AND e.hasta IS NULL LIMIT 1) AS espera_desde, "
                + "(SELECT e.motivo_espera_id FROM esperas e WHERE e.solicitud_id = s.solicitud_id AND e.hasta IS NULL LIMIT 1) AS espera_motivo, "
                + "EXISTS (SELECT 1 FROM comunicaciones co JOIN intervenciones i ON i.intervencion_id = co.intervencion_id "
                + " WHERE i.solicitud_id = s.solicitud_id AND co.tipo = 'RESPUESTA_CLIENTE') AS con_respuesta "
                + "FROM solicitudes s" + DESDE_SOLICITUD_A_CLIENTE + ") t "
                + "WHERE t.estado_id IN (?, ?, ?) ORDER BY t.fecha_recepcion";
        List<FilaDemorada> filas = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Estado.NUEVA.getId());
            ps.setInt(2, Estado.EN_CURSO.getId());
            ps.setInt(3, Estado.EN_ESPERA.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long diasAbierta = Duration.between(rs.getObject("fecha_recepcion", LocalDateTime.class), ahora).toDays();
                    LocalDateTime esperaDesde = rs.getObject("espera_desde", LocalDateTime.class);
                    long diasEspera = esperaDesde == null ? 0 : Duration.between(esperaDesde, ahora).toDays();
                    List<String> situacion = new ArrayList<>();
                    if (esperaDesde != null && diasEspera > diasUmbral) {
                        situacion.add("Espera prolongada: "
                                + Catalogo.porId(MotivoEspera.class, rs.getInt("espera_motivo")).getNombre());
                    }
                    if (!rs.getBoolean("con_respuesta") && diasAbierta > diasUmbral) {
                        situacion.add("Sin respuesta al cliente");
                    }
                    if (!situacion.isEmpty()) {
                        Integer responsableId = RegistroDAO.enteroONulo(rs, "responsable_id");
                        filas.add(new FilaDemorada(rs.getInt("solicitud_id"), rs.getString("cliente"),
                                rs.getString("asunto"), Catalogo.porId(Estado.class, rs.getInt("estado_id")).getNombre(),
                                responsableId == null ? "" : maestros.integrante(responsableId).getNombre(),
                                diasAbierta, diasEspera, String.join(". ", situacion)));
                    }
                }
            }
        }
        return filas;
    }

    // ------------------------------------------------------------------
    // Ayudas
    // ------------------------------------------------------------------

    private static String filtroProducto(Producto producto) {
        return producto == null ? "" : " AND s.producto_id = ?";
    }

    private static LocalDateTime finExcluido(LocalDate hasta) {
        return hasta.plusDays(1).atStartOfDay();
    }

    private static TipoSolicitud tipoONulo(ResultSet rs, int columna) throws SQLException {
        int id = rs.getInt(columna);
        return rs.wasNull() ? null : Catalogo.porId(TipoSolicitud.class, id);
    }

    private static Map<String, BigDecimal> consultarPorNombre(Connection con, String sql, Producto producto,
            LocalDateTime inicio, LocalDateTime fin) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, inicio);
            ps.setObject(2, fin);
            if (producto != null) {
                ps.setInt(3, producto.getId());
            }
            return leerPorNombre(ps);
        }
    }

    /** Lee pares nombre y valor de una consulta agrupada. */
    private static Map<String, BigDecimal> leerPorNombre(PreparedStatement ps) throws SQLException {
        Map<String, BigDecimal> resultado = new LinkedHashMap<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BigDecimal valor = rs.getBigDecimal(2);
                resultado.put(rs.getString(1), valor == null ? BigDecimal.ZERO : valor.setScale(2, RoundingMode.HALF_UP));
            }
        }
        return resultado;
    }

    /**
     * Criterio de orden de los clientes: alfabético, con el cliente no identificado siempre al
     * final, para que no se mezcle con los demás (CU11, alternativa 4.A).
     */
    private static int compararClientes(String a, String b) {
        boolean aNoIdentificado = a.equals(CLIENTE_NO_IDENTIFICADO);
        boolean bNoIdentificado = b.equals(CLIENTE_NO_IDENTIFICADO);
        if (aNoIdentificado || bNoIdentificado) {
            return Boolean.compare(aNoIdentificado, bNoIdentificado);
        }
        return a.compareToIgnoreCase(b);
    }
}
