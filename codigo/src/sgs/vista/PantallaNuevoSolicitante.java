package sgs.vista;

import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import sgs.controlador.ControladorRegistrarSolicitud;
import sgs.controlador.Sesion;
import sgs.modelo.Cliente;
import sgs.modelo.Solicitante;

/**
 * CU01, alternativa 3.A: registrar un solicitante que todavía no existe. El sistema propone
 * el cliente según el dominio del correo y el agente lo confirma o lo cambia.
 */
public final class PantallaNuevoSolicitante extends Pantalla {

    private static final long serialVersionUID = 1L;

    private final transient ControladorRegistrarSolicitud controlador;
    private final JTextField txtNombre = new JTextField(24);
    private final JTextField txtCorreo = new JTextField(24);
    private final JComboBox<Cliente> cmbCliente = new JComboBox<>();
    private transient Solicitante solicitante;

    public PantallaNuevoSolicitante(Window duenio, Sesion sesion, ControladorRegistrarSolicitud controlador) {
        super(duenio, sesion, "Nuevo solicitante");
        this.controlador = controlador;
        for (Cliente c : controlador.getClientes()) {
            cmbCliente.addItem(c);
        }
        cmbCliente.setSelectedIndex(-1);
        txtCorreo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                proponerCliente();
            }
        });

        JPanel datos = Estilo.grupo("Datos del solicitante");
        int h = GridBagConstraints.HORIZONTAL;
        int n = GridBagConstraints.NONE;
        Estilo.poner(datos, Estilo.etiqueta("Nombre", true), 0, 0, 1, 0, n);
        Estilo.poner(datos, txtNombre, 1, 0, 1, 1, h);
        Estilo.poner(datos, Estilo.etiqueta("Correo", true), 0, 1, 1, 0, n);
        Estilo.poner(datos, txtCorreo, 1, 1, 1, 1, h);
        Estilo.poner(datos, Estilo.etiqueta("Cliente", true), 0, 2, 1, 0, n);
        Estilo.poner(datos, cmbCliente, 1, 2, 1, 1, h);
        Estilo.poner(datos, Estilo.nota("Se propone según el dominio del correo"), 1, 3, 1, 1, h);
        JPanel cuerpo = cuerpoVertical();
        cuerpo.add(datos);

        JButton cancelar = Estilo.boton("Cancelar");
        JButton guardar = Estilo.boton("Guardar solicitante");
        cancelar.addActionListener(e -> dispose());
        guardar.addActionListener(e -> guardar());
        armar("Nuevo solicitante", cuerpo, Estilo.botonera("* Datos obligatorios", cancelar, guardar));
    }

    /** Si el dominio del correo corresponde a un cliente, lo deja seleccionado. */
    private void proponerCliente() {
        Cliente propuesto = controlador.proponerCliente(txtCorreo.getText());
        if (propuesto != null) {
            cmbCliente.setSelectedItem(propuesto);
        }
    }

    private void guardar() {
        ejecutar(() -> {
            solicitante = controlador.crearSolicitante((Cliente) cmbCliente.getSelectedItem(), txtNombre.getText(),
                    txtCorreo.getText());
            terminar();
        });
    }

    /** El solicitante registrado, o null si se canceló. */
    public Solicitante getSolicitante() {
        return solicitante;
    }
}
