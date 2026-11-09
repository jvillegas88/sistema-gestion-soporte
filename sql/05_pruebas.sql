-- Sistema de gestión y análisis del soporte técnico y funcional
-- 05_pruebas.sql: pruebas de caja negra sobre la base (construcción del TP2)
-- MySQL 8.4
--
-- Se ejecuta sobre una copia vacía de la base, nunca sobre la base real.
-- La copia se crea con el mismo 01_creacion.sql, cambiando el nombre de la
-- base y sin el bloque final del usuario de la aplicación:
--
--   sed -e '/^DROP USER IF EXISTS/,$d' \
--       -e 's/gestion_soporte/gestion_soporte_pruebas/g' 01_creacion.sql | mysql -uroot -p
--   mysql -uroot -p -t --default-character-set=utf8mb4 < 05_pruebas.sql
--
-- Si la base de pruebas no existe, el USE falla y el script se corta sin
-- tocar nada.
--
-- Cada prueba se ejecuta con el procedimiento probar(), que intenta la
-- sentencia, captura el error si lo hay y guarda en resultados_prueba lo
-- esperado, lo obtenido y el mensaje de MySQL. Así una sentencia rechazada
-- no corta el script y todas las pruebas quedan registradas.
-- "Acepta" significa que la sentencia se ejecutó. "Rechaza", que MySQL
-- devolvió un error. Una prueba pasa cuando lo obtenido es igual a lo esperado.

USE gestion_soporte_pruebas;

-- Corrección de DEF-02: sin esto, el cliente del contenedor usa latin1 y
-- los textos con acentos quedan mal guardados.
SET NAMES utf8mb4;
-- Hora de Argentina para la fecha de ejecución
SET time_zone = '-03:00';

SELECT VERSION() AS version_mysql, DATABASE() AS base, NOW() AS fecha_ejecucion;

-- ------------------------------------------------------------------
-- 1. Datos mínimos (ficticios)
-- ------------------------------------------------------------------

INSERT INTO roles (rol_id, nombre) VALUES (1, 'Soporte técnico');

INSERT INTO integrantes (integrante_id, rol_id, nombre, usuario, clave_hash)
VALUES (1, 1, 'Integrante de prueba', 'prueba', SHA2('prueba', 256));

INSERT INTO clientes (cliente_id, nombre, dominio_correo)
VALUES (1, 'Cliente A', 'cliente-a.test');

INSERT INTO solicitantes (solicitante_id, cliente_id, nombre, correo)
VALUES (1, 1, 'Solicitante A1', 'a1@cliente-a.test');

INSERT INTO productos (producto_id, nombre, correo_soporte) VALUES
    (1, 'Producto A', 'soporte-producto-a@empresa.test'),
    (2, 'Producto B', 'soporte-producto-b@empresa.test');

-- El módulo 3 existe solo en Producto B: lo usa CP-N-04
INSERT INTO modulos (producto_id, modulo_nro, nombre) VALUES
    (1, 1, 'Informes'),
    (1, 2, 'Usuarios'),
    (2, 1, 'Informes'),
    (2, 2, 'Usuarios'),
    (2, 3, 'Cobranzas');

INSERT INTO canales (canal_id, nombre) VALUES (1, 'Correo');
INSERT INTO tipos_intervencion (tipo_intervencion_id, nombre) VALUES (1, 'Análisis');
INSERT INTO motivos_espera (motivo_espera_id, nombre) VALUES (1, 'Espera del cliente');

-- Solicitud 1, abierta, de Producto A y sin módulo, con una intervención.
-- La usan CP-N-01, CP-N-04, CP-N-05, CP-N-06 y CP-N-07.
INSERT INTO solicitudes (solicitud_id, solicitante_id, producto_id, canal_id,
                         fecha_recepcion, asunto)
VALUES (1, 1, 1, 1, '2026-09-01 09:00:00', 'Solicitud base para las pruebas');

