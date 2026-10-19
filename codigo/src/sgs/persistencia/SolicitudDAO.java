package sgs.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import sgs.modelo.Integrante;
import sgs.modelo.Modulo;
import sgs.modelo.Producto;
import sgs.modelo.RegistroSolicitud;
import sgs.modelo.Solicitud;
import sgs.modelo.catalogo.Canal;
import sgs.modelo.catalogo.Catalogo;
import sgs.modelo.catalogo.Estado;
import sgs.modelo.catalogo.Prioridad;
import sgs.modelo.catalogo.TipoSolicitud;
import sgs.modelo.catalogo.TipoVinculo;
import sgs.modelo.consulta.ResumenSolicitud;

/**
 * Acceso a las solicitudes. Lee y guarda la solicitud junto con sus registros, que maneja con
 * un DAO por tipo de registro.
 */
public class SolicitudDAO implements DAO<Solicitud> {

    /** Estado actual y responsable actual, calculados con el último registro de cada tipo. */
    private static final String ESTADO_ACTUAL = "(SELECT ce.estado_id FROM cambios_estado ce "
            + "WHERE ce.solicitud_id = s.solicitud_id ORDER BY ce.fecha DESC, ce.cambio_estado_id DESC LIMIT 1)";
    private static final String RESPONSABLE_ACTUAL = "(SELECT a.integrante_id FROM asignaciones a "
            + "WHERE a.solicitud_id = s.solicitud_id ORDER BY a.fecha DESC, a.asignacion_id DESC LIMIT 1)";

    private final DatosMaestros maestros;
    private final List<RegistroDAO<?>> daosRegistros = new ArrayList<>();

    public SolicitudDAO(DatosMaestros maestros) {
        this.maestros = maestros;
        daosRegistros.add(new ReclasificacionDAO(maestros));
        daosRegistros.add(new CambioEstadoDAO(maestros));
        daosRegistros.add(new AsignacionDAO(maestros));
        daosRegistros.add(new IntervencionDAO(maestros));
        daosRegistros.add(new EsperaDAO(maestros));
        daosRegistros.add(new HitoTecnicoDAO(maestros));
    }

    /**
     * Guarda la solicitud y todos sus registros pendientes. Si es nueva la inserta y, si no,
     * actualiza los datos que pueden cambiar. Los registros se guardan en el orden en que
     * entraron a la cola, que es el orden en que ocurrieron. Tiene que llamarse dentro de una
     * transacción, para que se guarde todo o nada.
     */
    public void guardar(Connection con, Solicitud solicitud) throws SQLException {
        if (solicitud.getId() == 0) {
            insertar(con, solicitud);
        } else {
            actualizar(con, solicitud);
        }
        while (solicitud.tienePendientes()) {
            RegistroSolicitud registro = solicitud.tomarPendiente();
            daoPara(registro).guardar(con, registro);
        }
    }

    /** Búsqueda secuencial del DAO que acepta el registro: hay uno por cada tipo. */
    private RegistroDAO<?> daoPara(RegistroSolicitud registro) {
        for (RegistroDAO<?> dao : daosRegistros) {
            if (dao.acepta(registro)) {
                return dao;
            }
        }
        throw new IllegalStateException("No hay un DAO para " + registro.getClass().getSimpleName());
    }

