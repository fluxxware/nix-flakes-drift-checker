#!/usr/bin/env bash
#
# Regenerate a generalized staging-machine fixture: mirrors --flake-root-path
# (structure + flake.nix) into --out-path and regenerates every flake.lock from
# scratch (fixed-point content hashes + nixpkgs remote on leaf flakes). Any
# drift markers (sha256-DRIFT-*) present in the source locks are replicated into
# the same locks in the output. Shell + jq only, no python.
#
# Usage:
#   gen_fixture.sh --flake-root-path <src> --out-path <dst>
set -euo pipefail

usage() {
  cat <<'EOF'
Regenerate a generalized staging-machine fixture.

USAGE:
  gen_fixture.sh --flake-root-path <src> --out-path <dst>

ARGS:
  --flake-root-path <src>  (required) the staging-machine fixture root whose
                           structure + flake.nix get mirrored.
  --out-path <dst>         (required) directory where the new fixture is written.

flake.lock files are regenerated with fixed-point content hashes; remote
nixpkgs inputs on leaf flakes are re-applied.
EOF
}

SRC=""
DST=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --flake-root-path) SRC="${2:-}"; shift 2 ;;
    --out-path) DST="${2:-}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "error: unknown argument: $1" >&2; usage >&2; exit 2 ;;
  esac
done
if [[ -z "$SRC" || -z "$DST" ]]; then
  echo "error: --flake-root-path and --out-path are required" >&2; usage >&2; exit 2
fi
SRC="$(cd "$SRC" && pwd)"
DST="$(cd "$(dirname "$DST")" && pwd)/$(basename "$DST")"
if [[ ! -f "$SRC/flake.nix" ]]; then
  echo "error: --flake-root-path has no flake.nix: $SRC" >&2; exit 1
fi

# ---- mirror structure + flake.nix (no nix/store, no locks) ----
rm -rf "$DST"
mkdir -p "$DST"
while IFS= read -r f; do
  rel="${f#"$SRC"/}"
  [[ "$rel" == nix/* ]] && continue
  mkdir -p "$(dirname "$DST/$rel")"
  cp "$f" "$DST/$rel"
done < <(find "$SRC" -name flake.nix)
echo "mirrored $(find "$DST" -name flake.nix | wc -l) flake.nix into $DST" >&2

# ---- parse flake.nix inputs -> "attr|url" lines ----
flake_inputs() {
  awk '
    /^[[:space:]]*inputs[[:space:]]*=/ { f=1; next }
    f && /}/ { exit }
    f && /url[[:space:]]*=[[:space:]]*"/ {
      line=$0
      if (line ~ /\.[[:space:]]*url/) { split(line,a,"."); attr=a[1] }
      else { sub(/^[[:space:]]*/,"",line); sub(/[[:space:]]*=.*/,"",line); attr=line }
      sub(/.*url[[:space:]]*=[[:space:]]*"/,"",line); sub(/".*/,"",line)
      gsub(/^[[:space:]]+|[[:space:]]+$/,"",attr)
      print attr "|" line
    }' "$1"
}

# per-flake lock hashmap handle (relative target dir -> narHash)
HMAP="$(mktemp)"
trap 'rm -f "$HMAP"' EXIT

refresh_hashes() {
  : > "$HMAP"
  while IFS= read -r d; do
    printf '%s|%s\n' "${d#$DST/}" "$(nix hash path "$d" 2>/dev/null || echo missing)" >> "$HMAP"
  done < <(find "$DST" -name flake.nix -printf '%h\n' | sort)
}

rel_hash() {
  local rel="$1"
  grep -m1 "^${rel#$DST/}|" "$HMAP" | cut -d'|' -f2
}

# Collect drift markers from the source fixture: lines "relLock|attr|marker"
# for every lock entry whose narHash starts with sha256-DRIFT-.
collect_drift() {
  while IFS= read -r lf; do
    rel="${lf#"$SRC"/}"
    [[ "$rel" == nix/* ]] && continue
    jq -r --arg rel "$rel" '
      .nodes | to_entries[]
      | select(.value.locked.narHash? // "" | startswith("sha256-DRIFT-"))
      | $rel + "|" + .key + "|" + .value.locked.narHash' "$lf" 2>/dev/null || true
  done < <(find "$SRC" -name flake.lock)
}

# Replicate drift markers gathered from the source into the same output locks:
# set nodes[attr].locked.narHash to the source marker value.
apply_drift() {
  while IFS='|' read -r rel attr marker; do
    [[ -n "$rel" && -n "$attr" && -n "$marker" ]] || continue
    local out="$DST/$rel"
    [[ -f "$out" ]] || continue
    jq --arg a "$attr" --arg m "$marker" '.nodes[$a].locked.narHash=$m' "$out" > "$out.t" && mv "$out.t" "$out"
  done <<< "$DRIFT_TSV"
}

# Generate a flake.lock for one flake dir from its inputs (path → real narHash,
# remote nixpkgs → synthetic github node) + root inputs.
gen_lock() {
  local d="$1" nixfile="$2"
  local -a attrs urls
  local attr url i
  attrs=(); urls=()
  while IFS='|' read -r attr url; do
    [[ -n "$attr" ]] || continue
    attrs+=("$attr"); urls+=("$url")
  done < <(flake_inputs "$nixfile")

  local json="{}" ri="{}"
  for i in "${!attrs[@]}"; do
    attr="${attrs[$i]}"; url="${urls[$i]}"
    if [[ "$url" == path:* ]]; then
      local rel="${url#path:}"
      local tgt; tgt="$(cd "$d" && realpath -m "$rel")"
      local h; h="$(rel_hash "$tgt")"
      ri="$(jq -c --arg a "$attr" '. + {($a): $a}' <<<"$ri")"
      json="$(jq -c --arg a "$attr" --arg rel "$rel" --arg nar "$h" --argjson n "$i" \
        '. + {($a): {"inputs":{},"locked":{"lastModified":(1786700000+$n),"narHash":$nar,"path":$rel,"type":"path"},"original":{"path":$rel,"type":"path"}}}' <<<"$json")"
    else
      local nph="sha256-nixpkgs-$(printf '%s' "$attr" | sha256sum | cut -c1-20)"
      ri="$(jq -c --arg a "$attr" '. + {($a): $a}' <<<"$ri")"
      json="$(jq -c --arg a "$attr" --arg nar "$nph" --argjson n "$i" \
        '. + {($a): {"inputs":{},"locked":{"lastModified":(1786660000+$n),"narHash":$nar,"rev":"a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5","type":"github"},"original":{"owner":"NixOS","repo":"nixpkgs","type":"github"}}}' <<<"$json")"
    fi
  done
  jq -n --argjson nodes "$json" --argjson ri "$ri" \
    '{nodes: ($nodes + {root: {inputs: $ri}}), root: "root", version: 7}' > "$d/flake.lock"
}

# ---- drift markers to replicate (sourced from the reference fixture) ----
DRIFT_TSV="$(collect_drift)"

# ---- fixed-point to convergence (hash depends on lock content) ----
for _it in $(seq 1 4); do
  refresh_hashes
  while IFS= read -r d; do
    [[ -f "$d/flake.nix" ]] || continue
    gen_lock "$d" "$d/flake.nix"
  done < <(find "$DST" -name flake.nix -printf '%h\n' | sort)
  apply_drift
done

echo "regenerated $(find "$DST" -name flake.lock | wc -l) flake.lock (fixed-point)" >&2
echo "wrote fixture to $DST" >&2
