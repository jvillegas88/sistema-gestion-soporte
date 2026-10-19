package sgs.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import sgs.modelo.Asignacion;
import sgs.modelo.Solicitud;

/** Guarda y lee las asignaciones. En la tabla, el autor es asignado_por_id. */
public class AsignacionDAO extends RegistroDAO<Asignacion> {

    public AsignacionDAO(DatosMaestros maestros) {
        super(Asignacion.class, maestros);
    }

    @Override
    public int insertar(Connection con, Asignacion asignacion) throws SQLException {
        String sql = "INSERT INTO asignaciones (solicitud_id, integrante_id, asignado_por_id) VALUES (?, ?, ?)";
        int id;
        try (PreparedStatement ps = prepararInsert(con, sql)) {
            ps.setInt(1, asignacion.getSolicitud().getId());
            ps.setInt(2, asignacion.getAsignado().getId());
            ps.setInt(3, asignacion.getAutor().getId());
            id = ejecutarInsert(ps);
        }
        asignacion.asignarIdentidad(id, leerFecha(con, "SELECT fecha FROM asignaciones WHERE asignacion_id = ?", id));
        return id;
    }

    @Override
    public List<Asignacion> listarPorSolicitud(Connection con, Solicitud solicitud) throws SQLException {
        List<Asignacion> lista = new ArrayList<>();
        String sql = "SELECT asignacion_id, integrante_id, asignado_por_id, fecha FROM asignaciones "
                + "WHERE solicitud_id = ? ORDER BY fecha, asignacion_id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, solicitud.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Asignacion(rs.getInt("asignacion_id"), solicitud,
                            maestros.integrante(rs.getInt("asignado_por_id")), rs.getObject("fecha", LocalDateTime.class),
                            maestros.integrante(rs.getInt("integrante_id"))));
                }
            }
        }
        return lista;
    }
}
