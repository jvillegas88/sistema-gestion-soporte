package sgs.prueba;

import java.awt.Window;
import java.io.File;
import java.util.Locale;
import javax.swing.JDialog;
import sgs.Aplicacion;
import sgs.modelo.Formatos;

/**
 * Captura el mensaje que muestra el sistema cuando no puede conectarse a la base, que es como
 * se ve una SQLException para el usuario. Se ejecuta con el contenedor de la base detenido,
 * desde probar-sin-base.sh.
 */
public final class CapturaSinBase {

    private CapturaSinBase() {
    }

    public static void main(String[] args) {
        Locale.setDefault(Formatos.ARGENTINA);
        Manejador m = new Manejador(new File(args.length > 0 ? args[0] : "../capturas"));
        Aplicacion.main(new String[0]);
        Window login = m.esperarVentana("Inicio de sesión");
        m.escribir(login, "Usuario", "lgomez");
        m.escribir(login, "Contraseña", "clave_de_ejemplo");
        m.presionar(login, "Ingresar");
        JDialog mensaje = m.esperarMensaje();
        System.out.println("Mensaje: " + m.textoMensaje(mensaje));
        m.capturar("20-excepcion-base.png", login, mensaje);
        System.exit(0);
    }
}
