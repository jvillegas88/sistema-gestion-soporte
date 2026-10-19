package sgs.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import sgs.modelo.Reclasificacion;
import sgs.modelo.Solicitud;
import sgs.modelo.catalogo.Catalogo;
import sgs.modelo.catalogo.Prioridad;
import sgs.modelo.catalogo.TipoSolicitud;

/** Guarda y lee las reclasificaciones, con la clasificación anterior. */
public class ReclasificacionDAO extends RegistroDAO<Reclasificacion> {

    public ReclasificacionDAO(DatosMaestros maestros) {
        super(Reclasificacion.class, maestros);
    }

    @Override
    public int insertar(Connection con, Reclasificacion r) throws SQLException {
        String sql = "INSERT INTO reclasificaciones (solicitud_id, autor_id, motivo, tipo_anterior_id, "
                + "prioridad_anterior_id) VALUES (?, ?, ?, ?, ?)";
        int id;
        try (PreparedStatement ps = prepararInsert(con, sql)) {
            ps.setInt(1, r.getSolicitud().getId());
            ps.setInt(2, r.getAutor().getId());
            ps.setString(3, r.getMotivo());
            if (r.getTipoAnterior() == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, r.getTipoAnterior().getId());
            }
            if (r.getPrioridadAnterior() == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, r.getPrioridadAnterior().getId());
            }
            id = ejecutarInsert(ps);
        }
        r.asignarIdentidad(id, leerFecha(con, "SELECT fecha FROM reclasificaciones WHERE reclasificacion_id = ?", id));
        return id;
    }

    @Override
    public List<Reclasificacion> listarPorSolicitud(Connection con, Solicitud solicitud) throws SQLException {
        List<Reclasificacion> lista = new ArrayList<>();
        String sql = "SELECT reclasificacion_id, autor_id, fecha, motivo, tipo_anterior_id, prioridad_anterior_id "
                + "FROM reclasificaciones WHERE solicitud_id = ? ORDER BY fecha, reclasificacion_id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, solicitud.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer tipoAnterior = enteroONulo(rs, "tipo_anterior_id");
                    Integer prioridadAnterior = enteroONulo(rs, "prioridad_anterior_id");
                    lista.add(new Reclasificacion(rs.getInt("reclasificacion_id"), solicitud,
                            maestros.integrante(rs.getInt("autor_id")), rs.getObject("fecha", LocalDateTime.class),
                            rs.getString("motivo"),
                            tipoAnterior == null ? null : Catalogo.porId(TipoSolicitud.class, tipoAnterior),
                            prioridadAnterior == null ? null : Catalogo.porId(Prioridad.class, prioridadAnterior)));
                }
            }
        }
        return lista;
    }
}
