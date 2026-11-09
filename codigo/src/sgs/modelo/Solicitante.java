package sgs.modelo;

import sgs.modelo.excepcion.ReglaNegocioException;

/** Persona de un cliente que escribe a soporte (tabla solicitantes). El cliente de la solicitud sale de acá. */
public class Solicitante {

    /** Largos de las columnas nombre y correo de la tabla solicitantes. */
    public static final int LARGO_NOMBRE = 100;
    public static final int LARGO_CORREO = 120;

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

    /**
     * Verifica los datos de un solicitante nuevo antes de guardarlo, así un dato que la base
     * rechazaría se informa como una regla y no como un error de la base (CU01 y CU02).
     */
    public void validar() throws ReglaNegocioException {
        if (cliente == null || Formatos.estaVacio(nombre) || Formatos.estaVacio(correo)) {
            throw new ReglaNegocioException("Completá el nombre, el correo y el cliente del solicitante");
        }
        if (!Formatos.esCorreoValido(correo)) {
            throw new ReglaNegocioException("El correo no tiene un formato válido");
        }
        if (nombre.length() > LARGO_NOMBRE) {
            throw new ReglaNegocioException("El nombre del solicitante admite hasta " + LARGO_NOMBRE + " caracteres");
        }
        if (correo.length() > LARGO_CORREO) {
            throw new ReglaNegocioException("El correo del solicitante admite hasta " + LARGO_CORREO + " caracteres");
        }
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
