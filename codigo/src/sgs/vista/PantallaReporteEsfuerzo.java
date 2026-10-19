package sgs.vista;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import sgs.controlador.ControladorReportes;
import sgs.controlador.Sesion;
import sgs.modelo.Formatos;
import sgs.modelo.Producto;
import sgs.modelo.consulta.FilaEsfuerzoCliente;
import sgs.modelo.consulta.FilaPorTipo;

/**
 * CU11 Consultar reporte de esfuerzo por cliente, según el prototipo de la sección 13. Muestra
 * por cliente las solicitudes, las horas trabajadas y los días en espera del período, con la
 * comparación opcional contra otro período (alternativa 3.A) y la distribución por tipo.
 */
public final class PantallaReporteEsfuerzo extends Pantalla {

    private static final long serialVersionUID = 1L;
    private static final String TODOS = "Todos";

    private final transient ControladorReportes controlador;
    private final JTextField txtDesde = new JTextField(inicioDeTrimestre(1).format(Formatos.FECHA), 8);
    private final JTextField txtHasta = new JTextField(inicioDeTrimestre(0).minusDays(1).format(Formatos.FECHA), 8);
    private final JComboBox<Object> cmbProducto = new JComboBox<>();
    private final JCheckBox chkComparar = new JCheckBox("Comparar con");
    private final JTextField txtCompDesde = new JTextField(inicioDeTrimestre(2).format(Formatos.FECHA), 8);
    private final JTextField txtCompHasta = new JTextField(inicioDeTrimestre(1).minusDays(1).format(Formatos.FECHA), 8);
    private final DefaultTableModel modeloEsfuerzo = Estilo.modeloTabla("Cliente", "Solicitudes",
            Estilo.dosLineas("Horas", "trabajadas"), Estilo.dosLineas("Tiempo en", "espera (días)"),
            Estilo.dosLineas("Horas trabajadas", "período comparado"), Estilo.dosLineas("Variación", "de horas"));
    private final JTable tablaEsfuerzo = Estilo.tabla(modeloEsfuerzo, new int[] {230, 110, 130, 130, 170, 140},
            new int[] {Estilo.IZQUIERDA, Estilo.DERECHA, Estilo.DERECHA, Estilo.DERECHA, Estilo.DERECHA, Estilo.DERECHA},
            true);
    private final DefaultTableModel modeloTipos = Estilo.modeloTabla(titulosPorTipo("Cliente"));
    private final JTable tablaTipos = Estilo.tabla(modeloTipos, anchosPorTipo(), alineacionesPorTipo(), true);
    private final JLabel lblResumen = Estilo.nota(" ");

