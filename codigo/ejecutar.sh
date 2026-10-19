#!/bin/bash
# Ejecuta el sistema. Antes hay que compilarlo con compilar.sh y tener la base levantada.
# Las opciones se pasan a Java, por ejemplo la dirección de la base:
#   codigo/ejecutar.sh -Dsgs.url=jdbc:mysql://servidor:3306/gestion_soporte
# Uso, desde cualquier carpeta: codigo/ejecutar.sh [opciones]
set -e
cd "$(dirname "$0")"
java "$@" -cp 'clases:lib/*' sgs.Aplicacion
