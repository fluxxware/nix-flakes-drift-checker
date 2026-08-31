#!/usr/bin/env bash
#
# Regenerate staging-machine.json as a faithful mirror of the staging-machine
# fixture, ported to shell + jq from gen_dataset.py. Walks the flake tree in
# pre-order, computes the same drift semantics as DriftCompareServiceImpl, and
# writes {nodes, live, expected{statuses, unrefreshed, totals}}.
#
# Usage:
#   gen_dataset.sh --path <fixture-root> --out-path <dataset.json>
set -euo pipefail

usage() {
  cat <<'EOF'
Regenerate staging-machine.json mirror from the staging-machine fixture.

USAGE:
  gen_dataset.sh --path <fixture-root> --out-path <dataset.json>

ARGS:
  --path <root>       (required) the staging-machine fixture root (dir with flake.nix).
  --out-path <file>   (required) destination JSON file (full path, incl. file name).
EOF
}

ROOT=""
OUT_PATH=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --path)     ROOT="${2:-}"; shift 2 ;;
    --out-path) OUT_PATH="${2:-}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "error: unknown argument: $1" >&2; usage >&2; exit 2 ;;
  esac
done
if [[ -z "$ROOT" || -z "$OUT_PATH" ]]; then
  echo "error: --path and --out-path are required" >&2; usage >&2; exit 2
fi
ROOT="$(cd "$ROOT" && pwd)"
if [[ ! -f "$ROOT/flake.nix" ]]; then
  echo "error: not a flake fixture root (missing flake.nix) at: $ROOT" >&2
  exit 1
fi
mkdir -p "$(dirname "$OUT_PATH")"

LINES="$(mktemp)"
trap 'rm -f "$LINES" "$LINES.n" "$LINES.t"' EXIT

# Extract "attr|url" lines from a flake.nix inputs block (one-pass awk).
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

# narHash of an input in a parent lock (empty if absent).
lock_narhash() {
  jq -r --arg a "$2" '.nodes[$a].locked.narHash // empty' "$1" 2>/dev/null || true
}

