package sgs.modelo.catalogo;

/** Tipo de intervención que registra un integrante (tabla tipos_intervencion, CU06). */
public enum TipoIntervencion implements Catalogo {
    ANALISIS(1, "Análisis"),
    RESPUESTA_CLIENTE(2, "Respuesta al cliente"),
    CORRECCION(3, "Corrección"),
    REVISION_CODIGO(4, "Revisión de código"),
    DEPLOY(5, "Deploy"),
    CAMBIO_SERVIDOR(6, "Cambio en el servidor"),
    ESTIMACION(7, "Estimación"),
    REUNION_CLIENTE(8, "Reunión con el cliente");

    private final int id;
    private final String nombre;

    TipoIntervencion(int id, String nombre) {
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

    @Override
    public String toString() {
        return nombre;
    }
}
