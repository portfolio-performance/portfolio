#!/bin/bash

PROJECT_BASEIDR=$1
if [ -z $PROJECT_BASEIDR ]; then
  echo "No PROJECT_BASEIDR given. Must point to Maven project."
  exit 1
fi

APP_BUNDLE=$2
if [ -z $APP_BUNDLE ]; then
  echo "No APP_BUNDLE given. Must point to macOS application bundle."
  exit 1
fi

if [ -z $CODESIGN_ID ]; then
  echo "No CODESIGN_ID given. Configure Code Signing ID via environment."
  exit 1
fi

# delete p2 binary cache (zipped binary is not accepted by Apple notarization process)

rm -rf $APP_BUNDLE/Contents/Eclipse/p2/org.eclipse.equinox.p2.core/cache


# sign dylib

find $APP_BUNDLE -type f -name '*.dylib' \
  -exec codesign \
  -s "$CODESIGN_ID" \
  --deep --timestamp \
  --options runtime \
  --entitlement $PROJECT_BASEIDR/signing/entitlements.plist \
  --force -vvvv {} \;

# sign executables

find $APP_BUNDLE -perm +111 -type f \
  -exec codesign \
  -s "$CODESIGN_ID" \
  --deep --timestamp \
  --options runtime \
  --entitlement $PROJECT_BASEIDR/signing/entitlements.plist \
  --force -vvvv {} \;

# sign app bundle

 codesign -s "$CODESIGN_ID" \
  --deep --timestamp \
  --options runtime \
  --entitlement $PROJECT_BASEIDR/signing/entitlements.plist \
  --force -vvvv \
  $APP_BUNDLE
