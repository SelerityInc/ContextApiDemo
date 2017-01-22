#!/bin/bash
#
# Copyright (C) 2017 Selerity, Inc. (support@seleritycorp.com)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e

JAVA="$(which java 2>/dev/null || true)"
MAVEN="$(which mvn 2>/dev/null || true)"

error() {
    echo "Error:" "$@" >&2
    exit 1
}

log() {
    echo "$@"
}

SCRIPT_DIR="$(dirname "$0")"
log "Switching to $SCRIPT_DIR directory ..."
cd "$SCRIPT_DIR"

JAR_VERSION=$(grep '<version>' pom.xml \
    | sed -n -e '2{ ' -e 's@^.*<version>\([^<]*\)</.*@\1@' -e 'p' -e '}')
JAR_FILE="target/ContextApiDemo-$JAR_VERSION.jar"

log "Checking for java ..."
[[ -x "$JAVA" ]] || error 'Could not find a usable java executable in $PATH. Please install Java.'

log "Checking for jar file ..."
if [ ! -e "$JAR_FILE" ]
then
    log "Jar file '$JAR_FILE' does not exist. Trying to build it ..."

    [[ -x "$MAVEN" ]] || error "Jar file '$JAR_FILE' does not exist. Cannot build it, as" \
        "Maven (mvn) could not be found. Please either build yourself, or install Maven."

    log "Hit Ctrl-C now to avoid building ..."
    for COUNTER in $(seq 5 -1 1)
    do
        log -n "$COUNTER "
        sleep 1s
    done
    mvn clean package
    if [ ! -e "$JAR_FILE" ]
    then
        error "Maven build passed, but jar file '$JAR_FILE' still does not exist."
    fi
fi

# Splitting passed arguments into java arguments and arguments for the
# demo jar. That way, users can override java settings like logging.
DEMO_ARGS=()
JAVA_ARGS=()
for ARG in "$@"
do
    if [ "${ARG:0:2}" = "-D" -o "${ARG:0:2}" = "-X" ]
    then
        JAVA_ARGS+=("$ARG")
    else
        DEMO_ARGS+=("$ARG")
    fi
done

COMMAND=( java "${JAVA_ARGS[@]}" -jar "$JAR_FILE" "${DEMO_ARGS[@]}" )
log "Starting command:" "${COMMAND[@]}"
log
exec "${COMMAND[@]}"