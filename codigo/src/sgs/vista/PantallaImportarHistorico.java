package sgs.vista;

import java.awt.GridBagConstraints;
import java.awt.Window;
import java.io.File;
import java.nio.file.Path;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import sgs.controlador.ControladorImportarHistorico;
import sgs.controlador.Sesion;
import sgs.modelo.Formatos;
import sgs.modelo.Importacion;
import sgs.modelo.excepcion.ReglaNegocioException;

/**
 * CU02 Importar histórico de solicitudes (RF02). El actor elige el CSV, el sistema lo revisa y
 * muestra qué va a importar, qué omite y qué rechaza, y recién cuando el actor confirma con
 * "Importar" registra las solicitudes.
 */
public final class PantallaImportarHistorico extends Pantalla {

    private static final long serialVersionUID = 1L;

    private final transient ControladorImportarHistorico controlador;
    private transient Importacion importacion;

    private final JTextField txtArchivo = new JTextField(40);
    private final JTextField txtRegistros = Estilo.soloLectura("", 6);
    private final JTextField txtOmitidas = Estilo.soloLectura("", 6);
    private final JTextField txtAImportar = Estilo.soloLectura("", 6);
    private final JTextField txtSinCliente = Estilo.soloLectura("", 6);
    private final JTextField txtRechazadas = Estilo.soloLectura("", 6);
    private final JTextArea areaRechazos = Estilo.areaDeTexto(8);
    private final JButton importar = Estilo.boton("Importar");

    public PantallaImportarHistorico(Window duenio, Sesion sesion) {
        super(duenio, sesion, "Importar histórico");
        this.controlador = sesion.importarHistorico();
        int h = GridBagConstraints.HORIZONTAL;
        int n = GridBagConstraints.NONE;

        // Pasos 2 y 3: el archivo
        JButton examinar = Estilo.boton("Examinar...");
        JButton revisar = Estilo.boton("Revisar");
        examinar.addActionListener(e -> examinar());
        revisar.addActionListener(e -> revisar());
        JPanel archivo = Estilo.grupo("Archivo");
        Estilo.poner(archivo, Estilo.etiqueta("Archivo CSV", true), 0, 0, 1, 0, n);
        Estilo.poner(archivo, Estilo.fila(txtArchivo, examinar, revisar), 1, 0, 1, 1, h);
        Estilo.poner(archivo, Estilo.nota("El CSV que exporta la herramienta actual, con el correo del solicitante"),
                1, 1, 1, 1, h);

        // Paso 5: lo que encontró la revisión
        JPanel revision = Estilo.grupo("Revisión del archivo");
        Estilo.poner(revision, Estilo.etiqueta("Registros en el archivo", false), 0, 0, 1, 0, n);
        Estilo.poner(revision, txtRegistros, 1, 0, 1, 0.2, h);
        Estilo.poner(revision, Estilo.etiqueta("A importar", false), 2, 0, 1, 0, n);
        Estilo.poner(revision, txtAImportar, 3, 0, 1, 0.2, h);
        Estilo.poner(revision, Estilo.etiqueta("Sin cliente identificado", false), 4, 0, 1, 0, n);
        Estilo.poner(revision, txtSinCliente, 5, 0, 1, 0.2, h);
        Estilo.poner(revision, Estilo.etiqueta("Ya importados (se omiten)", false), 0, 1, 1, 0, n);
        Estilo.poner(revision, txtOmitidas, 1, 1, 1, 0.2, h);
        Estilo.poner(revision, Estilo.etiqueta("Rechazados", false), 2, 1, 1, 0, n);
        Estilo.poner(revision, txtRechazadas, 3, 1, 1, 0.2, h);
        areaRechazos.setEditable(false);
        Estilo.ponerEtiquetaArriba(revision, Estilo.etiqueta("Registros rechazados y motivo", false), 0, 2);
        Estilo.ponerArea(revision, Estilo.conDesplazamiento(areaRechazos), 0, 3, 6, 1);

        JPanel cuerpo = cuerpoVertical();
        cuerpo.add(archivo);
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(revision);

        JButton cancelar = Estilo.boton("Cancelar");
        cancelar.addActionListener(e -> dispose());
        importar.addActionListener(e -> importar());
        importar.setEnabled(false);
        armar("Importar histórico de solicitudes", cuerpo,
                Estilo.botonera("Se importan las solicitudes cerradas. Las abiertas entran cuando se cierren",
                        cancelar, importar));
        getRootPane().setDefaultButton(revisar);
    }

    private void examinar() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Elegir el archivo del histórico");
        selector.setFileFilter(new FileNameExtensionFilter("Archivos CSV", "csv"));
        if (!txtArchivo.getText().isBlank()) {
            selector.setCurrentDirectory(new File(txtArchivo.getText().trim()).getParentFile());
        }
        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtArchivo.setText(selector.getSelectedFile().getAbsolutePath());
            revisar();
        }
    }

    /** Pasos 4 y 5: revisa el archivo y muestra qué va a pasar, sin guardar nada. */
    private void revisar() {
        importacion = null;
        mostrar(null);
        String ruta = txtArchivo.getText().trim();
        ejecutar(() -> {
            if (Formatos.estaVacio(ruta)) {
                throw new ReglaNegocioException("Elegí el archivo CSV del histórico");
            }
            importacion = controlador.revisar(Path.of(ruta));
            mostrar(importacion);
        });
    }

    /** Pasos 7 a 9: el actor confirma, se registran las solicitudes y se muestra el resumen. */
    private void importar() {
        if (importacion == null) {
            return;
        }
        Importacion confirmada = importacion;
        if (ejecutar(() -> controlador.importar(confirmada))) {
            Dialogos.informacion(this, confirmada.resumen());
            terminar();
        } else {
            // Si falló, la transacción se deshizo y hay que revisar el archivo otra vez
            importacion = null;
            importar.setEnabled(false);
        }
    }

    private void mostrar(Importacion revisada) {
        boolean hay = revisada != null;
        txtRegistros.setText(hay ? String.valueOf(revisada.getRegistros()) : "");
        txtAImportar.setText(hay ? String.valueOf(revisada.getSolicitudes().size()) : "");
        txtSinCliente.setText(hay ? String.valueOf(revisada.getSinCliente()) : "");
        txtOmitidas.setText(hay ? String.valueOf(revisada.getOmitidas()) : "");
        txtRechazadas.setText(hay ? String.valueOf(revisada.getRechazos().size()) : "");
        areaRechazos.setText(hay ? String.join("\n", revisada.getRechazos()) : "");
        areaRechazos.setCaretPosition(0);
        importar.setEnabled(hay && !revisada.getSolicitudes().isEmpty());
    }
}
