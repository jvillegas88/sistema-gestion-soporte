package sgs.vista;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import sgs.controlador.ControladorEsperaEstado;
import sgs.controlador.Sesion;
import sgs.modelo.Formatos;
import sgs.modelo.RegistroSolicitud;
import sgs.modelo.Solicitud;
import sgs.modelo.catalogo.Rol;

/**
 * Una solicitud abierta, con sus datos, su historial completo y las acciones de los casos de
 * uso que trabajan sobre ella. Qué acciones se habilitan depende del estado, según el diagrama
 * de estados de la sección 10, y del rol del usuario (RNF06).
 */
public final class PantallaSolicitud extends Pantalla {

    private static final long serialVersionUID = 1L;

    private final transient ControladorEsperaEstado controlador;
    private transient Solicitud solicitud;

    private final JTextField txtNumero = Estilo.soloLectura("", 6);
    private final JTextField txtEstado = Estilo.soloLectura("", 12);
    private final JTextField txtResponsable = Estilo.soloLectura("", 14);
    private final JTextField txtCliente = Estilo.soloLectura("", 12);
    private final JTextField txtSolicitante = Estilo.soloLectura("", 22);
    private final JTextField txtProducto = Estilo.soloLectura("", 10);
    private final JTextField txtModulo = Estilo.soloLectura("", 12);
    private final JTextField txtTipo = Estilo.soloLectura("", 16);
    private final JTextField txtPrioridad = Estilo.soloLectura("", 10);
    private final JTextField txtCanal = Estilo.soloLectura("", 12);
    private final JTextField txtRecibida = Estilo.soloLectura("", 14);
    private final JTextField txtTiempo = Estilo.soloLectura("", 8);
    private final JTextField txtVinculo = Estilo.soloLectura("", 40);
    private final JTextField txtAsunto = Estilo.soloLectura("", 40);
    private final JTextArea txtDescripcion = Estilo.areaDeTexto(3);
    private final JLabel lblSituacion = Estilo.nota(" ");

    private final DefaultTableModel modeloHistorial = Estilo.modeloTabla("Fecha", "Registro", "Integrante", "Detalle");
    private final JTable tablaHistorial = Estilo.tabla(modeloHistorial, new int[] {125, 125, 125, 560},
            new int[] {Estilo.IZQUIERDA, Estilo.IZQUIERDA, Estilo.IZQUIERDA, Estilo.IZQUIERDA}, false);

    private final JButton btnClasificar = Estilo.boton("Clasificar...");
    private final JButton btnIntervencion = Estilo.boton("Registrar intervención...");
    private final JButton btnAbrirEspera = Estilo.boton("Abrir espera...");
    private final JButton btnTerminarEspera = Estilo.boton("Terminar espera");
    private final JButton btnHito = Estilo.boton("Registrar hito...");
    private final JButton btnCerrar = Estilo.boton("Cerrar solicitud...");
    private final JButton btnReabrir = Estilo.boton("Reabrir...");

    /**
     * Lee la solicitud y crea la pantalla. Si no se puede leer, avisa y devuelve null.
     */
    public static PantallaSolicitud crear(Window duenio, Sesion sesion, int numero) {
        ControladorEsperaEstado controlador = sesion.esperaEstado();
        if (!Dialogos.ejecutar(duenio, () -> controlador.abrirSolicitud(numero))) {
            return null;
        }
        return new PantallaSolicitud(duenio, sesion, controlador);
    }

