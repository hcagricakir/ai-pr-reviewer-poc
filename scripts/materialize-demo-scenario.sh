#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "Usage: $0 <scenario-id> <base|head> <destination-root>" >&2
  exit 1
fi

SCENARIO="$1"
VARIANT="$2"
DESTINATION_ROOT="$3"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

if [[ "${VARIANT}" != "base" && "${VARIANT}" != "head" ]]; then
  echo "Variant must be either 'base' or 'head'." >&2
  exit 1
fi

copy_or_remove() {
  local source_file="$1"
  local destination_file="$2"

  mkdir -p "$(dirname "${destination_file}")"

  if [[ -f "${source_file}" ]]; then
    cp "${source_file}" "${destination_file}"
  else
    rm -f "${destination_file}"
  fi
}

case "${SCENARIO}" in
  clean-implementation)
    copy_or_remove \
      "${REPO_ROOT}/demo/scenarios/clean-implementation/${VARIANT}/DeliveryFeePolicy.java" \
      "${DESTINATION_ROOT}/src/main/java/com/acme/pricing/DeliveryFeePolicy.java"
    ;;
  naming-problem)
    copy_or_remove \
      "${REPO_ROOT}/demo/scenarios/naming-problem/${VARIANT}/CheckoutService.java" \
      "${DESTINATION_ROOT}/src/main/java/com/acme/checkout/CheckoutService.java"
    ;;
  null-edge-case)
    copy_or_remove \
      "${REPO_ROOT}/demo/scenarios/null-edge-case/${VARIANT}/CustomerLookupService.java" \
      "${DESTINATION_ROOT}/src/main/java/com/acme/customer/CustomerLookupService.java"
    ;;
  responsibility-violation)
    copy_or_remove \
      "${REPO_ROOT}/demo/scenarios/responsibility-violation/${VARIANT}/OrderPlacementService.java" \
      "${DESTINATION_ROOT}/src/main/java/com/acme/order/OrderPlacementService.java"
    ;;
  performance-smell)
    copy_or_remove \
      "${REPO_ROOT}/demo/scenarios/performance-smell/${VARIANT}/InventoryProjectionService.java" \
      "${DESTINATION_ROOT}/src/main/java/com/acme/report/InventoryProjectionService.java"
    ;;
  *)
    echo "Unknown scenario: ${SCENARIO}" >&2
    exit 1
    ;;
esac

echo "Materialized '${SCENARIO}' (${VARIANT}) into ${DESTINATION_ROOT}"
