package sgs.persistencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Abre las conexiones a MySQL. La aplicación entra con el usuario app_soporte, que solo tiene
 * permisos de SELECT, INSERT y UPDATE, y la conexión va cifrada con TLS 1.2 o 1.3 (RNF06).
 * <p>
 * Los datos de conexión tienen valores para la base local de desarrollo. En otra instalación
 * se cambian al ejecutar, sin tocar el código: java -Dsgs.url=... -Dsgs.usuario=... -Dsgs.clave=...
 */
public final class ConexionBD {

    private static final String URL = System.getProperty("sgs.url",
            "jdbc:mysql://127.0.0.1:3306/gestion_soporte");
    private static final String USUARIO = System.getProperty("sgs.usuario", "app_soporte");
    private static final String CLAVE = System.getProperty("sgs.clave", "Cambiar_en_la_instalacion_2026");

    private ConexionBD() {
        // Solo métodos de clase
    }

    /**
     * Abre una conexión nueva. Quien la pide la cierra, con try-with-resources.
     * La zona horaria de la sesión es la de Argentina, así las fechas que completa la base
     * (RNF07) quedan en la misma hora que el resto de los datos.
     */
    public static Connection obtenerConexion() throws SQLException {
        Properties propiedades = new Properties();
        propiedades.setProperty("user", USUARIO);
        propiedades.setProperty("password", CLAVE);
        propiedades.setProperty("sslMode", "REQUIRED");
        propiedades.setProperty("enabledTLSProtocols", "TLSv1.2,TLSv1.3");
        propiedades.setProperty("characterEncoding", "UTF-8");
        propiedades.setProperty("sessionVariables", "time_zone='-03:00'");
        propiedades.setProperty("connectTimeout", "5000");
        return DriverManager.getConnection(URL, propiedades);
    }
}
