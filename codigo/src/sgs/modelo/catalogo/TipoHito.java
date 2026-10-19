package sgs.modelo.catalogo;

/** Hito técnico de una solicitud (tabla tipos_hito, CU08). */
public enum TipoHito implements Catalogo {
    CORRECCION(1, "Corrección"),
    PUBLICACION_TESTING(2, "Publicación en testing"),
    PUBLICACION_PRODUCCION(3, "Publicación en producción"),
    INTERVENCION_SERVIDOR(4, "Intervención en el servidor");

    private final int id;
    private final String nombre;

    TipoHito(int id, String nombre) {
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
