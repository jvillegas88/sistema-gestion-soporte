package sgs.vista;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.math.BigDecimal;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import sgs.controlador.ControladorRegistrarIntervencion;
import sgs.controlador.Sesion;
import sgs.modelo.Formatos;
import sgs.modelo.Intervencion;
import sgs.modelo.Solicitud;

/**
 * CU06 Registrar intervención y tiempo (prototipo de la sección 13.2). Una sola pantalla con
 * los tres datos obligatorios (RNF04), la comunicación opcional, los ítems si es una estimación
 * y las intervenciones ya registradas con el tiempo total de la solicitud.
 */
public final class PantallaRegistrarIntervencion extends Pantalla {

    private static final long serialVersionUID = 1L;

    private final transient ControladorRegistrarIntervencion controlador;
    private final PanelIntervencion panel = new PanelIntervencion("Nueva intervención");

    /** Lee la solicitud y crea la pantalla. Si no se puede leer, avisa y devuelve null. */
    public static PantallaRegistrarIntervencion crear(Window duenio, Sesion sesion, int solicitudId) {
        ControladorRegistrarIntervencion controlador = sesion.registrarIntervencion();
        if (!Dialogos.ejecutar(duenio, () -> controlador.abrirSolicitud(solicitudId))) {
            return null;
        }
        return new PantallaRegistrarIntervencion(duenio, sesion, controlador);
    }

    private PantallaRegistrarIntervencion(Window duenio, Sesion sesion, ControladorRegistrarIntervencion controlador) {
        super(duenio, sesion, "Registrar intervención");
        this.controlador = controlador;
        Solicitud s = controlador.getSolicitud();

        JPanel cuerpo = cuerpoVertical();
        cuerpo.add(grupoSolicitud(s));
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(panel);
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(armarRegistradas(s));

        JButton cancelar = Estilo.boton("Cancelar");
        JButton registrar = Estilo.boton("Registrar intervención");
        cancelar.addActionListener(e -> dispose());
        registrar.addActionListener(e -> registrar());
        getRootPane().setDefaultButton(registrar);
        armar("Registrar intervención", cuerpo, Estilo.botonera("* Datos obligatorios", cancelar, registrar));
    }

    /** Intervenciones de la solicitud, cada una a nombre de quien la hizo, y el total (P01). */
    static JPanel armarRegistradas(Solicitud s) {
        DefaultTableModel modelo = Estilo.modeloTabla("Fecha", "Integrante", "Tipo", "Horas", "Descripción");
        for (Intervencion i : s.getIntervenciones()) {
            modelo.addRow(new Object[] {Formatos.fechaHora(i.getFecha()), i.getAutor().getNombre(), i.getTipo(),
                Formatos.horas(i.getTiempoHoras()), i.getDescripcion()});
        }
        JTable tabla = Estilo.tabla(modelo, new int[] {130, 130, 160, 55, 450},
                new int[] {Estilo.IZQUIERDA, Estilo.IZQUIERDA, Estilo.IZQUIERDA, Estilo.DERECHA, Estilo.IZQUIERDA}, false);
        JPanel grupo = Estilo.grupo("Intervenciones registradas en la solicitud");
        Estilo.ponerArea(grupo, Estilo.tablaConDesplazamiento(tabla, 4), 0, 0, 1, 1);
        BigDecimal total = s.calcularTiempoTotal();
        String texto = "Tiempo total de la solicitud: " + Formatos.horas(total) + " h";
        if (s.getTiempoHistorico() != null) {
            texto += " (incluye " + Formatos.horas(s.getTiempoHistorico()) + " h del histórico importado)";
        }
        JLabel lblTotal = new JLabel(texto);
        lblTotal.setFont(Estilo.negrita());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 1;
        g.anchor = GridBagConstraints.EAST;
        g.insets = new Insets(4, 6, 2, 10);
        grupo.add(lblTotal, g);
        return grupo;
    }

    /** Pasos 5 a 11: verifica, registra y muestra el tiempo total. */
    private void registrar() {
        ejecutar(() -> {
            panel.cargarBorrador(controlador);
            BigDecimal total = controlador.registrar(panel.getTipo(), panel.getDescripcion(), panel.getTiempo());
            Dialogos.informacion(this, "Intervención registrada.\nTiempo total de la solicitud: "
                    + Formatos.horas(total) + " h");
            terminar();
        });
    }
}
