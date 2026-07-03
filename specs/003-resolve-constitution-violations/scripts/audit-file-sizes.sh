#!/bin/bash
# Audit script to find all Kotlin files exceeding 300 lines
# Usage: ./audit-file-sizes.sh [--json]
#   --json: Output as JSON array for programmatic consumption

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
OUTPUT_MODE="${1:-text}"

cd "$PROJECT_ROOT"

case "$OUTPUT_MODE" in
    --json)
        find app/src -name "*.kt" -exec wc -l {} + | sort -rn | awk '$1 > 300 {printf "{\"file\":\"%s\",\"lines\":%d}\n", $2, $1}' | jq -s '.'
        ;;
    *)
        find app/src -name "*.kt" -exec wc -l {} + | sort -rn | awk '$1 > 300 {print $1, $2}'
        ;;
esac