INSERT INTO intervenciones (solicitud_id, integrante_id, tipo_intervencion_id,
                            fecha, descripcion, tiempo_horas)
VALUES (1, 1, 1, '2026-09-01 10:00:00', 'Intervención de la carga inicial', 1.00);

-- ------------------------------------------------------------------
-- 2. Registro de resultados y procedimiento de prueba
-- ------------------------------------------------------------------

DROP TABLE IF EXISTS resultados_prueba;
CREATE TABLE resultados_prueba (
    ejecucion_id  INT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo        VARCHAR(10)  NOT NULL,
    dato          VARCHAR(80)  NOT NULL,
    esperado      VARCHAR(10)  NOT NULL,
    obtenido      VARCHAR(10)  NOT NULL,
    error_nro     INT          NULL,
    mensaje       VARCHAR(300) NULL,
    PRIMARY KEY (ejecucion_id)
);

DROP PROCEDURE IF EXISTS probar;
DELIMITER //
CREATE PROCEDURE probar(IN p_codigo VARCHAR(10), IN p_dato VARCHAR(80),
                        IN p_esperado VARCHAR(10), IN p_sentencia TEXT)
BEGIN
    DECLARE v_obtenido VARCHAR(10)  DEFAULT 'Acepta';
    DECLARE v_mensaje  VARCHAR(300) DEFAULT NULL;
    DECLARE v_errno    INT          DEFAULT NULL;
    DECLARE v_cant     INT          DEFAULT 0;
    BEGIN
        DECLARE EXIT HANDLER FOR SQLEXCEPTION
        BEGIN
            -- El error es la última condición (antes puede haber avisos)
            GET DIAGNOSTICS v_cant = NUMBER;
            GET DIAGNOSTICS CONDITION v_cant
                v_errno = MYSQL_ERRNO, v_mensaje = MESSAGE_TEXT;
            SET v_obtenido = 'Rechaza';
        END;
        SET @sentencia = p_sentencia;
        PREPARE sentencia FROM @sentencia;
        EXECUTE sentencia;
        DEALLOCATE PREPARE sentencia;
    END;
    INSERT INTO resultados_prueba (codigo, dato, esperado, obtenido, error_nro, mensaje)
    VALUES (p_codigo, p_dato, p_esperado, v_obtenido, v_errno, v_mensaje);
END//
DELIMITER ;

-- ------------------------------------------------------------------
-- 3. Pruebas
-- ------------------------------------------------------------------

-- CP-N-01. CU06, paso 6 y alternativa 6.A. RF08.
-- Tiempo dedicado de una intervención: mayor que cero y hasta 24 horas,
-- con hasta dos decimales (la precisión de tiempo_horas).
-- Clases: CEV1 0 < t <= 24, CEI1 t <= 0, CEI2 t > 24 (rango).
--         CEV2 hasta dos decimales, CEI3 más de dos decimales (precisión).

-- Representante de CEI1. Esperado: rechaza (CHECK)
CALL probar('CP-N-01', '-1 (CEI1)', 'Rechaza',
    'INSERT INTO intervenciones (solicitud_id, integrante_id, tipo_intervencion_id, descripcion, tiempo_horas) VALUES (1, 1, 1, ''CP-N-01 -1'', -1)');
-- Frontera de 0, un paso abajo. Esperado: rechaza
CALL probar('CP-N-01', '-0,01 (CEI1, frontera)', 'Rechaza',
    'INSERT INTO intervenciones (solicitud_id, integrante_id, tipo_intervencion_id, descripcion, tiempo_horas) VALUES (1, 1, 1, ''CP-N-01 -0,01'', -0.01)');
-- Frontera de 0. Esperado: rechaza (el cero es inválido)
CALL probar('CP-N-01', '0 (CEI1, frontera)', 'Rechaza',
    'INSERT INTO intervenciones (solicitud_id, integrante_id, tipo_intervencion_id, descripcion, tiempo_horas) VALUES (1, 1, 1, ''CP-N-01 0'', 0)');
