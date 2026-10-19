package sgs.prueba;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;

/**
 * Componente de prueba que maneja las pantallas reales del sistema como lo haría un usuario:
 * busca los campos por su etiqueta, escribe, elige opciones, presiona botones y lee los mensajes.
 * También pinta las ventanas en un PNG para usarlas como capturas del informe.
 * <p>
 * Las acciones que pueden abrir una ventana modal se ejecutan con invokeLater, porque una
 * ventana modal detiene el hilo de eventos hasta que se cierra, y este hilo espera a que aparezca.
 */
final class Manejador {

    private static final long ESPERA_MAXIMA = 15_000;
    private static final int ESCALA = 2;
    private static final int BARRA = 26;

    private final File carpetaCapturas;

    Manejador(File carpetaCapturas) {
        this.carpetaCapturas = carpetaCapturas;
    }

    // ------------------------------------------------------------------
    // Ventanas
    // ------------------------------------------------------------------

    /** Espera a que se muestre la ventana cuyo título termina con el texto indicado. */
    Window esperarVentana(String titulo) {
        return esperar("la ventana " + titulo, () -> {
            for (Window w : Window.getWindows()) {
                if (w.isShowing() && titulo(w).endsWith(titulo) && !esMensaje(w)) {
                    return w;
                }
            }
            return null;
        });
    }

    /** Espera a que se muestre la ventana principal del sistema. */
    JFrame esperarPrincipal() {
        return esperar("la ventana principal", () -> {
            for (Window w : Window.getWindows()) {
                if (w.isShowing() && w instanceof JFrame f) {
                    return f;
                }
            }
            return null;
        });
    }

    /** Espera a que aparezca un mensaje (un JOptionPane) y lo devuelve. */
    JDialog esperarMensaje() {
        return esperar("un mensaje", () -> {
            for (Window w : Window.getWindows()) {
                if (w.isShowing() && esMensaje(w)) {
                    return (JDialog) w;
                }
            }
            return null;
        });
    }

    /** Texto del mensaje, sin los saltos de línea. */
    String textoMensaje(JDialog mensaje) {
        return enEDT(() -> {
            JOptionPane panel = buscar(mensaje, JOptionPane.class, p -> true);
            Object contenido = panel.getMessage();
            if (contenido instanceof Object[] partes) {
                contenido = partes[0];
            }
            return String.valueOf(contenido).replace('\n', ' ');
        });
    }

    /** Presiona un botón del mensaje y espera a que se cierre. */
    void responder(JDialog mensaje, String boton) {
        presionar(mensaje, boton);
        esperarCierre(mensaje);
    }

    /** Presiona el único botón de un mensaje informativo y espera a que se cierre. */
    void aceptar(JDialog mensaje) {
        JButton boton = enEDT(() -> buscar(mensaje, JButton.class, b -> true));
        SwingUtilities.invokeLater(boton::doClick);
        esperarCierre(mensaje);
    }

    void esperarCierre(Window w) {
        esperar("que se cierre " + titulo(w), () -> w.isShowing() ? null : Boolean.TRUE);
    }

    boolean estaAbierta(Window w) {
        return enEDT(w::isShowing);
    }

    private static boolean esMensaje(Window w) {
        return w instanceof JDialog d && buscarOpcional(d, JOptionPane.class, p -> true) != null;
    }

    private static String titulo(Window w) {
        if (w instanceof JDialog d) {
            return d.getTitle() == null ? "" : d.getTitle();
        }
        if (w instanceof JFrame f) {
            return f.getTitle() == null ? "" : f.getTitle();
        }
        return "";
    }

    // ------------------------------------------------------------------
    // Campos y botones
    // ------------------------------------------------------------------

    /** El recuadro con el título indicado, para buscar dentro de él. */
    JComponent grupo(Container raiz, String titulo) {
        return enEDT(() -> buscar(raiz, JComponent.class, c -> titulo.equals(tituloDeBorde(c.getBorder()))));
    }

    private static String tituloDeBorde(Border borde) {
        if (borde instanceof TitledBorder t) {
            return t.getTitle();
        }
        if (borde instanceof CompoundBorder c) {
            return tituloDeBorde(c.getOutsideBorder());
        }
        return null;
    }

