package sgs.modelo.catalogo;

/** Tipo de solicitud que asigna la clasificación (tabla tipos_solicitud, CU03). */
public enum TipoSolicitud implements Catalogo {
    CONSULTA_FUNCIONAL(1, "Consulta funcional"),
    ERROR_DESARROLLO(2, "Error de desarrollo"),
    ERROR_INFRAESTRUCTURA(3, "Error de infraestructura"),
    PEDIDO_PRESUPUESTO(4, "Pedido de presupuesto"),
    PEDIDO_INFRAESTRUCTURA(5, "Pedido de infraestructura");

    private final int id;
    private final String nombre;

    TipoSolicitud(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getNombre() {
        return nombre;
    }

    /** Los errores exigen el hito de publicación en producción para cerrarse (CU09). */
    public boolean esError() {
        return this == ERROR_DESARROLLO || this == ERROR_INFRAESTRUCTURA;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