-- Frontera de 0, un paso arriba. Esperado: acepta
CALL probar('CP-N-01', '0,01 (CEV1, frontera)', 'Acepta',
    'INSERT INTO intervenciones (solicitud_id, integrante_id, tipo_intervencion_id, descripcion, tiempo_horas) VALUES (1, 1, 1, ''CP-N-01 0,01'', 0.01)');
-- Representante de CEV1. Esperado: acepta
CALL probar('CP-N-01', '1,5 (CEV1)', 'Acepta',
    'INSERT INTO intervenciones (solicitud_id, integrante_id, tipo_intervencion_id, descripcion, tiempo_horas) VALUES (1, 1, 1, ''CP-N-01 1,5'', 1.5)');
-- Frontera de 24, un paso abajo. Esperado: acepta
CALL probar('CP-N-01', '23,99 (CEV1, frontera)', 'Acepta',
    'INSERT INTO intervenciones (solicitud_id, integrante_id, tipo_intervencion_id, descripcion, tiempo_horas) VALUES (1, 1, 1, ''CP-N-01 23,99'', 23.99)');
-- Frontera de 24. Esperado: acepta (24 es el máximo válido)
CALL probar('CP-N-01', '24 (CEV1, frontera)', 'Acepta',
    'INSERT INTO intervenciones (solicitud_id, integrante_id, tipo_intervencion_id, descripcion, tiempo_horas) VALUES (1, 1, 1, ''CP-N-01 24'', 24)');
-- Frontera de 24, un paso arriba. Esperado: rechaza
CALL probar('CP-N-01', '24,01 (CEI2, frontera)', 'Rechaza',
    'INSERT INTO intervenciones (solicitud_id, integrante_id, tipo_intervencion_id, descripcion, tiempo_horas) VALUES (1, 1, 1, ''CP-N-01 24,01'', 24.01)');
-- Representante de CEI2. Esperado: rechaza
CALL probar('CP-N-01', '30 (CEI2)', 'Rechaza',
    'INSERT INTO intervenciones (solicitud_id, integrante_id, tipo_intervencion_id, descripcion, tiempo_horas) VALUES (1, 1, 1, ''CP-N-01 30'', 30)');
-- Representante de CEI3, dentro del rango. Esperado: rechaza
CALL probar('CP-N-01', '2,125 (CEI3)', 'Rechaza',
    'INSERT INTO intervenciones (solicitud_id, integrante_id, tipo_intervencion_id, descripcion, tiempo_horas) VALUES (1, 1, 1, ''CP-N-01 2,125'', 2.125)');

-- CP-N-02. CU01, alternativa 4.A. RNF08.
-- Solicitud completa. Esperado: acepta
CALL probar('CP-N-02', 'Solicitud completa', 'Acepta',
    'INSERT INTO solicitudes (solicitante_id, producto_id, canal_id, fecha_recepcion, asunto) VALUES (1, 1, 1, ''2026-09-02 09:00:00'', ''CP-N-02 completa'')');
-- Solicitud sin producto. Esperado: rechaza (NOT NULL)
CALL probar('CP-N-02', 'Solicitud sin producto', 'Rechaza',
    'INSERT INTO solicitudes (solicitante_id, producto_id, canal_id, fecha_recepcion, asunto) VALUES (1, NULL, 1, ''2026-09-02 09:30:00'', ''CP-N-02 sin producto'')');

-- CP-N-03. CU02. RF02 y RNF09.
-- Primera importación de la solicitud 5001 de la herramienta actual. Esperado: acepta
CALL probar('CP-N-03', 'Importa el número de origen 5001', 'Acepta',
    'INSERT INTO solicitudes (numero_origen, solicitante_id, producto_id, canal_id, fecha_recepcion, asunto) VALUES (5001, 1, 2, 1, ''2025-03-10 11:00:00'', ''CP-N-03 importada'')');
