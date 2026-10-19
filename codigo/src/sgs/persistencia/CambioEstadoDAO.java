package sgs.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import sgs.modelo.CambioEstado;
import sgs.modelo.Solicitud;
import sgs.modelo.catalogo.Catalogo;
import sgs.modelo.catalogo.Estado;

/** Guarda y lee los cambios de estado. El último de la lista es el estado actual. */
public class CambioEstadoDAO extends RegistroDAO<CambioEstado> {

    public CambioEstadoDAO(DatosMaestros maestros) {
        super(CambioEstado.class, maestros);
    }

    @Override
    public int insertar(Connection con, CambioEstado cambio) throws SQLException {
        String sql = "INSERT INTO cambios_estado (solicitud_id, estado_id, autor_id, motivo) VALUES (?, ?, ?, ?)";
        int id;
        try (PreparedStatement ps = prepararInsert(con, sql)) {
            ps.setInt(1, cambio.getSolicitud().getId());
            ps.setInt(2, cambio.getEstado().getId());
            ps.setInt(3, cambio.getAutor().getId());
            ps.setString(4, cambio.getMotivo());
            id = ejecutarInsert(ps);
        }
        cambio.asignarIdentidad(id, leerFecha(con, "SELECT fecha FROM cambios_estado WHERE cambio_estado_id = ?", id));
        return id;
    }

    /** Ordenados por fecha y, si coinciden en el mismo segundo, por el orden en que se guardaron. */
    @Override
    public List<CambioEstado> listarPorSolicitud(Connection con, Solicitud solicitud) throws SQLException {
        List<CambioEstado> lista = new ArrayList<>();
        String sql = "SELECT cambio_estado_id, estado_id, autor_id, fecha, motivo FROM cambios_estado "
                + "WHERE solicitud_id = ? ORDER BY fecha, cambio_estado_id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, solicitud.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new CambioEstado(rs.getInt("cambio_estado_id"), solicitud,
                            maestros.integrante(rs.getInt("autor_id")), rs.getObject("fecha", LocalDateTime.class),
                            Catalogo.porId(Estado.class, rs.getInt("estado_id")), rs.getString("motivo")));
                }
            }
        }
        return lista;
    }
}
