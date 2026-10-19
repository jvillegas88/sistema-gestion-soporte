package sgs.vista;

import java.awt.GridBagConstraints;
import java.awt.Window;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import sgs.controlador.ControladorEsperaEstado;
import sgs.controlador.Sesion;
import sgs.modelo.catalogo.MotivoEspera;

/** CU07, pasos 1 a 7: abrir una espera con su motivo. La solicitud pasa a En espera. */
public final class PantallaAbrirEspera extends Pantalla {

    private static final long serialVersionUID = 1L;

    private final transient ControladorEsperaEstado controlador;
    private final JComboBox<MotivoEspera> cmbMotivo = new JComboBox<>(MotivoEspera.values());

    /** Lee la solicitud y crea la pantalla. Si no se puede leer, avisa y devuelve null. */
    public static PantallaAbrirEspera crear(Window duenio, Sesion sesion, int solicitudId) {
        ControladorEsperaEstado controlador = sesion.esperaEstado();
        if (!Dialogos.ejecutar(duenio, () -> controlador.abrirSolicitud(solicitudId))) {
            return null;
        }
        return new PantallaAbrirEspera(duenio, sesion, controlador);
    }

    private PantallaAbrirEspera(Window duenio, Sesion sesion, ControladorEsperaEstado controlador) {
        super(duenio, sesion, "Registrar espera");
        this.controlador = controlador;
        cmbMotivo.setSelectedIndex(-1);

        JPanel datos = Estilo.grupo("Espera");
        Estilo.poner(datos, Estilo.etiqueta("¿Qué o a quién se espera?", true), 0, 0, 1, 0, GridBagConstraints.NONE);
        Estilo.poner(datos, cmbMotivo, 1, 0, 1, 1, GridBagConstraints.HORIZONTAL);
        Estilo.poner(datos, Estilo.nota("La espera empieza ahora y queda abierta hasta que indiques que llegó lo esperado. "
                + "Para la validación del cliente en testing, elegí Espera del cliente."), 0, 1, 2, 1, GridBagConstraints.HORIZONTAL);

        JPanel cuerpo = cuerpoVertical();
        cuerpo.add(grupoSolicitud(controlador.getSolicitud()));
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(datos);

        JButton cancelar = Estilo.boton("Cancelar");
        JButton abrir = Estilo.boton("Abrir espera");
        cancelar.addActionListener(e -> dispose());
        abrir.addActionListener(e -> ejecutar(() -> {
            controlador.abrirEspera((MotivoEspera) cmbMotivo.getSelectedItem());
            terminar();
        }));
        armar("Registrar espera", cuerpo, Estilo.botonera("* Datos obligatorios", cancelar, abrir));
    }
}
