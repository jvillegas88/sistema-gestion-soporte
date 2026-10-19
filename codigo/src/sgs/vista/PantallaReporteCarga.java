package sgs.vista;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import sgs.controlador.ControladorReportes;
import sgs.controlador.Sesion;
import sgs.modelo.Formatos;
import sgs.modelo.consulta.FilaCargaIntegrante;
import sgs.modelo.consulta.FilaDemorada;

/**
 * CU12 Consultar reporte de carga por integrante. Muestra las horas y las intervenciones de cada
 * integrante en el período, ordenadas por nombre para que no funcione como un ranking (RNF05),
 * y en otra pestaña las solicitudes demoradas (alternativa 6.A, RF23).
 */
public final class PantallaReporteCarga extends Pantalla {

    private static final long serialVersionUID = 1L;

    private final transient ControladorReportes controlador;
    private final JTextField txtDesde = new JTextField(inicioDeTrimestre(1).format(Formatos.FECHA), 8);
    private final JTextField txtHasta = new JTextField(inicioDeTrimestre(0).minusDays(1).format(Formatos.FECHA), 8);
    private final DefaultTableModel modeloCarga = Estilo.modeloTabla("Integrante", "Rol",
            Estilo.dosLineas("Horas", "registradas"), "Intervenciones");
    private final JTable tablaCarga = Estilo.tabla(modeloCarga, new int[] {230, 200, 130, 130},
            new int[] {Estilo.IZQUIERDA, Estilo.IZQUIERDA, Estilo.DERECHA, Estilo.DERECHA}, true);
    private final DefaultTableModel modeloTipos = Estilo.modeloTabla(titulosConTotal());
    private final JTable tablaTipos = Estilo.tabla(modeloTipos, anchosConTotal(), alineacionesConTotal(), true);
    private final JTextField txtDias = new JTextField("7", 3);
    private final DefaultTableModel modeloDemoradas = Estilo.modeloTabla("N.º", "Cliente", "Asunto", "Estado",
            "Responsable", Estilo.dosLineas("Días", "abierta"), Estilo.dosLineas("Días", "en espera"), "Situación");
    private final JTable tablaDemoradas = Estilo.tabla(modeloDemoradas, new int[] {50, 110, 300, 90, 120, 70, 80, 250},
            new int[] {Estilo.DERECHA, Estilo.IZQUIERDA, Estilo.IZQUIERDA, Estilo.IZQUIERDA, Estilo.IZQUIERDA,
                Estilo.DERECHA, Estilo.DERECHA, Estilo.IZQUIERDA}, false);
    private final JLabel lblResumen = Estilo.nota(" ");
    private final JLabel lblDemoradas = Estilo.nota(" ");

    public PantallaReporteCarga(Window duenio, Sesion sesion) {
        super(duenio, sesion, "Reporte de carga por integrante");
        controlador = sesion.reportes();

        JTabbedPane pestanias = new JTabbedPane();
        pestanias.addTab("Carga del equipo", armarCarga());
        pestanias.addTab("Solicitudes demoradas", armarDemoradas());
        JPanel cuerpo = cuerpoVertical();
        cuerpo.add(pestanias);

        JButton cerrar = Estilo.boton("Cerrar");
        cerrar.addActionListener(e -> dispose());
        JPanel botones = Estilo.botonera(null, cerrar);
        botones.add(lblResumen, BorderLayout.WEST);
        armar("Reporte de carga por integrante", cuerpo, botones);
    }

    private JPanel armarCarga() {
        JPanel parametros = Estilo.grupo("Parámetros");
        int n = GridBagConstraints.NONE;
        Estilo.poner(parametros, Estilo.etiqueta("Período", true), 0, 0, 1, 0, n);
        Estilo.poner(parametros, txtDesde, 1, 0, 1, 0, n);
        Estilo.poner(parametros, new JLabel("al"), 2, 0, 1, 0, n);
        Estilo.poner(parametros, txtHasta, 3, 0, 1, 0, n);
        JButton consultar = Estilo.boton("Consultar");
        consultar.addActionListener(e -> consultar());
        Estilo.poner(parametros, consultar, 4, 0, 1, 1, n);

        JPanel carga = Estilo.grupo("Tiempo registrado en soporte, por integrante");
        Estilo.ponerArea(carga, Estilo.tablaConDesplazamiento(tablaCarga, 8), 0, 0, 1, 1);
        Estilo.poner(carga, Estilo.nota("Ordenado por nombre. El reporte informa la carga del equipo y no el "
                + "rendimiento individual."), 0, 1, 1, 1, GridBagConstraints.HORIZONTAL);
        JPanel tipos = Estilo.grupo("Horas por tipo de solicitud");
        Estilo.ponerArea(tipos, Estilo.tablaConDesplazamiento(tablaTipos, 8), 0, 0, 1, 1);

        JPanel panel = cuerpoVertical();
        panel.add(parametros);
        panel.add(Box.createVerticalStrut(8));
        panel.add(carga);
        panel.add(Box.createVerticalStrut(8));
        panel.add(tipos);
        return panel;
    }

