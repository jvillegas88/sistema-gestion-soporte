package sgs.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import sgs.modelo.HitoTecnico;
import sgs.modelo.Solicitud;
import sgs.modelo.catalogo.Catalogo;
import sgs.modelo.catalogo.TipoHito;

/** Guarda y lee los hitos técnicos. La columna fecha no tiene valor por defecto: se envía NOW(). */
public class HitoTecnicoDAO extends RegistroDAO<HitoTecnico> {

    public HitoTecnicoDAO(DatosMaestros maestros) {
        super(HitoTecnico.class, maestros);
    }

    @Override
    public int insertar(Connection con, HitoTecnico hito) throws SQLException {
        String sql = "INSERT INTO hitos_tecnicos (solicitud_id, tipo_hito_id, autor_id, fecha, referencia, "
                + "solicitud_deploy_id) VALUES (?, ?, ?, NOW(), ?, ?)";
        int id;
        try (PreparedStatement ps = prepararInsert(con, sql)) {
            ps.setInt(1, hito.getSolicitud().getId());
            ps.setInt(2, hito.getTipo().getId());
            ps.setInt(3, hito.getAutor().getId());
            ps.setString(4, hito.getReferencia());
            if (hito.getSolicitudDeployId() == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, hito.getSolicitudDeployId());
            }
            id = ejecutarInsert(ps);
        }
        hito.asignarIdentidad(id, leerFecha(con, "SELECT fecha FROM hitos_tecnicos WHERE hito_id = ?", id));
        return id;
    }

    @Override
    public List<HitoTecnico> listarPorSolicitud(Connection con, Solicitud solicitud) throws SQLException {
        List<HitoTecnico> lista = new ArrayList<>();
        String sql = "SELECT hito_id, tipo_hito_id, autor_id, fecha, referencia, solicitud_deploy_id FROM hitos_tecnicos "
                + "WHERE solicitud_id = ? ORDER BY fecha, hito_id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, solicitud.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new HitoTecnico(rs.getInt("hito_id"), solicitud, maestros.integrante(rs.getInt("autor_id")),
                            rs.getObject("fecha", LocalDateTime.class), Catalogo.porId(TipoHito.class, rs.getInt("tipo_hito_id")),
                            rs.getString("referencia"), enteroONulo(rs, "solicitud_deploy_id")));
                }
            }
        }
        return lista;
    }
}
