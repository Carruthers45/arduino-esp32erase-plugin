#!/usr/bin/env bash

if [[ -z "$INSTALLDIR" ]]; then
    INSTALLDIR="$HOME/Documents/Arduino"
fi
echo "INSTALLDIR: $INSTALLDIR"

pde_path=`find ../Arduino/app/ -name pde.jar`
core_path=`find ../Arduino/arduino-core/ -name arduino-core.jar`
lib_path=`find ../Arduino/app/ -name commons-codec-1.7.jar`
if [[ -z "$core_path" || -z "$pde_path" ]]; then
    echo "Some java libraries have not been built yet (did you run ant build?)"
    return 1
fi
echo "pde_path: $pde_path"
echo "core_path: $core_path"
echo "lib_path: $lib_path"

set -e

mkdir -p bin
javac -target 1.8 -cp "$pde_path:$core_path:$lib_path" \
      -d bin src/ESP32Erase.java

pushd bin
mkdir -p $INSTALLDIR/tools
rm -rf $INSTALLDIR/tools/ESP32Erase
mkdir -p $INSTALLDIR/tools/ESP32Erase/tool
zip -r $INSTALLDIR/tools/ESP32Erase/tool/esp32erase.jar *
popd

dist=$PWD/dist
rev=$(git describe --tags)
mkdir -p $dist
pushd $INSTALLDIR/tools
zip -r $dist/ESP32Erase-$rev.zip ESP32Erase/
popd
