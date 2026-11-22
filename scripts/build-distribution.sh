#!/usr/bin/env bash
# Builds the self-contained runnable jar (game, JavaFX for this platform and Jackson) into
# distribution/.
#
# Usage: scripts/build-distribution.sh
# Then:  java -jar distribution/schotten-totten-1.0.0-all.jar [--with-cli]
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"
mvn -q clean package
mkdir -p distribution
cp target/schotten-totten-*-all.jar distribution/
ls -lh distribution/schotten-totten-*-all.jar
