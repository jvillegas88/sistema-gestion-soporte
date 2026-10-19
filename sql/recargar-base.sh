#!/bin/bash
# Vuelve a crear la base gestion_soporte con los datos de prueba (scripts 01 y 02).
# Borra todo lo que se haya cargado desde la aplicación. Requiere el contenedor sgs-mysql levantado.
# Uso: sql/recargar-base.sh
set -e
cd "$(dirname "$0")"
for f in 01_creacion.sql 02_datos.sql; do
    docker exec -i sgs-mysql mysql -uroot -psoporte --default-character-set=utf8mb4 --force < "$f" 2>&1 \
        | grep -v -e "Using a password on the command line" -e "intervenciones_tiempo_ck" || true
done
echo "Base gestion_soporte recargada con los datos de prueba"
