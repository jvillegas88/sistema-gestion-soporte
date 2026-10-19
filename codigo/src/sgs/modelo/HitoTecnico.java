package sgs.modelo;

import java.time.LocalDateTime;
import sgs.modelo.catalogo.TipoHito;
import sgs.modelo.excepcion.ReglaNegocioException;

/**
 * Hito técnico de la solicitud: corrección, publicación en testing o en producción, o
 * intervención en el servidor (RF11, CU08). La solicitud del deploy se guarda por su número,
 * para no cargar en memoria otra solicitud completa.
 */
public class HitoTecnico extends RegistroSolicitud {

    public static final int LARGO_REFERENCIA = 120;

    private final TipoHito tipo;
    private final String referencia;
    private final Integer solicitudDeployId;

    public HitoTecnico(Solicitud solicitud, Integrante autor, TipoHito tipo, String referencia,
            Integer solicitudDeployId) {
        super(solicitud, autor);
        this.tipo = tipo;
        this.referencia = referencia;
        this.solicitudDeployId = solicitudDeployId;
    }

    public HitoTecnico(int id, Solicitud solicitud, Integrante autor, LocalDateTime fecha, TipoHito tipo,
            String referencia, Integer solicitudDeployId) {
        super(id, solicitud, autor, fecha);
        this.tipo = tipo;
        this.referencia = referencia;
        this.solicitudDeployId = solicitudDeployId;
    }

    public TipoHito getTipo() {
        return tipo;
    }

    public String getReferencia() {
        return referencia;
    }

    public Integer getSolicitudDeployId() {
        return solicitudDeployId;
    }

    @Override
    public void validar() throws ReglaNegocioException {
        if (tipo == null) {
            throw new ReglaNegocioException("Seleccioná el tipo de hito");
        }
        if (referencia != null && referencia.length() > LARGO_REFERENCIA) {
            throw new ReglaNegocioException("La referencia admite hasta " + LARGO_REFERENCIA + " caracteres");
        }
        if (solicitudDeployId != null && solicitudDeployId == solicitud.getId()) {
            throw new ReglaNegocioException("La solicitud del deploy tiene que ser otra solicitud");
        }
    }

    @Override
    public String getTipoRegistro() {
        return "Hito técnico";
    }

    @Override
    public String getDetalle() {
        String detalle = tipo.getNombre();
        if (!Formatos.estaVacio(referencia)) {
            detalle += ": " + referencia;
        }
        if (solicitudDeployId != null) {
            detalle += " (deploy en la solicitud " + solicitudDeployId + ")";
        }
        return detalle;
    }
}
