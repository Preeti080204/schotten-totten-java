#!/usr/bin/env bash
# Builds the LO02 submission archive: src/, classes/ and doc/ (Javadoc, final class diagram,
# status report and the other project documents), plus README.md and pom.xml.
#
# Usage: scripts/package-submission.sh <NAME1> <NAME2> [--term A25|A26]
#
# The specification (A26) prints the file name XXX_YYY_Project_LO02_A25.zip. That name is used
# unless --term is given explicitly; confirm the expected suffix with the teacher.
set -euo pipefail

usage() {
    echo "Usage: $0 <NAME1> <NAME2> [--term A25|A26]" >&2
    exit 1
}

[[ $# -ge 2 ]] || usage
name1=$1
name2=$2
shift 2
term=A25
if [[ $# -gt 0 ]]; then
    [[ $1 == --term && $# -eq 2 && ( $2 == A25 || $2 == A26 ) ]] || usage
    term=$2
fi
for n in "$name1" "$name2"; do
    [[ $n =~ ^[A-Za-z][A-Za-z-]*$ ]] || { echo "Names may only contain letters and hyphens: '$n'" >&2; exit 1; }
done

root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"
archive="${name1}_${name2}_Project_LO02_${term}.zip"

echo "== Building, testing and generating the Javadoc"
mvn -q clean package javadoc:javadoc

apidocs=""
for candidate in target/reports/apidocs target/site/apidocs; do
    if [[ -f $candidate/index.html ]]; then
        apidocs=$candidate
        break
    fi
done
[[ -n $apidocs ]] || { echo "Javadoc output not found" >&2; exit 1; }

stage=submission/stage
rm -rf "$stage"
mkdir -p "$stage/doc"
cp -R src "$stage/src"
cp -R target/classes "$stage/classes"
cp -R "$apidocs" "$stage/doc/javadoc"
cp -R docs/uml "$stage/doc/uml"
cp -R docs/screenshots "$stage/doc/screenshots"
cp docs/*.md "$stage/doc/"
cp README.md pom.xml "$stage/"

mkdir -p submission
rm -f "submission/$archive"
(cd "$stage" && zip -qr "../$archive" .)
rm -rf "$stage"

echo "== Created submission/$archive"
unzip -l "submission/$archive" | tail -1
if [[ $term == A25 ]]; then
    echo "WARNING: the A26 specification prints the name '..._Project_LO02_A25.zip'."
    echo "         This archive follows it literally. Confirm the expected suffix with the teacher;"
    echo "         use --term A26 to produce the other name."
fi
