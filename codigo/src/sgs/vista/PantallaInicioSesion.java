package sgs.vista;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import sgs.controlador.ControladorSesion;
import sgs.modelo.Integrante;

/**
 * Inicio de sesión con usuario y contraseña (RNF06). No hereda de Pantalla porque todavía no
 * hay una sesión.
 */
public final class PantallaInicioSesion extends JDialog {

    private static final long serialVersionUID = 1L;

    private final transient ControladorSesion controlador;
    private final JTextField txtUsuario = new JTextField(18);
    private final JPasswordField txtClave = new JPasswordField(18);
    private transient Integrante integrante;

    public PantallaInicioSesion(ControladorSesion controlador) {
        super((java.awt.Frame) null, Estilo.SISTEMA + " - Inicio de sesión", true);
        this.controlador = controlador;

        JPanel datos = Estilo.grupo("Ingresá con tu usuario del equipo");
        Estilo.poner(datos, Estilo.etiqueta("Usuario", true), 0, 0, 1, 0, GridBagConstraints.NONE);
        Estilo.poner(datos, txtUsuario, 1, 0, 1, 1, GridBagConstraints.HORIZONTAL);
        Estilo.poner(datos, Estilo.etiqueta("Contraseña", true), 0, 1, 1, 0, GridBagConstraints.NONE);
        Estilo.poner(datos, txtClave, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL);
        JPanel cuerpo = new JPanel(new BorderLayout());
        cuerpo.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));
        cuerpo.add(datos);

        JButton salir = Estilo.boton("Salir");
        JButton ingresar = Estilo.boton("Ingresar");
        salir.addActionListener(e -> dispose());
        ingresar.addActionListener(e -> ingresar());

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(Estilo.encabezado("Inicio de sesión", null), BorderLayout.NORTH);
        raiz.add(cuerpo, BorderLayout.CENTER);
        raiz.add(Estilo.botonera(null, salir, ingresar), BorderLayout.SOUTH);
        setContentPane(raiz);
        getRootPane().setDefaultButton(ingresar);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private void ingresar() {
        boolean ok = Dialogos.ejecutar(this, () -> integrante = controlador.iniciarSesion(txtUsuario.getText(),
                new String(txtClave.getPassword())));
        if (ok) {
            dispose();
        } else {
            txtClave.setText("");
            txtClave.requestFocusInWindow();
        }
    }

    /**
     * Muestra la pantalla y espera.
     *
     * @return el integrante que inició sesión, o null si salió
     */
    public Integrante mostrar() {
        setVisible(true);
        return integrante;
    }
}
