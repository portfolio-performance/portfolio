#!/bin/bash

PROVIDER_ARG="$1"
if [ -z "$PROVIDER_ARG" ]; then
  echo "No PROVIDER_ARG given."
  exit 1
fi

STOREPASS="$2"
if [ -z "$STOREPASS" ]; then
  echo "No STOREPASS given."
  exit 1
fi

ALIAS="$3"
if [ -z "$ALIAS" ]; then
  echo "No ALIAS given."
  exit 1
fi

echo "Signing all PortfolioPerformance.exe files recursively..."

# Find and process each PortfolioPerformance executable
find . -type f -name 'PortfolioPerformance.exe' | while read -r EXECUTABLE; do
  echo "Processing: $EXECUTABLE"

  # Remove existing signature
  if osslsigncode remove-signature "$EXECUTABLE" "$EXECUTABLE.clean" >/dev/null; then
    mv "$EXECUTABLE.clean" "$EXECUTABLE"
  else
    echo "Warning: could not remove signature from $EXECUTABLE (maybe unsigned)"
  fi

  # Sign the executable
  jsign \
    --keystore "$PROVIDER_ARG" \
    --storetype PKCS11 \
    --storepass "$STOREPASS" \
    --alias "$ALIAS" \
    --tsaurl http://time.certum.pl/ \
    --name "Portfolio Performance" \
    --url https://www.portfolio-performance.info \
    "$EXECUTABLE"

  echo "Signed: $EXECUTABLE"
done
