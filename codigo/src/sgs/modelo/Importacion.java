package sgs.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resultado de revisar el archivo del histórico antes de importarlo (CU02, pasos 4 y 5): las
 * solicitudes listas para registrar, las que ya estaban importadas y las rechazadas con su
 * motivo. Con eso el sistema informa qué va a pasar, el actor confirma (paso 7) y después se
 * registran las solicitudes de la lista (paso 8).
 */
public class Importacion {

    private final String archivo;
    private final List<Solicitud> solicitudes = new ArrayList<>();
    private final List<Solicitante> solicitantesNuevos = new ArrayList<>();
    private final List<String> rechazos = new ArrayList<>();
    private int registros;
    private int omitidas;
    private int sinCliente;
    private boolean registrada;

    public Importacion(String archivo) {
        this.archivo = archivo;
    }

    public void contarRegistro() {
        registros++;
    }

    /** Una solicitud que ya estaba importada se omite, para no duplicarla (RNF09). */
    public void omitir() {
        omitidas++;
    }

    public void rechazar(FilaHistorico fila, String motivo) {
        String numero = fila.numeroTalComoVino().isEmpty() ? "sin número" : "número " + fila.numeroTalComoVino();
        // Los motivos que vienen de las reglas del modelo empiezan con mayúscula: acá siguen a los dos puntos
        String texto = motivo.isEmpty() ? motivo : Character.toLowerCase(motivo.charAt(0)) + motivo.substring(1);
        rechazos.add("Registro " + fila.getRegistro() + " (" + numero + "): " + texto);
    }

    public void agregar(Solicitud solicitud) {
        solicitudes.add(solicitud);
        if (solicitud.getCliente().esNoIdentificado()) {
            sinCliente++;
        }
    }

    /** Un solicitante que no estaba en el sistema y se registra junto con sus solicitudes. */
    public void agregarSolicitante(Solicitante solicitante) {
        solicitantesNuevos.add(solicitante);
    }

    public void marcarRegistrada() {
        registrada = true;
    }

    public String getArchivo() {
        return archivo;
    }

    public int getRegistros() {
        return registros;
    }

    public int getOmitidas() {
        return omitidas;
    }

    public int getSinCliente() {
        return sinCliente;
    }

    public boolean estaRegistrada() {
        return registrada;
    }

    public List<Solicitud> getSolicitudes() {
        return Collections.unmodifiableList(solicitudes);
    }

    public List<Solicitante> getSolicitantesNuevos() {
        return Collections.unmodifiableList(solicitantesNuevos);
    }

    public List<String> getRechazos() {
        return Collections.unmodifiableList(rechazos);
    }

    /** Resumen de la importación (paso 9), con los números que se informan al actor. */
    public String resumen() {
        StringBuilder texto = new StringBuilder();
        texto.append(registrada ? "Se importaron " : "Se van a importar ").append(solicitudes.size())
                .append(solicitudes.size() == 1 ? " solicitud" : " solicitudes");
        if (sinCliente > 0) {
            texto.append(" (").append(sinCliente).append(" sin cliente identificado)");
        }
        texto.append(".\n").append(omitidas)
                .append(omitidas == 1 ? " ya estaba importada y se omite.\n" : " ya estaban importadas y se omiten.\n")
                .append(rechazos.size()).append(rechazos.size() == 1 ? " se rechazó." : " se rechazaron.");
        return texto.toString();
    }
}
