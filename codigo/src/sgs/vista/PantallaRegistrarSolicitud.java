package sgs.vista;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.time.LocalDateTime;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import sgs.controlador.ControladorRegistrarSolicitud;
import sgs.controlador.Sesion;
import sgs.modelo.Formatos;
import sgs.modelo.Modulo;
import sgs.modelo.Producto;
import sgs.modelo.Solicitante;
import sgs.modelo.Solicitud;
import sgs.modelo.catalogo.Canal;
import sgs.modelo.catalogo.Prioridad;
import sgs.modelo.catalogo.TipoSolicitud;

/**
 * CU01 Registrar solicitud, según el prototipo de la sección 13. Incluye la clasificación
 * opcional (alternativa 13.A), el solicitante nuevo (3.A), la continuación de una solicitud
 * cerrada (3.B) y el descarte de lo que no corresponde a soporte (10.A).
 */
public final class PantallaRegistrarSolicitud extends Pantalla {

    private static final long serialVersionUID = 1L;
    private static final String SIN_CLASIFICAR = "(sin clasificar)";
    private static final String SIN_DEFINIR = "(sin definir)";

    private final transient ControladorRegistrarSolicitud controlador;
    private final transient Solicitud origenFijo;
    private final JComboBox<Solicitante> cmbSolicitante = new JComboBox<>();
    private final JTextField txtCliente = Estilo.soloLectura("", 14);
    private final JComboBox<Producto> cmbProducto = new JComboBox<>();
    private final JRadioButton rbCorreo = new JRadioButton(Canal.CORREO.getNombre(), true);
    private final JRadioButton rbInterna = new JRadioButton(Canal.DETECCION_INTERNA.getNombre());
    private final JTextField txtFecha = new JTextField(LocalDateTime.now().format(Formatos.FECHA_HORA), 12);
    private final JTextField txtAsunto = new JTextField(40);
    private final JTextArea txtDescripcion = Estilo.areaDeTexto(4);
    private final JTextField txtOrigen = new JTextField(6);
    private final JComboBox<Object> cmbTipo = new JComboBox<>();
    private final JComboBox<Object> cmbModulo = new JComboBox<>();
    private final JComboBox<Object> cmbPrioridad = new JComboBox<>();
    private transient Solicitud registrada;

    /**
     * @param origen la solicitud cerrada de la que sale el tema nuevo, cuando la pantalla se abre
     *               desde la reapertura (CU07, alternativa 1.A.3.A), o null
     */
    public PantallaRegistrarSolicitud(Window duenio, Sesion sesion, Solicitud origen) {
        super(duenio, sesion, "Registrar solicitud");
        controlador = sesion.registrarSolicitud();
        origenFijo = origen;

        for (Solicitante s : controlador.getSolicitantes()) {
            cmbSolicitante.addItem(s);
        }
        for (Producto p : controlador.getProductos()) {
            cmbProducto.addItem(p);
        }
        cmbTipo.addItem(SIN_CLASIFICAR);
        for (TipoSolicitud t : TipoSolicitud.values()) {
            cmbTipo.addItem(t);
        }
        cmbPrioridad.addItem(SIN_CLASIFICAR);
        for (Prioridad p : Prioridad.values()) {
            cmbPrioridad.addItem(p);
        }
        cmbSolicitante.setSelectedIndex(-1);
        cmbSolicitante.addActionListener(e -> mostrarCliente());
        cmbProducto.addActionListener(e -> cargarModulos());
        ButtonGroup canal = new ButtonGroup();
        canal.add(rbCorreo);
        canal.add(rbInterna);
        if (origen != null) {
            cmbSolicitante.setSelectedItem(origen.getSolicitante());
            cmbProducto.setSelectedItem(origen.getProducto());
            txtOrigen.setText(String.valueOf(origen.getId()));
            txtOrigen.setEditable(false);
        }
        cargarModulos();

        JPanel cuerpo = cuerpoVertical();
        cuerpo.add(armarAlta());
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(armarDatos());
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(armarClasificacion());

        JButton cancelar = Estilo.boton("Cancelar");
        JButton descartar = Estilo.boton("Descartar...");
        JButton registrar = Estilo.boton("Registrar solicitud");
        descartar.setToolTipText("Registra el pedido como Descartada, con su motivo, porque no corresponde a soporte");
        descartar.setEnabled(origen == null);
        cancelar.addActionListener(e -> dispose());
        descartar.addActionListener(e -> descartar());
        registrar.addActionListener(e -> registrar());
        armar("Registrar solicitud", cuerpo, Estilo.botonera("* Datos obligatorios", cancelar, descartar, registrar));
        getRootPane().setDefaultButton(registrar);
    }

