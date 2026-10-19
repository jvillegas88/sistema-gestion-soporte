package sgs.modelo.consulta;

import java.math.BigDecimal;

/**
 * Una fila del reporte de carga por integrante (CU12). Informa carga y no rendimiento: el
 * reporte se ordena por nombre y no compara a los integrantes entre sí (RNF05).
 */
public class FilaCargaIntegrante {

    private final String integrante;
    private final String rol;
    private final BigDecimal horas;
    private final int intervenciones;

    public FilaCargaIntegrante(String integrante, String rol, BigDecimal horas, int intervenciones) {
        this.integrante = integrante;
        this.rol = rol;
        this.horas = horas;
        this.intervenciones = intervenciones;
    }

    public String getIntegrante() {
        return integrante;
    }

    public String getRol() {
        return rol;
    }

    public BigDecimal getHoras() {
        return horas;
    }

    public int getIntervenciones() {
        return intervenciones;
    }
}