    @Override
    public int insertar(Connection con, Solicitud s) throws SQLException {
        String sql = "INSERT INTO solicitudes (numero_origen, solicitante_id, producto_id, modulo_nro, canal_id, "
                + "tipo_solicitud_id, prioridad_id, solicitud_origen_id, tipo_vinculo, fecha_recepcion, asunto, "
                + "descripcion, tiempo_historico) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ponerEnteroONulo(ps, 1, s.getNumeroOrigen());
            ps.setInt(2, s.getSolicitante().getId());
            ps.setInt(3, s.getProducto().getId());
            ponerEnteroONulo(ps, 4, s.getModulo() == null ? null : s.getModulo().getNumero());
            ps.setInt(5, s.getCanal().getId());
            ponerEnteroONulo(ps, 6, s.getTipo() == null ? null : s.getTipo().getId());
            ponerEnteroONulo(ps, 7, s.getPrioridad() == null ? null : s.getPrioridad().getId());
            ponerEnteroONulo(ps, 8, s.getSolicitudOrigenId());
            ps.setString(9, s.getTipoVinculo() == null ? null : s.getTipoVinculo().name());
            ps.setObject(10, s.getFechaRecepcion());
            ps.setString(11, s.getAsunto());
            ps.setString(12, s.getDescripcion());
            ps.setBigDecimal(13, s.getTiempoHistorico());
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                claves.next();
                s.setId(claves.getInt(1));
            }
        }
        return s.getId();
    }

    /** Actualiza lo que cambia después del alta: la clasificación, el módulo y el vínculo. */
    private void actualizar(Connection con, Solicitud s) throws SQLException {
        String sql = "UPDATE solicitudes SET modulo_nro = ?, tipo_solicitud_id = ?, prioridad_id = ?, "
                + "solicitud_origen_id = ?, tipo_vinculo = ? WHERE solicitud_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ponerEnteroONulo(ps, 1, s.getModulo() == null ? null : s.getModulo().getNumero());
            ponerEnteroONulo(ps, 2, s.getTipo() == null ? null : s.getTipo().getId());
            ponerEnteroONulo(ps, 3, s.getPrioridad() == null ? null : s.getPrioridad().getId());
            ponerEnteroONulo(ps, 4, s.getSolicitudOrigenId());
            ps.setString(5, s.getTipoVinculo() == null ? null : s.getTipoVinculo().name());
            ps.setInt(6, s.getId());
            ps.executeUpdate();
        }
    }

    /** Lee la solicitud con todos sus registros. Devuelve null si no existe. */
    @Override
    public Solicitud buscarPorId(Connection con, int id) throws SQLException {
        Solicitud solicitud;
        String sql = "SELECT solicitud_id, numero_origen, solicitante_id, producto_id, modulo_nro, canal_id, "
                + "tipo_solicitud_id, prioridad_id, solicitud_origen_id, tipo_vinculo, fecha_recepcion, asunto, "
                + "descripcion, tiempo_historico FROM solicitudes WHERE solicitud_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                solicitud = armar(rs);
            }
        }
        for (RegistroDAO<?> dao : daosRegistros) {
            for (RegistroSolicitud registro : dao.listarPorSolicitud(con, solicitud)) {
                solicitud.cargarRegistroGuardado(registro);
            }
        }
        return solicitud;
    }

    private Solicitud armar(ResultSet rs) throws SQLException {
        Producto producto = maestros.producto(rs.getInt("producto_id"));
        Integer moduloNro = RegistroDAO.enteroONulo(rs, "modulo_nro");
        Modulo modulo = moduloNro == null ? null : producto.buscarModulo(moduloNro);
        Integer tipoId = RegistroDAO.enteroONulo(rs, "tipo_solicitud_id");
        Integer prioridadId = RegistroDAO.enteroONulo(rs, "prioridad_id");
        String vinculo = rs.getString("tipo_vinculo");
        return new Solicitud(rs.getInt("solicitud_id"), RegistroDAO.enteroONulo(rs, "numero_origen"),
                maestros.solicitante(rs.getInt("solicitante_id")), producto, modulo,
                Catalogo.porId(Canal.class, rs.getInt("canal_id")),
                tipoId == null ? null : Catalogo.porId(TipoSolicitud.class, tipoId),
                prioridadId == null ? null : Catalogo.porId(Prioridad.class, prioridadId),
                RegistroDAO.enteroONulo(rs, "solicitud_origen_id"),
                vinculo == null ? null : TipoVinculo.valueOf(vinculo),
                rs.getObject("fecha_recepcion", LocalDateTime.class), rs.getString("asunto"),
                rs.getString("descripcion"), rs.getBigDecimal("tiempo_historico"));
    }

    /**
     * Solicitudes abiertas para la bandeja: primero las de mayor prioridad y, dentro de cada
     * prioridad, las más antiguas. Las que todavía no tienen prioridad van al final.
     */
    public List<ResumenSolicitud> listarAbiertas(Connection con) throws SQLException {
        String sql = "SELECT * FROM (SELECT s.solicitud_id, s.fecha_recepcion, c.nombre AS cliente, "
                + "p.nombre AS producto, s.asunto, s.tipo_solicitud_id, s.prioridad_id, "
                + ESTADO_ACTUAL + " AS estado_id, " + RESPONSABLE_ACTUAL + " AS responsable_id "
                + "FROM solicitudes s JOIN solicitantes so ON so.solicitante_id = s.solicitante_id "
                + "JOIN clientes c ON c.cliente_id = so.cliente_id JOIN productos p ON p.producto_id = s.producto_id) t "
                + "WHERE t.estado_id IN (?, ?, ?) "
                + "ORDER BY t.prioridad_id IS NULL, t.prioridad_id, t.fecha_recepcion";
        List<ResumenSolicitud> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Estado.NUEVA.getId());
            ps.setInt(2, Estado.EN_CURSO.getId());
            ps.setInt(3, Estado.EN_ESPERA.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer tipoId = RegistroDAO.enteroONulo(rs, "tipo_solicitud_id");
                    Integer prioridadId = RegistroDAO.enteroONulo(rs, "prioridad_id");
                    Integer responsableId = RegistroDAO.enteroONulo(rs, "responsable_id");
                    Integrante responsable = responsableId == null ? null : maestros.integrante(responsableId);
                    lista.add(new ResumenSolicitud(rs.getInt("solicitud_id"),
                            rs.getObject("fecha_recepcion", LocalDateTime.class), rs.getString("cliente"),
                            rs.getString("producto"), rs.getString("asunto"),
                            tipoId == null ? null : Catalogo.porId(TipoSolicitud.class, tipoId),
                            prioridadId == null ? null : Catalogo.porId(Prioridad.class, prioridadId),
                            Catalogo.porId(Estado.class, rs.getInt("estado_id")),
                            responsable == null ? 0 : responsable.getId(),
                            responsable == null ? "" : responsable.getNombre()));
                }
            }
        }
        return lista;
    }

    private static void ponerEnteroONulo(PreparedStatement ps, int posicion, Integer valor) throws SQLException {
        if (valor == null) {
            ps.setNull(posicion, Types.INTEGER);
        } else {
            ps.setInt(posicion, valor);
        }
    }
}
