package sgs.vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import sgs.Aplicacion;
import sgs.controlador.Sesion;
import sgs.modelo.Formatos;
import sgs.modelo.catalogo.Rol;
import sgs.modelo.consulta.ResumenSolicitud;
import sgs.modelo.excepcion.ReglaNegocioException;

/**
 * Ventana principal con el menú de selección. Las opciones se habilitan según el rol del
 * integrante (RNF06): soporte funcional registra solicitudes, los agentes trabajan sobre la
 * bandeja de solicitudes abiertas y gestión consulta los reportes.
 */
public final class VentanaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;

    private final transient Sesion sesion;
    private final DefaultTableModel modelo = Estilo.modeloTabla("N.º", "Prioridad", "Estado", "Recibida", "Cliente",
            "Producto", "Tipo", "Asunto", "Responsable");
    private final JTable tabla = Estilo.tabla(modelo, new int[] {50, 110, 90, 140, 110, 85, 160, 290, 120},
            new int[] {Estilo.DERECHA, Estilo.IZQUIERDA, Estilo.IZQUIERDA, Estilo.IZQUIERDA, Estilo.IZQUIERDA,
                Estilo.IZQUIERDA, Estilo.IZQUIERDA, Estilo.IZQUIERDA, Estilo.IZQUIERDA}, false);
    private final JCheckBox chkSoloMias = new JCheckBox("Solo las solicitudes de las que soy responsable");
    private final JLabel lblCantidad = Estilo.nota("");
    private transient List<ResumenSolicitud> filas = new ArrayList<>();

    public VentanaPrincipal(Sesion sesion) {
        super(Estilo.SISTEMA);
        this.sesion = sesion;
        Rol rol = sesion.getUsuario().getRol();

        setJMenuBar(armarMenu(rol));
        JPanel raiz = new JPanel(new BorderLayout());
        if (rol.esAgente()) {
            raiz.add(Estilo.encabezado("Bandeja de solicitudes abiertas", sesion.getDescripcionUsuario()), BorderLayout.NORTH);
            raiz.add(armarBandeja(), BorderLayout.CENTER);
        } else {
            raiz.add(Estilo.encabezado("Reportes de gestión", sesion.getDescripcionUsuario()), BorderLayout.NORTH);
            raiz.add(armarPanelGestion(), BorderLayout.CENTER);
        }
        setContentPane(raiz);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 420));
        pack();
        setLocationRelativeTo(null);
        if (rol.esAgente()) {
            actualizar();
        }
    }

    // ------------------------------------------------------------------
    // Menú
    // ------------------------------------------------------------------

    private JMenuBar armarMenu(Rol rol) {
        JMenuBar barra = new JMenuBar();

        JMenu solicitudes = new JMenu("Solicitudes");
        solicitudes.setMnemonic(KeyEvent.VK_S);
        solicitudes.add(item("Registrar solicitud...", rol.puedeRegistrarSolicitudes(), "Solo soporte funcional",
                KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), e -> registrarSolicitud()));
        solicitudes.add(item("Abrir solicitud por número...", rol.esAgente(), "Solo los agentes de soporte",
                KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), e -> abrirPorNumero()));
        solicitudes.add(item("Importar histórico...", rol.puedeImportarHistorico(), "Solo soporte funcional", null,
                e -> new PantallaImportarHistorico(this, sesion).mostrar()));
        solicitudes.addSeparator();
        solicitudes.add(item("Actualizar la bandeja", rol.esAgente(), "Solo los agentes de soporte",
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), e -> actualizar()));
        barra.add(solicitudes);

        JMenu reportes = new JMenu("Reportes");
        reportes.setMnemonic(KeyEvent.VK_R);
        reportes.add(item("Esfuerzo por cliente...", rol.puedeVerReportes(), "Solo el rol de gestión", null,
                e -> new PantallaReporteEsfuerzo(this, sesion).mostrar()));
        reportes.add(item("Carga por integrante...", rol.puedeVerReportes(), "Solo el rol de gestión", null,
                e -> new PantallaReporteCarga(this, sesion).mostrar()));
        barra.add(reportes);

        JMenu sesionMenu = new JMenu("Sesión");
        sesionMenu.setMnemonic(KeyEvent.VK_E);
        sesionMenu.add(item("Cerrar sesión", true, null, null, e -> cerrarSesion()));
        sesionMenu.add(item("Salir", true, null, KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK),
                e -> System.exit(0)));
        barra.add(sesionMenu);
        return barra;
    }

    /** Opción del menú. Si el rol no la tiene permitida queda deshabilitada y explica por qué. */
    private static JMenuItem item(String texto, boolean habilitado, String motivo, KeyStroke atajo, ActionListener accion) {
        JMenuItem item = new JMenuItem(texto);
        item.setEnabled(habilitado);
        if (!habilitado && motivo != null) {
            item.setToolTipText(motivo);
        }
        if (atajo != null) {
            item.setAccelerator(atajo);
        }
        item.addActionListener(accion);
        return item;
    }

    // ------------------------------------------------------------------
    // Bandeja
    // ------------------------------------------------------------------

    private JPanel armarBandeja() {
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    abrirSeleccionada();
                }
            }
        });
        chkSoloMias.addActionListener(e -> actualizar());

        JPanel lista = Estilo.grupo("Ordenadas por prioridad y, dentro de cada prioridad, de la más antigua a la más nueva");
        Estilo.ponerArea(lista, Estilo.tablaConDesplazamiento(tabla, 12), 0, 0, 1, 1);
        JPanel opciones = new JPanel(new BorderLayout());
        opciones.add(chkSoloMias, BorderLayout.WEST);
        opciones.add(lblCantidad, BorderLayout.EAST);
        Estilo.poner(lista, opciones, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL);

        JPanel cuerpo = new JPanel(new BorderLayout());
        cuerpo.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));
        cuerpo.add(lista, BorderLayout.CENTER);

        JButton actualizar = Estilo.boton("Actualizar");
        JButton abrir = Estilo.boton("Abrir solicitud");
        actualizar.addActionListener(e -> actualizar());
        abrir.addActionListener(e -> abrirSeleccionada());
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(cuerpo, BorderLayout.CENTER);
        panel.add(Estilo.botonera("Doble clic en una fila para abrir la solicitud", actualizar, abrir), BorderLayout.SOUTH);
        getRootPane().setDefaultButton(abrir);
        return panel;
    }

    /** Vuelve a leer las solicitudes abiertas y llena la tabla. */
    private void actualizar() {
        Dialogos.ejecutar(this, () -> {
            filas = sesion.bandeja().listarAbiertas(chkSoloMias.isSelected());
            modelo.setRowCount(0);
            for (ResumenSolicitud r : filas) {
                modelo.addRow(new Object[] {r.getId(), textoONada(r.getPrioridad()), r.getEstado(),
                    Formatos.fechaHora(r.getFechaRecepcion()), r.getCliente(), r.getProducto(),
                    textoONada(r.getTipo()), r.getAsunto(), r.getResponsable()});
            }
            lblCantidad.setText(filas.size() == 1 ? "1 solicitud" : filas.size() + " solicitudes");
        });
    }

    private static String textoONada(Object valor) {
        return valor == null ? "Sin clasificar" : valor.toString();
    }

    private void abrirSeleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Dialogos.aviso(this, "Seleccioná una solicitud de la lista");
            return;
        }
        abrir(filas.get(tabla.convertRowIndexToModel(fila)).getId());
    }

    /** Abre cualquier solicitud por su número, también una cerrada, por ejemplo para reabrirla. */
    private void abrirPorNumero() {
        String texto = JOptionPane.showInputDialog(this, "Número de la solicitud:", Estilo.SISTEMA,
                JOptionPane.QUESTION_MESSAGE);
        if (texto == null) {
            return;
        }
        Dialogos.ejecutar(this, () -> {
            Integer numero = Entrada.entero(texto, "El número de la solicitud");
            if (numero == null) {
                throw new ReglaNegocioException("Ingresá el número de la solicitud");
            }
            abrir(numero);
        });
    }

    private void abrir(int numero) {
        PantallaSolicitud pantalla = PantallaSolicitud.crear(this, sesion, numero);
        if (pantalla != null) {
            pantalla.mostrar();
            actualizar();
        }
    }

    private void registrarSolicitud() {
        if (new PantallaRegistrarSolicitud(this, sesion, null).mostrar()) {
            actualizar();
        }
    }

    // ------------------------------------------------------------------
    // Gestión
    // ------------------------------------------------------------------

    private JPanel armarPanelGestion() {
        JButton esfuerzo = Estilo.boton("Esfuerzo por cliente");
        JButton carga = Estilo.boton("Carga por integrante");
        esfuerzo.setPreferredSize(new Dimension(240, 60));
        carga.setPreferredSize(new Dimension(240, 60));
        esfuerzo.addActionListener(e -> new PantallaReporteEsfuerzo(this, sesion).mostrar());
        carga.addActionListener(e -> new PantallaReporteCarga(this, sesion).mostrar());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 0));
        botones.add(esfuerzo);
        botones.add(carga);
        JPanel centro = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.insets = new Insets(0, 0, 18, 0);
        centro.add(Estilo.nota("Elegí un reporte acá o desde el menú Reportes. Los reportes no modifican ningún dato."), g);
        g.gridy = 1;
        centro.add(botones, g);
        centro.setPreferredSize(new Dimension(900, 320));
        return centro;
    }

    private void cerrarSesion() {
        dispose();
        Aplicacion.iniciar();
    }
}
