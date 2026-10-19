package sgs.modelo.catalogo;

/** De qué o de quién se espera mientras la solicitud está detenida (tabla motivos_espera, CU07). */
public enum MotivoEspera implements Catalogo {
    CLIENTE(1, "Espera del cliente"),
    INTERNA(2, "Espera interna"),
    VENTANA_DEPLOY(3, "Ventana de deploy");

    private final int id;
    private final String nombre;

    MotivoEspera(int id, String nombre) {
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