-- Se vuelve a importar la 5001. Esperado: rechaza (UNIQUE)
CALL probar('CP-N-03', 'Vuelve a importar el 5001', 'Rechaza',
    'INSERT INTO solicitudes (numero_origen, solicitante_id, producto_id, canal_id, fecha_recepcion, asunto) VALUES (5001, 1, 2, 1, ''2025-03-10 11:00:00'', ''CP-N-03 repetida'')');
-- Carga manual, sin número de origen, cuando ya hay otras sin número. Esperado: acepta
CALL probar('CP-N-03', 'Otra solicitud manual sin número de origen', 'Acepta',
    'INSERT INTO solicitudes (numero_origen, solicitante_id, producto_id, canal_id, fecha_recepcion, asunto) VALUES (NULL, 1, 1, 1, ''2026-09-02 10:00:00'', ''CP-N-03 manual'')');

-- CP-N-04. CU03, paso 6 y alternativas 5.A y 6.A. RF04.
-- La solicitud 1 es de Producto A. Módulo 2 de Producto A. Esperado: acepta
CALL probar('CP-N-04', 'Módulo 2 de Producto A', 'Acepta',
    'UPDATE solicitudes SET modulo_nro = 2 WHERE solicitud_id = 1');
-- Módulo 3, que existe solo en Producto B. Esperado: rechaza (clave foránea compuesta)
CALL probar('CP-N-04', 'Módulo 3, que es de Producto B', 'Rechaza',
    'UPDATE solicitudes SET modulo_nro = 3 WHERE solicitud_id = 1');
-- Módulo sin informar. Esperado: acepta
CALL probar('CP-N-04', 'Módulo sin informar', 'Acepta',
    'UPDATE solicitudes SET modulo_nro = NULL WHERE solicitud_id = 1');

-- CP-N-05. CU01, alternativa 10.A. RNF07.
-- Borrado físico de la solicitud 1, que tiene intervenciones. Esperado: rechaza (clave foránea)
CALL probar('CP-N-05', 'Borra la solicitud 1, con intervenciones', 'Rechaza',
    'DELETE FROM solicitudes WHERE solicitud_id = 1');

-- CP-N-06. CU07, curso normal y alternativa 8.A. RF10.
-- Espera cerrada, con fin posterior al inicio. Esperado: acepta
CALL probar('CP-N-06', 'Espera cerrada, fin posterior al inicio', 'Acepta',
    'INSERT INTO esperas (solicitud_id, motivo_espera_id, autor_id, desde, hasta) VALUES (1, 1, 1, ''2026-09-02 10:00:00'', ''2026-09-02 16:00:00'')');
-- Espera abierta, sin fecha de fin. Esperado: acepta
CALL probar('CP-N-06', 'Espera abierta, sin fin', 'Acepta',
    'INSERT INTO esperas (solicitud_id, motivo_espera_id, autor_id, desde, hasta) VALUES (1, 1, 1, ''2026-09-03 10:00:00'', NULL)');
-- Espera que termina antes de empezar. Esperado: rechaza (CHECK)
CALL probar('CP-N-06', 'Espera con fin anterior al inicio', 'Rechaza',
    'INSERT INTO esperas (solicitud_id, motivo_espera_id, autor_id, desde, hasta) VALUES (1, 1, 1, ''2026-09-04 10:00:00'', ''2026-09-04 09:00:00'')');

-- CP-N-07. CU04 y RF17. RF03.
-- Derivada de la solicitud 1, con tipo de vínculo. Esperado: acepta
CALL probar('CP-N-07', 'Origen y tipo de vínculo', 'Acepta',
    'INSERT INTO solicitudes (solicitante_id, producto_id, canal_id, solicitud_origen_id, tipo_vinculo, fecha_recepcion, asunto) VALUES (1, 1, 1, 1, ''DERIVADA'', ''2026-09-05 09:00:00'', ''CP-N-07 derivada'')');
