package sgs.vista;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import sgs.controlador.Sesion;
import sgs.modelo.Solicitud;

/**
 * Base de las pantallas de los casos de uso. Cada una es un diálogo modal con la misma
 * estructura: el encabezado con el nombre y el usuario, el cuerpo con los datos y la botonera.
 * La subclase arma su cuerpo y sus botones y llama a armar() al final de su constructor.
 */
public abstract class Pantalla extends JDialog {

    private static final long serialVersionUID = 1L;

    protected final transient Sesion sesion;
    private boolean confirmada;

    protected Pantalla(Window duenio, Sesion sesion, String titulo) {
        super(duenio, Estilo.SISTEMA + " - " + titulo, ModalityType.APPLICATION_MODAL);
        this.sesion = sesion;
    }

    /** Une el encabezado, el cuerpo y los botones, y ubica la pantalla sobre la ventana dueña. */
    protected void armar(String titulo, JComponent cuerpo, JComponent botones) {
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(Estilo.encabezado(titulo, sesion.getDescripcionUsuario()), BorderLayout.NORTH);
        raiz.add(cuerpo, BorderLayout.CENTER);
        raiz.add(botones, BorderLayout.SOUTH);
        setContentPane(raiz);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getOwner());
    }

    /**
     * Recuadro con los datos principales de la solicitud sobre la que se trabaja, para que el
     * actor confirme que es la correcta.
     */
    protected static JPanel grupoSolicitud(Solicitud s) {
        JPanel p = Estilo.grupo("Solicitud");
        int h = GridBagConstraints.HORIZONTAL;
        int n = GridBagConstraints.NONE;
        Estilo.poner(p, Estilo.etiqueta("Número", false), 0, 0, 1, 0, n);
        Estilo.poner(p, Estilo.soloLectura(String.valueOf(s.getId()), 5), 1, 0, 1, 0.1, h);
        Estilo.poner(p, Estilo.etiqueta("Cliente", false), 2, 0, 1, 0, n);
        Estilo.poner(p, Estilo.soloLectura(s.getCliente().getNombre(), 10), 3, 0, 1, 0.25, h);
        Estilo.poner(p, Estilo.etiqueta("Producto", false), 4, 0, 1, 0, n);
        Estilo.poner(p, Estilo.soloLectura(s.getProducto().getNombre(), 8), 5, 0, 1, 0.2, h);
        Estilo.poner(p, Estilo.etiqueta("Tipo", false), 6, 0, 1, 0, n);
        Estilo.poner(p, Estilo.soloLectura(s.getTipo() == null ? "Sin clasificar" : s.getTipo().getNombre(), 14),
                7, 0, 1, 0.45, h);
        Estilo.poner(p, Estilo.etiqueta("Estado", false), 0, 1, 1, 0, n);
        Estilo.poner(p, Estilo.soloLectura(String.valueOf(s.getEstadoActual()), 8), 1, 1, 1, 0.1, h);
        Estilo.poner(p, Estilo.etiqueta("Asunto", false), 2, 1, 1, 0, n);
        Estilo.poner(p, Estilo.soloLectura(s.getAsunto(), 40), 3, 1, 5, 1, h);
        return p;
    }

    /** Panel vertical con el margen de los cuerpos de las pantallas. */
    protected static JPanel cuerpoVertical() {
        JPanel cuerpo = new JPanel();
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
        cuerpo.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));
        return cuerpo;
    }

    /**
     * Primer día de un trimestre calendario: 0 es el trimestre actual, 1 el anterior y así.
     * Los reportes proponen por defecto el último trimestre completo.
     */
    protected static LocalDate inicioDeTrimestre(int atras) {
        LocalDate hoy = LocalDate.now();
        LocalDate actual = hoy.withDayOfMonth(1).minusMonths((hoy.getMonthValue() - 1) % 3L);
        return actual.minusMonths(3L * atras);
    }

    /**
     * Pide dónde guardar un archivo CSV. Si falta la extensión la agrega, y si el archivo ya
     * existe pide confirmación para reemplazarlo.
     *
     * @return el archivo elegido, o null si el usuario canceló
     */
    protected Path elegirArchivoParaGuardar(String nombreSugerido) {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Exportar el reporte");
        selector.setFileFilter(new FileNameExtensionFilter("Archivos CSV", "csv"));
        selector.setSelectedFile(new File(nombreSugerido));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        Path archivo = selector.getSelectedFile().toPath();
        if (!archivo.getFileName().toString().toLowerCase().endsWith(".csv")) {
            archivo = archivo.resolveSibling(archivo.getFileName() + ".csv");
        }
        if (Files.exists(archivo)
                && !Dialogos.confirmar(this, "El archivo " + archivo.getFileName() + " ya existe. ¿Lo reemplazo?")) {
            return null;
        }
        return archivo;
    }

    /**
     * Contenido de un reporte para exportarlo, tal como se ve en la pantalla: el título, la
     * línea con el período y los parámetros, y cada tabla con sus títulos de columna. Entre una
     * tabla y la otra queda una línea vacía.
     */
    protected static List<String[]> contenidoDeReporte(String titulo, String parametros, JTable... tablas) {
        List<String[]> contenido = new ArrayList<>();
        contenido.add(new String[] {titulo});
        contenido.add(new String[] {parametros});
        for (JTable tabla : tablas) {
            contenido.add(new String[] {""});
            String[] titulos = new String[tabla.getColumnCount()];
            for (int c = 0; c < titulos.length; c++) {
                // Los títulos en dos líneas tienen HTML: en el archivo van en una sola
                titulos[c] = tabla.getColumnName(c).replace("<br>", " ").replaceAll("<[^>]+>", "");
            }
            contenido.add(titulos);
            for (int f = 0; f < tabla.getRowCount(); f++) {
                String[] fila = new String[titulos.length];
                for (int c = 0; c < titulos.length; c++) {
                    Object valor = tabla.getValueAt(f, c);
                    fila[c] = valor == null ? "" : valor.toString();
                }
                contenido.add(fila);
            }
        }
        return contenido;
    }

    /** Ejecuta una acción mostrando los mensajes que correspondan si falla. */
    protected boolean ejecutar(Dialogos.Accion accion) {
        return Dialogos.ejecutar(this, accion);
    }

    /** Cierra la pantalla indicando que la operación se completó. */
    protected void terminar() {
        confirmada = true;
        dispose();
    }

    /**
     * Muestra la pantalla y espera a que se cierre.
     *
     * @return true si el usuario completó la operación, false si canceló
     */
    public boolean mostrar() {
        setVisible(true);
        return confirmada;
    }
}
