package sgs.modelo.consulta;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Una fila del reporte de esfuerzo por cliente (CU11): lo que consumió un cliente en el período. */
public class FilaEsfuerzoCliente {

    private final String cliente;
    private int solicitudes;
    private BigDecimal horasTrabajadas = BigDecimal.ZERO;
    private BigDecimal diasEspera = BigDecimal.ZERO;
    private BigDecimal horasComparacion;

    public FilaEsfuerzoCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getCliente() {
        return cliente;
    }

    public int getSolicitudes() {
        return solicitudes;
    }

    public void setSolicitudes(int solicitudes) {
        this.solicitudes = solicitudes;
    }

    public BigDecimal getHorasTrabajadas() {
        return horasTrabajadas;
    }

    public void setHorasTrabajadas(BigDecimal horasTrabajadas) {
        this.horasTrabajadas = horasTrabajadas;
    }

    public BigDecimal getDiasEspera() {
        return diasEspera;
    }

    public void setDiasEspera(BigDecimal diasEspera) {
        this.diasEspera = diasEspera;
    }

    /** Horas del período de comparación. Es null si no se pidió comparar (CU11, alternativa 3.A). */
    public BigDecimal getHorasComparacion() {
        return horasComparacion;
    }

    public void setHorasComparacion(BigDecimal horasComparacion) {
        this.horasComparacion = horasComparacion;
    }

    /**
     * Variación porcentual de las horas respecto del período de comparación.
     * Devuelve null si no hay comparación o si en ese período no hubo horas.
     */
    public BigDecimal getVariacion() {
        return calcularVariacion(horasTrabajadas, horasComparacion);
    }

    public static BigDecimal calcularVariacion(BigDecimal actual, BigDecimal anterior) {
        if (anterior == null || anterior.signum() == 0) {
            return null;
        }
        return actual.subtract(anterior).multiply(BigDecimal.valueOf(100)).divide(anterior, 1, RoundingMode.HALF_UP);
    }
}