# Pre-order walk; writes one line per node:
#   name|remote|path|disk|lock|live|kidsCsv
walk() {
  local d="$1" name="$2" lockfile="$3" is_root="$4"
  local kids=() attr url tgt kid pe csv
  if [[ -f "$d/flake.nix" ]]; then
    while IFS='|' read -r attr url; do
      [[ -n "$attr" ]] || continue
      if [[ "$url" == path:* ]]; then
        kid="$(basename "$(realpath -m "$d/${url#path:}")")"
      else
        kid="$attr"
      fi
      kids+=("$kid")
    done < <(flake_inputs "$d/flake.nix")
  fi
  csv=""; [[ ${#kids[@]} -gt 0 ]] && { local IFS=,; csv="${kids[*]}"; }
  if [[ "$is_root" == 1 ]]; then
    printf 'root|0|/fixtures/machine|sha256-root|sha256-root|true|%s\n' "$csv" >> "$LINES"
  else
    pe="$(lock_narhash "$lockfile" "$name")"
    if [[ "$pe" == sha256-DRIFT-* ]]; then
      printf '%s|0|/fixtures/flakes/%s|sha256-%s|sha256-drift-%s|false|%s\n' \
        "$name" "$name" "$name" "$name" "$csv" >> "$LINES"
    else
      printf '%s|0|/fixtures/flakes/%s|sha256-%s|sha256-%s|true|%s\n' \
        "$name" "$name" "$name" "$name" "$csv" >> "$LINES"
    fi
  fi
  if [[ -f "$d/flake.nix" ]]; then
    while IFS='|' read -r attr url; do
      [[ -n "$attr" ]] || continue
      if [[ "$url" == path:* ]]; then
        tgt="$(realpath -m "$d/${url#path:}")"
        walk "$tgt" "$(basename "$tgt")" "$d/flake.lock" 0
      else
        printf '%s|1||sha256-%s|sha256-remote-%s|false|\n' \
          "$attr" "$attr" "$attr" >> "$LINES"
      fi
    done < <(flake_inputs "$d/flake.nix")
  fi
}

walk "$ROOT" root "" 1
echo "walked $(wc -l < "$LINES") nodes" >&2

# Reverse pass (awk): resolve base types bottom-up, propagate
# CHAIN_STALE_TRANSITIVE, compute unrefreshed and synced/drifted totals.
# In : name|remote|path|disk|lock|live|kidsCsv
# Out: name|remote|path|disk|lock|live|kidsCsv|typesCsv|unrefCsv
awk -F'|' '
{
  n=$1; r=$2; p=$3; d=$4; l=$5; v=$6; k=$7
  if (r==1) t="REMOTE"
  else if (l=="") t="NARHASH_ABSENT"
  else if (l!=d) t="LOCAL_DRIFT,CHAIN_STALE_CAUSE"
  else if (v!="true") t="UNDEPLOYED"
  else t=""
  lines[NR]=n FS r FS p FS d FS l FS v FS k
  types[NR]=t
}
END {
  for (i=NR; i>=1; i--) {
    split(lines[i], a, FS)
    n=a[1]; k=a[7]; t=types[i]
    split(k,kids,",")
    un=""; tr=""
    for (j=1; j<=length(kids); j++) {
      kid=kids[j]; if (kid=="") continue
      if (chain[kid]=="1") { un=un (un==""?"":",") kid; tr="1" }
    }
    if (tr=="1" && index(t,"REMOTE")==0 && index(t,"CHAIN_STALE_TRANSITIVE")==0)
      t=t (t==""?"":",") "CHAIN_STALE_TRANSITIVE"
    if (index(t,"CHAIN_STALE_CAUSE")>0 || index(t,"CHAIN_STALE_TRANSITIVE")>0) chain[n]="1"
    types[i]=t; unref[i]=un
  }
  drifted=0
  for (i=1; i<=NR; i++) {
    split(lines[i], a, FS)
    if (types[i]!="" && index(types[i],"REMOTE")==0) drifted++
    print a[1] FS a[2] FS a[3] FS a[4] FS a[5] FS a[6] FS a[7] FS types[i] FS unref[i]
  }
  print "TOTALS " NR " " (NR-drifted) " " drifted > "/dev/stderr"
}' "$LINES" > "$LINES.t"

# Assemble final JSON (trivial TSV -> JSON conversion).
jq -n --rawfile l "$LINES.t" '
  def N:
    ($l | split("\n") | map(select(length>0))
      | map(split("|")
        | { name: .[0],
            remote: (.[1] == "1"),
            path: (if .[2] == "" then null else .[2] end),
            disk: .[3], lock: .[4],
            live: (.[5] == "true"),
            kids: (if .[6] == "" then [] else (.[6] | split(",")) end),
            types: (if .[7] == "" then [] else (.[7] | split(",")) end),
            unref: (if .[8] == "" then [] else (.[8] | split(",")) end) }));
  N as $N
  | { nodes: [ $N[] | { name: .name, path: .path, diskHash: .disk, lockHash: .lock, children: .kids } ],
      live: (reduce $N[] as $x ({}; .[$x.name] = $x.live)),
      expected: {
        statuses: (reduce $N[] as $x ({}; .[$x.name] = ($x.types | sort))),
        unrefreshed: (reduce $N[] as $x ({}; .[$x.name] = $x.unref)),
        totals: { total: ($N | length),
                  synced: ($N | map(select((.types == []) or ((.types | index("REMOTE")) != null))) | length),
                  drifted: ($N | map(select((.types != []) and ((.types | index("REMOTE")) == null))) | length) } } }
' > "$OUT_PATH"

echo "wrote $OUT_PATH (nodes: $(jq '.nodes|length' "$OUT_PATH"))" >&2
echo "totals: $(jq -c '.expected.totals' "$OUT_PATH")" >&2