    /**
     * El campo que acompaña a una etiqueta: el componente que está a su derecha en la misma
     * fila del GridBagLayout o, si es un panel, el primero del tipo pedido que contiene.
     */
    <T extends Component> T campo(Container raiz, String etiqueta, Class<T> tipo) {
        return campoJunto(raiz, etiqueta, 1, tipo);
    }

    /**
     * El componente que está a cierta cantidad de columnas a la derecha de una etiqueta o de una
     * casilla, en la misma fila. Por ejemplo, en "Período [desde] al [hasta]" el hasta está a 3.
     */
    <T extends Component> T campoJunto(Container raiz, String etiqueta, int columnas, Class<T> tipo) {
        return enEDT(() -> {
            JComponent l = buscar(raiz, JComponent.class, x -> etiqueta.equals(texto(x)) || (etiqueta + " *").equals(texto(x)));
            Container padre = l.getParent();
            GridBagLayout layout = (GridBagLayout) padre.getLayout();
            GridBagConstraints gl = layout.getConstraints(l);
            for (Component c : padre.getComponents()) {
                GridBagConstraints gc = layout.getConstraints(c);
                if (gc.gridy == gl.gridy && gc.gridx == gl.gridx + columnas) {
                    if (tipo.isInstance(c)) {
                        return tipo.cast(c);
                    }
                    if (c instanceof Container contenedor) {
                        return buscar(contenedor, tipo, x -> true);
                    }
                }
            }
            throw new IllegalStateException("No hay un campo junto a " + etiqueta);
        });
    }

    private static String texto(JComponent c) {
        if (c instanceof JLabel l) {
            return l.getText();
        }
        if (c instanceof AbstractButton b && !(c instanceof JButton)) {
            return b.getText();
        }
        return null;
    }

    /** Escribe en el segundo campo de una fila de período: "Período [desde] al [hasta]". */
    void escribirSegundo(Container raiz, String etiqueta, String texto) {
        escribirJunto(raiz, etiqueta, 3, texto);
    }

    void escribirJunto(Container raiz, String etiqueta, int columnas, String texto) {
        JTextComponent campo = campoJunto(raiz, etiqueta, columnas, JTextComponent.class);
        enEDT(() -> {
            campo.setText(texto);
            return null;
        });
    }

    /** La tabla visible número n de la ventana, en el orden en que aparecen. */
    JTable tabla(Container raiz, int n) {
        return enEDT(() -> {
            List<JTable> tablas = new ArrayList<>();
            juntar(raiz, JTable.class, tablas);
            return tablas.get(n);
        });
    }

    private static <T extends Component> void juntar(Container raiz, Class<T> tipo, List<T> lista) {
        for (Component c : raiz.getComponents()) {
            if (tipo.isInstance(c) && c.isVisible()) {
                lista.add(tipo.cast(c));
            }
            if (c instanceof Container hijo && c.isVisible()) {
                juntar(hijo, tipo, lista);
            }
        }
    }

    /**
     * Lee un campo esperando a que tenga el valor indicado, porque la pantalla se actualiza
     * después de que se cierra la ventana del caso de uso. Si no llega a ese valor, devuelve el
     * que tenga, y la verificación lo marca como falla.
     */
    String leerEsperando(Container raiz, String etiqueta, String esperado) {
        long limite = System.currentTimeMillis() + 5_000;
        String valor = leer(raiz, etiqueta);
        while (!esperado.equals(valor) && System.currentTimeMillis() < limite) {
            pausa(100);
            valor = leer(raiz, etiqueta);
        }
        return valor;
    }

    void escribir(Container raiz, String etiqueta, String texto) {
        JTextComponent campo = campo(raiz, etiqueta, JTextComponent.class);
        enEDT(() -> {
            campo.setText(texto);
            return null;
        });
    }

