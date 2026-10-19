package sgs.persistencia;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import sgs.modelo.Cliente;
import sgs.modelo.Integrante;
import sgs.modelo.Ordenamiento;
import sgs.modelo.Producto;
import sgs.modelo.Solicitante;

/**
 * Clientes, productos con sus módulos, integrantes y solicitantes, leídos una vez al iniciar.
 * Cambian poco y se necesitan en cada pantalla, así que se guardan en memoria. Los DAO de las
 * solicitudes los usan para pasar de una clave foránea al objeto que corresponde.
 */
public class DatosMaestros {

    private final Map<Integer, Cliente> clientes = new LinkedHashMap<>();
    private final Map<Integer, Producto> productos = new LinkedHashMap<>();
    private final Map<Integer, Integrante> integrantes = new LinkedHashMap<>();
    private final Map<Integer, Solicitante> solicitantes = new LinkedHashMap<>();

    private DatosMaestros() {
        // Se crea solo con cargar(), que lo completa con lo que hay en la base
    }

    /** Lee todos los datos maestros con una conexión abierta. */
    public static DatosMaestros cargar(Connection con) throws SQLException {
        DatosMaestros datos = new DatosMaestros();
        for (Cliente c : new ClienteDAO().listar(con)) {
            datos.clientes.put(c.getId(), c);
        }
        for (Producto p : new ProductoDAO().listar(con)) {
            datos.productos.put(p.getId(), p);
        }
        for (Integrante i : new IntegranteDAO().listar(con)) {
            datos.integrantes.put(i.getId(), i);
        }
        for (Solicitante s : new SolicitanteDAO(datos.clientes).listar(con)) {
            datos.solicitantes.put(s.getId(), s);
        }
        return datos;
    }

    public Map<Integer, Cliente> getMapaClientes() {
        return clientes;
    }

    public List<Cliente> getClientes() {
        return new ArrayList<>(clientes.values());
    }

    public List<Producto> getProductos() {
        return new ArrayList<>(productos.values());
    }

    public List<Integrante> getIntegrantes() {
        return new ArrayList<>(integrantes.values());
    }

    /** Solicitantes ordenados por nombre, incluidos los que se agregaron durante la sesión. */
    public List<Solicitante> getSolicitantes() {
        List<Solicitante> lista = new ArrayList<>(solicitantes.values());
        Ordenamiento.porInsercion(lista, Comparator.comparing(Solicitante::getNombre, String.CASE_INSENSITIVE_ORDER));
        return lista;
    }

    public Cliente cliente(int id) {
        return clientes.get(id);
    }

    public Producto producto(int id) {
        return productos.get(id);
    }

    public Integrante integrante(int id) {
        return integrantes.get(id);
    }

    public Solicitante solicitante(int id) {
        return solicitantes.get(id);
    }

    /** Suma un solicitante creado durante la sesión (CU01, alternativa 3.A). */
    public void agregarSolicitante(Solicitante solicitante) {
        solicitantes.put(solicitante.getId(), solicitante);
    }
}
