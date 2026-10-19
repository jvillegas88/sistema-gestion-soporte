package sgs.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import sgs.modelo.Integrante;
import sgs.modelo.catalogo.Catalogo;
import sgs.modelo.catalogo.Rol;

/** Lectura de los integrantes del equipo, para el inicio de sesión y los registros. */
public class IntegranteDAO {

    private static final String COLUMNAS = "SELECT integrante_id, rol_id, nombre, usuario, clave_hash, activo FROM integrantes";

    public List<Integrante> listar(Connection con) throws SQLException {
        List<Integrante> integrantes = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(COLUMNAS + " ORDER BY nombre");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                integrantes.add(armar(rs));
            }
        }
        return integrantes;
    }

    /** Busca por usuario para iniciar sesión. Devuelve null si no existe. */
    public Integrante buscarPorUsuario(Connection con, String usuario) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(COLUMNAS + " WHERE usuario = ?")) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? armar(rs) : null;
            }
        }
    }

    private static Integrante armar(ResultSet rs) throws SQLException {
        return new Integrante(rs.getInt("integrante_id"), Catalogo.porId(Rol.class, rs.getInt("rol_id")),
                rs.getString("nombre"), rs.getString("usuario"), rs.getString("clave_hash"), rs.getBoolean("activo"));
    }
}
