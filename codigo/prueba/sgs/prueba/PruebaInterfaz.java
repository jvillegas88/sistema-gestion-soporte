package sgs.prueba;

import java.awt.Window;
import java.io.File;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import sgs.Aplicacion;
import sgs.controlador.ControladorReportes;
import sgs.modelo.Integrante;
import sgs.modelo.catalogo.Rol;
import sgs.modelo.excepcion.ReglaNegocioException;
import sgs.persistencia.DatosMaestros;
import sgs.modelo.Formatos;
import sgs.persistencia.ConexionBD;

/**
 * Pruebas desde la interfaz del TP3, sobre la aplicación real conectada a la base:
 * <ul>
 * <li>PR-N-01 desde la pantalla de CU06 (caso CP-N-01), con los valores del TP2 más 24,004
 * para verificar DEF-01.</li>
 * <li>PR-I-01 (caso CP-I-01): una solicitud nueva recorre CU01, CU06, CU08, CU07 y CU09 con
 * los roles que intervienen en el proceso, hasta cerrarse como Resuelta.</li>
 * <li>PR-I-02, parte del TP3 (caso CP-I-02): el reporte de CU11 se compara con el total
 * calculado a mano sobre los datos de prueba, y el de CU12 con el mismo total.</li>
 * <li>PR-S-03 (caso CP-S-03): los usuarios sin rol de gestión no pueden abrir los reportes,
 * ni desde el menú ni pidiéndoselos directamente al controlador.</li>
 * </ul>
 * En el camino guarda las capturas del informe. Parte de los datos de prueba del TP2, así que
 * antes hay que recargar la base. Se ejecuta con probar-interfaz.sh, en una pantalla virtual.
 */
public final class PruebaInterfaz {

    private static final String CLAVE = "clave_de_ejemplo";
    private static final String SOLICITUD_RECIBIDA = "Solicitud (\\d+) registrada";

    private final Manejador m;
    private final PrintStream salida;
    private final List<String> fallas = new ArrayList<>();
    private int ejecuciones;
    private JFrame principal;

    private PruebaInterfaz(Manejador m, PrintStream salida) {
        this.m = m;
        this.salida = salida;
    }

    public static void main(String[] args) throws Exception {
        File capturas = new File(args.length > 0 ? args[0] : "../capturas");
        File archivoSalida = new File(args.length > 1 ? args[1] : "prueba/salidas/prueba-interfaz.txt");
        capturas.mkdirs();
        archivoSalida.getParentFile().mkdirs();
        Locale.setDefault(Formatos.ARGENTINA);
        int codigo;
        try (PrintStream salida = new PrintStream(archivoSalida, StandardCharsets.UTF_8)) {
            PruebaInterfaz prueba = new PruebaInterfaz(new Manejador(capturas), salida);
            Aplicacion.main(new String[0]);
            codigo = prueba.ejecutar() ? 0 : 1;
        }
        System.exit(codigo);
    }

    private boolean ejecutar() throws SQLException {
        escribir("Pruebas desde la interfaz - " + Formatos.fechaHora(LocalDateTime.now()));
        escribir("");
        inicioDeSesion();
        prN01();
        int numero = prI01();
        prI02();
        clasificarSolicitud12();
        prS03();
        escribir("");
        escribir("Solicitud creada por PR-I-01: " + numero);
        escribir("Ejecuciones: " + ejecuciones + ". Dieron lo esperado: " + (ejecuciones - fallas.size())
                + ". No dieron lo esperado: " + fallas.size());
        for (String f : fallas) {
            escribir("  FALLÓ: " + f);
        }
        return fallas.isEmpty();
    }

    // ------------------------------------------------------------------
    // Inicio de sesión
    // ------------------------------------------------------------------

    private void inicioDeSesion() {
        escribir("Inicio de sesión (RNF06)");
        Window login = m.esperarVentana("Inicio de sesión");
        m.escribir(login, "Usuario", "pmedina");
        m.escribir(login, "Contraseña", "otra_clave");
        m.presionar(login, "Ingresar");
        JDialog mensaje = m.esperarMensaje();
        String texto = m.textoMensaje(mensaje);
        m.capturar("20-inicio-sesion-rechazado.png", login, mensaje);
        m.aceptar(mensaje);
        verificar("Contraseña incorrecta", texto, "El usuario o la contraseña no son correctos");
        m.escribir(login, "Contraseña", CLAVE);
        m.capturar("20-inicio-sesion.png", login);
        m.presionar(login, "Ingresar");
        principal = m.esperarPrincipal();
        verificar("Contraseña correcta", "Ingresó", "Ingresó");
        m.capturar("20-bandeja.png", principal);
        abrirMenuParaCaptura("Solicitudes", "20-menu-soporte-tecnico.png", true);
    }