    public PantallaReporteEsfuerzo(Window duenio, Sesion sesion) {
        super(duenio, sesion, "Reporte de esfuerzo por cliente");
        controlador = sesion.reportes();
        cmbProducto.addItem(TODOS);
        for (Producto p : controlador.getProductos()) {
            cmbProducto.addItem(p);
        }
        chkComparar.addActionListener(e -> habilitarComparacion());
        habilitarComparacion();

        JPanel parametros = Estilo.grupo("Parámetros");
        int n = GridBagConstraints.NONE;
        Estilo.poner(parametros, Estilo.etiqueta("Período", true), 0, 0, 1, 0, n);
        Estilo.poner(parametros, txtDesde, 1, 0, 1, 0, n);
        Estilo.poner(parametros, new JLabel("al"), 2, 0, 1, 0, n);
        Estilo.poner(parametros, txtHasta, 3, 0, 1, 0, n);
        Estilo.poner(parametros, Estilo.etiqueta("Producto", false), 4, 0, 1, 0, n);
        Estilo.poner(parametros, cmbProducto, 5, 0, 1, 1, n);
        Estilo.poner(parametros, chkComparar, 0, 1, 1, 0, n);
        Estilo.poner(parametros, txtCompDesde, 1, 1, 1, 0, n);
        Estilo.poner(parametros, new JLabel("al"), 2, 1, 1, 0, n);
        Estilo.poner(parametros, txtCompHasta, 3, 1, 1, 0, n);
        JButton consultar = Estilo.boton("Consultar");
        consultar.addActionListener(e -> consultar());
        Estilo.poner(parametros, consultar, 5, 1, 1, 1, n);

        JPanel esfuerzo = Estilo.grupo("Esfuerzo por cliente");
        Estilo.ponerArea(esfuerzo, Estilo.tablaConDesplazamiento(tablaEsfuerzo, 6), 0, 0, 1, 1);
        Estilo.poner(esfuerzo, Estilo.nota("Horas: intervenciones registradas en el período. Espera: días que las "
                + "solicitudes estuvieron en espera dentro del período."), 0, 1, 1, 1, GridBagConstraints.HORIZONTAL);
        JPanel tipos = Estilo.grupo("Distribución por tipo de solicitud (cantidad de solicitudes recibidas)");
        Estilo.ponerArea(tipos, Estilo.tablaConDesplazamiento(tablaTipos, 6), 0, 0, 1, 1);

        JPanel cuerpo = cuerpoVertical();
        cuerpo.add(parametros);
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(esfuerzo);
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(tipos);

        JButton exportar = Estilo.boton("Exportar...");
        exportar.setEnabled(false);
        exportar.setToolTipText("La exportación del reporte a un archivo (RF24) todavía no está disponible");
        JButton cerrar = Estilo.boton("Cerrar");
        cerrar.addActionListener(e -> dispose());
        JPanel botones = Estilo.botonera(null, exportar, cerrar);
        botones.add(lblResumen, BorderLayout.WEST);
        armar("Reporte de esfuerzo por cliente", cuerpo, botones);
        getRootPane().setDefaultButton(consultar);
    }

    private void habilitarComparacion() {
        txtCompDesde.setEnabled(chkComparar.isSelected());
        txtCompHasta.setEnabled(chkComparar.isSelected());
    }

    /** Pasos 3 y 4: lee los parámetros, consulta y llena las dos tablas. */
    private void consultar() {
        ejecutar(() -> {
            LocalDate desde = Entrada.fecha(txtDesde.getText(), "El inicio del período");
            LocalDate hasta = Entrada.fecha(txtHasta.getText(), "El fin del período");
            LocalDate compDesde = null;
            LocalDate compHasta = null;
            if (chkComparar.isSelected()) {
                compDesde = Entrada.fecha(txtCompDesde.getText(), "El inicio del período de comparación");
                compHasta = Entrada.fecha(txtCompHasta.getText(), "El fin del período de comparación");
            }
            Object elegido = cmbProducto.getSelectedItem();
            Producto producto = elegido instanceof Producto p ? p : null;
            List<FilaEsfuerzoCliente> filas = controlador.esfuerzoPorCliente(desde, hasta, producto, compDesde, compHasta);
            List<FilaPorTipo> porTipo = controlador.solicitudesPorTipo(desde, hasta, producto);

            String comparado = compDesde == null ? "período comparado"
                    : compDesde.format(Formatos.FECHA) + " al " + compHasta.format(Formatos.FECHA);
            tablaEsfuerzo.getColumnModel().getColumn(4).setHeaderValue(Estilo.dosLineas("Horas trabajadas", comparado));
            tablaEsfuerzo.getTableHeader().repaint();
            llenarEsfuerzo(filas, compDesde != null);
            llenarPorTipo(modeloTipos, porTipo, false);

            String resumen = "Período del " + desde.format(Formatos.FECHA) + " al " + hasta.format(Formatos.FECHA);
            if (compDesde != null) {
                resumen += ", comparado con el " + comparado;
            }
            lblResumen.setText(resumen + ". Producto: " + (producto == null ? "todos" : producto.getNombre()) + ".");
        });
    }

