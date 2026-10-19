package sgs.modelo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sgs.modelo.catalogo.Rol;

/** Persona del equipo que usa el sistema, con su rol y su usuario (tabla integrantes, RNF06). */
public class Integrante {

    private final int id;
    private final Rol rol;
    private final String nombre;
    private final String usuario;
    private final String claveHash;
    private final boolean activo;

    public Integrante(int id, Rol rol, String nombre, String usuario, String claveHash, boolean activo) {
        this.id = id;
        this.rol = rol;
        this.nombre = nombre;
        this.usuario = usuario;
        this.claveHash = claveHash;
        this.activo = activo;
    }

    public int getId() {
        return id;
    }

    public Rol getRol() {
        return rol;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUsuario() {
        return usuario;
    }

    public boolean estaActivo() {
        return activo;
    }

    /**
     * Compara la clave ingresada con la guardada. La base no guarda la clave sino su
     * resumen SHA-256, así que se calcula el resumen de lo ingresado y se comparan los dos.
     */
    public boolean verificarClave(String clave) {
        if (clave == null) {
            return false;
        }
        return calcularHash(clave).equalsIgnoreCase(claveHash);
    }

    /** Resumen SHA-256 en hexadecimal, el mismo que calcula SHA2(clave, 256) en MySQL. */
    private static String calcularHash(String clave) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(clave.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : resumen) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // Toda máquina virtual de Java trae SHA-256: si falta, la instalación está rota
            throw new IllegalStateException("La máquina virtual no tiene el algoritmo SHA-256", e);
        }
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Integrante i && i.id == id;
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
