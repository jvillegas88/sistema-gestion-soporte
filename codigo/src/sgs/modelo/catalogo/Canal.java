package sgs.modelo.catalogo;

/** Canal por el que entró la solicitud (tabla canales). */
public enum Canal implements Catalogo {
    CORREO(1, "Correo"),
    DETECCION_INTERNA(2, "Detección interna");

    private final int id;
    private final String nombre;

    Canal(int id, String nombre) {
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
