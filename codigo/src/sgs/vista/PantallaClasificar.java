package sgs.vista;

import java.awt.GridBagConstraints;
import java.awt.Window;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import sgs.controlador.ControladorClasificarSolicitud;
import sgs.controlador.Sesion;
import sgs.modelo.Modulo;
import sgs.modelo.Solicitud;
import sgs.modelo.catalogo.Prioridad;
import sgs.modelo.catalogo.TipoSolicitud;

/**
 * CU03 Clasificar solicitud: tipo, módulo y prioridad con su criterio explícito (RF13). Si la
 * solicitud ya estaba clasificada, pide el motivo de la reclasificación (alternativa 9.A).
 */
public final class PantallaClasificar extends Pantalla {

    private static final long serialVersionUID = 1L;

    private final transient ControladorClasificarSolicitud controlador;
    private final JComboBox<TipoSolicitud> cmbTipo = new JComboBox<>(TipoSolicitud.values());
    private final JComboBox<Prioridad> cmbPrioridad = new JComboBox<>(Prioridad.values());
    private final JComboBox<Object> cmbModulo = new JComboBox<>();
    private final JLabel lblCriterio = Estilo.nota(" ");
    private final JTextArea txtMotivo = Estilo.areaDeTexto(2);

    /** Lee la solicitud y crea la pantalla. Si no se puede leer, avisa y devuelve null. */
    public static PantallaClasificar crear(Window duenio, Sesion sesion, int solicitudId) {
        ControladorClasificarSolicitud controlador = sesion.clasificarSolicitud();
        if (!Dialogos.ejecutar(duenio, () -> controlador.abrirSolicitud(solicitudId))) {
            return null;
        }
        return new PantallaClasificar(duenio, sesion, controlador);
    }

    private PantallaClasificar(Window duenio, Sesion sesion, ControladorClasificarSolicitud controlador) {
        super(duenio, sesion, "Clasificar solicitud");
        this.controlador = controlador;
        Solicitud s = controlador.getSolicitud();

        cmbTipo.setSelectedItem(s.getTipo());
        if (s.getTipo() == null) {
            cmbTipo.setSelectedIndex(-1);
        }
        cmbPrioridad.setSelectedItem(s.getPrioridad());
        if (s.getPrioridad() == null) {
            cmbPrioridad.setSelectedIndex(-1);
        }
        cmbModulo.addItem(SIN_DEFINIR);
        for (Modulo m : s.getProducto().getModulos()) {
            cmbModulo.addItem(m);
        }
        cmbModulo.setSelectedItem(s.getModulo() == null ? SIN_DEFINIR : s.getModulo());
        cmbPrioridad.addActionListener(e -> mostrarCriterio());
        mostrarCriterio();

        JPanel datos = Estilo.grupo("Clasificación");
        int h = GridBagConstraints.HORIZONTAL;
        int n = GridBagConstraints.NONE;
        Estilo.poner(datos, Estilo.etiqueta("Tipo de solicitud", true), 0, 0, 1, 0, n);
        Estilo.poner(datos, cmbTipo, 1, 0, 1, 1, h);
        Estilo.poner(datos, Estilo.etiqueta("Módulo", false), 0, 1, 1, 0, n);
        Estilo.poner(datos, Estilo.fila(cmbModulo, Estilo.nota("Puede quedar sin definir hasta que el análisis lo determine")), 1, 1, 1, 1, h);
        Estilo.poner(datos, Estilo.etiqueta("Prioridad", true), 0, 2, 1, 0, n);
        Estilo.poner(datos, cmbPrioridad, 1, 2, 1, 1, h);
        Estilo.poner(datos, lblCriterio, 1, 3, 1, 1, h);
        boolean reclasifica = controlador.esReclasificacion();
        Estilo.ponerEtiquetaArriba(datos, Estilo.etiqueta("Motivo del cambio", reclasifica), 0, 4);
        Estilo.ponerArea(datos, Estilo.conDesplazamiento(txtMotivo), 1, 4, 1, 1);
        txtMotivo.setEnabled(reclasifica);
        Estilo.poner(datos, Estilo.nota(reclasifica
                ? "La solicitud ya estaba clasificada: la clasificación anterior queda en el historial con este motivo."
                : "Es la primera clasificación: queda registrada con tu nombre y la fecha."), 1, 5, 1, 1, h);

        JPanel cuerpo = cuerpoVertical();
        cuerpo.add(grupoSolicitud(s));
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(datos);

        JButton cancelar = Estilo.boton("Cancelar");
        JButton guardar = Estilo.boton("Guardar clasificación");
        cancelar.addActionListener(e -> dispose());
        guardar.addActionListener(e -> guardar());
        armar("Clasificar solicitud", cuerpo, Estilo.botonera("* Datos obligatorios", cancelar, guardar));
    }

    private static final String SIN_DEFINIR = "(sin definir)";

    /** Muestra el criterio de la prioridad elegida, para que la clasificación sea consistente (P09). */
    private void mostrarCriterio() {
        Prioridad p = (Prioridad) cmbPrioridad.getSelectedItem();
        lblCriterio.setText(p == null ? " " : "Criterio: " + p.getCriterio());
    }

    private void guardar() {
        Object modulo = cmbModulo.getSelectedItem();
        ejecutar(() -> {
            controlador.clasificar((TipoSolicitud) cmbTipo.getSelectedItem(), (Prioridad) cmbPrioridad.getSelectedItem(),
                    modulo instanceof Modulo m ? m : null, txtMotivo.getText());
            terminar();
        });
    }
}
