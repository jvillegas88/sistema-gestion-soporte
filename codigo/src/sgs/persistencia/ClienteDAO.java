package sgs.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import sgs.modelo.Cliente;

/** Lectura de los clientes. Su administración (RF25) queda fuera del prototipo. */
public class ClienteDAO {

    public List<Cliente> listar(Connection con) throws SQLException {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT cliente_id, nombre, dominio_correo FROM clientes ORDER BY nombre";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clientes.add(new Cliente(rs.getInt("cliente_id"), rs.getString("nombre"), rs.getString("dominio_correo")));
            }
        }
        return clientes;
    }
}
