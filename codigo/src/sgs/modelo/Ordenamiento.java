package sgs.modelo;

import java.util.Comparator;
import java.util.List;

/**
 * Ordenación por inserción directa (Taller de Algoritmos y Estructuras de Datos I, M4L1),
 * para cualquier lista y cualquier criterio de orden.
 * <p>
 * Se eligió la inserción porque las listas del sistema son cortas y casi siempre llegan casi
 * ordenadas, que es su mejor caso (cercano a O(n)). Además es estable: dos elementos que el
 * criterio considera iguales conservan el orden que tenían.
 */
public final class Ordenamiento {

    private Ordenamiento() {
        // Solo métodos de clase
    }

    /**
     * Ordena la lista en el lugar, de menor a mayor según el criterio.
     * Recorre desde el segundo elemento y desplaza hacia la derecha los anteriores que son
     * mayores, hasta encontrar la posición del elemento actual.
     */
    public static <T> void porInsercion(List<T> lista, Comparator<? super T> criterio) {
        for (int p = 1; p < lista.size(); p++) {
            T actual = lista.get(p);
            int j = p - 1;
            while (j >= 0 && criterio.compare(lista.get(j), actual) > 0) {
                lista.set(j + 1, lista.get(j));
                j--;
            }
            lista.set(j + 1, actual);
        }
    }
}
