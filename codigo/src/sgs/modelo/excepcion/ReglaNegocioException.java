package sgs.modelo.excepcion;

/**
 * Se lanza cuando una operación no cumple una regla del negocio: un tiempo fuera de rango,
 * un cierre sin la información mínima, una transición que el diagrama de estados no permite.
 * Es una excepción comprobada, así que quien llama está obligado a capturarla o declararla.
 * El controlador la captura y la pantalla muestra el mensaje, que está escrito para el usuario.
 */
public class ReglaNegocioException extends Exception {

    private static final long serialVersionUID = 1L;

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
