package sgs.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Producto de software al que se le da soporte (tabla productos), con sus módulos. */
public class Producto {

    private final int id;
    private final String nombre;
    private final String correoSoporte;
    private final List<Modulo> modulos = new ArrayList<>();

    public Producto(int id, String nombre, String correoSoporte) {
        this.id = id;
        this.nombre = nombre;
        this.correoSoporte = correoSoporte;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreoSoporte() {
        return correoSoporte;
    }

    /**
     * Crea un módulo del producto. Es una composición: el módulo no existe sin su producto
     * y su número solo identifica dentro del producto.
     */
    public Modulo agregarModulo(int numero, String nombre) {
        Modulo modulo = new Modulo(this, numero, nombre);
        modulos.add(modulo);
        return modulo;
    }

    /** Los módulos se devuelven sin posibilidad de modificar la lista desde afuera. */
    public List<Modulo> getModulos() {
        return Collections.unmodifiableList(modulos);
    }

    /** Busca un módulo por su número. Devuelve null si el producto no lo tiene. */
    public Modulo buscarModulo(int numero) {
        for (Modulo m : modulos) {
            if (m.getNumero() == numero) {
                return m;
            }
        }
        return null;
    }

    /** Verifica que el módulo pertenezca a este producto (CU03, paso 6). */
    public boolean tieneModulo(Modulo modulo) {
        return modulo != null && modulo.getProducto().id == id && buscarModulo(modulo.getNumero()) != null;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Producto p && p.id == id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
