#!/usr/bin/env bash

set -euo pipefail

proxy_host='localhost'
proxy_port='1080'
dst_host=''
dst_port='0'
connections='1'
payload='1024m'
rate='256m'

usage() {
  cat <<'EOF'
Usage: run-speedtest.sh [options]

Options:
  --proxy-host HOST       Proxy host (default: localhost)
  --proxy-port PORT       Proxy port (default: 1080)
  --dst-host HOST         Destination host
  --dst-port PORT         Destination port (0 disables it)
  --connections COUNT     Number of connections (default: 1)
  --payload SIZE          Payload size (default: 1024m)
  --rate RATE             Transfer rate (default: 256m)
  -h, --help              Show this help
EOF
}

die() {
  printf 'Error: %s\n' "$1" >&2
  exit 1
}

while (($# > 0)); do
  case "$1" in
    --proxy-host)
      (($# >= 2)) || die "$1 requires a value"
      proxy_host=$2
      shift 2
      ;;
    --proxy-host=*)
      proxy_host=${1#*=}
      shift
      ;;
    --proxy-port)
      (($# >= 2)) || die "$1 requires a value"
      proxy_port=$2
      shift 2
      ;;
    --proxy-port=*)
      proxy_port=${1#*=}
      shift
      ;;
    --dst-host)
      (($# >= 2)) || die "$1 requires a value"
      dst_host=$2
      shift 2
      ;;
    --dst-host=*)
      dst_host=${1#*=}
      shift
      ;;
    --dst-port)
      (($# >= 2)) || die "$1 requires a value"
      dst_port=$2
      shift 2
      ;;
    --dst-port=*)
      dst_port=${1#*=}
      shift
      ;;
    --connections)
      (($# >= 2)) || die "$1 requires a value"
      connections=$2
      shift 2
      ;;
    --connections=*)
      connections=${1#*=}
      shift
      ;;
    --payload)
      (($# >= 2)) || die "$1 requires a value"
      payload=$2
      shift 2
      ;;
    --payload=*)
      payload=${1#*=}
      shift
      ;;
    --rate)
      (($# >= 2)) || die "$1 requires a value"
      rate=$2
      shift 2
      ;;
    --rate=*)
      rate=${1#*=}
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    --)
      shift
      (($# == 0)) || die "unexpected argument: $1"
      ;;
    *)
      die "unknown option: $1"
      ;;
  esac
done

[[ $dst_port =~ ^[0-9]+$ ]] || die "--dst-port must be an integer between 0 and 65535"
((dst_port <= 65535)) || die "--dst-port must be an integer between 0 and 65535"

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
test_root=$(cd -- "$script_dir/.." && pwd)
root=$(cd -- "$test_root/.." && pwd)

[[ -f "$test_root/pom.xml" && -f "$root/pom.xml" ]] || \
  die "cannot locate the urban-spork-test module from '$script_dir'"

test_target=$test_root/target
deps_file=$test_target/speedtest-deps.txt

if [[ -n ${JAVA_HOME:-} && -x "$JAVA_HOME/bin/java" ]]; then
  java_bin=$JAVA_HOME/bin/java
else
  java_bin=$(command -v java || true)
fi
[[ -n ${java_bin:-} ]] || die "java was not found; set JAVA_HOME or add java to PATH"

mvn_bin=$(command -v mvn || true)
[[ -n ${mvn_bin:-} ]] || die "mvn was not found; add Maven to PATH"

settings=${MAVEN_SETTINGS:-}
if [[ -z $settings ]]; then
  mvn_home=${MAVEN_HOME:-}
  if [[ -n $mvn_home ]]; then
    for candidate in "$mvn_home/settings.xml" "$mvn_home/conf/settings.xml"; do
      if [[ -f $candidate ]]; then
        settings=$candidate
        break
      fi
    done
  fi
fi

# A stale classpath from a previous run must never mask a failed build.
rm -f -- "$deps_file"
mvn_args=(-q -pl urban-spork-test -am -DskipTests -Dskip=true compile
  dependency:build-classpath -Dmdep.outputFile=target/speedtest-deps.txt
  -Dmdep.includeScope=runtime)
if [[ -n $settings ]]; then
  mvn_args=(-s "$settings" "${mvn_args[@]}")
fi

if ! (cd -- "$root" && "$mvn_bin" "${mvn_args[@]}"); then
  die 'Maven failed'
fi

[[ -s "$deps_file" ]] || die "Maven did not write the dependency classpath to '$deps_file'"
deps=$(<"$deps_file")
class_path="$test_target/classes:$deps"

vm_args=("-DproxyHost=$proxy_host" "-DproxyPort=$proxy_port" "-Dconnections=$connections" "-Dpayload=$payload")
[[ -n $rate ]] && vm_args+=("-Drate=$rate")
((dst_port > 0)) && vm_args+=("-DdstPort=$dst_port")
[[ -n $dst_host ]] && vm_args+=("-DdstHost=$dst_host")

if [[ -t 1 ]]; then
  terminal_size=$(stty size 2>/dev/null || true)
  if [[ $terminal_size =~ ^([0-9]+)[[:space:]]+([0-9]+)$ ]]; then
    terminal_rows=${BASH_REMATCH[1]}
    terminal_columns=${BASH_REMATCH[2]}
    if ((terminal_columns > 1 && terminal_rows > 1)); then
      vm_args+=("-DterminalColumns=$terminal_columns" "-DterminalRows=$terminal_rows")
    fi
  fi
fi

vm_args+=(--enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8
  -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8
  -classpath "$class_path" com.urbanspork.test.tool.SpeedTestLocalServer)

exec "$java_bin" "${vm_args[@]}"