    private void llenarEsfuerzo(List<FilaEsfuerzoCliente> filas, boolean compara) {
        modeloEsfuerzo.setRowCount(0);
        int solicitudes = 0;
        BigDecimal horas = BigDecimal.ZERO;
        BigDecimal dias = BigDecimal.ZERO;
        BigDecimal horasComparacion = BigDecimal.ZERO;
        for (FilaEsfuerzoCliente f : filas) {
            modeloEsfuerzo.addRow(new Object[] {f.getCliente(), f.getSolicitudes(), Formatos.horas(f.getHorasTrabajadas()),
                Formatos.horas(f.getDiasEspera()), compara ? Formatos.horas(f.getHorasComparacion()) : "",
                compara ? variacion(f.getVariacion()) : ""});
            solicitudes += f.getSolicitudes();
            horas = horas.add(f.getHorasTrabajadas());
            dias = dias.add(f.getDiasEspera());
            if (compara) {
                horasComparacion = horasComparacion.add(f.getHorasComparacion());
            }
        }
        modeloEsfuerzo.addRow(new Object[] {"Total", solicitudes, Formatos.horas(horas), Formatos.horas(dias),
            compara ? Formatos.horas(horasComparacion) : "",
            compara ? variacion(FilaEsfuerzoCliente.calcularVariacion(horas, horasComparacion)) : ""});
    }

    private static String variacion(BigDecimal porcentaje) {
        if (porcentaje == null) {
            return "Sin horas antes";
        }
        return String.format(Formatos.ARGENTINA, "%+.1f %%", porcentaje);
    }

    // ------------------------------------------------------------------
    // Tablas por tipo de solicitud, compartidas con el reporte de carga
    // ------------------------------------------------------------------

    /** Títulos de una tabla por tipo: la primera columna y un tipo por columna, en dos líneas cortadas en el último espacio. */
    static String[] titulosPorTipo(String primera) {
        String[] tipos = FilaPorTipo.titulos();
        String[] titulos = new String[tipos.length + 1];
        titulos[0] = primera;
        for (int i = 0; i < tipos.length; i++) {
            int espacio = tipos[i].lastIndexOf(' ');
            titulos[i + 1] = espacio < 0 ? tipos[i] : Estilo.dosLineas(tipos[i].substring(0, espacio),
                    tipos[i].substring(espacio + 1));
        }
        return titulos;
    }

    static int[] anchosPorTipo() {
        int[] anchos = new int[FilaPorTipo.COLUMNAS + 1];
        anchos[0] = 230;
        for (int i = 1; i < anchos.length; i++) {
            anchos[i] = 115;
        }
        return anchos;
    }

    static int[] alineacionesPorTipo() {
        int[] alineaciones = new int[FilaPorTipo.COLUMNAS + 1];
        alineaciones[0] = Estilo.IZQUIERDA;
        for (int i = 1; i < alineaciones.length; i++) {
            alineaciones[i] = Estilo.DERECHA;
        }
        return alineaciones;
    }

    /**
     * Llena una tabla por tipo y agrega la fila de total, que suma cada columna.
     *
     * @param conTotalPorFila si la tabla tiene una última columna con el total de cada fila
     */
    static void llenarPorTipo(DefaultTableModel modelo, List<FilaPorTipo> filas, boolean conTotalPorFila) {
        modelo.setRowCount(0);
        int columnas = FilaPorTipo.COLUMNAS + (conTotalPorFila ? 2 : 1);
        BigDecimal[] totales = new BigDecimal[FilaPorTipo.COLUMNAS];
        for (int i = 0; i < totales.length; i++) {
            totales[i] = BigDecimal.ZERO;
        }
        for (FilaPorTipo f : filas) {
            Object[] fila = new Object[columnas];
            fila[0] = f.getNombre();
            for (int i = 0; i < FilaPorTipo.COLUMNAS; i++) {
                fila[i + 1] = Formatos.horas(f.getValor(i));
                totales[i] = totales[i].add(f.getValor(i));
            }
            if (conTotalPorFila) {
                fila[columnas - 1] = Formatos.horas(f.getTotal());
            }
            modelo.addRow(fila);
        }
        Object[] total = new Object[columnas];
        total[0] = "Total";
        BigDecimal general = BigDecimal.ZERO;
        for (int i = 0; i < totales.length; i++) {
            total[i + 1] = Formatos.horas(totales[i]);
            general = general.add(totales[i]);
        }
        if (conTotalPorFila) {
            total[columnas - 1] = Formatos.horas(general);
        }
        modelo.addRow(total);
    }
}
