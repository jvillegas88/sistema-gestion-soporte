package sgs.modelo.consulta;

/** Una solicitud abierta demorada, sin respuesta o con una espera prolongada (CU12, alternativa 6.A, RF23). */
public class FilaDemorada {

    private final int id;
    private final String cliente;
    private final String asunto;
    private final String estado;
    private final String responsable;
    private final long diasAbierta;
    private final long diasEnEspera;
    private final String situacion;

    public FilaDemorada(int id, String cliente, String asunto, String estado, String responsable, long diasAbierta,
            long diasEnEspera, String situacion) {
        this.id = id;
        this.cliente = cliente;
        this.asunto = asunto;
        this.estado = estado;
        this.responsable = responsable;
        this.diasAbierta = diasAbierta;
        this.diasEnEspera = diasEnEspera;
        this.situacion = situacion;
    }

    public int getId() {
        return id;
    }

    public String getCliente() {
        return cliente;
    }

    public String getAsunto() {
        return asunto;
    }

    public String getEstado() {
        return estado;
    }

    public String getResponsable() {
        return responsable;
    }

    public long getDiasAbierta() {
        return diasAbierta;
    }

    public long getDiasEnEspera() {
        return diasEnEspera;
    }

    public String getSituacion() {
        return situacion;
    }
}
