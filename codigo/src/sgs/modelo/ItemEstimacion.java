package sgs.modelo;

import java.math.BigDecimal;

/** Ítem de una estimación con sus horas estimadas (tabla items_estimacion). */
public class ItemEstimacion {

    private final int numero;
    private final String descripcion;
    private final BigDecimal horas;

    public ItemEstimacion(int numero, String descripcion, BigDecimal horas) {
        this.numero = numero;
        this.descripcion = descripcion;
        this.horas = horas;
    }

    public int getNumero() {
        return numero;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getHoras() {
        return horas;
    }
}
