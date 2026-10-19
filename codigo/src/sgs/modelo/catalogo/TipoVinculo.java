package sgs.modelo.catalogo;

/**
 * Relación entre una solicitud y su solicitud de origen. En la base es una columna ENUM,
 * por eso no tiene id: el DAO guarda el nombre de la constante.
 */
public enum TipoVinculo {
    DERIVADA("Derivada"),
    DUPLICADA("Duplicada"),
    CONTINUACION("Continuación");

    private final String nombre;

    TipoVinculo(String nombre) {
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
