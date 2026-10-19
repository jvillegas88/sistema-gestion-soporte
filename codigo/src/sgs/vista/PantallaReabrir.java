package sgs.vista;

import java.awt.GridBagConstraints;
import java.awt.Window;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import sgs.controlador.ControladorEsperaEstado;
import sgs.controlador.Sesion;

/**
 * CU07, alternativa 1.A: el cliente responde sobre una solicitud cerrada. Si es el mismo
 * problema, la solicitud se reabre. Si es un tema nuevo, se registra una solicitud nueva
 * vinculada como continuación (CU01, alternativa 3.B) y esta queda cerrada.
 */
public final class PantallaReabrir extends Pantalla {

    private static final long serialVersionUID = 1L;

    private final transient ControladorEsperaEstado controlador;
    private final JRadioButton rbMismo = new JRadioButton("Trata el mismo problema: reabrir esta solicitud", true);
    private final JRadioButton rbNuevo = new JRadioButton("Plantea un tema nuevo: registrar una solicitud nueva vinculada");
    private final JTextArea txtMotivo = Estilo.areaDeTexto(2);

    /** Lee la solicitud y crea la pantalla. Si no se puede leer, avisa y devuelve null. */
    public static PantallaReabrir crear(Window duenio, Sesion sesion, int solicitudId) {
        ControladorEsperaEstado controlador = sesion.esperaEstado();
        if (!Dialogos.ejecutar(duenio, () -> controlador.abrirSolicitud(solicitudId))) {
            return null;
        }
        return new PantallaReabrir(duenio, sesion, controlador);
    }

    private PantallaReabrir(Window duenio, Sesion sesion, ControladorEsperaEstado controlador) {
        super(duenio, sesion, "Reabrir solicitud");
        this.controlador = controlador;

        ButtonGroup grupo = new ButtonGroup();
        grupo.add(rbMismo);
        grupo.add(rbNuevo);
        rbMismo.addActionListener(e -> txtMotivo.setEnabled(true));
        rbNuevo.addActionListener(e -> txtMotivo.setEnabled(false));

        JPanel datos = Estilo.grupo("Respuesta del cliente sobre la solicitud cerrada");
        Estilo.poner(datos, rbMismo, 0, 0, 2, 1, GridBagConstraints.HORIZONTAL);
        Estilo.poner(datos, rbNuevo, 0, 1, 2, 1, GridBagConstraints.HORIZONTAL);
        Estilo.ponerEtiquetaArriba(datos, Estilo.etiqueta("Motivo de la reapertura", true), 0, 2);
        Estilo.ponerArea(datos, Estilo.conDesplazamiento(txtMotivo), 1, 2, 1, 1);

        JPanel cuerpo = cuerpoVertical();
        cuerpo.add(grupoSolicitud(controlador.getSolicitud()));
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(datos);

        JButton cancelar = Estilo.boton("Cancelar");
        JButton continuar = Estilo.boton("Continuar");
        cancelar.addActionListener(e -> dispose());
        continuar.addActionListener(e -> continuar());
        armar("Reabrir solicitud", cuerpo, Estilo.botonera("* Datos obligatorios", cancelar, continuar));
    }

    private void continuar() {
        if (rbMismo.isSelected()) {
            ejecutar(() -> {
                controlador.reabrir(txtMotivo.getText());
                terminar();
            });
            return;
        }
        // Tema nuevo (1.A.3.A): lo registra soporte funcional como solicitud nueva vinculada
        if (!sesion.getUsuario().getRol().puedeRegistrarSolicitudes()) {
            Dialogos.aviso(this, "El tema nuevo lo registra soporte funcional como una solicitud nueva, "
                    + "vinculada a la solicitud " + controlador.getSolicitud().getId() + ". Esta solicitud queda cerrada.");
            return;
        }
        if (new PantallaRegistrarSolicitud(this, sesion, controlador.getSolicitud()).mostrar()) {
            terminar();
        }
    }
}