    private PantallaSolicitud(Window duenio, Sesion sesion, ControladorEsperaEstado controlador) {
        super(duenio, sesion, "Solicitud " + controlador.getSolicitud().getId());
        this.controlador = controlador;
        this.solicitud = controlador.getSolicitud();

        JPanel cuerpo = new JPanel();
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
        cuerpo.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));
        cuerpo.add(armarDatos());
        cuerpo.add(Box.createVerticalStrut(8));
        JPanel historial = Estilo.grupo("Historial: todo lo registrado sobre la solicitud, del más antiguo al más reciente");
        Estilo.ponerArea(historial, Estilo.tablaConDesplazamiento(tablaHistorial, 10), 0, 0, 1, 1);
        cuerpo.add(historial);

        btnClasificar.addActionListener(e -> accion(PantallaClasificar.crear(this, sesion, solicitud.getId())));
        btnIntervencion.addActionListener(e -> accion(PantallaRegistrarIntervencion.crear(this, sesion, solicitud.getId())));
        btnAbrirEspera.addActionListener(e -> accion(PantallaAbrirEspera.crear(this, sesion, solicitud.getId())));
        btnTerminarEspera.addActionListener(e -> terminarEspera());
        btnHito.addActionListener(e -> accion(PantallaRegistrarHito.crear(this, sesion, solicitud.getId())));
        btnCerrar.addActionListener(e -> accion(PantallaCerrarSolicitud.crear(this, sesion, solicitud.getId())));
        btnReabrir.addActionListener(e -> accion(PantallaReabrir.crear(this, sesion, solicitud.getId())));
        JButton volver = Estilo.boton("Volver");
        volver.addActionListener(e -> dispose());

        JPanel abajo = new JPanel(new BorderLayout());
        JPanel situacion = new JPanel(new BorderLayout());
        situacion.setBorder(BorderFactory.createEmptyBorder(8, 14, 0, 14));
        situacion.add(lblSituacion, BorderLayout.WEST);
        abajo.add(situacion, BorderLayout.NORTH);
        abajo.add(Estilo.botonera(null, btnClasificar, btnIntervencion, btnAbrirEspera, btnTerminarEspera, btnHito,
                btnCerrar, btnReabrir, volver), BorderLayout.SOUTH);

        mostrarDatos();
        armar("Solicitud " + solicitud.getId(), cuerpo, abajo);
    }

    private JPanel armarDatos() {
        JPanel d = Estilo.grupo("Datos de la solicitud");
        int h = GridBagConstraints.HORIZONTAL;
        int n = GridBagConstraints.NONE;
        Estilo.poner(d, Estilo.etiqueta("Número", false), 0, 0, 1, 0, n);
        Estilo.poner(d, txtNumero, 1, 0, 1, 0.2, h);
        Estilo.poner(d, Estilo.etiqueta("Estado", false), 2, 0, 1, 0, n);
        Estilo.poner(d, txtEstado, 3, 0, 1, 0.4, h);
        Estilo.poner(d, Estilo.etiqueta("Responsable", false), 4, 0, 1, 0, n);
        Estilo.poner(d, txtResponsable, 5, 0, 1, 0.4, h);

        Estilo.poner(d, Estilo.etiqueta("Cliente", false), 0, 1, 1, 0, n);
        Estilo.poner(d, txtCliente, 1, 1, 1, 0.2, h);
        Estilo.poner(d, Estilo.etiqueta("Solicitante", false), 2, 1, 1, 0, n);
        Estilo.poner(d, txtSolicitante, 3, 1, 1, 0.4, h);
        Estilo.poner(d, Estilo.etiqueta("Producto", false), 4, 1, 1, 0, n);
        Estilo.poner(d, txtProducto, 5, 1, 1, 0.4, h);

        Estilo.poner(d, Estilo.etiqueta("Tipo", false), 0, 2, 1, 0, n);
        Estilo.poner(d, txtTipo, 1, 2, 1, 0.2, h);
        Estilo.poner(d, Estilo.etiqueta("Prioridad", false), 2, 2, 1, 0, n);
        Estilo.poner(d, txtPrioridad, 3, 2, 1, 0.4, h);
        Estilo.poner(d, Estilo.etiqueta("Módulo", false), 4, 2, 1, 0, n);
        Estilo.poner(d, txtModulo, 5, 2, 1, 0.4, h);

        Estilo.poner(d, Estilo.etiqueta("Canal", false), 0, 3, 1, 0, n);
        Estilo.poner(d, txtCanal, 1, 3, 1, 0.2, h);
        Estilo.poner(d, Estilo.etiqueta("Recibida", false), 2, 3, 1, 0, n);
        Estilo.poner(d, txtRecibida, 3, 3, 1, 0.4, h);
        Estilo.poner(d, Estilo.etiqueta("Tiempo total", false), 4, 3, 1, 0, n);
        Estilo.poner(d, txtTiempo, 5, 3, 1, 0.4, h);

        Estilo.poner(d, Estilo.etiqueta("Vínculo", false), 0, 4, 1, 0, n);
        Estilo.poner(d, txtVinculo, 1, 4, 5, 1, h);
        Estilo.poner(d, Estilo.etiqueta("Asunto", false), 0, 5, 1, 0, n);
        Estilo.poner(d, txtAsunto, 1, 5, 5, 1, h);
        Estilo.ponerEtiquetaArriba(d, Estilo.etiqueta("Descripción", false), 0, 6);
        txtDescripcion.setEditable(false);
        txtDescripcion.setFocusable(false);
        txtDescripcion.setBackground(Estilo.FONDO_SOLO_LECTURA);
        Estilo.ponerArea(d, Estilo.conDesplazamiento(txtDescripcion), 1, 6, 5, 1);
        return d;
    }

    /** Muestra los datos de la solicitud y habilita las acciones que corresponden. */
    private void mostrarDatos() {
        txtNumero.setText(String.valueOf(solicitud.getId()));
        txtEstado.setText(String.valueOf(solicitud.getEstadoActual()));
        txtResponsable.setText(solicitud.getResponsable() == null ? "" : solicitud.getResponsable().getNombre());
        txtCliente.setText(solicitud.getCliente().getNombre());
        txtSolicitante.setText(solicitud.getSolicitante().toString());
        txtProducto.setText(solicitud.getProducto().getNombre());
        txtModulo.setText(solicitud.getModulo() == null ? "Sin definir" : solicitud.getModulo().getNombre());
        txtTipo.setText(solicitud.getTipo() == null ? "Sin clasificar" : solicitud.getTipo().getNombre());
        txtPrioridad.setText(solicitud.getPrioridad() == null ? "Sin definir" : solicitud.getPrioridad().getNombre());
        txtCanal.setText(solicitud.getCanal().getNombre());
        txtRecibida.setText(Formatos.fechaHora(solicitud.getFechaRecepcion()));
        txtTiempo.setText(Formatos.horas(solicitud.calcularTiempoTotal()) + " h");
        txtVinculo.setText(solicitud.getTipoVinculo() == null ? "Sin vínculo"
                : solicitud.getTipoVinculo() + " de la solicitud " + solicitud.getSolicitudOrigenId());
        txtAsunto.setText(solicitud.getAsunto());
        txtDescripcion.setText(solicitud.getDescripcion() == null ? "" : solicitud.getDescripcion());
        txtDescripcion.setCaretPosition(0);

        modeloHistorial.setRowCount(0);
        List<RegistroSolicitud> historial = solicitud.getHistorial();
        for (RegistroSolicitud r : historial) {
            modeloHistorial.addRow(new Object[] {Formatos.fechaHora(r.getFecha()), r.getTipoRegistro(),
                r.getAutor() == null ? "" : r.getAutor().getNombre(), r.getDetalle()});
        }
        configurarAcciones();
    }

    /**
     * Habilita cada acción según el estado, siguiendo el diagrama de estados, y según el rol.
     * Las acciones deshabilitadas explican por qué en su ayuda emergente.
     */
    private void configurarAcciones() {
        Rol rol = sesion.getUsuario().getRol();
        boolean esResponsable = sesion.getUsuario().equals(solicitud.getResponsable());
        boolean clasificar = false;
        boolean intervenir = false;
        boolean abrirEspera = false;
        boolean terminarEspera = false;
        boolean hito = false;
        boolean cerrar = false;
        boolean reabrir = false;
        String situacion;
        switch (solicitud.getEstadoActual()) {
            case NUEVA -> {
                clasificar = intervenir = hito = cerrar = true;
                situacion = "Solicitud nueva: con la primera intervención pasa a En curso.";
            }
            case EN_CURSO -> {
                clasificar = intervenir = abrirEspera = hito = cerrar = true;
                situacion = "Solicitud en curso.";
            }
            case EN_ESPERA -> {
                clasificar = intervenir = terminarEspera = hito = cerrar = true;
                situacion = "En espera desde el " + Formatos.fechaHora(solicitud.getEsperaAbierta().getFecha())
                        + " (" + solicitud.getEsperaAbierta().getMotivo() + ").";
            }
            case RESUELTA, CERRADA_SIN_CONFIRMACION -> {
                reabrir = true;
                situacion = "Solicitud cerrada el " + Formatos.fechaHora(solicitud.getFechaCierre())
                        + ". Si el cliente vuelve a escribir, se puede reabrir.";
            }
            default -> situacion = "Solicitud " + solicitud.getEstadoActual() + ": es un estado final.";
        }
        habilitar(btnClasificar, clasificar, rol.puedeClasificar(), "Clasifica soporte funcional o técnico");
        habilitar(btnIntervencion, intervenir, rol.esAgente(), "Solo los agentes de soporte");
        habilitar(btnAbrirEspera, abrirEspera, rol.esAgente(), "Solo los agentes de soporte");
        habilitar(btnTerminarEspera, terminarEspera, rol.esAgente(), "Solo los agentes de soporte");
        habilitar(btnHito, hito, rol.puedeRegistrarHitos(), "Registran hitos soporte técnico e infraestructura");
        habilitar(btnCerrar, cerrar, esResponsable, "Solo el responsable de la solicitud la puede cerrar");
        habilitar(btnReabrir, reabrir, rol.esAgente(), "Solo los agentes de soporte");
        lblSituacion.setText(situacion);
    }

    private static void habilitar(JButton boton, boolean segunEstado, boolean segunUsuario, String motivo) {
        boton.setEnabled(segunEstado && segunUsuario);
        if (!segunEstado) {
            boton.setToolTipText("No corresponde en el estado actual de la solicitud");
        } else if (!segunUsuario) {
            boton.setToolTipText(motivo);
        } else {
            boton.setToolTipText(null);
        }
    }

    /** Muestra la pantalla de un caso de uso y, si se completó, vuelve a leer la solicitud. Null si no se pudo abrir. */
    private void accion(Pantalla pantalla) {
        if (pantalla != null && pantalla.mostrar()) {
            recargar();
        }
    }

    /** CU07, pasos 8 y 9: termina la espera y la solicitud vuelve a En curso. */
    private void terminarEspera() {
        if (!Dialogos.confirmar(this, "¿Llegó lo que se esperaba? La espera se cierra y la solicitud vuelve a En curso.")) {
            return;
        }
        ejecutar(controlador::cerrarEspera);
        // Si falló, el controlador ya volvió a leer la solicitud: se muestra lo que hay en la base
        solicitud = controlador.getSolicitud();
        mostrarDatos();
    }

    private void recargar() {
        if (ejecutar(() -> solicitud = controlador.abrirSolicitud(solicitud.getId()))) {
            mostrarDatos();
        }
    }
}
