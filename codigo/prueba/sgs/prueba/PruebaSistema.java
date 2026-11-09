package sgs.prueba;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import sgs.controlador.ControladorImportarHistorico;
import sgs.controlador.ControladorReportes;
import sgs.controlador.ControladorSesion;
import sgs.controlador.Sesion;
import sgs.modelo.Formatos;
import sgs.modelo.Importacion;
import sgs.modelo.Integrante;
import sgs.modelo.consulta.FilaEsfuerzoCliente;

/**
 * Pruebas de sistema del TP4, sin pantalla, sobre la base de los datos de prueba:
 * <ul>
 * <li>CP-S-01, volumen (RNF09): se importa un CSV ficticio con 1154 registros, la misma
 * cantidad que el histórico real, y se repite la importación sin que se duplique nada.</li>
 * <li>CP-S-02, desempeño (RNF11): con esos datos cargados, los dos reportes de un período de
 * doce meses tienen que mostrarse en menos de cinco segundos.</li>
 * </ul>
 * El CSV se genera siempre igual, con una semilla fija, así la prueba se puede repetir. No
 * tiene ningún dato real: los nombres, los correos y los asuntos son inventados.
 * Se ejecuta con probar-sistema.sh, que antes recarga la base.
 */
public final class PruebaSistema {

    static final int REGISTROS = 1154;
    private static final long LIMITE_REPORTE_MS = 5_000;
    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private final PrintStream salida;
    private int ejecuciones;
    private int fallas;

    private PruebaSistema(PrintStream salida) {
        this.salida = salida;
    }

    public static void main(String[] args) throws Exception {
        Path csv = Path.of(args.length > 0 ? args[0] : "prueba/datos/historico-volumen.csv");
        Path archivoSalida = Path.of(args.length > 1 ? args[1] : "prueba/salidas/pruebas-sistema.txt");
        Locale.setDefault(Formatos.ARGENTINA);
        generarHistorico(csv);
        Files.createDirectories(archivoSalida.getParent());
        int codigo;
        try (PrintStream salida = new PrintStream(archivoSalida.toFile(), StandardCharsets.UTF_8)) {
            PruebaSistema prueba = new PruebaSistema(salida);
            prueba.cpS01(csv);
            prueba.cpS02();
            prueba.escribir("");
            prueba.escribir("Ejecuciones: " + prueba.ejecuciones + ". Dieron lo esperado: "
                    + (prueba.ejecuciones - prueba.fallas) + ". No dieron lo esperado: " + prueba.fallas);
            codigo = prueba.fallas == 0 ? 0 : 1;
        }
        System.exit(codigo);
    }

    // ------------------------------------------------------------------
    // CP-S-01: volumen
    // ------------------------------------------------------------------

    private void cpS01(Path csv) throws Exception {
        escribir("CP-S-01, volumen (RNF09): importar " + csv.getFileName() + " con " + REGISTROS
                + " registros y repetir la importación. Usuario lgomez (soporte funcional)");
        ControladorImportarHistorico controlador = sesion("lgomez").importarHistorico();

        long inicio = System.nanoTime();
        Importacion primera = controlador.revisar(csv);
        long revision = milisegundos(inicio);
        inicio = System.nanoTime();
        controlador.importar(primera);
        long registro = milisegundos(inicio);
        int importadas = primera.getSolicitudes().size();
        int omitidas = primera.getOmitidas();
        int rechazadas = primera.getRechazos().size();
        escribir("  Primera importación: " + importadas + " importadas (" + primera.getSinCliente()
                + " sin cliente identificado), " + omitidas + " omitidas y " + rechazadas + " rechazadas");
        escribir("  Tiempo: revisión del archivo " + revision + " ms, registro en la base " + registro + " ms");
        verificar("Se leyeron todos los registros", String.valueOf(primera.getRegistros()), String.valueOf(REGISTROS));
        verificar("Importadas, omitidas y rechazadas suman el total",
                String.valueOf(importadas + omitidas + rechazadas), String.valueOf(REGISTROS));

        inicio = System.nanoTime();
        Importacion segunda = controlador.revisar(csv);
        long repeticion = milisegundos(inicio);
        escribir("  Repetición: " + segunda.getSolicitudes().size() + " a importar, " + segunda.getOmitidas()
                + " omitidas y " + segunda.getRechazos().size() + " rechazadas, en " + repeticion + " ms");
        verificar("La repetición no importa nada", String.valueOf(segunda.getSolicitudes().size()), "0");
        verificar("La repetición omite lo ya importado", String.valueOf(segunda.getOmitidas()),
                String.valueOf(importadas + omitidas));
        verificar("La repetición rechaza lo mismo", String.valueOf(segunda.getRechazos().size()),
                String.valueOf(rechazadas));
    }

    // ------------------------------------------------------------------
    // CP-S-02: desempeño
    // ------------------------------------------------------------------

