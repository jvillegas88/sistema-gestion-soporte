package sgs.modelo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ítems de una intervención de tipo Estimación. No tiene tabla propia: agrupa los ítems
 * y calcula el total, que la base no guarda. Las horas estimadas no son tiempo trabajado
 * (CU06, observación 3). Se crea con todos sus ítems, que después no cambian.
 */
public class Estimacion {

    private final List<ItemEstimacion> items;

    /** Recibe los ítems en orden y guarda una copia, así la lista no se modifica desde afuera. */
    public Estimacion(List<ItemEstimacion> items) {
        this.items = new ArrayList<>(items);
    }

    public List<ItemEstimacion> getItems() {
        return Collections.unmodifiableList(items);
    }

    public boolean tieneItems() {
        return !items.isEmpty();
    }

    public BigDecimal calcularHorasTotales() {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemEstimacion item : items) {
            total = total.add(item.getHoras());
        }
        return total;
    }
}