    /** Elige en el combo de la etiqueta la opción cuyo texto empieza con el indicado. */
    void elegir(Container raiz, String etiqueta, String opcion) {
        @SuppressWarnings("unchecked")
        JComboBox<Object> combo = campo(raiz, etiqueta, JComboBox.class);
        enEDT(() -> {
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (String.valueOf(combo.getItemAt(i)).startsWith(opcion)) {
                    combo.setSelectedIndex(i);
                    return null;
                }
            }
            throw new IllegalStateException("El combo " + etiqueta + " no tiene la opción " + opcion);
        });
    }

    /** Marca una opción (radio o casilla) por su texto. */
    void marcar(Container raiz, String texto) {
        AbstractButton opcion = enEDT(() -> buscar(raiz, AbstractButton.class, b -> texto.equals(b.getText())));
        enEDT(() -> {
            if (!opcion.isSelected()) {
                opcion.doClick();
            }
            return null;
        });
    }

    /** Presiona un botón por su texto. Si el botón abre una ventana modal, no se queda esperando. */
    void presionar(Container raiz, String texto) {
        JButton boton = enEDT(() -> buscar(raiz, JButton.class, b -> texto.equals(b.getText())));
        boolean habilitado = enEDT(boton::isEnabled);
        if (!habilitado) {
            throw new IllegalStateException("El botón " + texto + " está deshabilitado");
        }
        SwingUtilities.invokeLater(boton::doClick);
    }

    boolean estaHabilitado(Container raiz, String textoBoton) {
        return enEDT(() -> buscar(raiz, AbstractButton.class, b -> textoBoton.equals(b.getText())).isEnabled());
    }

    /**
     * Selecciona en la primera tabla la fila cuya primera columna tiene el valor indicado. Espera
     * a que aparezca, porque la tabla se vuelve a llenar después de cerrar otras ventanas.
     */
    void seleccionarFila(Container raiz, Object valor) {
        esperar("la fila " + valor, () -> {
            JTable tabla = buscar(raiz, JTable.class, t -> true);
            for (int i = 0; i < tabla.getRowCount(); i++) {
                if (String.valueOf(tabla.getValueAt(i, 0)).equals(String.valueOf(valor))) {
                    tabla.setRowSelectionInterval(i, i);
                    return Boolean.TRUE;
                }
            }
            return null;
        });
    }

    /** Texto de un componente con texto, buscado por su etiqueta. */
    String leer(Container raiz, String etiqueta) {
        JTextComponent campo = campo(raiz, etiqueta, JTextComponent.class);
        return enEDT(campo::getText);
    }

    /** Busca un texto en las etiquetas visibles, por ejemplo una ayuda o un total. */
    String etiquetaQueEmpieza(Container raiz, String inicio) {
        return enEDT(() -> buscar(raiz, JLabel.class, l -> l.getText() != null && l.getText().replaceAll("<[^>]+>", "")
                .startsWith(inicio)).getText().replaceAll("<[^>]+>", ""));
    }

    // ------------------------------------------------------------------
    // Capturas
    // ------------------------------------------------------------------

    /**
     * Pinta las ventanas indicadas, una sobre otra en el orden dado y en su posición real, en
     * un PNG al doble de resolución. Cada ventana lleva una barra con su título, como la que
     * dibuja el sistema operativo, porque la pantalla virtual de la prueba no tiene decoraciones.
     */
    void capturar(String archivo, Window... ventanas) {
        capturarFranja(archivo, 0, Integer.MAX_VALUE, ventanas);
    }

    /**
     * Como capturar(), pero guarda solo una franja horizontal de la imagen, entre dos alturas
     * medidas desde el borde de arriba, en la misma unidad que las ventanas. Sirve para dejar
     * en la captura la parte de la pantalla que importa.
     */
    void capturarFranja(String archivo, int desde, int hasta, Window... ventanas) {
        pausa(500);
        enEDT(() -> {
            Rectangle total = null;
            for (Window w : ventanas) {
                Rectangle r = w.getBounds();
                r = new Rectangle(r.x, r.y - BARRA, r.width, r.height + BARRA);
                total = total == null ? r : total.union(r);
            }
            BufferedImage img = new BufferedImage(total.width * ESCALA, total.height * ESCALA, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.scale(ESCALA, ESCALA);
            for (Window w : ventanas) {
                Rectangle r = w.getBounds();
                Graphics2D gv = (Graphics2D) g.create(r.x - total.x, r.y - BARRA - total.y, r.width, r.height + BARRA);
                gv.setColor(new Color(214, 219, 226));
                gv.fillRect(0, 0, r.width, BARRA);
                gv.setColor(new Color(40, 40, 40));
                gv.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                gv.drawString(titulo(w), 10, 17);
                gv.translate(0, BARRA);
                ((RootPaneContainer) w).getRootPane().printAll(gv);
                gv.translate(0, -BARRA);
                gv.setColor(new Color(150, 156, 164));
                gv.drawRect(0, 0, r.width - 1, r.height + BARRA - 1);
                gv.dispose();
            }
            g.dispose();
            int arriba = Math.max(0, desde) * ESCALA;
            int abajo = (int) Math.min(img.getHeight(), (long) hasta * ESCALA);
            BufferedImage franja = img.getSubimage(0, arriba, img.getWidth(), abajo - arriba);
            try {
                ImageIO.write(franja, "png", new File(carpetaCapturas, archivo));
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo guardar " + archivo, e);
            }
            return null;
        });
        System.out.println("  captura: " + archivo);
    }

    /** Alto, en la unidad de las ventanas, desde el borde de arriba de la primera hasta el de abajo de la otra. */
    int altoHasta(Window primera, Window otra) {
        return enEDT(() -> otra.getY() + otra.getHeight() - (primera.getY() - BARRA));
    }

    /** Alto desde el borde de arriba de la primera ventana hasta el borde de arriba de la otra. */
    int altoHastaElComienzo(Window primera, Window otra) {
        return enEDT(() -> otra.getY() - BARRA - (primera.getY() - BARRA));
    }

    /** Alto desde el borde de arriba de la ventana hasta el final de una fila de una tabla. */
    int altoHastaLaFila(Window ventana, JTable tabla, int fila) {
        return enEDT(() -> {
            Rectangle celda = tabla.getCellRect(fila, 0, true);
            Point abajo = SwingUtilities.convertPoint(tabla, 0, celda.y + celda.height, ventana);
            return abajo.y + BARRA;
        });
    }

    // ------------------------------------------------------------------
    // Ayudas
    // ------------------------------------------------------------------

    static void pausa(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Repite la búsqueda hasta que devuelve algo o se agota la espera. */
    private <T> T esperar(String que, Callable<T> busqueda) {
        long limite = System.currentTimeMillis() + ESPERA_MAXIMA;
        while (System.currentTimeMillis() < limite) {
            T encontrado = enEDT(busqueda);
            if (encontrado != null) {
                pausa(150);
                return encontrado;
            }
            pausa(100);
        }
        throw new IllegalStateException("Se agotó la espera de " + que);
    }

    /** Ejecuta en el hilo de eventos de Swing y devuelve el resultado. */
    static <T> T enEDT(Callable<T> accion) {
        List<T> resultado = new ArrayList<>();
        List<Exception> error = new ArrayList<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    resultado.add(accion.call());
                } catch (Exception e) {
                    error.add(e);
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        }
        if (!error.isEmpty()) {
            throw error.get(0) instanceof RuntimeException r ? r : new IllegalStateException(error.get(0));
        }
        return resultado.get(0);
    }

    /** Búsqueda recursiva en el árbol de componentes. Falla si no lo encuentra. */
    static <T extends Component> T buscar(Container raiz, Class<T> tipo, Predicate<T> condicion) {
        T encontrado = buscarOpcional(raiz, tipo, condicion);
        if (encontrado == null) {
            throw new IllegalStateException("No se encontró un " + tipo.getSimpleName() + " con esa condición");
        }
        return encontrado;
    }

    static <T extends Component> T buscarOpcional(Container raiz, Class<T> tipo, Predicate<T> condicion) {
        for (Component c : raiz.getComponents()) {
            if (tipo.isInstance(c) && c.isVisible() && condicion.test(tipo.cast(c))) {
                return tipo.cast(c);
            }
            if (c instanceof Container hijo) {
                T encontrado = buscarOpcional(hijo, tipo, condicion);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
    }
}
