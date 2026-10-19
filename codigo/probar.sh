#!/bin/bash
# Compila y ejecuta las pruebas automáticas que no necesitan la pantalla ni la base (CP-B-01).
# La salida queda en prueba/salidas/validar-cierre.txt.
# Uso: codigo/probar.sh
set -e
cd "$(dirname "$0")"
./compilar.sh > /dev/null
mkdir -p prueba/salidas
java -cp 'clases:lib/*' sgs.prueba.PruebaValidarCierre | tee prueba/salidas/validar-cierre.txt
