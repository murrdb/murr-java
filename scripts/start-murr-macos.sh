#!/usr/bin/env bash
# Starts a native murr server in the background for tests on macOS, where there is no Docker.
# The server version follows the client version (0.2.2-1 -> 0.2.2), same as MurrContainer.
# Set MURR_VERSION to use another release.
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ "$(uname -m)" != "arm64" ]]; then
  echo "only murr-macos-arm64 is published, got $(uname -m)" >&2
  exit 1
fi

version="${MURR_VERSION:-}"
if [[ -z "$version" ]]; then
  client=$(mvn -B -q help:evaluate -Dexpression=project.version -DforceStdout)
  version=$(echo "$client" | sed -E 's/-SNAPSHOT$//; s/-[0-9]+$//')
fi

dir="${RUNNER_TEMP:-${TMPDIR:-/tmp}}/murr"
bin="$dir/murr"
endpoint="http://127.0.0.1:8080"
mkdir -p "$dir/data"

echo "downloading murr $version"
curl -fsSL -o "$bin" "https://github.com/murrdb/murr/releases/download/v$version/murr-macos-arm64"
chmod +x "$bin"

MURR_STORAGE_PATH="$dir/data" "$bin" > "$dir/murr.log" 2>&1 &
echo "started murr (pid $!), logs in $dir/murr.log"

for _ in $(seq 1 60); do
  if curl -fs "$endpoint/health" > /dev/null; then
    echo "murr is up at $endpoint"
    if [[ -n "${GITHUB_ENV:-}" ]]; then
      echo "MURR_ENDPOINT=$endpoint" >> "$GITHUB_ENV"
      echo "MURR_LOG_DIR=$dir" >> "$GITHUB_ENV"
    else
      echo "run tests with: MURR_ENDPOINT=$endpoint mvn verify"
    fi
    exit 0
  fi
  sleep 1
done

echo "murr did not become healthy in 60s" >&2
cat "$dir/murr.log" >&2
exit 1
