package sgs.vista;

import java.awt.GridBagConstraints;
import java.awt.Window;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import sgs.controlador.ControladorRegistrarHito;
import sgs.controlador.Sesion;
import sgs.modelo.catalogo.TipoHito;

/** CU08 Registrar hito técnico (RF11): corrección, publicación en testing o producción, o servidor. */
public final class PantallaRegistrarHito extends Pantalla {

    private static final long serialVersionUID = 1L;

    private final transient ControladorRegistrarHito controlador;
    private final JComboBox<TipoHito> cmbTipo = new JComboBox<>(TipoHito.values());
    private final JTextField txtReferencia = new JTextField(30);
    private final JTextField txtDeploy = new JTextField(6);

    /** Lee la solicitud y crea la pantalla. Si no se puede leer, avisa y devuelve null. */
    public static PantallaRegistrarHito crear(Window duenio, Sesion sesion, int solicitudId) {
        ControladorRegistrarHito controlador = sesion.registrarHito();
        if (!Dialogos.ejecutar(duenio, () -> controlador.abrirSolicitud(solicitudId))) {
            return null;
        }
        return new PantallaRegistrarHito(duenio, sesion, controlador);
    }

    private PantallaRegistrarHito(Window duenio, Sesion sesion, ControladorRegistrarHito controlador) {
        super(duenio, sesion, "Registrar hito técnico");
        this.controlador = controlador;
        cmbTipo.setSelectedIndex(-1);

        JPanel datos = Estilo.grupo("Hito técnico");
        int h = GridBagConstraints.HORIZONTAL;
        int n = GridBagConstraints.NONE;
        Estilo.poner(datos, Estilo.etiqueta("Tipo de hito", true), 0, 0, 1, 0, n);
        Estilo.poner(datos, cmbTipo, 1, 0, 1, 1, h);
        Estilo.poner(datos, Estilo.etiqueta("Referencia", false), 0, 1, 1, 0, n);
        Estilo.poner(datos, txtReferencia, 1, 1, 1, 1, h);
        Estilo.poner(datos, Estilo.nota("Por ejemplo, el número del pull request o del deploy"), 1, 2, 1, 1, h);
        Estilo.poner(datos, Estilo.etiqueta("Solicitud del deploy", false), 0, 3, 1, 0, n);
        Estilo.poner(datos, Estilo.fila(txtDeploy, Estilo.nota("Número de otra solicitud, si el deploy se registró en ella")), 1, 3, 1, 1, h);

        JPanel cuerpo = cuerpoVertical();
        cuerpo.add(grupoSolicitud(controlador.getSolicitud()));
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(datos);

        JButton cancelar = Estilo.boton("Cancelar");
        JButton registrar = Estilo.boton("Registrar hito");
        cancelar.addActionListener(e -> dispose());
        registrar.addActionListener(e -> registrar());
        armar("Registrar hito técnico", cuerpo, Estilo.botonera("* Datos obligatorios", cancelar, registrar));
    }

    /** Pasos 4 a 8. Si falta la publicación en testing, avisa y pide confirmación (alternativa 6.A). */
    private void registrar() {
        TipoHito tipo = (TipoHito) cmbTipo.getSelectedItem();
        if (controlador.faltaPublicacionEnTesting(tipo) && !Dialogos.confirmar(this,
                "La solicitud no tiene registrada la publicación en testing.\n¿Registrar igual la publicación en producción?")) {
            return;
        }
        ejecutar(() -> {
            controlador.registrar(tipo, txtReferencia.getText(),
                    Entrada.entero(txtDeploy.getText(), "El número de la solicitud del deploy"));
            terminar();
        });
    }
}
