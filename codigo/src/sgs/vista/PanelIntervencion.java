package sgs.vista;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import sgs.controlador.ControladorRegistrarIntervencion;
import sgs.modelo.Formatos;
import sgs.modelo.catalogo.TipoComunicacion;
import sgs.modelo.catalogo.TipoIntervencion;
import sgs.modelo.excepcion.ReglaNegocioException;

/**
 * Datos de una intervención: tipo, tiempo y descripción, que son los tres obligatorios de
 * RNF04, más la comunicación opcional (CU06, alternativa 3.A) y los ítems de una estimación
 * (alternativa 3.B). Lo usan la pantalla de CU06 y la de CU09, que incluye a CU06.
 */
public final class PanelIntervencion extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JComboBox<TipoIntervencion> cmbTipo = new JComboBox<>(TipoIntervencion.values());
    private final JTextField txtTiempo = new JTextField(5);
    private final JTextArea txtDescripcion = Estilo.areaDeTexto(3);
    private final JCheckBox chkComunicacion = new JCheckBox("Agregar una comunicación");
    private final JRadioButton rbNota = new JRadioButton(TipoComunicacion.NOTA_INTERNA.getNombre(), true);
    private final JRadioButton rbRespuesta = new JRadioButton(TipoComunicacion.RESPUESTA_CLIENTE.getNombre());
    private final JTextArea txtComunicacion = Estilo.areaDeTexto(3);

    private final DefaultTableModel modeloItems = Estilo.modeloTabla("N.º", "Descripción", "Horas");
    private final JTable tablaItems = Estilo.tabla(modeloItems, new int[] {44, 250, 62},
            new int[] {Estilo.CENTRO, Estilo.IZQUIERDA, Estilo.DERECHA}, false);
    private final JButton btnAgregarItem = new JButton("Agregar");
    private final JButton btnQuitarItem = new JButton("Quitar");
    private final JLabel lblTotalEstimado = new JLabel();
    private final transient List<String> descripcionesItems = new ArrayList<>();
    private final transient List<BigDecimal> horasItems = new ArrayList<>();

    public PanelIntervencion(String titulo) {
        super(new GridBagLayout());

        JPanel nueva = Estilo.grupo(titulo);
        int h = GridBagConstraints.HORIZONTAL;
        cmbTipo.setSelectedIndex(-1);
        Estilo.poner(nueva, Estilo.etiqueta("Tipo de intervención", true), 0, 0, 1, 0, GridBagConstraints.NONE);
        Estilo.poner(nueva, cmbTipo, 1, 0, 1, 1, h);
        Estilo.poner(nueva, Estilo.etiqueta("Tiempo dedicado", true), 0, 1, 1, 0, GridBagConstraints.NONE);
        txtTiempo.setHorizontalAlignment(JTextField.RIGHT);
        Estilo.poner(nueva, Estilo.fila(txtTiempo, Estilo.nota("horas (mayor que 0 y hasta 24, por ejemplo 1,5)")), 1, 1, 1, 1, h);
        Estilo.ponerEtiquetaArriba(nueva, Estilo.etiqueta("Descripción", true), 0, 2);
        Estilo.ponerArea(nueva, Estilo.conDesplazamiento(txtDescripcion), 1, 2, 1, 0.5);
        Estilo.poner(nueva, chkComunicacion, 0, 3, 2, 0, GridBagConstraints.NONE);
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(rbNota);
        grupo.add(rbRespuesta);
        Estilo.poner(nueva, Estilo.etiqueta("Tipo de comunicación", false), 0, 4, 1, 0, GridBagConstraints.NONE);
        Estilo.poner(nueva, Estilo.fila(rbNota, Box.createHorizontalStrut(10), rbRespuesta), 1, 4, 1, 1, h);
        Estilo.ponerEtiquetaArriba(nueva, Estilo.etiqueta("Texto", false), 0, 5);
        Estilo.ponerArea(nueva, Estilo.conDesplazamiento(txtComunicacion), 1, 5, 1, 0.5);

        JPanel items = Estilo.grupo("Ítems de la estimación");
        Estilo.ponerArea(items, Estilo.tablaConDesplazamiento(tablaItems, 5), 0, 0, 3, 1);
        Estilo.poner(items, btnAgregarItem, 0, 1, 1, 0, GridBagConstraints.NONE);
        Estilo.poner(items, btnQuitarItem, 1, 1, 1, 0, GridBagConstraints.NONE);
        lblTotalEstimado.setFont(Estilo.negrita());
        GridBagConstraints gt = new GridBagConstraints();
        gt.gridx = 2;
        gt.gridy = 1;
        gt.weightx = 1;
        gt.anchor = GridBagConstraints.EAST;
        gt.insets = new Insets(4, 6, 4, 10);
        items.add(lblTotalEstimado, gt);

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 0.55;
        g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        g.insets = new Insets(0, 0, 0, 8);
        add(nueva, g);
        g.gridx = 1;
        g.weightx = 0.45;
        g.insets = new Insets(0, 0, 0, 0);
        add(items, g);

        chkComunicacion.addActionListener(e -> actualizarHabilitados());
        cmbTipo.addActionListener(e -> actualizarHabilitados());
        btnAgregarItem.addActionListener(e -> agregarItem());
        btnQuitarItem.addActionListener(e -> quitarItem());
        actualizarHabilitados();
        mostrarItems();
    }

    /** Los datos de la comunicación solo se habilitan si se marca, y los ítems solo en una estimación. */
    private void actualizarHabilitados() {
        boolean conComunicacion = chkComunicacion.isSelected();
        rbNota.setEnabled(conComunicacion);
        rbRespuesta.setEnabled(conComunicacion);
        txtComunicacion.setEnabled(conComunicacion);
        boolean esEstimacion = getTipo() == TipoIntervencion.ESTIMACION;
        tablaItems.setEnabled(esEstimacion);
        btnAgregarItem.setEnabled(esEstimacion);
        btnQuitarItem.setEnabled(esEstimacion);
    }

    private void agregarItem() {
        JTextField descripcion = new JTextField(24);
        JTextField horas = new JTextField(5);
        JPanel datos = new JPanel(new GridBagLayout());
        Estilo.poner(datos, Estilo.etiqueta("Descripción", true), 0, 0, 1, 0, GridBagConstraints.NONE);
        Estilo.poner(datos, descripcion, 1, 0, 1, 1, GridBagConstraints.HORIZONTAL);
        Estilo.poner(datos, Estilo.etiqueta("Horas estimadas", true), 0, 1, 1, 0, GridBagConstraints.NONE);
        Estilo.poner(datos, horas, 1, 1, 1, 0, GridBagConstraints.NONE);
        int respuesta = JOptionPane.showConfirmDialog(this, datos, "Ítem de la estimación", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (respuesta != JOptionPane.OK_OPTION) {
            return;
        }
        Dialogos.ejecutar(this, () -> {
            BigDecimal valor = Entrada.horas(horas.getText(), "Las horas del ítem");
            if (Formatos.estaVacio(descripcion.getText()) || valor == null) {
                throw new ReglaNegocioException("Completá la descripción y las horas del ítem");
            }
            descripcionesItems.add(descripcion.getText().trim());
            horasItems.add(valor);
            mostrarItems();
        });
    }

    private void quitarItem() {
        int fila = tablaItems.getSelectedRow();
        if (fila < 0) {
            Dialogos.aviso(this, "Seleccioná el ítem que querés quitar");
            return;
        }
        descripcionesItems.remove(fila);
        horasItems.remove(fila);
        mostrarItems();
    }

    private void mostrarItems() {
        modeloItems.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < descripcionesItems.size(); i++) {
            modeloItems.addRow(new Object[] {i + 1, descripcionesItems.get(i), Formatos.horas(horasItems.get(i))});
            total = total.add(horasItems.get(i));
        }
        lblTotalEstimado.setText("Total estimado: " + Formatos.horas(total) + " h");
    }

    public TipoIntervencion getTipo() {
        return (TipoIntervencion) cmbTipo.getSelectedItem();
    }

    public String getDescripcion() {
        return txtDescripcion.getText();
    }

    /** El tiempo escrito, convertido a número. Si no es un número válido, avisa con un mensaje claro. */
    public BigDecimal getTiempo() throws ReglaNegocioException {
        return Entrada.horas(txtTiempo.getText(), "El tiempo dedicado");
    }

    /** Selecciona un tipo de intervención, por ejemplo Respuesta al cliente al cerrar. */
    public void sugerirTipo(TipoIntervencion tipo) {
        cmbTipo.setSelectedItem(tipo);
    }

    /**
     * Pasa al borrador del controlador la comunicación y los ítems, si los hay. Si se marcó la
     * comunicación sin escribir el texto, avisa.
     */
    public void cargarBorrador(ControladorRegistrarIntervencion controlador) throws ReglaNegocioException {
        controlador.descartarBorrador();
        if (chkComunicacion.isSelected()) {
            if (Formatos.estaVacio(txtComunicacion.getText())) {
                throw new ReglaNegocioException("Escribí el texto de la comunicación o desmarcá la opción");
            }
            TipoComunicacion tipo = rbRespuesta.isSelected() ? TipoComunicacion.RESPUESTA_CLIENTE : TipoComunicacion.NOTA_INTERNA;
            controlador.agregarComunicacion(tipo, txtComunicacion.getText());
        }
        if (getTipo() == TipoIntervencion.ESTIMACION) {
            for (int i = 0; i < descripcionesItems.size(); i++) {
                controlador.agregarItem(descripcionesItems.get(i), horasItems.get(i));
            }
        }
    }
}