-- Origen sin tipo de vínculo. Esperado: rechaza (CHECK)
CALL probar('CP-N-07', 'Origen sin tipo de vínculo', 'Rechaza',
    'INSERT INTO solicitudes (solicitante_id, producto_id, canal_id, solicitud_origen_id, tipo_vinculo, fecha_recepcion, asunto) VALUES (1, 1, 1, 1, NULL, ''2026-09-05 09:30:00'', ''CP-N-07 sin tipo'')');
-- Tipo de vínculo sin origen. Esperado: rechaza (CHECK)
CALL probar('CP-N-07', 'Tipo de vínculo sin origen', 'Rechaza',
    'INSERT INTO solicitudes (solicitante_id, producto_id, canal_id, solicitud_origen_id, tipo_vinculo, fecha_recepcion, asunto) VALUES (1, 1, 1, NULL, ''DUPLICADA'', ''2026-09-05 10:00:00'', ''CP-N-07 sin origen'')');

-- Reproducción de DEF-01 (defecto que detectó CP-N-01 con 2,125).
-- No es un caso del plan: muestra el efecto del redondeo en la frontera de 24.
-- 24,004 se tendría que rechazar, porque supera 24 y tiene tres decimales.
CALL probar('DEF-01', '24,004', 'Rechaza',
    'INSERT INTO intervenciones (solicitud_id, integrante_id, tipo_intervencion_id, descripcion, tiempo_horas) VALUES (1, 1, 1, ''DEF-01 24,004'', 24.004)');

-- ------------------------------------------------------------------
-- 4. Resultados
-- ------------------------------------------------------------------

-- Resultado de cada ejecución. En los errores de clave foránea se muestra
-- solo el comienzo del mensaje y el nombre de la restricción.
SELECT codigo, dato, esperado, obtenido,
       IF(esperado = obtenido, 'Pasó', 'Falló') AS resultado,
       error_nro,
       IF(error_nro IN (1451, 1452),
          CONCAT(SUBSTRING_INDEX(mensaje, ':', 1), ' (',
                 REGEXP_SUBSTR(mensaje, '[a-z_]+_fk'), ')'),
          mensaje) AS mensaje
FROM resultados_prueba
ORDER BY ejecucion_id;

-- Valor que quedó guardado en cada intervención aceptada
SELECT descripcion AS dato_informado, tiempo_horas AS valor_guardado
FROM intervenciones
WHERE descripcion LIKE 'CP-N-01%' OR descripcion LIKE 'DEF-01%'
ORDER BY intervencion_id;

-- Verificación de DEF-02: la "á" tiene que quedar guardada como C3A1
SELECT nombre, HEX(nombre) AS bytes
FROM tipos_intervencion;

-- La solicitud 1 sigue existiendo, con sus intervenciones (CP-N-05)
SELECT s.solicitud_id, s.asunto, COUNT(i.intervencion_id) AS intervenciones
FROM solicitudes s
JOIN intervenciones i ON i.solicitud_id = s.solicitud_id
WHERE s.solicitud_id = 1
GROUP BY s.solicitud_id, s.asunto;

-- Resumen por caso de prueba (sin la reproducción del defecto)
SELECT codigo,
       COUNT(*) AS ejecutadas,
       SUM(esperado = obtenido) AS pasaron,
       SUM(esperado <> obtenido) AS fallaron
FROM resultados_prueba
WHERE codigo LIKE 'CP-%'
GROUP BY codigo
ORDER BY codigo;

-- Resumen general
SELECT COUNT(DISTINCT codigo) AS casos,
       COUNT(*) AS ejecutadas,
       SUM(esperado = obtenido) AS pasaron,
       SUM(esperado <> obtenido) AS fallaron
FROM resultados_prueba
WHERE codigo LIKE 'CP-%';