    private JPanel armarDemoradas() {
        JPanel parametros = Estilo.grupo("Parámetros");
        int n = GridBagConstraints.NONE;
        Estilo.poner(parametros, Estilo.etiqueta("Abiertas hace más de", true), 0, 0, 1, 0, n);
        Estilo.poner(parametros, Estilo.fila(txtDias, new JLabel("días")), 1, 0, 1, 0, n);
        JButton buscar = Estilo.boton("Buscar");
        buscar.addActionListener(e -> buscarDemoradas());
        Estilo.poner(parametros, buscar, 2, 0, 1, 1, n);

        JPanel lista = Estilo.grupo("Solicitudes demoradas, sin respuesta al cliente o con espera prolongada");
        Estilo.ponerArea(lista, Estilo.tablaConDesplazamiento(tablaDemoradas, 10), 0, 0, 1, 1);
        Estilo.poner(lista, lblDemoradas, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL);

        // Con BorderLayout los parámetros conservan su alto y la lista ocupa el resto de la pestaña
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));
        panel.add(parametros, BorderLayout.NORTH);
        panel.add(lista, BorderLayout.CENTER);
        return panel;
    }

    /** Pasos 4 y 5: consulta la carga del período y la reparte por tipo de solicitud. */
    private void consultar() {
        ejecutar(() -> {
            LocalDate desde = Entrada.fecha(txtDesde.getText(), "El inicio del período");
            LocalDate hasta = Entrada.fecha(txtHasta.getText(), "El fin del período");
            List<FilaCargaIntegrante> filas = controlador.cargaPorIntegrante(desde, hasta);
            modeloCarga.setRowCount(0);
            BigDecimal horas = BigDecimal.ZERO;
            int intervenciones = 0;
            for (FilaCargaIntegrante f : filas) {
                modeloCarga.addRow(new Object[] {f.getIntegrante(), f.getRol(), Formatos.horas(f.getHoras()),
                    f.getIntervenciones()});
                horas = horas.add(f.getHoras());
                intervenciones += f.getIntervenciones();
            }
            modeloCarga.addRow(new Object[] {"Total", "", Formatos.horas(horas), intervenciones});
            PantallaReporteEsfuerzo.llenarPorTipo(modeloTipos, controlador.horasPorTipo(desde, hasta), true);
            lblResumen.setText("Período del " + desde.format(Formatos.FECHA) + " al " + hasta.format(Formatos.FECHA) + ".");
        });
    }

    /** Alternativa 6.A: las solicitudes abiertas que superan la cantidad de días indicada. */
    private void buscarDemoradas() {
        ejecutar(() -> {
            Integer dias = Entrada.entero(txtDias.getText(), "La cantidad de días");
            List<FilaDemorada> filas = controlador.solicitudesDemoradas(dias == null ? 0 : dias);
            modeloDemoradas.setRowCount(0);
            for (FilaDemorada f : filas) {
                modeloDemoradas.addRow(new Object[] {f.getId(), f.getCliente(), f.getAsunto(), f.getEstado(),
                    f.getResponsable(), f.getDiasAbierta(), f.getDiasEnEspera() > 0 ? f.getDiasEnEspera() : "",
                    f.getSituacion()});
            }
            lblDemoradas.setText(filas.isEmpty() ? "No hay solicitudes demoradas con ese criterio."
                    : filas.size() + (filas.size() == 1 ? " solicitud" : " solicitudes") + ", de la más antigua a la más nueva.");
        });
    }

    private static String[] titulosConTotal() {
        String[] base = PantallaReporteEsfuerzo.titulosPorTipo("Integrante");
        String[] titulos = Arrays.copyOf(base, base.length + 1);
        titulos[base.length] = "Total";
        return titulos;
    }

    private static int[] anchosConTotal() {
        int[] base = PantallaReporteEsfuerzo.anchosPorTipo();
        int[] anchos = Arrays.copyOf(base, base.length + 1);
        anchos[base.length] = 90;
        return anchos;
    }

    private static int[] alineacionesConTotal() {
        int[] base = PantallaReporteEsfuerzo.alineacionesPorTipo();
        int[] alineaciones = Arrays.copyOf(base, base.length + 1);
        alineaciones[base.length] = Estilo.DERECHA;
        return alineaciones;
    }
}
