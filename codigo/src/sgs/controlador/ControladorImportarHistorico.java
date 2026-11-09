package sgs.controlador;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import sgs.modelo.Cliente;
import sgs.modelo.FilaHistorico;
import sgs.modelo.Importacion;
import sgs.modelo.Integrante;
import sgs.modelo.Producto;
import sgs.modelo.Solicitante;
import sgs.modelo.Solicitud;
import sgs.modelo.catalogo.Estado;
import sgs.modelo.excepcion.ReglaNegocioException;
import sgs.persistencia.ArchivoCSV;
import sgs.persistencia.DatosMaestros;
import sgs.persistencia.SolicitanteDAO;
import sgs.persistencia.SolicitudDAO;

/**
 * CU02 Importar histórico de solicitudes (RF02, RNF09). Lee el CSV que exporta la herramienta
 * actual, informa qué se va a importar y, si el actor confirma, registra todo en una sola
 * transacción: o entran todas las solicitudes que aceptó la revisión o no entra ninguna.
 */
public class ControladorImportarHistorico extends Controlador {

    /** La herramienta actual separa los campos con coma. */
    private static final char SEPARADOR = ',';

    private final SolicitudDAO solicitudDAO;
    private final SolicitanteDAO solicitanteDAO;

    public ControladorImportarHistorico(Integrante usuario, DatosMaestros maestros, SolicitudDAO solicitudDAO) {
        super(usuario, maestros);
        this.solicitudDAO = solicitudDAO;
        this.solicitanteDAO = new SolicitanteDAO(maestros.getMapaClientes());
    }

    /**
     * Pasos 3 a 5: lee el archivo, verifica el formato y arma la importación con lo que se va a
     * registrar, lo que se omite por estar ya importado y lo que se rechaza. No guarda nada.
     *
     * @throws ReglaNegocioException si el archivo no tiene el formato esperado (alternativa 4.A)
     * @throws IOException           si el archivo no se puede leer
     */
    public Importacion revisar(Path archivo) throws IOException, SQLException, ReglaNegocioException {
        exigirPermiso(usuario.getRol().puedeImportarHistorico(), "importar el histórico");
        List<String[]> registros;
        try {
            registros = ArchivoCSV.leer(archivo, SEPARADOR);
        } catch (CharacterCodingException e) {
            throw new ReglaNegocioException("El archivo no tiene el formato esperado: no está codificado en UTF-8");
        } catch (ArchivoCSV.FormatoInvalidoException e) {
            throw new ReglaNegocioException("El archivo no tiene el formato esperado: " + e.getMessage()
                    + ". No se importa ningún registro");
        }
        if (registros.isEmpty()) {
            throw new ReglaNegocioException("El archivo está vacío");
        }
        String[] encabezado = registros.get(0);
        int[] posiciones = FilaHistorico.posiciones(encabezado);
        Cliente noIdentificado = clienteNoIdentificado();
        Set<Integer> importadas = consultar(solicitudDAO::numerosOrigenImportados);
        Map<String, Solicitante> solicitantes = solicitantesPorCorreo();

        Importacion importacion = new Importacion(archivo.getFileName().toString());
        for (int i = 1; i < registros.size(); i++) {
            String[] campos = registros.get(i);
            FilaHistorico fila = new FilaHistorico(i, campos, posiciones);
            importacion.contarRegistro();
            try {
                // Un registro con otra cantidad de campos está corrido: sus datos no son confiables
                if (campos.length != encabezado.length) {
                    throw new ReglaNegocioException("tiene " + campos.length + " campos y el encabezado tiene "
                            + encabezado.length);
                }
                int numero = fila.numeroOrigen();
                if (importadas.contains(numero)) {
                    importacion.omitir();
                    continue;
                }
                Estado cierre = fila.estadoDeCierre();
                Producto producto = buscarProducto(fila.nombreProducto());
                String correo = fila.correo();
                Solicitante solicitante = solicitantes.get(correo);
                boolean solicitanteNuevo = solicitante == null;
                if (solicitanteNuevo) {
                    solicitante = nuevoSolicitante(fila, correo, noIdentificado);
                }
                Solicitud solicitud = Solicitud.importada(numero, solicitante, producto, fila.fechaRecepcion(),
                        fila.asunto(), fila.tiempoHistorico());
                solicitud.importar(cierre, usuario);
                // Recién cuando el registro está completo se toma en cuenta: así un rechazo no
                // deja un solicitante suelto ni marca el número como importado
                if (solicitanteNuevo) {
                    solicitantes.put(correo, solicitante);
                    importacion.agregarSolicitante(solicitante);
                }
                importadas.add(numero);
                importacion.agregar(solicitud);
            } catch (ReglaNegocioException e) {
                importacion.rechazar(fila, e.getMessage());
            }
        }
        return importacion;
    }

