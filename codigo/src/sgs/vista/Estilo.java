package sgs.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * Aspecto común de las pantallas, el mismo de los prototipos de la sección 13: Nimbus,
 * encabezado con la pantalla y el usuario, datos agrupados en recuadros, obligatorios en
 * negrita con asterisco, datos del sistema en gris y botones abajo a la derecha.
 */
public final class Estilo {

    public static final String SISTEMA = "Sistema de gestión y análisis del soporte";
    public static final Color GRIS_TEXTO = new Color(90, 90, 90);
    public static final Color FONDO_SOLO_LECTURA = new Color(236, 238, 241);
    public static final Color FONDO_TOTAL = new Color(222, 228, 236);
    public static final int IZQUIERDA = SwingConstants.LEFT;
    public static final int DERECHA = SwingConstants.RIGHT;
    public static final int CENTRO = SwingConstants.CENTER;

    private static Font base = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private static Font negrita = base.deriveFont(Font.BOLD);

    private Estilo() {
        // Solo métodos de clase
    }

    /** Aplica Nimbus y la tipografía. Se llama una vez, al iniciar. */
    public static void configurarAspecto() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            // Si Nimbus no está disponible se sigue con el aspecto por defecto de Java
        }
        String[] fuentes = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        String familia = Arrays.asList(fuentes).contains("Noto Sans") ? "Noto Sans" : Font.SANS_SERIF;
        base = new Font(familia, Font.PLAIN, 13);
        negrita = base.deriveFont(Font.BOLD);
        UIManager.getLookAndFeelDefaults().put("defaultFont", new FontUIResource(base));
    }

    public static Font base() {
        return base;
    }

    public static Font negrita() {
        return negrita;
    }

    /** Barra superior con el nombre de la pantalla y el usuario con su rol. */
    public static JPanel encabezado(String pantalla, String usuario) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(190, 196, 204)),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        JLabel titulo = new JLabel(pantalla);
        titulo.setFont(base.deriveFont(Font.BOLD, 18f));
        p.add(titulo, BorderLayout.WEST);
        if (usuario != null) {
            JLabel quien = new JLabel("Usuario: " + usuario);
            quien.setForeground(GRIS_TEXTO);
            p.add(quien, BorderLayout.EAST);
        }
        return p;
    }

    /** Recuadro con título y GridBagLayout para ubicar etiquetas y campos. */
    public static JPanel grupo(String titulo) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(titulo),
                BorderFactory.createEmptyBorder(2, 6, 6, 6)));
        return p;
    }

    /** Etiqueta de un campo. Los obligatorios van en negrita y con asterisco. */
    public static JLabel etiqueta(String texto, boolean obligatorio) {
        JLabel l = new JLabel(obligatorio ? texto + " *" : texto);
        if (obligatorio) {
            l.setFont(negrita);
        }
        return l;
    }

    /** Texto de ayuda en gris. */
    public static JLabel nota(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(GRIS_TEXTO);
        l.setFont(base.deriveFont(12f));
        return l;
    }

    /** Campo que completa el sistema: gris y sin edición. */
    public static JTextField soloLectura(String texto, int columnas) {
        JTextField t = new JTextField(texto, columnas);
        t.setEditable(false);
        t.setFocusable(false);
        t.setBackground(FONDO_SOLO_LECTURA);
        return t;
    }

    public static JTextArea areaDeTexto(int filas) {
        JTextArea a = new JTextArea(filas, 20);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(base);
        return a;
    }

    public static JScrollPane conDesplazamiento(JComponent c) {
        return new JScrollPane(c, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    public static JButton boton(String texto) {
        JButton b = new JButton(texto);
        b.setPreferredSize(new Dimension(Math.max(110, b.getPreferredSize().width + 16), 30));
        return b;
    }

    /** Fila de botones abajo a la derecha, con una nota opcional a la izquierda. */
    public static JPanel botonera(String nota, JButton... botones) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 14, 12, 14));
        if (nota != null) {
            p.add(nota(nota), BorderLayout.WEST);
        }
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        for (JButton b : botones) {
            fila.add(b);
        }
        p.add(fila, BorderLayout.EAST);
        return p;
    }

    public static JPanel fila(Component... componentes) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        for (Component c : componentes) {
            p.add(c);
        }
        return p;
    }

    /** Agrega un componente en una celda de un panel con GridBagLayout. */
    public static void poner(JPanel p, Component c, int x, int y, int ancho, double pesoX, int relleno) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x;
        g.gridy = y;
        g.gridwidth = ancho;
        g.weightx = pesoX;
        g.fill = relleno;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(4, 6, 4, 6);
        p.add(c, g);
    }

    /** Etiqueta alineada arriba, para los campos de varias líneas. */
    public static void ponerEtiquetaArriba(JPanel p, JLabel l, int x, int y) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x;
        g.gridy = y;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.insets = new Insets(8, 6, 4, 6);
        p.add(l, g);
    }

    /** Componente que ocupa el espacio disponible a lo ancho y a lo alto. */
    public static void ponerArea(JPanel p, Component c, int x, int y, int ancho, double pesoY) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x;
        g.gridy = y;
        g.gridwidth = ancho;
        g.weightx = 1;
        g.weighty = pesoY;
        g.fill = GridBagConstraints.BOTH;
        g.insets = new Insets(4, 6, 4, 6);
        p.add(c, g);
    }

    /** Modelo de tabla de solo lectura. */
    public static DefaultTableModel modeloTabla(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    /**
     * Tabla de solo lectura con el ancho y la alineación de cada columna. Si se pide, la última
     * fila se muestra como total, en negrita y con fondo.
     */
    public static JTable tabla(DefaultTableModel modelo, int[] anchos, int[] alineaciones, boolean ultimaEsTotal) {
        JTable t = new JTable(modelo);
        boolean dosLineas = false;
        for (int i = 0; i < modelo.getColumnCount(); i++) {
            dosLineas |= modelo.getColumnName(i).contains("<br>");
        }
        if (dosLineas) {
            // El encabezado no calcula solo el alto de los títulos en dos líneas
            t.setTableHeader(new JTableHeader(t.getColumnModel()) {
                private static final long serialVersionUID = 1L;

                @Override
                public Dimension getPreferredSize() {
                    Dimension d = super.getPreferredSize();
                    d.height = Math.max(d.height, 46);
                    return d;
                }
            });
        }
        t.setRowHeight(24);
        t.setFillsViewportHeight(true);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setFont(negrita);
        for (int i = 0; i < anchos.length; i++) {
            final int alineacion = alineaciones[i];
            t.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
            t.getColumnModel().getColumn(i).setCellRenderer(new DefaultTableCellRenderer() {
                private static final long serialVersionUID = 1L;

                @Override
                public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                        boolean foco, int fila, int columna) {
                    JLabel l = (JLabel) super.getTableCellRendererComponent(tabla, valor, seleccionada, foco, fila, columna);
                    l.setHorizontalAlignment(alineacion);
                    l.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                    boolean total = ultimaEsTotal && fila == tabla.getRowCount() - 1;
                    l.setFont(total ? negrita : base);
                    if (total && !seleccionada) {
                        l.setBackground(FONDO_TOTAL);
                    } else if (!seleccionada) {
                        l.setBackground(tabla.getBackground());
                    }
                    return l;
                }
            });
        }
        return t;
    }

    /** Ancho preferido de la tabla según sus columnas y la cantidad de filas visibles. */
    public static JScrollPane tablaConDesplazamiento(JTable tabla, int filasVisibles) {
        int ancho = 0;
        for (int i = 0; i < tabla.getColumnCount(); i++) {
            ancho += tabla.getColumnModel().getColumn(i).getPreferredWidth();
        }
        tabla.setPreferredScrollableViewportSize(new Dimension(ancho, filasVisibles * tabla.getRowHeight()));
        return new JScrollPane(tabla);
    }

    /** Título de columna en dos líneas. */
    public static String dosLineas(String a, String b) {
        return "<html>" + a + "<br>" + b + "</html>";
    }
}
