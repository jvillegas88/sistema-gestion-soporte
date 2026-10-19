#!/bin/bash
# Vuelve a crear la base gestion_soporte con los datos de prueba (scripts 01 y 02).
# Borra todo lo que se haya cargado desde la aplicación. Requiere el contenedor sgs-mysql levantado.
# Uso: sql/recargar-base.sh
set -e
cd "$(dirname "$0")"

if [ "$(docker inspect -f '{{.State.Running}}' sgs-mysql 2>/dev/null)" != "true" ]; then
    echo "El contenedor sgs-mysql no está levantado (docker start sgs-mysql)." >&2
    exit 1
fi

# Recién después de docker run, MySQL tarda unos segundos en aceptar conexiones: se espera hasta
# un minuto. La prueba va por TCP porque, mientras se inicializa, el contenedor levanta un
# servidor provisorio que solo atiende por el socket.
for i in $(seq 60); do
    if docker exec sgs-mysql mysqladmin ping -h 127.0.0.1 -uroot -psoporte --silent > /dev/null 2>&1; then
        break
    fi
    if [ "$i" -eq 60 ]; then
        echo "MySQL no responde en el contenedor sgs-mysql después de un minuto." >&2
        exit 1
    fi
    sleep 1
done

# --force sigue después de un error porque 02_datos.sql termina con una inserción que la base
# tiene que rechazar. Ese error esperado no se muestra.
for f in 01_creacion.sql 02_datos.sql; do
    docker exec -i sgs-mysql mysql -uroot -psoporte --default-character-set=utf8mb4 --force < "$f" 2>&1 \
        | grep -v -e "Using a password on the command line" -e "intervenciones_tiempo_ck" || true
done

# Como los errores no cortan el script, al final se comprueba que los datos quedaron cargados
solicitudes=$(docker exec sgs-mysql mysql -uroot -psoporte -N \
    -e "SELECT COUNT(*) FROM gestion_soporte.solicitudes" 2>/dev/null || true)
if [ -z "$solicitudes" ] || [ "$solicitudes" -eq 0 ]; then
    echo "No se pudo recargar la base: revisá los mensajes de arriba." >&2
    exit 1
fi
echo "Base gestion_soporte recargada con los datos de prueba ($solicitudes solicitudes)"
