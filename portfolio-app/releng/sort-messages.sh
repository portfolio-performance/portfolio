#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

mvn -f "$SCRIPT_DIR/../pom.xml" -pl :portfolio-build-tools-check -am verify -Dportfolio.build.command=write "$@"
