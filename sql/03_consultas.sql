-- Sistema de gestión y análisis del soporte técnico y funcional
-- 03_consultas.sql: consultas de los reportes y de la búsqueda, y una modificación
-- MySQL 8.4. Se ejecuta después de 02_datos.sql.
-- El período de los reportes es el tercer trimestre de 2026.

-- La conexión tiene que usar utf8mb4 para que las tildes se guarden bien
SET NAMES utf8mb4;

USE gestion_soporte;

-- ------------------------------------------------------------------
-- 1. Esfuerzo y cantidad de solicitudes por cliente en un período
--    (RF19, CU11). El cliente sale del solicitante.
-- ------------------------------------------------------------------

SELECT c.nombre                        AS cliente,
       COUNT(DISTINCT s.solicitud_id)  AS solicitudes,
       COUNT(i.intervencion_id)        AS intervenciones,
       SUM(i.tiempo_horas)             AS horas
FROM clientes c
JOIN solicitantes so   ON so.cliente_id = c.cliente_id
JOIN solicitudes s     ON s.solicitante_id = so.solicitante_id
JOIN intervenciones i  ON i.solicitud_id = s.solicitud_id
WHERE i.fecha BETWEEN '2026-07-01 00:00:00' AND '2026-09-30 23:59:59'
GROUP BY c.cliente_id, c.nombre
ORDER BY horas DESC;

-- ------------------------------------------------------------------
-- 2. Carga por integrante en el período (RF20, CU12).
--    LEFT JOIN para que aparezca también quien no registró tiempo.
--    Se ordena por rol y nombre, no por horas: el reporte informa
--    carga y no arma un ranking de integrantes (RNF05).
-- ------------------------------------------------------------------

SELECT ig.nombre                          AS integrante,
       r.nombre                           AS rol,
       COUNT(i.intervencion_id)           AS intervenciones,
       COUNT(DISTINCT i.solicitud_id)     AS solicitudes,
       COALESCE(SUM(i.tiempo_horas), 0)   AS horas
FROM integrantes ig
JOIN roles r ON r.rol_id = ig.rol_id
LEFT JOIN intervenciones i
       ON i.integrante_id = ig.integrante_id
      AND i.fecha BETWEEN '2026-07-01 00:00:00' AND '2026-09-30 23:59:59'
WHERE ig.activo = TRUE
GROUP BY ig.integrante_id, ig.nombre, r.rol_id, r.nombre
ORDER BY r.rol_id, ig.nombre;

-- ------------------------------------------------------------------
-- 3. Carga por integrante y tipo de solicitud (CU12, paso 4)
-- ------------------------------------------------------------------

SELECT ig.nombre                            AS integrante,
       COALESCE(t.nombre, 'Sin clasificar') AS tipo_solicitud,
       SUM(i.tiempo_horas)                  AS horas
FROM intervenciones i
JOIN integrantes ig        ON ig.integrante_id = i.integrante_id
JOIN solicitudes s         ON s.solicitud_id = i.solicitud_id
LEFT JOIN tipos_solicitud t ON t.tipo_solicitud_id = s.tipo_solicitud_id
WHERE i.fecha BETWEEN '2026-07-01 00:00:00' AND '2026-09-30 23:59:59'
GROUP BY ig.nombre, t.nombre
ORDER BY ig.nombre, horas DESC;

-- ------------------------------------------------------------------
-- 4. Quién trabajó en una solicitud y cuánto (P01): solicitud 3
-- ------------------------------------------------------------------

SELECT i.fecha,
       ig.nombre       AS integrante,
       r.nombre        AS rol,
       ti.nombre       AS tipo_intervencion,
       i.tiempo_horas  AS horas
FROM intervenciones i
JOIN integrantes ig        ON ig.integrante_id = i.integrante_id
JOIN roles r               ON r.rol_id = ig.rol_id
JOIN tipos_intervencion ti ON ti.tipo_intervencion_id = i.tipo_intervencion_id
WHERE i.solicitud_id = 3
ORDER BY i.fecha;

-- ------------------------------------------------------------------
-- 5. Tiempo total de cada solicitud: suma de sus intervenciones,
--    más el tiempo histórico en las importadas. No se guarda.
-- ------------------------------------------------------------------

SELECT s.solicitud_id,
       s.asunto,
       c.nombre                  AS cliente,
       COUNT(i.intervencion_id)  AS intervenciones,
       COALESCE(SUM(i.tiempo_horas), 0) + COALESCE(s.tiempo_historico, 0) AS tiempo_total
FROM solicitudes s
JOIN solicitantes so       ON so.solicitante_id = s.solicitante_id
JOIN clientes c            ON c.cliente_id = so.cliente_id
LEFT JOIN intervenciones i ON i.solicitud_id = s.solicitud_id
GROUP BY s.solicitud_id, s.asunto, c.nombre, s.tiempo_historico
ORDER BY s.solicitud_id;

-- ------------------------------------------------------------------
-- 6. Estado actual y responsable actual de cada solicitud (CU10, paso 4).
--    El estado es el del último cambio y el responsable, la última
--    asignación: se obtienen con subconsultas correlacionadas.
-- ------------------------------------------------------------------

SELECT s.solicitud_id,
       COALESCE(t.nombre, 'Sin clasificar')       AS tipo_solicitud,
       e.nombre                                   AS estado_actual,
       IF(e.es_cierre, 'Cerrada', 'Abierta')      AS situacion,
       ce.fecha                                   AS desde,
       (SELECT ig.nombre
        FROM asignaciones a
        JOIN integrantes ig ON ig.integrante_id = a.integrante_id
        WHERE a.solicitud_id = s.solicitud_id
        ORDER BY a.fecha DESC, a.asignacion_id DESC
        LIMIT 1)                                  AS responsable
