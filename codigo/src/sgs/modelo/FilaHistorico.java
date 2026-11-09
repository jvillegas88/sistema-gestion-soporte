package sgs.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import sgs.modelo.catalogo.Estado;
import sgs.modelo.excepcion.ReglaNegocioException;

/**
 * Un registro del archivo CSV que exporta la herramienta actual (CU02). Conoce el formato del
 * archivo y convierte cada campo al dato del sistema. Si un campo no sirve, avisa con una
 * ReglaNegocioException y el registro se rechaza.
 */
public class FilaHistorico {

    /**
     * Columnas que el sistema usa. El archivo puede traer otras (asignado, tipo, prioridad,
     * organización) y se ignoran: las importadas entran sin clasificar y el cliente sale del
     * correo del solicitante, como en CU01.
     */
    public static final String[] COLUMNAS = {
        "id", "subject", "requester", "requester_email", "status", "created_at", "tiempo_insumido", "sistema"
    };

    // Posición de cada columna dentro del arreglo COLUMNAS
    private static final int NUMERO = 0;
    private static final int ASUNTO = 1;
    private static final int SOLICITANTE = 2;
    private static final int CORREO = 3;
    private static final int ESTADO = 4;
    private static final int CREADA = 5;
    private static final int TIEMPO = 6;
    private static final int PRODUCTO = 7;

    /** Hora de Argentina: el archivo trae las fechas en UTC y la base guarda la hora local. */
    private static final ZoneOffset ARGENTINA = ZoneOffset.ofHours(-3);
    private static final BigDecimal TIEMPO_MAXIMO = new BigDecimal("9999.99");

    private final int registro;
    private final String[] valores;

    /**
     * @param registro número del registro dentro del archivo, sin contar el encabezado
     * @param campos   los campos del registro, en el orden del archivo
     * @param posiciones la posición en el archivo de cada columna de COLUMNAS
     */
    public FilaHistorico(int registro, String[] campos, int[] posiciones) {
        this.registro = registro;
        this.valores = new String[COLUMNAS.length];
        for (int i = 0; i < COLUMNAS.length; i++) {
            int posicion = posiciones[i];
            valores[i] = posicion < campos.length ? campos[posicion].trim() : "";
        }
    }

    /**
     * Busca en el encabezado la posición de cada columna que el sistema usa (CU02, paso 4).
     *
     * @throws ReglaNegocioException si falta alguna, que es la alternativa 4.A
     */
    public static int[] posiciones(String[] encabezado) throws ReglaNegocioException {
        int[] posiciones = new int[COLUMNAS.length];
        StringBuilder faltan = new StringBuilder();
        for (int i = 0; i < COLUMNAS.length; i++) {
            posiciones[i] = buscar(encabezado, COLUMNAS[i]);
            if (posiciones[i] < 0) {
                faltan.append(faltan.isEmpty() ? "" : ", ").append(COLUMNAS[i]);
            }
        }
        if (!faltan.isEmpty()) {
            throw new ReglaNegocioException("El archivo no tiene el formato esperado. Faltan las columnas: " + faltan);
        }
        return posiciones;
    }

    /** Búsqueda secuencial de una columna en el encabezado, sin distinguir mayúsculas. */
    private static int buscar(String[] encabezado, String columna) {
        for (int i = 0; i < encabezado.length; i++) {
            if (encabezado[i].trim().equalsIgnoreCase(columna)) {
                return i;
            }
        }
        return -1;
    }

    public int getRegistro() {
        return registro;
    }

    /** El número que tenía la solicitud en la herramienta actual, que la identifica (RNF09). */
    public int numeroOrigen() throws ReglaNegocioException {
        try {
            int numero = Integer.parseInt(valores[NUMERO]);
            if (numero > 0) {
                return numero;
            }
        } catch (NumberFormatException e) {
            // Se informa abajo, igual que un número negativo
        }
        throw new ReglaNegocioException("el número de origen \"" + valores[NUMERO] + "\" no es válido");
    }

    /** El texto del número tal como vino, para identificar el registro en el resumen. */
    public String numeroTalComoVino() {
        return valores[NUMERO];
    }

    /**
     * Estado en que entra la solicitud. Solo se importan las cerradas: las que siguen abiertas
     * se rechazan y entran en una importación posterior, cuando se cierren (RNF09).
     */
    public Estado estadoDeCierre() throws ReglaNegocioException {
        String estado = valores[ESTADO].toLowerCase(Locale.ROOT);
        return switch (estado) {
            case "closed", "solved" -> Estado.RESUELTA;
            case "deleted" -> Estado.DESCARTADA;
            case "new", "open", "pending", "hold" ->
                throw new ReglaNegocioException("sigue abierta en la herramienta actual. Se importa cuando se cierre");
            default -> throw new ReglaNegocioException("el estado \"" + valores[ESTADO] + "\" no se reconoce");
        };
    }

    /** Fecha de recepción en la hora de Argentina. El archivo la trae en UTC, como 2025-06-16T00:13:40Z. */
    public LocalDateTime fechaRecepcion() throws ReglaNegocioException {
        try {
            return OffsetDateTime.parse(valores[CREADA]).atZoneSameInstant(ARGENTINA).toLocalDateTime();
        } catch (DateTimeParseException e) {
            throw new ReglaNegocioException("la fecha \"" + valores[CREADA] + "\" no es válida");
        }
    }

    /** Tiempo total dedicado en la herramienta actual, en horas. Vacío si no se registró. */
    public BigDecimal tiempoHistorico() throws ReglaNegocioException {
        if (valores[TIEMPO].isEmpty()) {
            return null;
        }
        try {
            BigDecimal horas = new BigDecimal(valores[TIEMPO]);
            if (horas.signum() >= 0 && horas.scale() <= 2 && horas.compareTo(TIEMPO_MAXIMO) <= 0) {
                return horas;
            }
        } catch (NumberFormatException e) {
            // Se informa abajo, igual que un valor fuera de rango
        }
        throw new ReglaNegocioException("el tiempo \"" + valores[TIEMPO] + "\" no es válido");
    }

    /** El asunto. Si supera el largo de la base se acorta, porque es un título y no conviene perder la solicitud. */
    public String asunto() {
        String asunto = valores[ASUNTO];
        if (asunto.length() > Solicitud.LARGO_ASUNTO) {
            return asunto.substring(0, Solicitud.LARGO_ASUNTO - 3) + "...";
        }
        return asunto;
    }

    public String nombreSolicitante() {
        return valores[SOLICITANTE];
    }

    /** Correo del solicitante en minúsculas. Es obligatorio, como en la base. */
    public String correo() throws ReglaNegocioException {
        String correo = valores[CORREO].toLowerCase(Locale.ROOT);
        if (correo.isEmpty()) {
            throw new ReglaNegocioException("no tiene el correo del solicitante");
        }
        if (!Formatos.esCorreoValido(correo)) {
            throw new ReglaNegocioException("el correo \"" + valores[CORREO] + "\" no es válido");
        }
        return correo;
    }

    /** Nombre del producto tal como vino. Vacío si la herramienta actual no lo tenía. */
    public String nombreProducto() {
        return valores[PRODUCTO];
    }
}
