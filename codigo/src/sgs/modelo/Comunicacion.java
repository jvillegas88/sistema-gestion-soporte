package sgs.modelo;

import sgs.modelo.catalogo.TipoComunicacion;

/**
 * Texto que se escribe dentro de una intervención, como nota interna o como respuesta al
 * cliente (RF12). No hereda de RegistroSolicitud: su solicitud, su autor y su fecha son los
 * de la intervención en la que se escribió.
 */
public class Comunicacion {

    private int id;
    private final TipoComunicacion tipo;
    private final String texto;

    public Comunicacion(TipoComunicacion tipo, String texto) {
        this(0, tipo, texto);
    }

    public Comunicacion(int id, TipoComunicacion tipo, String texto) {
        this.id = id;
        this.tipo = tipo;
        this.texto = texto;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TipoComunicacion getTipo() {
        return tipo;
    }

    public String getTexto() {
        return texto;
    }

    public boolean esRespuestaAlCliente() {
        return tipo == TipoComunicacion.RESPUESTA_CLIENTE;
    }
}
