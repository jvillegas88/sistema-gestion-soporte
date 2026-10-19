package sgs.controlador;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import sgs.modelo.Integrante;
import sgs.modelo.consulta.ResumenSolicitud;
import sgs.persistencia.DatosMaestros;
import sgs.persistencia.SolicitudDAO;

/** Bandeja de la ventana principal: las solicitudes abiertas, para elegir cuál abrir. */
public class ControladorBandeja extends Controlador {

    private final SolicitudDAO solicitudDAO;

    public ControladorBandeja(Integrante usuario, DatosMaestros maestros, SolicitudDAO solicitudDAO) {
        super(usuario, maestros);
        this.solicitudDAO = solicitudDAO;
    }

    /**
     * Solicitudes abiertas ordenadas por prioridad. Si se pide, solo las que tienen como
     * responsable al usuario.
     */
    public List<ResumenSolicitud> listarAbiertas(boolean soloLasMias) throws SQLException {
        List<ResumenSolicitud> todas = consultar(con -> solicitudDAO.listarAbiertas(con));
        if (!soloLasMias) {
            return todas;
        }
        List<ResumenSolicitud> mias = new ArrayList<>();
        for (ResumenSolicitud r : todas) {
            if (r.getResponsableId() == usuario.getId()) {
                mias.add(r);
            }
        }
        return mias;
    }
}
