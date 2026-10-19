package sgs.modelo;

/** Persona de un cliente que escribe a soporte (tabla solicitantes). El cliente de la solicitud sale de acá. */
public class Solicitante {

    private int id;
    private final Cliente cliente;
    private final String nombre;
    private final String correo;

    /** Solicitante nuevo, que todavía no está en la base (CU01, alternativa 3.A). */
    public Solicitante(Cliente cliente, String nombre, String correo) {
        this(0, cliente, nombre, correo);
    }

    /** Solicitante leído de la base. */
    public Solicitante(int id, Cliente cliente, String nombre, String correo) {
        this.id = id;
        this.cliente = cliente;
        this.nombre = nombre;
        this.correo = correo;
    }

    public int getId() {
        return id;
    }

    /** Lo usa el DAO para guardar la clave que asignó la base. */
    public void setId(int id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Solicitante s && s.id == id && id != 0;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return nombre + " (" + correo + ")";
    }
}
