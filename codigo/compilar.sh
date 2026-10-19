#!/bin/bash
# Compila el sistema y las pruebas en codigo/clases.
# Si falta el driver JDBC de MySQL, lo baja de Maven Central y verifica su SHA-1.
# Uso, desde cualquier carpeta: codigo/compilar.sh
set -e
cd "$(dirname "$0")"

DRIVER=lib/mysql-connector-j-26.7.0.jar
SHA1=43e30438c4c8707272ead3cd3167a1e07abb2c3c
if [ ! -f "$DRIVER" ]; then
    mkdir -p lib
    echo "Bajando el driver JDBC de MySQL..."
    curl -fsSL -o "$DRIVER.tmp" \
        https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/26.7.0/mysql-connector-j-26.7.0.jar
    if [ "$(sha1sum "$DRIVER.tmp" | cut -d' ' -f1)" != "$SHA1" ]; then
        rm -f "$DRIVER.tmp"
        echo "El archivo bajado no coincide con el SHA-1 esperado" >&2
        exit 1
    fi
    mv "$DRIVER.tmp" "$DRIVER"
fi

rm -rf clases
mkdir -p clases
find src prueba -name '*.java' > clases/fuentes.txt
javac -encoding UTF-8 -Xlint:all -d clases -cp 'lib/*' @clases/fuentes.txt
rm clases/fuentes.txt
echo "Compilado en $(pwd)/clases"
