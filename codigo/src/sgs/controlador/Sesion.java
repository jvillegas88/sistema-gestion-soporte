package sgs.controlador;

import sgs.modelo.Integrante;
import sgs.persistencia.DatosMaestros;
import sgs.persistencia.SolicitudDAO;

/**
 * Lo que dura mientras el integrante tiene la sesión abierta: quién es, los datos maestros y
 * el DAO de solicitudes. Crea los controladores de cada caso de uso, así las pantallas no
 * tienen que conocer las clases de persistencia.
 */
public class Sesion {

    private final Integrante usuario;
    private final DatosMaestros maestros;
    private final SolicitudDAO solicitudDAO;

    public Sesion(Integrante usuario, DatosMaestros maestros) {
        this.usuario = usuario;
        this.maestros = maestros;
        this.solicitudDAO = new SolicitudDAO(maestros);
    }

    public Integrante getUsuario() {
        return usuario;
    }

    /** Nombre y rol, como aparecen en el encabezado de cada pantalla. */
    public String getDescripcionUsuario() {
        return usuario.getNombre() + " (" + usuario.getRol() + ")";
    }

    public ControladorBandeja bandeja() {
        return new ControladorBandeja(usuario, maestros, solicitudDAO);
    }

    public ControladorRegistrarSolicitud registrarSolicitud() {
        return new ControladorRegistrarSolicitud(usuario, maestros, solicitudDAO);
    }

    public ControladorImportarHistorico importarHistorico() {
        return new ControladorImportarHistorico(usuario, maestros, solicitudDAO);
    }

    public ControladorClasificarSolicitud clasificarSolicitud() {
        return new ControladorClasificarSolicitud(usuario, maestros, solicitudDAO);
    }

    public ControladorRegistrarIntervencion registrarIntervencion() {
        return new ControladorRegistrarIntervencion(usuario, maestros, solicitudDAO);
    }

    public ControladorEsperaEstado esperaEstado() {
        return new ControladorEsperaEstado(usuario, maestros, solicitudDAO);
    }

    public ControladorRegistrarHito registrarHito() {
        return new ControladorRegistrarHito(usuario, maestros, solicitudDAO);
    }

    public ControladorCerrarSolicitud cerrarSolicitud() {
        return new ControladorCerrarSolicitud(usuario, maestros, solicitudDAO);
    }

    public ControladorReportes reportes() {
        return new ControladorReportes(usuario, maestros);
    }
}
