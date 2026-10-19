#!/bin/bash
# Detiene la base, captura el mensaje de error de conexión (manejo de SQLException) y la vuelve
# a levantar. Uso: codigo/probar-sin-base.sh
set -e
cd "$(dirname "$0")"
./compilar.sh > /dev/null
docker stop sgs-mysql > /dev/null
trap 'docker start sgs-mysql > /dev/null' EXIT
xvfb-run -a -s "-screen 0 3600x2400x24" \
    java -Dsun.java2d.uiScale=2 -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true -cp 'clases:lib/*' \
    sgs.prueba.CapturaSinBase ../capturas
