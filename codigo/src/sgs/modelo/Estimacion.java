package sgs.modelo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ítems de una intervención de tipo Estimación. No tiene tabla propia: agrupa los ítems
 * y calcula el total, que la base no guarda. Las horas estimadas no son tiempo trabajado
 * (CU06, observación 3).
 */
public class Estimacion {

    private final List<ItemEstimacion> items = new ArrayList<>();

    public void agregarItem(ItemEstimacion item) {
        items.add(item);
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
