package sgs.modelo;

/** Organización que recibe el soporte (tabla clientes). */
public class Cliente {

    /** Nombre del cliente al que van las solicitudes importadas sin cliente (CU02, alternativa 8.A). */
    public static final String NO_IDENTIFICADO = "Cliente no identificado";

    private final int id;
    private final String nombre;
    private final String dominioCorreo;

    public Cliente(int id, String nombre, String dominioCorreo) {
        this.id = id;
        this.nombre = nombre;
        this.dominioCorreo = dominioCorreo;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDominioCorreo() {
        return dominioCorreo;
    }

    /** Indica si un correo es del dominio del cliente, para proponerlo al registrar un solicitante. */
    public boolean correspondeAlCorreo(String correo) {
        if (dominioCorreo == null || correo == null) {
            return false;
        }
        return correo.toLowerCase().endsWith("@" + dominioCorreo.toLowerCase());
    }

    public boolean esNoIdentificado() {
        return NO_IDENTIFICADO.equals(nombre);
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Cliente c && c.id == id;
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
