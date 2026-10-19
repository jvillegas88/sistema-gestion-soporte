package sgs.vista;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import sgs.modelo.excepcion.ReglaNegocioException;

/**
 * Convierte lo que escribe el usuario en los tipos que usa el modelo. Cada conversión captura
 * la excepción de Java que corresponde (NumberFormatException o DateTimeParseException) y la
 * cambia por una ReglaNegocioException con un mensaje que el usuario entiende, en lugar de
 * dejar que el programa falle con un error técnico.
 */
public final class Entrada {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private Entrada() {
        // Solo métodos de clase
    }

    /**
     * Lee una cantidad de horas. Acepta coma o punto como separador decimal, como se escribe en
     * el equipo. Un campo vacío devuelve null, y el modelo informa que falta.
     */
    public static BigDecimal horas(String texto, String campo) throws ReglaNegocioException {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ReglaNegocioException(campo + " tiene que ser un número, por ejemplo 1,5");
        }
    }

    /** Lee un número entero, por ejemplo el de una solicitud. Un campo vacío devuelve null. */
    public static Integer entero(String texto, String campo) throws ReglaNegocioException {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            throw new ReglaNegocioException(campo + " tiene que ser un número entero");
        }
    }

    /** Lee una fecha con el formato dd/mm/aaaa. */
    public static LocalDate fecha(String texto, String campo) throws ReglaNegocioException {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim(), FECHA);
        } catch (DateTimeParseException e) {
            throw new ReglaNegocioException(campo + " tiene que ser una fecha válida con el formato dd/mm/aaaa");
        }
    }

    /** Lee una fecha y hora con el formato dd/mm/aaaa hh:mm. */
    public static LocalDateTime fechaHora(String texto, String campo) throws ReglaNegocioException {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(texto.trim(), FECHA_HORA);
        } catch (DateTimeParseException e) {
            throw new ReglaNegocioException(campo + " tiene que ser una fecha válida con el formato dd/mm/aaaa hh:mm");
        }
    }
}