    /** Datos que completa el sistema: el número, el estado inicial y el responsable (pasos 11 y 12). */
    private JPanel armarAlta() {
        JPanel alta = Estilo.grupo("Datos que asigna el sistema");
        int h = GridBagConstraints.HORIZONTAL;
        int n = GridBagConstraints.NONE;
        Estilo.poner(alta, Estilo.etiqueta("Número", false), 0, 0, 1, 0, n);
        Estilo.poner(alta, Estilo.soloLectura("Se asigna al registrar", 14), 1, 0, 1, 0.3, h);
        Estilo.poner(alta, Estilo.etiqueta("Estado inicial", false), 2, 0, 1, 0, n);
        Estilo.poner(alta, Estilo.soloLectura("Nueva", 8), 3, 0, 1, 0.2, h);
        Estilo.poner(alta, Estilo.etiqueta("Responsable", false), 4, 0, 1, 0, n);
        Estilo.poner(alta, Estilo.soloLectura(sesion.getUsuario().getNombre(), 14), 5, 0, 1, 0.3, h);
        return alta;
    }

    private JPanel armarDatos() {
        JPanel datos = Estilo.grupo("Datos de la solicitud");
        int h = GridBagConstraints.HORIZONTAL;
        int n = GridBagConstraints.NONE;
        JButton nuevoSolicitante = new JButton("Nuevo solicitante...");
        nuevoSolicitante.addActionListener(e -> nuevoSolicitante());
        nuevoSolicitante.setEnabled(origenFijo == null);
        cmbSolicitante.setEnabled(origenFijo == null);
        JPanel filaSolicitante = new JPanel(new BorderLayout(8, 0));
        filaSolicitante.add(cmbSolicitante, BorderLayout.CENTER);
        filaSolicitante.add(nuevoSolicitante, BorderLayout.EAST);
        Estilo.poner(datos, Estilo.etiqueta("Solicitante", true), 0, 0, 1, 0, n);
        Estilo.poner(datos, filaSolicitante, 1, 0, 3, 1, h);

        JPanel filaCliente = new JPanel(new BorderLayout(8, 0));
        filaCliente.add(txtCliente, BorderLayout.CENTER);
        filaCliente.add(Estilo.nota("Se toma del solicitante"), BorderLayout.EAST);
        Estilo.poner(datos, Estilo.etiqueta("Cliente", false), 0, 1, 1, 0, n);
        Estilo.poner(datos, filaCliente, 1, 1, 1, 0.5, h);
        Estilo.poner(datos, Estilo.etiqueta("Producto", true), 2, 1, 1, 0, n);
        Estilo.poner(datos, cmbProducto, 3, 1, 1, 0.5, h);

        Estilo.poner(datos, Estilo.etiqueta("Canal", true), 0, 2, 1, 0, n);
        Estilo.poner(datos, Estilo.fila(rbCorreo, rbInterna), 1, 2, 1, 0.5, h);
        Estilo.poner(datos, Estilo.etiqueta("Fecha de recepción", true), 2, 2, 1, 0, n);
        Estilo.poner(datos, txtFecha, 3, 2, 1, 0.5, h);

        Estilo.poner(datos, Estilo.etiqueta("Asunto", true), 0, 3, 1, 0, n);
        Estilo.poner(datos, txtAsunto, 1, 3, 3, 1, h);
        Estilo.ponerEtiquetaArriba(datos, Estilo.etiqueta("Descripción", false), 0, 4);
        Estilo.ponerArea(datos, Estilo.conDesplazamiento(txtDescripcion), 1, 4, 3, 1);

        Estilo.poner(datos, Estilo.etiqueta("Continúa la solicitud", false), 0, 5, 1, 0, n);
        Estilo.poner(datos, Estilo.fila(txtOrigen, Estilo.nota(origenFijo == null
                ? "Número de una solicitud cerrada, si el cliente plantea un tema nuevo relacionado con ella"
                : "Tema nuevo planteado sobre la solicitud cerrada " + origenFijo.getId() + ": quedan vinculadas")),
                1, 5, 3, 1, h);
        return datos;
    }

