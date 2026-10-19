package sgs.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import sgs.modelo.Producto;

/** Lectura de los productos con sus módulos. */
public class ProductoDAO {

    public List<Producto> listar(Connection con) throws SQLException {
        List<Producto> productos = new ArrayList<>();
        String sql = "SELECT producto_id, nombre, correo_soporte FROM productos ORDER BY nombre";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                productos.add(new Producto(rs.getInt("producto_id"), rs.getString("nombre"), rs.getString("correo_soporte")));
            }
        }
        String sqlModulos = "SELECT producto_id, modulo_nro, nombre FROM modulos ORDER BY producto_id, modulo_nro";
        try (PreparedStatement ps = con.prepareStatement(sqlModulos); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Producto producto = buscar(productos, rs.getInt("producto_id"));
                producto.agregarModulo(rs.getInt("modulo_nro"), rs.getString("nombre"));
            }
        }
        return productos;
    }

    /** Búsqueda secuencial del producto de un módulo: son pocos productos. */
    private static Producto buscar(List<Producto> productos, int id) {
        for (Producto p : productos) {
            if (p.getId() == id) {
                return p;
            }
        }
        throw new IllegalStateException("Hay un módulo de un producto inexistente: " + id);
    }
}
