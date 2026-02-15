#!/bin/bash

# Configuration
REPLICATE_API_TOKEN=${REPLICATE_API_TOKEN:-""}
REPLICATE_MD="src/main/resources/rag/platforms/replicate.md"

if [ -z "$REPLICATE_API_TOKEN" ]; then
    echo "Error: REPLICATE_API_TOKEN is not set."
    echo "Usage: REPLICATE_API_TOKEN=your_token ./validateModels.sh"
    exit 1
fi

echo "Validating models in $REPLICATE_MD..."
echo "--------------------------------------------------"

# Temporary file to store owner/id pairs
TMP_MODELS=$(mktemp)

# Extract owner and id
# Matches:
# - **owner**: owner_name
# - **id**: model_id
# OR
# - **ID**: owner/id
awk '
/^- \*\*owner\*\*: / { owner=$0; sub(/^- \*\*owner\*\*: /, "", owner); }
/^- \*\*id\*\*: / { id=$0; sub(/^- \*\*id\*\*: /, "", id); print owner "|" id; owner=""; id=""; }
/^- \*\*ID\*\*: / { 
    id_full=$0; sub(/^- \*\*ID\*\*: /, "", id_full); 
    split(id_full, parts, "/");
    if (length(parts) == 2) {
        print parts[1] "|" parts[2];
    } else {
        print "|" id_full;
    }
}
' "$REPLICATE_MD" > "$TMP_MODELS"

total_models=$(wc -l < "$TMP_MODELS" | xargs)
current=0
failures=0

while IFS='|' read -r owner id; do
    current=$((current + 1))
    printf "[%d/%d] Checking %s/%s... " "$current" "$total_models" "$owner" "$id"
    
    if [ -z "$owner" ]; then
        echo "ERROR: Missing owner"
        failures=$((failures + 1))
        continue
    fi

    response=$(curl -s -H "Authorization: Bearer $REPLICATE_API_TOKEN" \
        "https://api.replicate.com/v1/models/$owner/$id")
    
    if echo "$response" | jq -e '.detail == "Not found"' > /dev/null; then
        echo "FAILED (Not Found)"
        failures=$((failures + 1))
        continue
    fi

    error_msg=$(echo "$response" | jq -r '.error // empty')
    if [ -n "$error_msg" ]; then
        echo "FAILED (API Error: $error_msg)"
        failures=$((failures + 1))
        continue
    fi

    visibility=$(echo "$response" | jq -r '.visibility')
    run_count=$(echo "$response" | jq -r '.run_count')
    latest_version=$(echo "$response" | jq -r '.latest_version')

    errors=()
    if [ "$visibility" != "public" ]; then
        errors+=("visibility=$visibility")
    fi
    if [ "$run_count" -eq 0 ] 2>/dev/null; then
        # Some models might not have run_count in the response if they are proxies? 
        # Actually Replicate models should have it.
        # But let's be safe.
        if [ "$run_count" == "0" ]; then
             errors+=("run_count=0")
        fi
    fi
    if [ "$latest_version" == "null" ]; then
        errors+=("no_latest_version")
    fi

    if [ ${#errors[@]} -eq 0 ]; then
        echo "OK"
    else
        echo "FAILED (${errors[*]})"
        failures=$((failures + 1))
    fi
done < "$TMP_MODELS"

rm "$TMP_MODELS"

echo "--------------------------------------------------"
if [ "$failures" -eq 0 ]; then
    echo "All models validated successfully!"
    exit 0
else
    echo "Validation failed for $failures models."
    exit 1
fi