    private void cpS02() throws Exception {
        LocalDate desde = LocalDate.of(2025, 4, 1);
        LocalDate hasta = LocalDate.of(2026, 3, 31);
        escribir("");
        escribir("CP-S-02, desempeño (RNF11): los dos reportes del " + desde.format(Formatos.FECHA) + " al "
                + hasta.format(Formatos.FECHA) + " (doce meses) en menos de " + LIMITE_REPORTE_MS / 1000
                + " segundos, con los datos de CP-S-01. Usuario scastro (responsable de gestión)");
        escribir("  Base local en Docker: la conexión no pasa por la VPN ni por el servidor de AWS");
        ControladorReportes reportes = sesion("scastro").reportes();

        long inicio = System.nanoTime();
        List<FilaEsfuerzoCliente> esfuerzo = reportes.esfuerzoPorCliente(desde, hasta, null, null, null);
        reportes.solicitudesPorTipo(desde, hasta, null);
        long cu11 = milisegundos(inicio);
        int solicitudes = 0;
        for (FilaEsfuerzoCliente f : esfuerzo) {
            solicitudes += f.getSolicitudes();
        }
        escribir("  CU11: " + esfuerzo.size() + " clientes y " + solicitudes + " solicitudes en " + cu11 + " ms");
        verificar("CU11 en menos de 5 segundos", String.valueOf(cu11 < LIMITE_REPORTE_MS), "true");

        inicio = System.nanoTime();
        reportes.cargaPorIntegrante(desde, hasta);
        reportes.horasPorTipo(desde, hasta);
        long cu12 = milisegundos(inicio);
        escribir("  CU12: en " + cu12 + " ms");
        verificar("CU12 en menos de 5 segundos", String.valueOf(cu12 < LIMITE_REPORTE_MS), "true");
    }

    // ------------------------------------------------------------------
    // El CSV ficticio
    // ------------------------------------------------------------------

    /**
     * Genera el CSV con el formato de la herramienta actual más el correo del solicitante. Las
     * proporciones imitan las del histórico real: la mayoría cerradas, algunas eliminadas y
     * unas pocas abiertas, y algunas sin producto o sin correo, que se tienen que rechazar.
     */
    static void generarHistorico(Path csv) throws IOException {
        Random azar = new Random(2026);
        String[] nombres = {"Ana", "Bruno", "Carla", "Damián", "Elena", "Federico", "Gisela", "Hugo", "Inés",
            "Joaquín", "Karina", "Lucas", "Malena", "Nicolás", "Olga", "Pedro", "Romina", "Santiago", "Tamara", "Ulises"};
        // Apellidos que no aparecen en el histórico real, así ningún nombre generado coincide con una persona real
        String[] apellidos = {"Aguirre", "Bustos", "Correa", "Domínguez", "Escobar", "Funes", "Godoy", "Ibarra",
            "Juárez", "Molina", "Vera", "Ojeda", "Zárate", "Quiroga", "Rivero", "Suárez"};
        String[] dominios = {"cliente-a.com.ar", "cliente-a.com.ar", "cliente-b.com.ar", "cliente-c.com.ar",
            "correo-personal.com"};
        String[] asuntos = {"No se puede cerrar una observación", "Error al generar el informe final",
            "Consulta sobre permisos de usuario", "Cambiar el responsable de una auditoría",
            "El sistema no responde", "Pedido de un reporte nuevo", "Error al anular una operación",
            "Consulta sobre el alta de un legajo", "No llegan las notificaciones por correo",
            "Restablecer la contraseña de un usuario", "Importar observaciones desde una planilla",
            "Lentitud al abrir los legajos", "Agregar un campo al formulario de operaciones"};
        String[] tiempos = {"0.25", "0.5", "1", "1.5", "2", "3", "4", "6", "8"};

        List<String[]> registros = new ArrayList<>();
        registros.add(new String[] {"id", "subject", "requester", "requester_email", "assignee", "status", "type",
            "priority", "created_at", "solved_at", "tiempo_insumido", "tiempo_insumido_anterior", "sistema",
            "organizacion"});
        LocalDateTime primera = LocalDateTime.of(2024, 4, 11, 12, 0);
        long minutos = java.time.Duration.between(primera, LocalDateTime.of(2026, 3, 31, 20, 0)).toMinutes();
        for (int i = 0; i < REGISTROS; i++) {
            String nombre = nombres[azar.nextInt(nombres.length)];
            String apellido = apellidos[azar.nextInt(apellidos.length)];
            String dominio = dominios[azar.nextInt(dominios.length)];
            String correo = azar.nextInt(100) < 2 ? ""
                    : (nombre.charAt(0) + apellido).toLowerCase(Locale.ROOT)
                            .replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u')
                            + "@" + dominio;
            int estado = azar.nextInt(100);
            String status = estado < 85 ? "closed" : estado < 93 ? "deleted" : estado < 96 ? "solved"
                    : estado < 98 ? "open" : "pending";
            int producto = azar.nextInt(100);
            String sistema = producto < 75 ? "Producto A" : producto < 95 ? "Producto B" : "";
            LocalDateTime creada = primera.plusMinutes((long) (azar.nextDouble() * minutos));
            String tiempo = azar.nextInt(100) < 40 ? tiempos[azar.nextInt(tiempos.length)] : "";
            registros.add(new String[] {String.valueOf(10_001 + i), asuntos[azar.nextInt(asuntos.length)],
                nombre + " " + apellido, correo, "", status, "", "normal",
                creada.atOffset(ZoneOffset.ofHours(-3)).withOffsetSameInstant(ZoneOffset.UTC).format(UTC),
                "", tiempo, "", sistema, ""});
        }
        Files.createDirectories(csv.getParent());
        sgs.persistencia.ArchivoCSV.escribir(csv, ',', registros);
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private static Sesion sesion(String usuario) throws Exception {
        ControladorSesion sesion = new ControladorSesion();
        Integrante integrante = sesion.iniciarSesion(usuario, "clave_de_ejemplo");
        return new Sesion(integrante, sesion.cargarDatosMaestros());
    }

    private static long milisegundos(long desde) {
        return (System.nanoTime() - desde) / 1_000_000;
    }

    private void verificar(String que, String obtenido, String esperado) {
        ejecuciones++;
        boolean paso = esperado.equals(obtenido);
        if (!paso) {
            fallas++;
        }
        escribir(String.format("  %-6s %s: %s", paso ? "PASÓ" : "FALLÓ", que, obtenido));
    }

    private void escribir(String texto) {
        salida.println(texto);
        System.out.println(texto);
    }
}
