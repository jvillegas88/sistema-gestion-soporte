package sgs.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import sgs.modelo.Cliente;
import sgs.modelo.Solicitante;

/** Acceso a los solicitantes. El alta se usa en CU01, alternativa 3.A. */
public class SolicitanteDAO implements DAO<Solicitante> {

    private static final String COLUMNAS = "SELECT solicitante_id, cliente_id, nombre, correo FROM solicitantes";

    private final Map<Integer, Cliente> clientes;

    /** Recibe los clientes ya leídos, para armar cada solicitante con el suyo. */
    public SolicitanteDAO(Map<Integer, Cliente> clientes) {
        this.clientes = clientes;
    }

    @Override
    public int insertar(Connection con, Solicitante solicitante) throws SQLException {
        String sql = "INSERT INTO solicitantes (cliente_id, nombre, correo) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, solicitante.getCliente().getId());
            ps.setString(2, solicitante.getNombre());
            ps.setString(3, solicitante.getCorreo());
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                claves.next();
                solicitante.setId(claves.getInt(1));
            }
        }
        return solicitante.getId();
    }

    @Override
    public Solicitante buscarPorId(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(COLUMNAS + " WHERE solicitante_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? armar(rs) : null;
            }
        }
    }

    public List<Solicitante> listar(Connection con) throws SQLException {
        List<Solicitante> solicitantes = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(COLUMNAS + " ORDER BY nombre");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                solicitantes.add(armar(rs));
            }
        }
        return solicitantes;
    }

    /** True si ya hay un solicitante con ese correo: la columna es única. */
    public boolean existeCorreo(Connection con, String correo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM solicitantes WHERE correo = ?")) {
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Solicitante armar(ResultSet rs) throws SQLException {
        return new Solicitante(rs.getInt("solicitante_id"), clientes.get(rs.getInt("cliente_id")),
                rs.getString("nombre"), rs.getString("correo"));
    }
}
