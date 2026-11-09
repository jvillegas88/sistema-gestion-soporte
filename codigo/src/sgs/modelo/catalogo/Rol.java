package sgs.modelo.catalogo;

/**
 * Rol de un integrante del equipo (tabla roles). El rol 5 es el de los actores
 * Responsable de gestión: dirección y gestión de proyecto.
 */
public enum Rol implements Catalogo {
    SOPORTE_FUNCIONAL(1, "Soporte funcional"),
    SOPORTE_TECNICO(2, "Soporte técnico"),
    INFRAESTRUCTURA(3, "Infraestructura"),
    DESARROLLO(4, "Desarrollo"),
    RESPONSABLE_GESTION(5, "Responsable de gestión");

    private final int id;
    private final String nombre;

    Rol(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getNombre() {
        return nombre;
    }

    // Qué puede hacer cada rol (RNF06). Las pantallas y los controladores consultan estos
    // métodos, así la regla de permisos está en un solo lugar.

    /** Los agentes de soporte (A01): todos menos gestión. Registran intervenciones y esperas. */
    public boolean esAgente() {
        return this != RESPONSABLE_GESTION;
    }

    /** Registrar solicitudes es de soporte funcional (A02, CU01). */
    public boolean puedeRegistrarSolicitudes() {
        return this == SOPORTE_FUNCIONAL;
    }

    /** Importar el histórico es de soporte funcional (A02, CU02). */
    public boolean puedeImportarHistorico() {
        return this == SOPORTE_FUNCIONAL;
    }

    /** Clasificar es de soporte funcional, y soporte técnico puede reclasificar (CU03). */
    public boolean puedeClasificar() {
        return this == SOPORTE_FUNCIONAL || this == SOPORTE_TECNICO;
    }

    /** Los hitos técnicos los registran soporte técnico e infraestructura (A03 y A04, CU08). */
    public boolean puedeRegistrarHitos() {
        return this == SOPORTE_TECNICO || this == INFRAESTRUCTURA;
    }

    /** Los reportes son solo para gestión (A06, CU11 y CU12, RNF05). */
    public boolean puedeVerReportes() {
        return this == RESPONSABLE_GESTION;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
