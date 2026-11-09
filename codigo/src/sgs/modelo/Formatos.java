package sgs.modelo;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatos de fecha y de horas que se repiten en el modelo y en las pantallas. */
public final class Formatos {

    public static final Locale ARGENTINA = Locale.of("es", "AR");
    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formatos() {
        // Solo métodos de clase: no se crean objetos
    }

    /** Horas con coma decimal y hasta dos decimales, como se escriben en el equipo: 1,5 o 0,25. */
    public static String horas(BigDecimal horas) {
        if (horas == null) {
            return "";
        }
        DecimalFormat formato = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(ARGENTINA));
        return formato.format(horas);
    }

    public static String fechaHora(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FECHA_HORA);
    }

    /** Devuelve true si el texto es null o solo tiene espacios. */
    public static boolean estaVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    /** Un correo tiene una arroba, algo antes y un dominio con punto, sin espacios. */
    public static boolean esCorreoValido(String correo) {
        return correo != null && correo.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    }
}
