package sgs.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sgs.modelo.Comunicacion;
import sgs.modelo.Estimacion;
import sgs.modelo.Intervencion;
import sgs.modelo.ItemEstimacion;
import sgs.modelo.Solicitud;
import sgs.modelo.catalogo.Catalogo;
import sgs.modelo.catalogo.TipoComunicacion;
import sgs.modelo.catalogo.TipoIntervencion;

/** Guarda y lee las intervenciones con sus comunicaciones y los ítems de su estimación. */
public class IntervencionDAO extends RegistroDAO<Intervencion> {

    public IntervencionDAO(DatosMaestros maestros) {
        super(Intervencion.class, maestros);
    }

    /**
     * Inserta la intervención sin la fecha, que la completa la base con su valor por defecto,
     * así todas las estaciones usan el mismo reloj. Después inserta sus comunicaciones y sus ítems.
     */
    @Override
    public int insertar(Connection con, Intervencion intervencion) throws SQLException {
        String sql = "INSERT INTO intervenciones (solicitud_id, integrante_id, tipo_intervencion_id, descripcion, "
                + "tiempo_horas) VALUES (?, ?, ?, ?, ?)";
        int id;
        try (PreparedStatement ps = prepararInsert(con, sql)) {
            ps.setInt(1, intervencion.getSolicitud().getId());
            ps.setInt(2, intervencion.getAutor().getId());
            ps.setInt(3, intervencion.getTipo().getId());
            ps.setString(4, intervencion.getDescripcion());
            ps.setBigDecimal(5, intervencion.getTiempoHoras());
            id = ejecutarInsert(ps);
        }
        intervencion.asignarIdentidad(id, leerFecha(con, "SELECT fecha FROM intervenciones WHERE intervencion_id = ?", id));
        insertarComunicaciones(con, intervencion);
        insertarItems(con, intervencion);
        return id;
    }

    private void insertarComunicaciones(Connection con, Intervencion intervencion) throws SQLException {
        String sql = "INSERT INTO comunicaciones (intervencion_id, tipo, texto) VALUES (?, ?, ?)";
        for (Comunicacion c : intervencion.getComunicaciones()) {
            try (PreparedStatement ps = prepararInsert(con, sql)) {
                ps.setInt(1, intervencion.getId());
                ps.setString(2, c.getTipo().name());
                ps.setString(3, c.getTexto());
                c.setId(ejecutarInsert(ps));
            }
        }
    }

    private void insertarItems(Connection con, Intervencion intervencion) throws SQLException {
        Estimacion estimacion = intervencion.getEstimacion();
        if (estimacion == null) {
            return;
        }
        String sql = "INSERT INTO items_estimacion (intervencion_id, item_nro, descripcion, horas) VALUES (?, ?, ?, ?)";
        for (ItemEstimacion item : estimacion.getItems()) {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, intervencion.getId());
                ps.setInt(2, item.getNumero());
                ps.setString(3, item.getDescripcion());
                ps.setBigDecimal(4, item.getHoras());
                ps.executeUpdate();
            }
        }
    }

    /**
     * Lee las intervenciones de la solicitud y después, con una consulta cada una, todas sus
     * comunicaciones y todos sus ítems, que se reparten con un mapa por número de intervención.
     */
    @Override
    public List<Intervencion> listarPorSolicitud(Connection con, Solicitud solicitud) throws SQLException {
        List<Intervencion> lista = new ArrayList<>();
        Map<Integer, Intervencion> porId = new HashMap<>();
        String sql = "SELECT intervencion_id, integrante_id, tipo_intervencion_id, fecha, descripcion, tiempo_horas "
                + "FROM intervenciones WHERE solicitud_id = ? ORDER BY fecha, intervencion_id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, solicitud.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Intervencion i = new Intervencion(rs.getInt("intervencion_id"), solicitud,
                            maestros.integrante(rs.getInt("integrante_id")), rs.getObject("fecha", LocalDateTime.class),
                            Catalogo.porId(TipoIntervencion.class, rs.getInt("tipo_intervencion_id")),
                            rs.getString("descripcion"), rs.getBigDecimal("tiempo_horas"));
                    lista.add(i);
                    porId.put(i.getId(), i);
                }
            }
        }
        cargarComunicaciones(con, solicitud, porId);
        cargarItems(con, solicitud, porId);
        return lista;
    }

    private void cargarComunicaciones(Connection con, Solicitud solicitud, Map<Integer, Intervencion> porId)
            throws SQLException {
        String sql = "SELECT c.comunicacion_id, c.intervencion_id, c.tipo, c.texto FROM comunicaciones c "
                + "JOIN intervenciones i ON i.intervencion_id = c.intervencion_id "
                + "WHERE i.solicitud_id = ? ORDER BY c.comunicacion_id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, solicitud.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    porId.get(rs.getInt("intervencion_id")).agregar(new Comunicacion(rs.getInt("comunicacion_id"),
                            TipoComunicacion.valueOf(rs.getString("tipo")), rs.getString("texto")));
                }
            }
        }
    }

    private void cargarItems(Connection con, Solicitud solicitud, Map<Integer, Intervencion> porId)
            throws SQLException {
        String sql = "SELECT e.intervencion_id, e.item_nro, e.descripcion, e.horas FROM items_estimacion e "
                + "JOIN intervenciones i ON i.intervencion_id = e.intervencion_id "
                + "WHERE i.solicitud_id = ? ORDER BY e.intervencion_id, e.item_nro";
        Map<Integer, List<ItemEstimacion>> itemsPorIntervencion = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, solicitud.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    itemsPorIntervencion.computeIfAbsent(rs.getInt("intervencion_id"), id -> new ArrayList<>())
                            .add(new ItemEstimacion(rs.getInt("item_nro"), rs.getString("descripcion"),
                                    rs.getBigDecimal("horas")));
                }
            }
        }
        for (Map.Entry<Integer, List<ItemEstimacion>> e : itemsPorIntervencion.entrySet()) {
            porId.get(e.getKey()).setEstimacion(new Estimacion(e.getValue()));
        }
    }
}
