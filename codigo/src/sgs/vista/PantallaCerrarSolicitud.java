package sgs.vista;

import java.awt.GridBagConstraints;
import java.awt.Window;
import java.util.List;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import sgs.controlador.ControladorCerrarSolicitud;
import sgs.controlador.Sesion;
import sgs.modelo.Solicitud;
import sgs.modelo.catalogo.Estado;
import sgs.modelo.catalogo.TipoIntervencion;

/**
 * CU09 Cerrar solicitud. Pide el tipo de cierre y la intervención del agente, que es la de
 * CU06 incluida en el paso 4. Muestra lo que le falta hoy a la solicitud para cerrarse, así el
 * agente lo puede completar con la misma intervención (por ejemplo, con la respuesta al cliente).
 */
public final class PantallaCerrarSolicitud extends Pantalla {

    private static final long serialVersionUID = 1L;

    private final transient ControladorCerrarSolicitud controlador;
    private final JRadioButton rbResuelta = new JRadioButton(Estado.RESUELTA.getNombre(), true);
    private final JRadioButton rbSinConfirmacion = new JRadioButton(Estado.CERRADA_SIN_CONFIRMACION.getNombre());
    private final JRadioButton rbCancelada = new JRadioButton(Estado.CANCELADA.getNombre());
    private final JTextArea txtMotivo = Estilo.areaDeTexto(2);
    private final JLabel lblMotivo = Estilo.etiqueta("Motivo", false);
    private final JLabel lblFaltantes = Estilo.nota(" ");
    private final PanelIntervencion panel = new PanelIntervencion("Intervención del cierre (CU06)");

    /** Lee la solicitud y crea la pantalla. Si no se puede leer, avisa y devuelve null. */
    public static PantallaCerrarSolicitud crear(Window duenio, Sesion sesion, int solicitudId) {
        ControladorCerrarSolicitud controlador = sesion.cerrarSolicitud();
        if (!Dialogos.ejecutar(duenio, () -> controlador.abrirSolicitud(solicitudId))) {
            return null;
        }
        return new PantallaCerrarSolicitud(duenio, sesion, controlador);
    }

    private PantallaCerrarSolicitud(Window duenio, Sesion sesion, ControladorCerrarSolicitud controlador) {
        super(duenio, sesion, "Cerrar solicitud");
        this.controlador = controlador;
        Solicitud s = controlador.getSolicitud();

        JPanel cierre = Estilo.grupo("Tipo de cierre");
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(rbResuelta);
        grupo.add(rbSinConfirmacion);
        grupo.add(rbCancelada);
        Estilo.poner(cierre, Estilo.etiqueta("Cierre", true), 0, 0, 1, 0, GridBagConstraints.NONE);
        Estilo.poner(cierre, Estilo.fila(rbResuelta, Box.createHorizontalStrut(10), rbSinConfirmacion,
                Box.createHorizontalStrut(10), rbCancelada), 1, 0, 1, 1, GridBagConstraints.HORIZONTAL);
        Estilo.poner(cierre, Estilo.nota("Resuelta: el cliente confirmó la solución. Cerrada sin confirmación: el "
                + "cliente dejó de responder. Cancelada: el cliente canceló el pedido."), 1, 1, 1, 1, GridBagConstraints.HORIZONTAL);
        Estilo.ponerEtiquetaArriba(cierre, lblMotivo, 0, 2);
        Estilo.ponerArea(cierre, Estilo.conDesplazamiento(txtMotivo), 1, 2, 1, 1);
        Estilo.poner(cierre, lblFaltantes, 1, 3, 1, 1, GridBagConstraints.HORIZONTAL);

        JPanel cuerpo = cuerpoVertical();
        cuerpo.add(grupoSolicitud(s));
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(cierre);
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(panel);
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(PantallaRegistrarIntervencion.armarRegistradas(s));

        rbResuelta.addActionListener(e -> actualizarSegunCierre());
        rbSinConfirmacion.addActionListener(e -> actualizarSegunCierre());
        rbCancelada.addActionListener(e -> actualizarSegunCierre());
        panel.sugerirTipo(TipoIntervencion.RESPUESTA_CLIENTE);
        actualizarSegunCierre();

        JButton cancelar = Estilo.boton("Cancelar");
        JButton cerrar = Estilo.boton("Cerrar solicitud");
        cancelar.addActionListener(e -> dispose());
        cerrar.addActionListener(e -> cerrar());
        armar("Cerrar solicitud", cuerpo, Estilo.botonera("* Datos obligatorios", cancelar, cerrar));
    }

    private Estado getCierre() {
        if (rbCancelada.isSelected()) {
            return Estado.CANCELADA;
        }
        return rbSinConfirmacion.isSelected() ? Estado.CERRADA_SIN_CONFIRMACION : Estado.RESUELTA;
    }

    /**
     * El motivo es obligatorio solo para cancelar (alternativa 3.A). Para los otros dos cierres
     * se muestra lo que falta hoy, según la información mínima de la observación 1.
     */
    private void actualizarSegunCierre() {
        Estado cierre = getCierre();
        boolean cancelada = cierre == Estado.CANCELADA;
        lblMotivo.setText(cancelada ? "Motivo *" : "Motivo");
        lblMotivo.setFont(cancelada ? Estilo.negrita() : Estilo.base());
        if (cancelada) {
            lblFaltantes.setText("Para cancelar hacen falta el motivo y tu intervención.");
            return;
        }
        List<String> faltan = controlador.faltantesActuales();
        if (faltan.isEmpty()) {
            lblFaltantes.setText("La solicitud tiene la información mínima para cerrarse como " + cierre + ".");
        } else {
            String ayuda = "Hoy le falta: " + String.join(", ", faltan) + ".";
            if (faltan.contains("respuesta al cliente")) {
                ayuda += " La respuesta se puede agregar como comunicación de esta intervención.";
            }
            lblFaltantes.setText("<html>" + ayuda + "</html>");
        }
    }

    /** Pasos 4 a 8: confirma y guarda la intervención y el cierre juntos. */
    private void cerrar() {
        Estado cierre = getCierre();
        if (!Dialogos.confirmar(this, "¿Cerrar la solicitud " + controlador.getSolicitud().getId() + " como " + cierre + "?")) {
            return;
        }
        ejecutar(() -> {
            panel.cargarBorrador(controlador);
            controlador.cerrar(cierre, txtMotivo.getText(), panel.getTipo(), panel.getDescripcion(), panel.getTiempo());
            Dialogos.informacion(this, "Solicitud cerrada como " + cierre + ".");
            terminar();
        });
    }
}
