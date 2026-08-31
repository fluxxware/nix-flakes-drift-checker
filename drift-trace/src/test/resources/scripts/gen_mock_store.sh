#!/usr/bin/env bash
#
# Generate a simulated /nix/store inside the staging-machine test fixture.
#
# Each store object is a content-addressed copy of a flake directory, named
# `<nix32-store-hash>-source` exactly like a real nix store entry. A copy hashes
# the same as its source (`nix hash path` of the object == the narHash recorded
# in the lock files), so LiveStateServiceImpl sees those flakes as DEPLOYED when
# the probe is pointed at this mock store via `--mock-store`.
#
# Usage:
#   gen_mock_store.sh --path <fixture-root> --out-path <store-dir>
#
#   --path and --out-path are REQUIRED. The store is written into --out-path
#   (created if missing), named <nix32-store-hash>-source per flake directory.
set -euo pipefail

usage() {
  cat <<'EOF'
Generate a simulated /nix/store inside the staging-machine test fixture.

USAGE:
  gen_mock_store.sh --path <fixture-root> --out-path <store-dir>

ARGS:
  --path <root>      (required) the staging-machine fixture root (dir with flake.nix).
  --out-path <dir>   (required) directory where the store is written (created if missing).

The store holds one object per flake directory, named <nix32-store-hash>-source
(content-addressed, like a real nix store).
EOF
}

ROOT=""
STORE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --path)
      ROOT="${2:-}"
      shift 2
      ;;
    --out-path)
      STORE="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "error: unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [[ -z "$ROOT" || -z "$STORE" ]]; then
  echo "error: --path and --out-path are required" >&2
  usage >&2
  exit 2
fi

ROOT="$(cd "$ROOT" && pwd)"
if [[ ! -f "$ROOT/flake.nix" ]]; then
  echo "error: not a flake fixture root (missing flake.nix) at: $ROOT" >&2
  exit 1
fi

rm -rf "$STORE"
mkdir -p "$STORE"

count=0
while IFS= read -r f; do
  d="$(dirname "$f")"
  base="$(basename "$d")"
  if [[ "$base" == "nix" ]] || [[ "$d" == "$ROOT" ]]; then
    continue
  fi
  hash="$(nix hash path "$d")"
  nix32="$(nix hash convert --hash-algo sha256 --from sri --to nix32 "$hash")"
  # Real nix store shape: /nix/store/<base32>-source/<flake-name>/...
  # cp -r <source> <dest>/ recreates dest/<basis-of-source>.
  mkdir -p "$STORE/${nix32}-source"
  cp -r "$d" "$STORE/${nix32}-source/"
  count=$((count + 1))
done < <(find "$ROOT" -name flake.nix -not -path '*/nix/*')

echo "created ${count} store objects in $STORE"
echo "sample:"
find "$STORE" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | head -3