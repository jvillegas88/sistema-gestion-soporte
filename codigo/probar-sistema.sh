#!/bin/bash
# Pruebas de sistema del TP4: CP-S-01 (volumen de la importación) y CP-S-02 (desempeño de los
# reportes). Recarga la base, genera el CSV ficticio de 1154 registros e importa sobre los datos
# de prueba. Al terminar vuelve a recargar la base, porque la importación le agrega datos.
# La salida queda en prueba/salidas/pruebas-sistema.txt.
# Uso: codigo/probar-sistema.sh
set -e
cd "$(dirname "$0")"
../sql/recargar-base.sh
./compilar.sh > /dev/null
codigo=0
java -cp 'clases:lib/*' sgs.prueba.PruebaSistema prueba/datos/historico-volumen.csv prueba/salidas/pruebas-sistema.txt \
    || codigo=$?
../sql/recargar-base.sh > /dev/null
exit $codigo
