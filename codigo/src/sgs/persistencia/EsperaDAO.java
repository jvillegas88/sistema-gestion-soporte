package sgs.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import sgs.modelo.Espera;
import sgs.modelo.Solicitud;
import sgs.modelo.catalogo.Catalogo;
import sgs.modelo.catalogo.MotivoEspera;

/**
 * Guarda y lee las esperas. Es el único registro que se modifica: se inserta abierta y,
 * cuando termina, se le completa el fin. El inicio y el fin los pone el reloj de la base (RNF07).
 */
public class EsperaDAO extends RegistroDAO<Espera> {

    public EsperaDAO(DatosMaestros maestros) {
        super(Espera.class, maestros);
    }

    @Override
    public int insertar(Connection con, Espera espera) throws SQLException {
        String sql = "INSERT INTO esperas (solicitud_id, motivo_espera_id, autor_id, desde, hasta) "
                + "VALUES (?, ?, ?, NOW(), NULL)";
        int id;
        try (PreparedStatement ps = prepararInsert(con, sql)) {
            ps.setInt(1, espera.getSolicitud().getId());
            ps.setInt(2, espera.getMotivo().getId());
            ps.setInt(3, espera.getAutor().getId());
            id = ejecutarInsert(ps);
        }
        espera.asignarIdentidad(id, leerFecha(con, "SELECT desde FROM esperas WHERE espera_id = ?", id));
        return id;
    }

    /** Completa el fin de una espera que estaba abierta, con la hora de la base. */
    @Override
    protected void actualizar(Connection con, Espera espera) throws SQLException {
        String sql = "UPDATE esperas SET hasta = NOW() WHERE espera_id = ? AND hasta IS NULL";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, espera.getId());
            if (ps.executeUpdate() != 1) {
                throw new SQLException("La espera " + espera.getId() + " ya estaba cerrada");
            }
        }
        espera.cerrar(leerFecha(con, "SELECT hasta FROM esperas WHERE espera_id = ?", espera.getId()));
    }

    @Override
    public List<Espera> listarPorSolicitud(Connection con, Solicitud solicitud) throws SQLException {
        List<Espera> lista = new ArrayList<>();
        String sql = "SELECT espera_id, motivo_espera_id, autor_id, desde, hasta FROM esperas "
                + "WHERE solicitud_id = ? ORDER BY desde, espera_id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, solicitud.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Espera(rs.getInt("espera_id"), solicitud, maestros.integrante(rs.getInt("autor_id")),
                            rs.getObject("desde", LocalDateTime.class),
                            Catalogo.porId(MotivoEspera.class, rs.getInt("motivo_espera_id")),
                            rs.getObject("hasta", LocalDateTime.class)));
                }
            }
        }
        return lista;
    }
}
