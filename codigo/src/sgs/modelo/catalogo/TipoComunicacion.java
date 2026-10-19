package sgs.modelo.catalogo;

/** Tipo de comunicación de una intervención (columna ENUM de comunicaciones, RF12). */
public enum TipoComunicacion {
    NOTA_INTERNA("Nota interna"),
    RESPUESTA_CLIENTE("Respuesta al cliente");

    private final String nombre;

    TipoComunicacion(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
