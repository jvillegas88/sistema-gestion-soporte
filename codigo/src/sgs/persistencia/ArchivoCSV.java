package sgs.persistencia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lectura y escritura de archivos CSV, el formato de intercambio con otros sistemas: el
 * histórico que exporta la herramienta actual (CU02) y los reportes que se exportan para
 * circularlos (CU11 y CU12, RF24).
 * <p>
 * Sigue la convención habitual del formato: un registro por línea, los campos separados por
 * un carácter, y entre comillas dobles los que contienen el separador, comillas o un salto de
 * línea. Una comilla dentro de un campo se escribe doble.
 */
public final class ArchivoCSV {

    private static final char COMILLA = '"';
    private static final char BOM = '\uFEFF';

    private ArchivoCSV() {
        // Solo métodos de clase
    }

    /**
     * Lee todo el archivo en UTF-8. Cada registro es un arreglo con un elemento por campo, y el
     * primero es el encabezado. Un campo entre comillas puede ocupar varias líneas.
     *
     * @throws FormatoInvalidoException si queda una comilla sin cerrar
     * @throws IOException si el archivo no existe, no se puede leer o no está en UTF-8
     */
    public static List<String[]> leer(Path archivo, char separador) throws IOException {
        List<String[]> registros = new ArrayList<>();
        try (BufferedReader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            StringBuilder registro = new StringBuilder();
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (registros.isEmpty() && registro.isEmpty() && !linea.isEmpty() && linea.charAt(0) == BOM) {
                    linea = linea.substring(1);
                }
                if (!registro.isEmpty()) {
                    registro.append('\n');
                }
                registro.append(linea);
                // Con comillas impares, un campo entre comillas sigue en la línea siguiente
                if (comillasCerradas(registro)) {
                    if (!registro.toString().isBlank()) {
                        registros.add(separar(registro.toString(), separador));
                    }
                    registro.setLength(0);
                }
            }
            if (!registro.isEmpty()) {
                // Una comilla sin cerrar se come todo lo que sigue: no hay forma de saber dónde
                // termina cada registro, así que el archivo entero es inválido
                throw new FormatoInvalidoException("el registro " + registros.size()
                        + " tiene una comilla sin cerrar");
            }
        }
        return registros;
    }

    /** El archivo no respeta el formato CSV y no se puede separar en registros. */
    public static final class FormatoInvalidoException extends IOException {

        private static final long serialVersionUID = 1L;

        public FormatoInvalidoException(String mensaje) {
            super(mensaje);
        }
    }

    /**
     * Escribe el archivo en UTF-8, un registro por línea. Empieza con la marca BOM, para que
     * las planillas de cálculo reconozcan las tildes. Si el archivo existe, lo reemplaza.
     */
    public static void escribir(Path archivo, char separador, List<String[]> registros) throws IOException {
        try (BufferedWriter escritor = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
            escritor.write(BOM);
            for (String[] registro : registros) {
                escribirRegistro(escritor, separador, registro);
            }
        }
    }

    /** Separa un registro en sus campos, respetando las comillas. */
    static String[] separar(String registro, char separador) {
        List<String> campos = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < registro.length(); i++) {
            char c = registro.charAt(i);
            if (entreComillas) {
                if (c == COMILLA && i + 1 < registro.length() && registro.charAt(i + 1) == COMILLA) {
                    campo.append(COMILLA);
                    i++;
                } else if (c == COMILLA) {
                    entreComillas = false;
                } else {
                    campo.append(c);
                }
            } else if (c == COMILLA) {
                entreComillas = true;
            } else if (c == separador) {
                campos.add(campo.toString());
                campo.setLength(0);
            } else {
                campo.append(c);
            }
        }
        campos.add(campo.toString());
        return campos.toArray(new String[0]);
    }

    private static void escribirRegistro(BufferedWriter escritor, char separador, String[] campos) throws IOException {
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                escritor.write(separador);
            }
            escritor.write(conComillasSiHaceFalta(campos[i], separador));
        }
        escritor.write("\r\n");
    }

    private static String conComillasSiHaceFalta(String campo, char separador) {
        if (campo == null) {
            return "";
        }
        boolean hacenFalta = campo.indexOf(separador) >= 0 || campo.indexOf(COMILLA) >= 0
                || campo.indexOf('\n') >= 0 || campo.indexOf('\r') >= 0;
        if (!hacenFalta) {
            return campo;
        }
        return COMILLA + campo.replace("\"", "\"\"") + COMILLA;
    }

    private static boolean comillasCerradas(CharSequence texto) {
        int comillas = 0;
        for (int i = 0; i < texto.length(); i++) {
            if (texto.charAt(i) == COMILLA) {
                comillas++;
            }
        }
        return comillas % 2 == 0;
    }
}
