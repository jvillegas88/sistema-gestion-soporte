package sgs.modelo.catalogo;

/** Prioridad de la solicitud con su criterio explícito (tabla prioridades, RF13). */
public enum Prioridad implements Catalogo {
    URGENTE(1, "Urgente", "El servicio del cliente está detenido en producción o hay riesgo sobre los datos"),
    ALTA(2, "Alta", "Un error impide una operación y no hay alternativa"),
    NORMAL(3, "Normal", "Error con alternativa, o consulta funcional"),
    BAJA(4, "Baja", "Pedidos de presupuesto y cambios que se pueden planificar");

    private final int id;
    private final String nombre;
    private final String criterio;

    Prioridad(int id, String nombre, String criterio) {
        this.id = id;
        this.nombre = nombre;
        this.criterio = criterio;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getNombre() {
        return nombre;
    }

    /** Criterio que justifica la prioridad, para mostrarlo al clasificar (P09). */
    public String getCriterio() {
        return criterio;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
