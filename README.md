# Sistema de gestión y análisis del soporte técnico y funcional

Proyecto de la asignatura Seminario de Práctica de Informática (Licenciatura en Informática, Universidad Siglo 21).

## El problema

Una empresa de software gestiona el soporte de sus dos productos con una herramienta que registra cada solicitud con un único responsable y un único tiempo. En la práctica, una solicitud pasa por varios roles y buena parte del tiempo es espera, así que no se puede saber cuánto soporte consume cada cliente ni cuánto trabaja cada integrante.

## La propuesta

Un sistema propio en el que cada integrante registra su intervención con su propio tiempo, para obtener reportes de esfuerzo por cliente, carga por integrante y recurrencia por módulo.

## Contenido

- `codigo/`: el prototipo en Java, desde el TP3.
  - `src/`: el programa, organizado en MVC: `sgs.vista`, `sgs.controlador`, `sgs.modelo` y `sgs.persistencia`.
  - `prueba/`: las pruebas automáticas y, en `prueba/salidas/`, la salida de su última ejecución.
- `sql/`: scripts de la base de datos, en el orden en que se ejecutan (01 a 05).
- `sql/salidas/`: salida de cada script.
- `diagramas/`: diagramas del informe y sus fuentes (Mermaid y draw.io).

El prototipo cubre el núcleo del sistema: registrar y clasificar solicitudes, registrar intervenciones con su tiempo, esperas y cambios de estado, hitos técnicos, el cierre con lo que exige cada tipo de solicitud, y los reportes de esfuerzo por cliente y de carga por integrante. Las opciones se habilitan según el rol de quien inicia sesión.

## Tecnologías

Java 21 con Swing, MySQL 8.4, JDBC y MVC, sin frameworks. La única biblioteca es el driver JDBC de MySQL, que `compilar.sh` baja de Maven Central la primera vez y verifica con su SHA-1.

## Cómo ejecutarlo

### 1. La base de datos

Hace falta un MySQL 8.4. Con Docker alcanza con:

```bash
docker run --name sgs-mysql -e MYSQL_ROOT_PASSWORD=soporte -p 127.0.0.1:3306:3306 -d mysql:8.4
```

Después se crean la base, el usuario de la aplicación y los datos de prueba, que son ficticios:

```bash
sql/recargar-base.sh
```

El script ejecuta `01_creacion.sql` y `02_datos.sql` dentro del contenedor `sgs-mysql`. Con otro servidor, se ejecutan esos dos scripts con un usuario administrador. La aplicación se conecta con el usuario `app_soporte` que crea `01_creacion.sql`, que exige conexión cifrada y no puede borrar registros.

### 2. Compilar y ejecutar

Desde la carpeta `codigo/`, con Java 21:

```bash
./compilar.sh
./ejecutar.sh
```

Si la base no está en `127.0.0.1:3306`, la dirección se indica al ejecutar:

```bash
./ejecutar.sh -Dsgs.url=jdbc:mysql://servidor:3306/gestion_soporte
```

Los usuarios de prueba tienen la clave `clave_de_ejemplo`:

| Usuario | Rol |
| --- | --- |
| lgomez | Soporte funcional |
| dferreyra, pmedina | Soporte técnico |
| mrojas | Infraestructura |
| vsosa, therrera | Desarrollo |
| scastro | Responsable de gestión |

### 3. Pruebas

- `./probar.sh`: prueba de caja blanca de la validación del cierre. No necesita la base.
- `./probar-interfaz.sh`: pruebas desde las pantallas, que recargan la base y la usan. Corren en una pantalla virtual, así que necesitan Linux con Xvfb (`xvfb-run`).
- `./probar-sin-base.sh`: detiene el contenedor un momento para comprobar el mensaje que se muestra cuando la base no está disponible.
