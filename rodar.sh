#!/usr/bin/env sh
set -e
cd "$(dirname "$0")"
if [ ! -f target/cleannow.jar ]; then
  mvn -q -DskipTests package
fi
java -Dfile.encoding=UTF-8 -jar target/cleannow.jar "$@"
