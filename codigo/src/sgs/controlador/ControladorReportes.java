package sgs.controlador;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import sgs.modelo.Integrante;
import sgs.modelo.Producto;
import sgs.modelo.consulta.FilaCargaIntegrante;
import sgs.modelo.consulta.FilaDemorada;
import sgs.modelo.consulta.FilaEsfuerzoCliente;
import sgs.modelo.consulta.FilaPorTipo;
import sgs.modelo.excepcion.ReglaNegocioException;
import sgs.persistencia.DatosMaestros;
import sgs.persistencia.ReporteDAO;

/** CU11 y CU12: los reportes de gestión. Solo los puede consultar el rol de gestión (RNF05, RNF06). */
public class ControladorReportes extends Controlador {

    private final ReporteDAO reporteDAO;

    public ControladorReportes(Integrante usuario, DatosMaestros maestros) {
        super(usuario, maestros);
        this.reporteDAO = new ReporteDAO(maestros);
    }

    public List<Producto> getProductos() {
        return maestros.getProductos();
    }

    /** CU11, paso 4 y alternativa 3.A. Los parámetros de comparación son null si no se compara. */
    public List<FilaEsfuerzoCliente> esfuerzoPorCliente(LocalDate desde, LocalDate hasta, Producto producto,
            LocalDate compDesde, LocalDate compHasta) throws SQLException, ReglaNegocioException {
        exigirAcceso();
        validarPeriodo(desde, hasta);
        if (compDesde != null || compHasta != null) {
            validarPeriodo(compDesde, compHasta);
        }
        return consultar(con -> reporteDAO.esfuerzoPorCliente(con, desde, hasta, producto, compDesde, compHasta));
    }

    public List<FilaPorTipo> solicitudesPorTipo(LocalDate desde, LocalDate hasta, Producto producto)
            throws SQLException, ReglaNegocioException {
        exigirAcceso();
        validarPeriodo(desde, hasta);
        return consultar(con -> reporteDAO.solicitudesPorTipo(con, desde, hasta, producto));
    }

    /** CU12, pasos 2 a 5. */
    public List<FilaCargaIntegrante> cargaPorIntegrante(LocalDate desde, LocalDate hasta)
            throws SQLException, ReglaNegocioException {
        exigirAcceso();
        validarPeriodo(desde, hasta);
        return consultar(con -> reporteDAO.cargaPorIntegrante(con, desde, hasta));
    }

    public List<FilaPorTipo> horasPorTipo(LocalDate desde, LocalDate hasta) throws SQLException, ReglaNegocioException {
        exigirAcceso();
        validarPeriodo(desde, hasta);
        return consultar(con -> reporteDAO.horasPorTipo(con, desde, hasta));
    }

    /** CU12, alternativa 6.A. */
    public List<FilaDemorada> solicitudesDemoradas(int diasUmbral) throws SQLException, ReglaNegocioException {
        exigirAcceso();
        if (diasUmbral < 1) {
            throw new ReglaNegocioException("La cantidad de días tiene que ser al menos 1");
        }
        return consultar(con -> reporteDAO.solicitudesDemoradas(con, diasUmbral));
    }

    /** CU12, alternativa 2.A: el reporte está restringido al rol de gestión. */
    private void exigirAcceso() throws ReglaNegocioException {
        if (!usuario.getRol().puedeVerReportes()) {
            throw new ReglaNegocioException("Los reportes están restringidos al rol de gestión");
        }
    }

    private static void validarPeriodo(LocalDate desde, LocalDate hasta) throws ReglaNegocioException {
        if (desde == null || hasta == null) {
            throw new ReglaNegocioException("Ingresá las dos fechas del período");
        }
        if (desde.isAfter(hasta)) {
            throw new ReglaNegocioException("El período empieza después de terminar: revisá las fechas");
        }
    }
}