    private void iniciarSesion(String usuario) {
        elegirMenu("Sesión", "Cerrar sesión");
        Window login = m.esperarVentana("Inicio de sesión");
        m.escribir(login, "Usuario", usuario);
        m.escribir(login, "Contraseña", CLAVE);
        m.presionar(login, "Ingresar");
        principal = m.esperarPrincipal();
    }

    // ------------------------------------------------------------------
    // PR-N-01 desde la pantalla de CU06
    // ------------------------------------------------------------------

    private void prN01() throws SQLException {
        escribir("");
        escribir("PR-N-01 desde la pantalla de CU06, solicitud 12 del Cliente A, usuario pmedina (soporte técnico)");
        String mayorQueCero = "La intervención tiene que tener un tiempo mayor que cero";
        String hasta24 = "El tiempo no puede superar las 24 horas. Si el trabajo siguió otro día, registrá otra intervención";
        String decimales = "El tiempo admite hasta dos decimales";
        // En los válidos, el total tiene que aumentar en el valor registrado (paso 7 de PR-N-01)
        String registrada = "Intervención registrada. Tiempo total de la solicitud: ";
        String[][] casos = {
            {"-1", "CEI1", mayorQueCero},
            {"-0,01", "CEI1", mayorQueCero},
            {"0", "CEI1", mayorQueCero},
            {"0,01", "CEV1", registrada + "0,01 h"},
            {"1,5", "CEV1", registrada + "1,51 h"},
            {"23,99", "CEV1", registrada + "25,5 h"},
            {"24", "CEV1", registrada + "49,5 h"},
            {"24,01", "CEI2", hasta24},
            {"30", "CEI2", hasta24},
            {"2,125", "CEI3", decimales},
            {"24,004", "CEI2 y CEI3", hasta24},
        };
        // Antes, una captura de una solicitud de los datos de prueba, con fechas reales de varios días
        Window enEspera = abrirSolicitud(10);
        m.capturar("20-solicitud-en-espera.png", enEspera);
        cerrarPantalla(enEspera);

        Window solicitud = abrirSolicitud(12);
        for (String[] caso : casos) {
            m.presionar(solicitud, "Registrar intervención...");
            Window cu06 = m.esperarVentana("Registrar intervención");
            m.elegir(cu06, "Tipo de intervención", "Análisis");
            m.escribir(cu06, "Descripción", "Prueba de tiempo " + caso[0]);
            m.escribir(cu06, "Tiempo dedicado", caso[0]);
            m.presionar(cu06, "Registrar intervención");
            JDialog mensaje = m.esperarMensaje();
            String texto = m.textoMensaje(mensaje);
            if (caso[0].equals("2,125")) {
                m.capturarFranja("12-cp-n-01-dos-decimales.png", m.altoHastaElComienzo(cu06, mensaje) - 150,
                        m.altoHasta(cu06, mensaje) + 30, cu06, mensaje);
            }
            m.aceptar(mensaje);
            Manejador.pausa(300);
            boolean seRegistro = !m.estaAbierta(cu06);
            if (!seRegistro) {
                m.presionar(cu06, "Cancelar");
                m.esperarCierre(cu06);
            }
            boolean debiaRegistrar = caso[2].startsWith(registrada);
            verificar(String.format("%-7s %-11s", caso[0], caso[1]), (seRegistro ? "Registra. " : "No registra. ") + texto,
                    (debiaRegistrar ? "Registra. " : "No registra. ") + caso[2]);
        }
        String total = m.leerEsperando(solicitud, "Tiempo total", "49,5 h");

        // CU08, alternativa 6.A: la publicación en producción sin la de testing pide confirmación
        m.presionar(solicitud, "Registrar hito...");
        Window cu08 = m.esperarVentana("Registrar hito técnico");
        m.elegir(cu08, "Tipo de hito", "Publicación en producción");
        m.presionar(cu08, "Registrar hito");
        JDialog aviso = m.esperarMensaje();
        String textoAviso = m.textoMensaje(aviso);
        m.responder(aviso, "No");
        Manejador.pausa(300);
        verificar("CU08 producción sin testing (6.A)", textoAviso + (m.estaAbierta(cu08) ? " / No registra" : " / Registra"),
                "La solicitud no tiene registrada la publicación en testing. "
                        + "¿Registrar igual la publicación en producción? / No registra");
        m.presionar(cu08, "Cancelar");
        m.esperarCierre(cu08);

        m.presionar(solicitud, "Volver");
        m.esperarCierre(solicitud);

        // Verificación en la base: solo los cuatro válidos, con el valor exacto
        List<String> guardados = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion();
                PreparedStatement ps = con.prepareStatement("SELECT tiempo_horas FROM intervenciones WHERE solicitud_id = 12 "
                        + "AND descripcion LIKE 'Prueba de tiempo%' ORDER BY intervencion_id")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    guardados.add(rs.getBigDecimal(1).toPlainString());
                }
            }
        }
        verificar("Valores guardados en la base", String.join(" ", guardados), "0.01 1.50 23.99 24.00");
        verificar("Tiempo total de la solicitud 12", total, "49,5 h");
    }

    // ------------------------------------------------------------------
    // PR-I-01: de la recepción al cierre
    // ------------------------------------------------------------------

    private int prI01() throws SQLException {
        escribir("");
        escribir("PR-I-01: una solicitud nueva hasta el cierre como Resuelta (CU01, CU06, CU08, CU07 y CU09)");

        // 1. Soporte funcional registra y clasifica la solicitud (CU01 con CU03)
        iniciarSesion("lgomez");
        elegirMenu("Solicitudes", "Registrar solicitud...");
        Window cu01 = m.esperarVentana("Registrar solicitud");
        m.elegir(cu01, "Solicitante", "Julieta Ramos");
        m.elegir(cu01, "Producto", "Producto B");
        m.marcar(cu01, "Correo");
        m.escribir(cu01, "Asunto", "Error al exportar el listado de operaciones a Excel");
        m.escribir(cu01, "Descripción", "Desde la actualización del lunes, al exportar el listado de operaciones "
                + "aprobadas a Excel aparece un error y el archivo no se genera. Pasa con listados de más de un mes.");
        m.elegir(cu01, "Tipo de solicitud", "Error de desarrollo");
        m.elegir(cu01, "Módulo", "Operaciones");
        m.elegir(cu01, "Prioridad", "Alta");
        m.escribir(cu01, "Fecha de recepción", "31/09/2026 09:05");
        m.presionar(cu01, "Registrar solicitud");
        JDialog mensaje = m.esperarMensaje();
        String texto = m.textoMensaje(mensaje);
        m.capturarFranja("20-excepcion-fecha.png", m.altoHastaElComienzo(cu01, mensaje) - 170,
                m.altoHasta(cu01, mensaje) + 40, cu01, mensaje);
        m.aceptar(mensaje);
        verificar("Fecha inexistente (31/09)", texto,
                "La fecha de recepción tiene que ser una fecha válida con el formato dd/mm/aaaa hh:mm");
        m.escribir(cu01, "Fecha de recepción", fechaDeHoy(9, 5));
        m.capturar("20-cu01-registrar.png", cu01);
        m.presionar(cu01, "Registrar solicitud");
        mensaje = m.esperarMensaje();
        texto = m.textoMensaje(mensaje);
        m.capturar("20-cu01-registrada.png", cu01, mensaje);
        m.aceptar(mensaje);
        Matcher numero = Pattern.compile(SOLICITUD_RECIBIDA).matcher(texto);
        verificar("CU01 registra la solicitud", numero.find() ? "Registrada en estado Nueva" : texto,
                "Registrada en estado Nueva");
        int id = Integer.parseInt(numero.group(1));
        m.esperarCierre(cu01);

        // 2. Primera intervención de soporte funcional: la solicitud pasa a En curso (CU06, 10.A)
        Window solicitud = abrirSolicitud(id);
        verificar("Estado al registrar", m.leerEsperando(solicitud, "Estado", "Nueva"), "Nueva");
        registrarIntervencion(solicitud, "Análisis", "0,5", "Reproduje el error con el usuario del cliente. "
                + "Falla con listados de más de 5000 operaciones.", "Nota interna",
                "Pasa a soporte técnico para revisar la exportación.", null);
        verificar("Estado después de la primera intervención", m.leerEsperando(solicitud, "Estado", "En curso"), "En curso");
        cerrarPantalla(solicitud);

        // 3. Soporte técnico corrige y publica en testing (CU06 y CU08)
        iniciarSesion("dferreyra");
        solicitud = abrirSolicitud(id);
        registrarIntervencion(solicitud, "Corrección", "2,5", "Corregí la exportación: el listado se arma por "
                + "partes para no superar el límite de filas.", null, null, null);
        registrarHito(solicitud, "Corrección", "Pull request 418", null);
        registrarHito(solicitud, "Publicación en testing", "Deploy de testing 2026-10-17", "20-cu08-hito.png");
        cerrarPantalla(solicitud);

        // 4. Soporte funcional pide la validación al cliente y espera (CU07)
        iniciarSesion("lgomez");
        solicitud = abrirSolicitud(id);
        registrarIntervencion(solicitud, "Respuesta al cliente", "0,25", "Le pedí al cliente que pruebe la corrección "
                + "en testing.", "Respuesta al cliente", "Hola Julieta, la corrección ya está en el ambiente de testing. "
                + "¿Podés probar la exportación y confirmarnos si funciona?", "20-cu06-intervencion.png");
        m.presionar(solicitud, "Abrir espera...");
        Window espera = m.esperarVentana("Registrar espera");
        m.elegir(espera, "¿Qué o a quién se espera?", "Espera del cliente");
        m.capturar("20-cu07-espera.png", espera);
        m.presionar(espera, "Abrir espera");
        m.esperarCierre(espera);
        verificar("CU07 abre la espera", m.leerEsperando(solicitud, "Estado", "En espera"), "En espera");

        // 5. El cliente valida: termina la espera (CU07, pasos 8 y 9)
        m.presionar(solicitud, "Terminar espera");
        mensaje = m.esperarMensaje();
        m.responder(mensaje, "Sí");
        verificar("CU07 termina la espera", m.leerEsperando(solicitud, "Estado", "En curso"), "En curso");

        // 6. Intento de cierre sin la publicación en producción (CU09, alternativa 5.A)
        m.presionar(solicitud, "Cerrar solicitud...");
        Window cierre = m.esperarVentana("Cerrar solicitud");
        String guia = m.etiquetaQueEmpieza(cierre, "Hoy le falta");
        llenarIntervencion(cierre, "Respuesta al cliente", "0,25", "El cliente confirmó que en testing funciona.",
                null, null);
        m.presionar(cierre, "Cerrar solicitud");
        m.responder(m.esperarMensaje(), "Sí");
        mensaje = m.esperarMensaje();
        texto = m.textoMensaje(mensaje);
        m.capturarFranja("20-cu09-falta-hito.png", 0, m.altoHasta(cierre, mensaje) + 30, cierre, mensaje);
        m.aceptar(mensaje);
        verificar("CU09 sin hito de producción", guia + " / " + texto, "Hoy le falta: hito de publicación en producción."
                + " / Para cerrar la solicitud falta: hito de publicación en producción");
        m.presionar(cierre, "Cancelar");
        m.esperarCierre(cierre);
        cerrarPantalla(solicitud);

        // 7. Soporte técnico publica en producción (CU08)
        iniciarSesion("dferreyra");
        solicitud = abrirSolicitud(id);
        registrarHito(solicitud, "Publicación en producción", "Deploy de producción 2026-10-17", null);
        cerrarPantalla(solicitud);

        // 8. Soporte funcional responde al cliente y cierra como Resuelta (CU09 con CU06)
        iniciarSesion("lgomez");
        solicitud = abrirSolicitud(id);
        m.presionar(solicitud, "Cerrar solicitud...");
        cierre = m.esperarVentana("Cerrar solicitud");
        m.marcar(cierre, "Resuelta");
        llenarIntervencion(cierre, "Respuesta al cliente", "0,25", "Avisé al cliente que la corrección está en "
                + "producción y confirmó que la exportación funciona.", "Respuesta al cliente",
                "Hola Julieta, la corrección ya está publicada en producción. Gracias por confirmar que funciona.");
        m.capturar("20-cu09-cerrar.png", cierre);
        m.presionar(cierre, "Cerrar solicitud");
        m.responder(m.esperarMensaje(), "Sí");
        mensaje = m.esperarMensaje();
        texto = m.textoMensaje(mensaje);
        m.capturar("20-cu09-cerrada.png", cierre, mensaje);
        m.aceptar(mensaje);
        verificar("CU09 cierra la solicitud", texto, "Solicitud cerrada como Resuelta.");
        m.esperarCierre(cierre);
        verificar("Estado final", m.leerEsperando(solicitud, "Estado", "Resuelta"), "Resuelta");
        verificar("Tiempo total", m.leerEsperando(solicitud, "Tiempo total", "3,5 h"), "3,5 h");
        m.capturar("20-solicitud-historial.png", solicitud);
        cerrarPantalla(solicitud);

        // Una solicitud cerrada de los datos de prueba, que no está en la bandeja
        Window cerrada = abrirPorNumero(3);
        m.capturar("20-solicitud-cerrada.png", cerrada);
        cerrarPantalla(cerrada);

        verificarEnLaBase(id);
        return id;
    }

    /** Lo que quedó guardado en cada tabla después de PR-I-01. */
    private void verificarEnLaBase(int id) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion()) {
            verificar("Base: cambios de estado", consultar(con, "SELECT GROUP_CONCAT(e.nombre ORDER BY ce.fecha, "
                    + "ce.cambio_estado_id SEPARATOR ' > ') FROM cambios_estado ce JOIN estados e USING (estado_id) "
                    + "WHERE ce.solicitud_id = ?", id), "Nueva > En curso > En espera > En curso > Resuelta");
            verificar("Base: intervenciones y horas", consultar(con, "SELECT CONCAT(COUNT(*), ' por ', SUM(tiempo_horas)) "
                    + "FROM intervenciones WHERE solicitud_id = ?", id), "4 por 3.50");
            verificar("Base: respuestas al cliente", consultar(con, "SELECT COUNT(*) FROM comunicaciones c JOIN "
                    + "intervenciones i USING (intervencion_id) WHERE i.solicitud_id = ? AND c.tipo = 'RESPUESTA_CLIENTE'", id), "2");
            verificar("Base: hitos", consultar(con, "SELECT GROUP_CONCAT(t.nombre ORDER BY h.fecha, h.hito_id SEPARATOR ', ') "
                    + "FROM hitos_tecnicos h JOIN tipos_hito t USING (tipo_hito_id) WHERE h.solicitud_id = ?", id),
                    "Corrección, Publicación en testing, Publicación en producción");
            verificar("Base: esperas cerradas", consultar(con, "SELECT CONCAT(COUNT(*), ' cerrada') FROM esperas "
                    + "WHERE solicitud_id = ? AND hasta IS NOT NULL", id), "1 cerrada");
            verificar("Base: clasificación inicial", consultar(con, "SELECT motivo FROM reclasificaciones "
                    + "WHERE solicitud_id = ?", id), "Clasificación inicial");
        }
    }

    // ------------------------------------------------------------------
    // CU03 sobre la solicitud 12, que quedó sin clasificar
    // ------------------------------------------------------------------

    private void clasificarSolicitud12() {
        escribir("");
        escribir("CU03 sobre la solicitud 12 (captura)");
        iniciarSesion("lgomez");
        Window solicitud = abrirSolicitud(12);
        m.presionar(solicitud, "Clasificar...");
        Window cu03 = m.esperarVentana("Clasificar solicitud");
        m.elegir(cu03, "Tipo de solicitud", "Pedido de infraestructura");
        m.elegir(cu03, "Módulo", "Usuarios y permisos");
        m.elegir(cu03, "Prioridad", "Normal");
        m.capturar("20-cu03-clasificar.png", cu03);
        m.presionar(cu03, "Guardar clasificación");
        m.esperarCierre(cu03);
        verificar("CU03 clasifica", m.leerEsperando(solicitud, "Tipo", "Pedido de infraestructura"), "Pedido de infraestructura");
        cerrarPantalla(solicitud);
    }

    // ------------------------------------------------------------------
    // PR-I-02, parte del TP3: los reportes contra el cálculo a mano
    // ------------------------------------------------------------------

    private void prI02() {
        escribir("");
        escribir("PR-I-02 (parte del TP3): reportes del 01/07/2026 al 30/09/2026 contra el cálculo a mano");
        iniciarSesion("scastro");
        m.capturar("20-principal-gestion.png", principal);
        abrirMenuParaCaptura("Reportes", "20-menu-gestion.png", false);

        elegirMenu("Reportes", "Esfuerzo por cliente...");
        Window cu11 = m.esperarVentana("Reporte de esfuerzo por cliente");
        m.escribir(cu11, "Período", "01/07/2026");
        m.escribirSegundo(cu11, "Período", "30/09/2026");
        m.marcar(cu11, "Comparar con");
        m.escribirJunto(cu11, "Comparar con", 1, "01/04/2026");
        m.escribirJunto(cu11, "Comparar con", 3, "30/06/2026");
        m.presionar(cu11, "Consultar");
        Manejador.pausa(800);
        m.capturar("20-cu11-esfuerzo.png", cu11);
        JTable esfuerzo = m.tabla(cu11, 0);
        verificar("CU11 Cliente A", fila(esfuerzo, "Cliente A", 1, 2, 3), "4 | 11,25 | 2,72");
        verificar("CU11 Cliente B", fila(esfuerzo, "Cliente B", 1, 2, 3), "3 | 8 | 10,54");
        verificar("CU11 Cliente C", fila(esfuerzo, "Cliente C", 1, 2, 3), "3 | 9,25 | 15,29");
        verificar("CU11 Cliente no identificado", fila(esfuerzo, "Cliente no identificado", 1, 2, 4),
                "0 | 0 | 3,5");
        verificar("CU11 Total", fila(esfuerzo, "Total", 1, 2, 3, 4, 5), "10 | 28,5 | 28,55 | 3,5 | +714,3 %");
        JTable tipos = m.tabla(cu11, 1);
        verificar("CU11 distribución por tipo, total", fila(tipos, "Total", 1, 2, 3, 4, 5, 6), "3 | 2 | 1 | 1 | 2 | 1");
        m.presionar(cu11, "Cerrar");
        m.esperarCierre(cu11);

        elegirMenu("Reportes", "Carga por integrante...");
        Window cu12 = m.esperarVentana("Reporte de carga por integrante");
        m.escribir(cu12, "Período", "01/07/2026");
        m.escribirSegundo(cu12, "Período", "30/09/2026");
        m.presionar(cu12, "Consultar");
        Manejador.pausa(800);
        m.capturar("20-cu12-carga.png", cu12);
        JTable carga = m.tabla(cu12, 0);
        verificar("CU12 Total", fila(carga, "Total", 2, 3), "28,5 | 29");
        verificar("CU12 primera fila (orden por nombre)", String.valueOf(Manejador.enEDT(() -> carga.getValueAt(0, 0))),
                "Diego Ferreyra");
        Manejador.enEDT(() -> {
            Manejador.buscar(cu12, JTabbedPane.class, t -> true).setSelectedIndex(1);
            return null;
        });
        m.escribir(cu12, "Abiertas hace más de", "7");
        m.presionar(cu12, "Buscar");
        Manejador.pausa(800);
        m.capturar("20-cu12-demoradas.png", cu12);
        m.presionar(cu12, "Cerrar");
        m.esperarCierre(cu12);
    }

    // ------------------------------------------------------------------
    // PR-S-03: los reportes, solo para el rol de gestión (RNF05, RNF06)
    // ------------------------------------------------------------------

    private void prS03() throws SQLException {
        escribir("");
        escribir("PR-S-03: usuarios sin rol de gestión intentan abrir los reportes (RNF05, RNF06)");
        // Primera barrera, el menú: un usuario de cada rol. Solo gestión tiene las opciones habilitadas
        String[][] usuarios = {
            {"lgomez", "Soporte funcional"}, {"pmedina", "Soporte técnico"}, {"mrojas", "Infraestructura"},
            {"vsosa", "Desarrollo"}, {"scastro", "Responsable de gestión"},
        };
        String negado = "deshabilitada (Solo el rol de gestión)";
        for (String[] u : usuarios) {
            iniciarSesion(u[0]);
            if (u[0].equals("pmedina")) {
                abrirMenuParaCaptura("Reportes", "12-cp-s-03-menu.png", true);
            }
            String esperado = u[1].equals("Responsable de gestión") ? "habilitada" : negado;
            verificar(String.format("Menú, %s (%s)", u[0], u[1]),
                    estadoOpcion("Reportes", "Esfuerzo por cliente...") + " / " + estadoOpcion("Reportes", "Carga por integrante..."),
                    esperado + " / " + esperado);
        }
        // Segunda barrera, el controlador: rechaza el pedido aunque no llegue desde el menú
        try (Connection con = ConexionBD.obtenerConexion()) {
            DatosMaestros maestros = DatosMaestros.cargar(con);
            for (Integrante integrante : maestros.getIntegrantes()) {
                String obtenido;
                try {
                    new ControladorReportes(integrante, maestros).cargaPorIntegrante(
                            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30));
                    obtenido = "Accede";
                } catch (ReglaNegocioException e) {
                    obtenido = "Rechaza: " + e.getMessage();
                }
                String esperado = integrante.getRol() == Rol.RESPONSABLE_GESTION ? "Accede"
                        : "Rechaza: Los reportes están restringidos al rol de gestión";
                verificar(String.format("Controlador, %s (%s)", integrante.getUsuario(), integrante.getRol()),
                        obtenido, esperado);
            }
        }
    }

    /** "habilitada", o "deshabilitada" con la explicación que muestra la opción. */
    private String estadoOpcion(String menu, String opcion) {
        return Manejador.enEDT(() -> {
            for (int i = 0; i < principal.getJMenuBar().getMenuCount(); i++) {
                JMenu j = principal.getJMenuBar().getMenu(i);
                if (j.getText().equals(menu)) {
                    for (java.awt.Component c : j.getMenuComponents()) {
                        if (c instanceof JMenuItem mi && mi.getText().equals(opcion)) {
                            return mi.isEnabled() ? "habilitada" : "deshabilitada (" + mi.getToolTipText() + ")";
                        }
                    }
                }
            }
            throw new IllegalStateException("No está la opción " + menu + " > " + opcion);
        });
    }

    // ------------------------------------------------------------------
    // Pasos comunes
    // ------------------------------------------------------------------

    /** Abre una solicitud desde la bandeja o, si no está en la bandeja, por su número. */
    private Window abrirSolicitud(int id) {
        m.seleccionarFila(principal, id);
        m.presionar(principal, "Abrir solicitud");
        return m.esperarVentana("Solicitud " + id);
    }

    /** Abre una solicitud con la opción del menú, como se hace con las cerradas. */
    private Window abrirPorNumero(int id) {
        elegirMenu("Solicitudes", "Abrir solicitud por número...");
        JDialog pregunta = m.esperarMensaje();
        Manejador.enEDT(() -> {
            Manejador.buscar(pregunta, JTextField.class, t -> true).setText(String.valueOf(id));
            return null;
        });
        m.aceptar(pregunta);
        return m.esperarVentana("Solicitud " + id);
    }

    private void cerrarPantalla(Window pantalla) {
        m.presionar(pantalla, "Volver");
        m.esperarCierre(pantalla);
    }

    private void registrarIntervencion(Window solicitud, String tipo, String horas, String descripcion,
            String tipoComunicacion, String comunicacion, String captura) {
        m.presionar(solicitud, "Registrar intervención...");
        Window cu06 = m.esperarVentana("Registrar intervención");
        llenarIntervencion(cu06, tipo, horas, descripcion, tipoComunicacion, comunicacion);
        if (captura != null) {
            m.capturar(captura, cu06);
        }
        m.presionar(cu06, "Registrar intervención");
        JDialog mensaje = m.esperarMensaje();
        String texto = m.textoMensaje(mensaje);
        m.aceptar(mensaje);
        verificar("CU06 " + tipo + " " + horas + " h", texto.startsWith("Intervención registrada.") ? "Registrada" : texto,
                "Registrada");
        m.esperarCierre(cu06);
    }

    private void llenarIntervencion(Window pantalla, String tipo, String horas, String descripcion,
            String tipoComunicacion, String comunicacion) {
        m.elegir(pantalla, "Tipo de intervención", tipo);
        m.escribir(pantalla, "Tiempo dedicado", horas);
        m.escribir(pantalla, "Descripción", descripcion);
        if (tipoComunicacion != null) {
            m.marcar(pantalla, "Agregar una comunicación");
            m.marcar(pantalla, tipoComunicacion);
            m.escribir(pantalla, "Texto", comunicacion);
        }
    }

    private void registrarHito(Window solicitud, String tipo, String referencia, String captura) {
        m.presionar(solicitud, "Registrar hito...");
        Window cu08 = m.esperarVentana("Registrar hito técnico");
        m.elegir(cu08, "Tipo de hito", tipo);
        m.escribir(cu08, "Referencia", referencia);
        if (captura != null) {
            m.capturar(captura, cu08);
        }
        m.presionar(cu08, "Registrar hito");
        m.esperarCierre(cu08);
        verificar("CU08 " + tipo, "Registrado", "Registrado");
    }

    private void elegirMenu(String menu, String opcion) {
        JMenuItem item = Manejador.enEDT(() -> {
            for (int i = 0; i < principal.getJMenuBar().getMenuCount(); i++) {
                JMenu j = principal.getJMenuBar().getMenu(i);
                if (j.getText().equals(menu)) {
                    for (java.awt.Component c : j.getMenuComponents()) {
                        if (c instanceof JMenuItem mi && mi.getText().equals(opcion)) {
                            return mi;
                        }
                    }
                }
            }
            throw new IllegalStateException("No está la opción " + menu + " > " + opcion);
        });
        SwingUtilities.invokeLater(item::doClick);
    }

    /** Abre un menú para mostrar qué opciones habilita el rol, lo captura y lo cierra. */
    private void abrirMenuParaCaptura(String menu, String captura, boolean hastaLaBandeja) {
        Manejador.enEDT(() -> {
            for (int i = 0; i < principal.getJMenuBar().getMenuCount(); i++) {
                JMenu j = principal.getJMenuBar().getMenu(i);
                if (j.getText().equals(menu)) {
                    j.doClick();
                }
            }
            return null;
        });
        if (hastaLaBandeja) {
            JTable bandeja = m.tabla(principal, 0);
            int filas = Manejador.enEDT(bandeja::getRowCount);
            m.capturarFranja(captura, 0, m.altoHastaLaFila(principal, bandeja, filas - 1) + 40, principal);
        } else {
            m.capturar(captura, principal);
        }
        Manejador.enEDT(() -> {
            MenuSelectionManager.defaultManager().clearSelectedPath();
            return null;
        });
    }

    /** Valores de algunas columnas de la fila cuya primera columna es el texto indicado. */
    private static String fila(JTable tabla, String primera, int... columnas) {
        return Manejador.enEDT(() -> {
            for (int i = 0; i < tabla.getRowCount(); i++) {
                if (primera.equals(String.valueOf(tabla.getValueAt(i, 0)))) {
                    List<String> valores = new ArrayList<>();
                    for (int c : columnas) {
                        valores.add(String.valueOf(tabla.getValueAt(i, c)));
                    }
                    return String.join(" | ", valores);
                }
            }
            return "(no está la fila " + primera + ")";
        });
    }

    private static String consultar(Connection con, String sql, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object valor = rs.getObject(1);
                return valor instanceof BigDecimal b ? b.toPlainString() : String.valueOf(valor);
            }
        }
    }

    /** Fecha de hoy a la hora indicada, o la hora actual si todavía no llegó. */
    private static String fechaDeHoy(int hora, int minuto) {
        LocalDateTime propuesta = LocalDate.now().atTime(hora, minuto);
        LocalDateTime ahora = LocalDateTime.now().withSecond(0).withNano(0);
        return (propuesta.isAfter(ahora) ? ahora : propuesta).format(Formatos.FECHA_HORA);
    }

    private void verificar(String que, String obtenido, String esperado) {
        ejecuciones++;
        boolean paso = esperado.equals(obtenido);
        if (!paso) {
            fallas.add(que + ": se esperaba [" + esperado + "] y se obtuvo [" + obtenido + "]");
        }
        escribir(String.format("  %-6s %s: %s", paso ? "PASÓ" : "FALLÓ", que, obtenido));
    }

    private void escribir(String linea) {
        System.out.println(linea);
        salida.println(linea);
    }
}