    /** Clasificación opcional (CU03 extiende CU01 en el paso 13). */
    private JPanel armarClasificacion() {
        JPanel clasif = Estilo.grupo("Clasificación (opcional)");
        int h = GridBagConstraints.HORIZONTAL;
        int n = GridBagConstraints.NONE;
        Estilo.poner(clasif, Estilo.etiqueta("Tipo de solicitud", false), 0, 0, 1, 0, n);
        Estilo.poner(clasif, cmbTipo, 1, 0, 1, 0.4, h);
        Estilo.poner(clasif, Estilo.etiqueta("Módulo", false), 2, 0, 1, 0, n);
        Estilo.poner(clasif, cmbModulo, 3, 0, 1, 0.3, h);
        Estilo.poner(clasif, Estilo.etiqueta("Prioridad", false), 4, 0, 1, 0, n);
        Estilo.poner(clasif, cmbPrioridad, 5, 0, 1, 0.3, h);
        Estilo.poner(clasif, Estilo.nota("Si se clasifica al registrar, hacen falta el tipo y la prioridad. "
                + "El módulo puede quedar sin definir."), 0, 1, 6, 1, h);
        return clasif;
    }

    private void mostrarCliente() {
        Solicitante s = (Solicitante) cmbSolicitante.getSelectedItem();
        txtCliente.setText(s == null ? "" : s.getCliente().getNombre());
    }

    /** Los módulos dependen del producto elegido. */
    private void cargarModulos() {
        cmbModulo.removeAllItems();
        cmbModulo.addItem(SIN_DEFINIR);
        Producto p = (Producto) cmbProducto.getSelectedItem();
        if (p != null) {
            for (Modulo m : p.getModulos()) {
                cmbModulo.addItem(m);
            }
        }
    }

    /** Alternativa 3.A: el solicitante no existe y se registra antes de seguir. */
    private void nuevoSolicitante() {
        PantallaNuevoSolicitante pantalla = new PantallaNuevoSolicitante(this, sesion, controlador);
        if (pantalla.mostrar()) {
            Solicitante nuevo = pantalla.getSolicitante();
            cmbSolicitante.addItem(nuevo);
            cmbSolicitante.setSelectedItem(nuevo);
        }
    }

    private Canal getCanal() {
        return rbInterna.isSelected() ? Canal.DETECCION_INTERNA : Canal.CORREO;
    }

    /** Pasos 9 a 14: valida, registra en estado Nueva y, si se completó, clasifica. */
    private void registrar() {
        ejecutar(() -> {
            LocalDateTime fecha = Entrada.fechaHora(txtFecha.getText(), "La fecha de recepción");
            Solicitud origen = origenFijo;
            Integer numeroOrigen = Entrada.entero(txtOrigen.getText(), "El número de la solicitud que continúa");
            if (origen == null && numeroOrigen != null) {
                origen = controlador.buscarSolicitudOrigen(numeroOrigen);
            }
            Object tipo = cmbTipo.getSelectedItem();
            Object prioridad = cmbPrioridad.getSelectedItem();
            Object modulo = cmbModulo.getSelectedItem();
            registrada = controlador.registrar((Solicitante) cmbSolicitante.getSelectedItem(),
                    (Producto) cmbProducto.getSelectedItem(), getCanal(), fecha, txtAsunto.getText(),
                    txtDescripcion.getText(), tipo instanceof TipoSolicitud t ? t : null,
                    prioridad instanceof Prioridad p ? p : null, modulo instanceof Modulo m ? m : null, origen);
            String mensaje = "Solicitud " + registrada.getId() + " registrada en estado Nueva.";
            if (registrada.getTipo() == null) {
                mensaje += "\nQueda sin clasificar hasta que se analice.";
            }
            if (origen != null) {
                mensaje += "\nQuedó vinculada como continuación de la solicitud " + origen.getId() + ".";
            }
            Dialogos.informacion(this, mensaje);
            terminar();
        });
    }

    /** Alternativa 10.A: el pedido no corresponde a soporte. Queda registrado como Descartada. */
    private void descartar() {
        String motivo = Dialogos.pedirTexto(this, "¿Por qué no corresponde a soporte? El pedido queda registrado "
                + "como Descartada con este motivo.");
        if (motivo == null) {
            return;
        }
        ejecutar(() -> {
            LocalDateTime fecha = Entrada.fechaHora(txtFecha.getText(), "La fecha de recepción");
            registrada = controlador.registrarDescartada((Solicitante) cmbSolicitante.getSelectedItem(),
                    (Producto) cmbProducto.getSelectedItem(), getCanal(), fecha, txtAsunto.getText(),
                    txtDescripcion.getText(), motivo);
            Dialogos.informacion(this, "Solicitud " + registrada.getId() + " registrada como Descartada.");
            terminar();
        });
    }
}
