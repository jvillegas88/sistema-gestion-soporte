-- Sistema de gestión y análisis del soporte técnico y funcional
-- 04_borrado.sql: borrado de registros
-- MySQL 8.4. Se ejecuta después de 02_datos.sql.

-- La conexión tiene que usar utf8mb4 para que las tildes se guarden bien
SET NAMES utf8mb4;

USE gestion_soporte;

-- ------------------------------------------------------------------
-- 1. Borrado de un ítem de estimación mal cargado.
--    En la estimación de la solicitud 6 el ítem 4 repite el ítem 2.
-- ------------------------------------------------------------------

-- Antes: cuatro ítems y 38 horas estimadas
SELECT item_nro, descripcion, horas
FROM items_estimacion
WHERE intervencion_id = 13
ORDER BY item_nro;

SELECT SUM(horas) AS horas_estimadas
FROM items_estimacion
WHERE intervencion_id = 13;

DELETE FROM items_estimacion
WHERE intervencion_id = 13
  AND item_nro = 4;

-- Después: tres ítems y 28 horas estimadas
SELECT item_nro, descripcion, horas
FROM items_estimacion
WHERE intervencion_id = 13
ORDER BY item_nro;

SELECT SUM(horas) AS horas_estimadas
FROM items_estimacion
WHERE intervencion_id = 13;

-- ------------------------------------------------------------------
-- 2. Intento de borrar una solicitud con historial (RNF07).
--    La clave foránea lo rechaza: se espera el error 1451.
-- ------------------------------------------------------------------

DELETE FROM solicitudes
WHERE solicitud_id = 3;

-- La solicitud sigue en la base con todo su recorrido
SELECT s.solicitud_id, s.asunto,
       (SELECT COUNT(*) FROM intervenciones i WHERE i.solicitud_id = s.solicitud_id) AS intervenciones,
       (SELECT COUNT(*) FROM cambios_estado ce WHERE ce.solicitud_id = s.solicitud_id) AS cambios_estado
FROM solicitudes s
WHERE s.solicitud_id = 3;


-- ==================================================================
-- 3. BORRADO DE TODOS LOS DATOS DE PRUEBA
--    Deja la base vacía, como queda después de 01_creacion.sql.
--    Se borra en orden inverso a las claves foráneas: primero las
--    tablas que referencian y al final las referenciadas.
--    Para volver a cargar los datos: ejecutar 01_creacion.sql y
--    02_datos.sql.
-- ==================================================================

DELETE FROM items_estimacion;
DELETE FROM comunicaciones;
DELETE FROM reclasificaciones;
DELETE FROM hitos_tecnicos;
DELETE FROM esperas;
DELETE FROM cambios_estado;
DELETE FROM intervenciones;
DELETE FROM asignaciones;

-- Primero las solicitudes derivadas, porque apuntan a su solicitud de origen
DELETE FROM solicitudes WHERE solicitud_origen_id IS NOT NULL;
DELETE FROM solicitudes;

DELETE FROM solicitantes;
DELETE FROM integrantes;
DELETE FROM modulos;
DELETE FROM productos;
DELETE FROM clientes;

DELETE FROM tipos_hito;
DELETE FROM motivos_espera;
DELETE FROM tipos_intervencion;
DELETE FROM canales;
DELETE FROM estados;
DELETE FROM prioridades;
DELETE FROM tipos_solicitud;
DELETE FROM roles;

-- Verificación: todas las tablas tienen que quedar en cero filas
SELECT 'solicitudes' AS tabla, COUNT(*) AS filas FROM solicitudes
UNION ALL SELECT 'asignaciones', COUNT(*) FROM asignaciones
UNION ALL SELECT 'intervenciones', COUNT(*) FROM intervenciones
UNION ALL SELECT 'items_estimacion', COUNT(*) FROM items_estimacion
UNION ALL SELECT 'comunicaciones', COUNT(*) FROM comunicaciones
UNION ALL SELECT 'cambios_estado', COUNT(*) FROM cambios_estado
UNION ALL SELECT 'esperas', COUNT(*) FROM esperas
UNION ALL SELECT 'hitos_tecnicos', COUNT(*) FROM hitos_tecnicos
UNION ALL SELECT 'reclasificaciones', COUNT(*) FROM reclasificaciones
UNION ALL SELECT 'clientes', COUNT(*) FROM clientes
UNION ALL SELECT 'productos', COUNT(*) FROM productos
UNION ALL SELECT 'modulos', COUNT(*) FROM modulos
UNION ALL SELECT 'solicitantes', COUNT(*) FROM solicitantes
UNION ALL SELECT 'integrantes', COUNT(*) FROM integrantes
UNION ALL SELECT 'roles', COUNT(*) FROM roles
UNION ALL SELECT 'tipos_solicitud', COUNT(*) FROM tipos_solicitud
UNION ALL SELECT 'prioridades', COUNT(*) FROM prioridades
UNION ALL SELECT 'estados', COUNT(*) FROM estados
UNION ALL SELECT 'canales', COUNT(*) FROM canales
UNION ALL SELECT 'tipos_intervencion', COUNT(*) FROM tipos_intervencion
UNION ALL SELECT 'motivos_espera', COUNT(*) FROM motivos_espera
UNION ALL SELECT 'tipos_hito', COUNT(*) FROM tipos_hito;
