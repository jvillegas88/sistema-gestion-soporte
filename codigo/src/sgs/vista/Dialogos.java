package sgs.vista;

import java.awt.Component;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import sgs.modelo.excepcion.ReglaNegocioException;

/**
 * Mensajes al usuario y ejecución de las acciones de las pantallas. Cada acción se ejecuta
 * dentro de un try-catch que separa los tipos de problema: una regla del negocio que no se
 * cumple, que el usuario puede corregir, un problema con la base de datos y un problema al
 * leer o escribir un archivo. Un error que no es ninguno de esos, que sería un defecto del
 * programa, también se informa.
 */
public final class Dialogos {

    /** Una acción de una pantalla, que puede chocar con una regla, con la base o con un archivo. */
    @FunctionalInterface
    public interface Accion {
        void ejecutar() throws SQLException, ReglaNegocioException, IOException;
    }

    private Dialogos() {
        // Solo métodos de clase
    }

    /**
     * Ejecuta la acción y, si falla, muestra el mensaje que corresponde.
     *
     * @return true si la acción terminó bien
     */
    public static boolean ejecutar(Component padre, Accion accion) {
        try {
            accion.ejecutar();
            return true;
        } catch (ReglaNegocioException e) {
            aviso(padre, e.getMessage());
        } catch (SQLException e) {
            // El mensaje del driver queda en la consola: al usuario no le sirve
            System.err.println("Error de base de datos: " + e.getMessage());
            error(padre, "No se pudo completar la operación por un problema con la base de datos.\n"
                    + "Verificá la conexión e intentá de nuevo.");
        } catch (NoSuchFileException e) {
            error(padre, "No se encontró el archivo " + e.getFile() + ".");
        } catch (AccessDeniedException e) {
            error(padre, "No hay permiso para usar el archivo " + e.getFile() + ".");
        } catch (IOException e) {
            // Como con la base, el detalle técnico queda en la consola
            System.err.println("Error de archivo: " + e);
            error(padre, "No se pudo leer o escribir el archivo.\nVerificá que exista y que no esté abierto en otro programa.");
        } catch (RuntimeException e) {
            // El detalle técnico queda solo en la consola, para poder corregirlo. Al usuario no
            // se le muestra la excepción.
            e.printStackTrace();
            error(padre, "Ocurrió un error inesperado y la operación no se completó.\n"
                    + "Si se repite, avisá al responsable del sistema.");
        }
        return false;
    }

    public static void aviso(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, Estilo.SISTEMA, JOptionPane.WARNING_MESSAGE);
    }

    public static void error(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, Estilo.SISTEMA, JOptionPane.ERROR_MESSAGE);
    }

    public static void informacion(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, Estilo.SISTEMA, JOptionPane.INFORMATION_MESSAGE);
    }

    /** Pide confirmación con Sí y No. Devuelve true si el usuario confirma. */
    public static boolean confirmar(Component padre, String pregunta) {
        Object[] opciones = {"Sí", "No"};
        int respuesta = JOptionPane.showOptionDialog(padre, pregunta, Estilo.SISTEMA, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
        return respuesta == 0;
    }

    /**
     * Pide un texto de varias líneas, por ejemplo un motivo.
     *
     * @return el texto ingresado, o null si el usuario canceló
     */
    public static String pedirTexto(Component padre, String pregunta) {
        JTextArea area = Estilo.areaDeTexto(3);
        Object[] contenido = {pregunta, Estilo.conDesplazamiento(area)};
        Object[] opciones = {"Aceptar", "Cancelar"};
        int respuesta = JOptionPane.showOptionDialog(padre, contenido, Estilo.SISTEMA, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
        return respuesta == 0 ? area.getText() : null;
    }
}
