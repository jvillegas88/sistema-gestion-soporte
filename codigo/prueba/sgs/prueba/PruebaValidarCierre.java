package sgs.prueba;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import sgs.modelo.Cliente;
import sgs.modelo.Comunicacion;
import sgs.modelo.Estimacion;
import sgs.modelo.HitoTecnico;
import sgs.modelo.Integrante;
import sgs.modelo.Intervencion;
import sgs.modelo.ItemEstimacion;
import sgs.modelo.Asignacion;
import sgs.modelo.CambioEstado;
import sgs.modelo.Producto;
import sgs.modelo.Solicitante;
import sgs.modelo.Solicitud;
import sgs.modelo.catalogo.Canal;
import sgs.modelo.catalogo.Estado;
import sgs.modelo.catalogo.Prioridad;
import sgs.modelo.catalogo.Rol;
import sgs.modelo.catalogo.TipoComunicacion;
import sgs.modelo.catalogo.TipoHito;
import sgs.modelo.catalogo.TipoIntervencion;
import sgs.modelo.catalogo.TipoSolicitud;

/**
 * Prueba de caja blanca CP-B-01 (sección 12.3): ejecuta validarCierre() de Solicitud con los
 * datos diseñados para cubrir todas las sentencias y todas las decisiones. Cada dato se arma en
 * memoria, sin base de datos ni pantalla, como prevé el plan de pruebas.
 * <p>
 * El dato 6 se agregó en el TP3 con la decisión D0: una solicitud sin tipo no se puede cerrar
 * como Resuelta ni como Cerrada sin confirmación.
 * No usa JUnit, que el docente no exige: compara lo obtenido con lo esperado e informa.
 * <p>
 * Uso: codigo/probar.sh
 */
public class PruebaValidarCierre {

    private static final Cliente CLIENTE = new Cliente(2, "Cliente A", "cliente-a.com.ar");
    private static final Solicitante SOLICITANTE = new Solicitante(1, CLIENTE, "Renata Ortega", "rortega@cliente-a.com.ar");
    private static final Producto PRODUCTO = new Producto(1, "Producto A", "soporte@ejemplo.com.ar");
    private static final Integrante RESPONSABLE = new Integrante(1, Rol.SOPORTE_FUNCIONAL, "Laura Gómez", "lgomez", "", true);
    private static final Integrante OTRO = new Integrante(2, Rol.SOPORTE_TECNICO, "Diego Ferreyra", "dferreyra", "", true);
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 1, 9, 0);

    private static int idRegistro = 1;

    public static void main(String[] args) {
        System.out.println("CP-B-01 Prueba de caja blanca de Solicitud.validarCierre()");
        System.out.println();
        int pasaron = 0;
        pasaron += probar(1, "Error de desarrollo, con respuesta, intervención y hito",
                armar(TipoSolicitud.ERROR_DESARROLLO, true, true, true, 0), true, List.of());
        pasaron += probar(2, "Error de infraestructura, sin respuesta, sin intervención del responsable, sin hito",
                armar(TipoSolicitud.ERROR_INFRAESTRUCTURA, false, false, false, 0), false,
                List.of("respuesta al cliente", "intervención del responsable", "hito de publicación en producción"));
        pasaron += probar(3, "Pedido de presupuesto con estimación de dos ítems",
                armar(TipoSolicitud.PEDIDO_PRESUPUESTO, true, true, false, 2), true, List.of());
        pasaron += probar(4, "Pedido de presupuesto con estimación sin ítems",
                armar(TipoSolicitud.PEDIDO_PRESUPUESTO, true, true, false, -1), false,
                List.of("estimación con al menos un ítem"));
        pasaron += probar(5, "Consulta funcional con respuesta e intervención",
                armar(TipoSolicitud.CONSULTA_FUNCIONAL, true, true, false, 0), true, List.of());
        pasaron += probar(6, "Sin tipo, con respuesta e intervención (D0, agregada en el TP3)",
                armar(null, true, true, false, 0), false, List.of("tipo de solicitud"));
        System.out.println();
        System.out.println("Resultado: " + pasaron + " de 6 datos dieron lo esperado");
        System.exit(pasaron == 6 ? 0 : 1);
    }

    private static int probar(int dato, String descripcion, Solicitud solicitud, boolean esperado,
            List<String> faltantesEsperados) {
        boolean obtenido = solicitud.validarCierre();
        List<String> faltantes = solicitud.getFaltantesCierre();
        boolean paso = obtenido == esperado && faltantes.equals(faltantesEsperados);
        System.out.printf("Dato %d: %s%n", dato, descripcion);
        System.out.printf("   esperado %s %s | obtenido %s %s | %s%n", esperado, faltantesEsperados, obtenido,
                faltantes, paso ? "PASÓ" : "FALLÓ");
        return paso ? 1 : 0;
    }

    /**
     * Arma una solicitud En curso cuyo responsable es Laura.
     *
     * @param items cantidad de ítems de la estimación: 0 sin estimación, -1 con una estimación vacía
     */
    private static Solicitud armar(TipoSolicitud tipo, boolean conRespuesta, boolean intervieneResponsable,
            boolean conHitoProduccion, int items) {
        Prioridad prioridad = tipo == null ? null : Prioridad.NORMAL;
        Solicitud s = new Solicitud(100, null, SOLICITANTE, PRODUCTO, null, Canal.CORREO, tipo, prioridad,
                null, null, FECHA, "Solicitud de prueba", null, null);
        s.cargarRegistroGuardado(new CambioEstado(idRegistro++, s, RESPONSABLE, FECHA, Estado.NUEVA, null));
        s.cargarRegistroGuardado(new Asignacion(idRegistro++, s, RESPONSABLE, FECHA, RESPONSABLE));
        s.cargarRegistroGuardado(new CambioEstado(idRegistro++, s, RESPONSABLE, FECHA, Estado.EN_CURSO, null));

        // La intervención la hace el responsable o, si no, otro integrante
        Integrante quien = intervieneResponsable ? RESPONSABLE : OTRO;
        Intervencion analisis = new Intervencion(idRegistro++, s, quien, FECHA, TipoIntervencion.ANALISIS,
                "Análisis del caso", new BigDecimal("1.00"));
        if (conRespuesta) {
            analisis.agregar(new Comunicacion(1, TipoComunicacion.RESPUESTA_CLIENTE, "Respuesta al cliente"));
        }
        s.cargarRegistroGuardado(analisis);

        if (conHitoProduccion) {
            s.cargarRegistroGuardado(new HitoTecnico(idRegistro++, s, OTRO, FECHA,
                    TipoHito.PUBLICACION_PRODUCCION, "Deploy en producción", null));
        }
        if (items != 0) {
            Intervencion estimacion = new Intervencion(idRegistro++, s, quien, FECHA, TipoIntervencion.ESTIMACION,
                    "Estimación del desarrollo", new BigDecimal("2.00"));
            Estimacion e = new Estimacion();
            for (int i = 1; i <= items; i++) {
                e.agregarItem(new ItemEstimacion(i, "Ítem " + i, new BigDecimal("5.00")));
            }
            estimacion.setEstimacion(e);
            s.cargarRegistroGuardado(estimacion);
        }
        return s;
    }
}
