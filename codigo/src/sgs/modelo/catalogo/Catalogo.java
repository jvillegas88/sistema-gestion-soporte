package sgs.modelo.catalogo;

/**
 * Valor de un catálogo de la base de datos (roles, estados, prioridades y los demás).
 * Cada enumeración que la implementa tiene una constante por fila de su tabla,
 * con el mismo id y el mismo nombre, así los DAO traducen la clave foránea.
 */
public interface Catalogo {

    /** Clave de la fila en su tabla de catálogo. */
    int getId();

    /** Nombre con el que se muestra en la interfaz, igual al de la tabla. */
    String getNombre();

    /**
     * Devuelve la constante de la enumeración que tiene ese id.
     * Recorre el arreglo de valores con una búsqueda secuencial. Son ocho como máximo, y así
     * el resultado no depende de que las constantes estén declaradas en el orden de los id.
     *
     * @throws IllegalArgumentException si ningún valor tiene ese id, lo que indica
     *         que la base y el código no coinciden
     */
    static <E extends Enum<E> & Catalogo> E porId(Class<E> tipo, int id) {
        E[] valores = tipo.getEnumConstants();
        for (int i = 0; i < valores.length; i++) {
            if (valores[i].getId() == id) {
                return valores[i];
            }
        }
        throw new IllegalArgumentException(tipo.getSimpleName() + " no tiene un valor con id " + id);
    }
}
