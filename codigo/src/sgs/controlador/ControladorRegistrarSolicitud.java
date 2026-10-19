package sgs.controlador;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import sgs.modelo.Cliente;
import sgs.modelo.Formatos;
import sgs.modelo.Integrante;
import sgs.modelo.Modulo;
import sgs.modelo.Producto;
import sgs.modelo.Solicitante;
import sgs.modelo.Solicitud;
import sgs.modelo.catalogo.Canal;
import sgs.modelo.catalogo.Prioridad;
import sgs.modelo.catalogo.TipoSolicitud;
import sgs.modelo.catalogo.TipoVinculo;
import sgs.modelo.excepcion.ReglaNegocioException;
import sgs.persistencia.DatosMaestros;
import sgs.persistencia.SolicitanteDAO;
import sgs.persistencia.SolicitudDAO;

/**
 * CU01 Registrar solicitud, con la clasificación opcional de CU03 (alternativa 13.A), el
 * solicitante nuevo (3.A), el vínculo con una solicitud cerrada (3.B) y el descarte (10.A).
 */
public class ControladorRegistrarSolicitud extends Controlador {

    private final SolicitudDAO solicitudDAO;
    private final SolicitanteDAO solicitanteDAO;

    public ControladorRegistrarSolicitud(Integrante usuario, DatosMaestros maestros, SolicitudDAO solicitudDAO) {
        super(usuario, maestros);
        this.solicitudDAO = solicitudDAO;
        this.solicitanteDAO = new SolicitanteDAO(maestros.getMapaClientes());
    }

    public List<Solicitante> getSolicitantes() {
        return maestros.getSolicitantes();
    }

    public List<Producto> getProductos() {
        return maestros.getProductos();
    }

    public List<Cliente> getClientes() {
        return maestros.getClientes();
    }

    /** Propone el cliente cuyo dominio coincide con el correo, o null si ninguno coincide. */
    public Cliente proponerCliente(String correo) {
        for (Cliente c : maestros.getClientes()) {
            if (c.correspondeAlCorreo(correo)) {
                return c;
            }
        }
        return null;
    }

    /** Registra un solicitante que todavía no existe (alternativa 3.A). */
    public Solicitante crearSolicitante(Cliente cliente, String nombre, String correo)
            throws SQLException, ReglaNegocioException {
        exigirPermiso(usuario.getRol().puedeRegistrarSolicitudes(), "registrar solicitantes");
        if (Formatos.estaVacio(nombre) || Formatos.estaVacio(correo) || cliente == null) {
            throw new ReglaNegocioException("Completá el nombre, el correo y el cliente del solicitante");
        }
        String correoLimpio = correo.trim().toLowerCase();
        if (!correoLimpio.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new ReglaNegocioException("El correo no tiene un formato válido");
        }
        Solicitante nuevo = new Solicitante(cliente, nombre.trim(), correoLimpio);
        enTransaccion(con -> {
            if (solicitanteDAO.existeCorreo(con, correoLimpio)) {
                throw new ReglaNegocioException("Ya hay un solicitante con el correo " + correoLimpio);
            }
            solicitanteDAO.insertar(con, nuevo);
        });
        maestros.agregarSolicitante(nuevo);
        return nuevo;
    }

    /** Busca la solicitud cerrada de la que sale un tema nuevo (alternativa 3.B). */
    public Solicitud buscarSolicitudOrigen(int numero) throws SQLException, ReglaNegocioException {
        Solicitud origen = consultar(con -> solicitudDAO.buscarPorId(con, numero));
        if (origen == null) {
            throw new ReglaNegocioException("No existe la solicitud " + numero);
        }
        if (origen.estaAbierta()) {
            throw new ReglaNegocioException("La solicitud " + numero + " sigue abierta: el tema se trata en ella");
        }
        return origen;
    }

    /**
     * Registra la solicitud en estado Nueva y, si se completó, la clasifica (pasos 3 a 13).
     *
     * @param origen la solicitud cerrada de la que sale el tema, o null
     * @return la solicitud registrada, con el número que le asignó la base
     */
    public Solicitud registrar(Solicitante solicitante, Producto producto, Canal canal, LocalDateTime fechaRecepcion,
            String asunto, String descripcion, TipoSolicitud tipo, Prioridad prioridad, Modulo modulo,
            Solicitud origen) throws SQLException, ReglaNegocioException {
        Solicitud nueva = armar(solicitante, producto, canal, fechaRecepcion, asunto, descripcion, origen);
        if (tipo != null || prioridad != null) {
            if (tipo == null || prioridad == null) {
                throw new ReglaNegocioException("Para clasificar al registrar, seleccioná el tipo y la prioridad");
            }
            nueva.clasificar(tipo, prioridad, usuario, null);
        }
        if (modulo != null) {
            nueva.asignarModulo(modulo);
        }
        enTransaccion(con -> solicitudDAO.guardar(con, nueva));
        return nueva;
    }

    /** Registra la solicitud como Descartada, con su motivo, porque no corresponde a soporte (10.A). */
    public Solicitud registrarDescartada(Solicitante solicitante, Producto producto, Canal canal,
            LocalDateTime fechaRecepcion, String asunto, String descripcion, String motivo)
            throws SQLException, ReglaNegocioException {
        Solicitud nueva = armar(solicitante, producto, canal, fechaRecepcion, asunto, descripcion, null);
        nueva.descartar(usuario, motivo);
        enTransaccion(con -> solicitudDAO.guardar(con, nueva));
        return nueva;
    }

    private Solicitud armar(Solicitante solicitante, Producto producto, Canal canal, LocalDateTime fechaRecepcion,
            String asunto, String descripcion, Solicitud origen) throws ReglaNegocioException {
        exigirPermiso(usuario.getRol().puedeRegistrarSolicitudes(), "registrar solicitudes");
        String descripcionLimpia = Formatos.estaVacio(descripcion) ? null : descripcion.trim();
        Solicitud nueva = new Solicitud(solicitante, producto, canal, fechaRecepcion,
                asunto == null ? null : asunto.trim(), descripcionLimpia);
        nueva.registrar(usuario);
        if (origen != null) {
            nueva.vincular(origen, TipoVinculo.CONTINUACION);
        }
        return nueva;
    }
}
