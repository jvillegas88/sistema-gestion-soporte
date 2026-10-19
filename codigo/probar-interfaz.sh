#!/bin/bash
# Pruebas desde la interfaz (PR-N-01 desde la pantalla, PR-I-01 y PR-I-02) y capturas del informe.
# Recarga la base con los datos de prueba, porque las pruebas parten de ellos y les agregan datos.
# Corre la aplicación en una pantalla virtual (Xvfb), así que no abre ventanas en el escritorio.
# Uso: codigo/probar-interfaz.sh
set -e
cd "$(dirname "$0")"
../sql/recargar-base.sh
./compilar.sh > /dev/null
xvfb-run -a -s "-screen 0 3600x2400x24" \
    java -Dsun.java2d.uiScale=2 -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true -cp 'clases:lib/*' \
    sgs.prueba.PruebaInterfaz ../capturas prueba/salidas/prueba-interfaz.txt
