package sgs.modelo.consulta;

import java.math.BigDecimal;
import sgs.modelo.catalogo.TipoSolicitud;

/**
 * Una fila de una distribución por tipo de solicitud: la de un cliente en CU11 o la de un
 * integrante en CU12. Los valores van en un arreglo con una posición por tipo, en el orden de
 * la enumeración, y una posición más al final para las solicitudes sin clasificar.
 */
public class FilaPorTipo {

    public static final int COLUMNAS = TipoSolicitud.values().length + 1;
    public static final int SIN_CLASIFICAR = COLUMNAS - 1;

    private final String nombre;
    private final BigDecimal[] valores = new BigDecimal[COLUMNAS];

    public FilaPorTipo(String nombre) {
        this.nombre = nombre;
        for (int i = 0; i < valores.length; i++) {
            valores[i] = BigDecimal.ZERO;
        }
    }

    public String getNombre() {
        return nombre;
    }

    /** Suma un valor en la columna del tipo. Un tipo null va a la columna "Sin clasificar". */
    public void sumar(TipoSolicitud tipo, BigDecimal valor) {
        int columna = tipo == null ? SIN_CLASIFICAR : tipo.ordinal();
        valores[columna] = valores[columna].add(valor);
    }

    public BigDecimal getValor(int columna) {
        return valores[columna];
    }

    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal v : valores) {
            total = total.add(v);
        }
        return total;
    }

    /** Título de cada columna: los tipos de solicitud y "Sin clasificar". */
    public static String[] titulos() {
        String[] titulos = new String[COLUMNAS];
        TipoSolicitud[] tipos = TipoSolicitud.values();
        for (int i = 0; i < tipos.length; i++) {
            titulos[i] = tipos[i].getNombre();
        }
        titulos[SIN_CLASIFICAR] = "Sin clasificar";
        return titulos;
    }
}
