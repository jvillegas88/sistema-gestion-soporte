package sgs.modelo.catalogo;

/** Estado de la solicitud, según el diagrama de estados de la sección 10 (tabla estados). */
public enum Estado implements Catalogo {
    NUEVA(1, "Nueva", false),
    EN_CURSO(2, "En curso", false),
    EN_ESPERA(3, "En espera", false),
    RESUELTA(4, "Resuelta", true),
    CERRADA_SIN_CONFIRMACION(5, "Cerrada sin confirmación", true),
    CANCELADA(6, "Cancelada", true),
    DESCARTADA(7, "Descartada", true),
    DUPLICADA(8, "Duplicada", true);

    private final int id;
    private final String nombre;
    private final boolean cierre;

    Estado(int id, String nombre, boolean cierre) {
        this.id = id;
        this.nombre = nombre;
        this.cierre = cierre;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getNombre() {
        return nombre;
    }

    /** Corresponde a la columna es_cierre: la solicitud ya no está abierta. */
    public boolean esCierre() {
        return cierre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