FROM solicitudes s
JOIN cambios_estado ce      ON ce.solicitud_id = s.solicitud_id
JOIN estados e              ON e.estado_id = ce.estado_id
LEFT JOIN tipos_solicitud t ON t.tipo_solicitud_id = s.tipo_solicitud_id
WHERE ce.cambio_estado_id = (
        SELECT ce2.cambio_estado_id
        FROM cambios_estado ce2
        WHERE ce2.solicitud_id = s.solicitud_id
        ORDER BY ce2.fecha DESC, ce2.cambio_estado_id DESC
        LIMIT 1)
ORDER BY s.solicitud_id;

-- ------------------------------------------------------------------
-- 7. Esperas abiertas y días que llevan (RF23, CU12 flujo 4a)
-- ------------------------------------------------------------------

SELECT s.solicitud_id,
       c.nombre                                AS cliente,
       s.asunto,
       m.nombre                                AS motivo,
       e.desde,
       TIMESTAMPDIFF(DAY, e.desde, NOW())      AS dias_en_espera
FROM esperas e
JOIN solicitudes s     ON s.solicitud_id = e.solicitud_id
JOIN solicitantes so   ON so.solicitante_id = s.solicitante_id
JOIN clientes c        ON c.cliente_id = so.cliente_id
JOIN motivos_espera m  ON m.motivo_espera_id = e.motivo_espera_id
WHERE e.hasta IS NULL
ORDER BY dias_en_espera DESC;

-- ------------------------------------------------------------------
-- 8. Búsqueda de antecedentes por texto y filtros (RF18, CU10).
--    La intercalación utf8mb4_0900_ai_ci no distingue tildes ni
--    mayúsculas: "observacion" encuentra "observación".
-- ------------------------------------------------------------------

SELECT s.solicitud_id,
       s.fecha_recepcion,
       c.nombre                              AS cliente,
       COALESCE(t.nombre, 'Sin clasificar')  AS tipo_solicitud,
       m.nombre                              AS modulo,
       s.asunto
FROM solicitudes s
JOIN solicitantes so        ON so.solicitante_id = s.solicitante_id
JOIN clientes c             ON c.cliente_id = so.cliente_id
JOIN productos p            ON p.producto_id = s.producto_id
LEFT JOIN modulos m         ON m.producto_id = s.producto_id
                           AND m.modulo_nro = s.modulo_nro
LEFT JOIN tipos_solicitud t ON t.tipo_solicitud_id = s.tipo_solicitud_id
WHERE p.nombre = 'Producto A'
  AND (s.asunto LIKE '%observacion%' OR s.descripcion LIKE '%observacion%')
ORDER BY s.fecha_recepcion DESC;

-- ------------------------------------------------------------------
-- 9. Clientes que superan 9 horas en el período (RF19, HAVING)
-- ------------------------------------------------------------------

SELECT c.nombre             AS cliente,
       SUM(i.tiempo_horas)  AS horas
FROM clientes c
JOIN solicitantes so   ON so.cliente_id = c.cliente_id
JOIN solicitudes s     ON s.solicitante_id = so.solicitante_id
JOIN intervenciones i  ON i.solicitud_id = s.solicitud_id
WHERE i.fecha BETWEEN '2026-07-01 00:00:00' AND '2026-09-30 23:59:59'
GROUP BY c.cliente_id, c.nombre
HAVING SUM(i.tiempo_horas) > 9
ORDER BY horas DESC;

-- ------------------------------------------------------------------
-- 10. Horas estimadas de cada estimación. El total sale de los ítems
--     y es distinto del tiempo que llevó estimar (CU09, presupuestos).
-- ------------------------------------------------------------------

SELECT s.solicitud_id,
       s.asunto,
       ig.nombre            AS estimo,
       i.tiempo_horas       AS horas_de_analisis,
       COUNT(ie.item_nro)   AS items,
       SUM(ie.horas)        AS horas_estimadas
FROM intervenciones i
JOIN items_estimacion ie ON ie.intervencion_id = i.intervencion_id
JOIN solicitudes s       ON s.solicitud_id = i.solicitud_id
JOIN integrantes ig      ON ig.integrante_id = i.integrante_id
GROUP BY s.solicitud_id, s.asunto, ig.nombre, i.intervencion_id, i.tiempo_horas;

-- ------------------------------------------------------------------
-- 11. Modificación: se identifica el cliente de un solicitante que
--     estaba como "no identificado" (CU02, 4a). Como el cliente se
--     guarda en el solicitante, un solo UPDATE corrige todas sus
--     solicitudes.
-- ------------------------------------------------------------------

-- Antes
SELECT s.solicitud_id, s.numero_origen, so.nombre AS solicitante, c.nombre AS cliente
FROM solicitudes s
JOIN solicitantes so ON so.solicitante_id = s.solicitante_id
JOIN clientes c      ON c.cliente_id = so.cliente_id
WHERE so.solicitante_id = 6;

UPDATE solicitantes
SET cliente_id = 4  -- Cliente C
WHERE solicitante_id = 6;

-- Después
SELECT s.solicitud_id, s.numero_origen, so.nombre AS solicitante, c.nombre AS cliente
FROM solicitudes s
JOIN solicitantes so ON so.solicitante_id = s.solicitante_id
JOIN clientes c      ON c.cliente_id = so.cliente_id
WHERE so.solicitante_id = 6;