    /**
     * Pasos 7 y 8: registra los solicitantes nuevos y las solicitudes en una sola transacción.
     * Si algo falla se deshace todo, y la importación ya no sirve: hay que revisar el archivo
     * de nuevo, porque los datos que se habían preparado se consumieron.
     */
    public void importar(Importacion importacion) throws SQLException, ReglaNegocioException {
        exigirPermiso(usuario.getRol().puedeImportarHistorico(), "importar el histórico");
        if (importacion.estaRegistrada()) {
            throw new ReglaNegocioException("Esta importación ya se registró");
        }
        if (importacion.getSolicitudes().isEmpty()) {
            throw new ReglaNegocioException("El archivo no tiene solicitudes para importar");
        }
        enTransaccion(con -> {
            for (Solicitante solicitante : importacion.getSolicitantesNuevos()) {
                solicitanteDAO.insertar(con, solicitante);
            }
            for (Solicitud solicitud : importacion.getSolicitudes()) {
                solicitudDAO.guardar(con, solicitud);
            }
        });
        for (Solicitante solicitante : importacion.getSolicitantesNuevos()) {
            maestros.agregarSolicitante(solicitante);
        }
        importacion.marcarRegistrada();
    }

    /** Los solicitantes se leen de la base, no de la sesión, por si otro integrante agregó alguno. */
    private Map<String, Solicitante> solicitantesPorCorreo() throws SQLException {
        Map<String, Solicitante> porCorreo = new HashMap<>();
        for (Solicitante s : consultar(solicitanteDAO::listar)) {
            porCorreo.put(s.getCorreo().toLowerCase(), s);
        }
        return porCorreo;
    }

    /** El cliente sale del dominio del correo, como en CU01. Si ninguno coincide, va al no identificado (8.A). */
    private Solicitante nuevoSolicitante(FilaHistorico fila, String correo, Cliente noIdentificado)
            throws ReglaNegocioException {
        String nombre = fila.nombreSolicitante();
        if (nombre.isEmpty()) {
            throw new ReglaNegocioException("no tiene el nombre del solicitante");
        }
        Cliente cliente = noIdentificado;
        for (Cliente c : maestros.getClientes()) {
            if (c.correspondeAlCorreo(correo)) {
                cliente = c;
                break;
            }
        }
        Solicitante nuevo = new Solicitante(cliente, nombre, correo);
        nuevo.validar();
        return nuevo;
    }

    /** El producto es obligatorio (RNF08): una solicitud sin producto se rechaza. */
    private Producto buscarProducto(String nombre) throws ReglaNegocioException {
        if (nombre.isEmpty()) {
            throw new ReglaNegocioException("no tiene producto, que es obligatorio (RNF08)");
        }
        for (Producto p : maestros.getProductos()) {
            if (p.getNombre().equalsIgnoreCase(nombre)) {
                return p;
            }
        }
        throw new ReglaNegocioException("el producto \"" + nombre + "\" no existe en el sistema");
    }

    private Cliente clienteNoIdentificado() throws ReglaNegocioException {
        for (Cliente c : maestros.getClientes()) {
            if (c.esNoIdentificado()) {
                return c;
            }
        }
        throw new ReglaNegocioException("Falta el \"" + Cliente.NO_IDENTIFICADO
                + "\" en la base, y hace falta para importar las solicitudes sin cliente");
    }
}
