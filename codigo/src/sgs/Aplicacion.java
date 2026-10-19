package sgs;

import java.sql.SQLException;
import java.util.Locale;
import javax.swing.SwingUtilities;
import sgs.controlador.ControladorSesion;
import sgs.controlador.Sesion;
import sgs.modelo.Formatos;
import sgs.modelo.Integrante;
import sgs.persistencia.DatosMaestros;
import sgs.vista.Dialogos;
import sgs.vista.Estilo;
import sgs.vista.PantallaInicioSesion;
import sgs.vista.VentanaPrincipal;

/**
 * Punto de inicio del sistema: aplica el aspecto, pide el inicio de sesión, lee los datos
 * maestros y abre la ventana principal con el menú según el rol del integrante.
 */
public class Aplicacion {

    public static void main(String[] args) {
        Locale.setDefault(Formatos.ARGENTINA);
        SwingUtilities.invokeLater(Aplicacion::iniciar);
    }

    /** También la usa la ventana principal al cerrar la sesión, para volver a pedir el usuario. */
    public static void iniciar() {
        Estilo.configurarAspecto();
        ControladorSesion controlador = new ControladorSesion();
        Integrante usuario = new PantallaInicioSesion(controlador).mostrar();
        if (usuario == null) {
            System.exit(0);
            return;
        }
        try {
            DatosMaestros maestros = controlador.cargarDatosMaestros();
            new VentanaPrincipal(new Sesion(usuario, maestros)).setVisible(true);
        } catch (SQLException e) {
            Dialogos.error(null, "No se pudieron leer los datos iniciales de la base.\n\nDetalle: " + e.getMessage());
            System.exit(1);
        }
    }
}
