package sgs.modelo;

/** Módulo de un producto (tabla modulos). Se identifica por su producto y su número. */
public class Modulo {

    private final Producto producto;
    private final int numero;
    private final String nombre;

    /** Lo crea Producto.agregarModulo(), por la composición. */
    Modulo(Producto producto, int numero, String nombre) {
        this.producto = producto;
        this.numero = numero;
        this.nombre = nombre;
    }

    public Producto getProducto() {
        return producto;
    }

    public int getNumero() {
        return numero;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Modulo m && m.numero == numero && m.producto.getId() == producto.getId();
    }

    @Override
    public int hashCode() {
        return producto.getId() * 1000 + numero;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
