#!/bin/bash

BINTRAY_USER=$1
if [ -z $BINTRAY_USER ]; then
  echo "Missing Bintray User"
  exit 1
fi

BINTRAY_API_KEY=$2
if [ -z $BINTRAY_API_KEY ]; then
  echo "Missing Bintray API Token"
  exit 1
fi

PCK_VERSION=$3
if [ -z $PCK_VERSION ]; then
  echo "There is no valid version available (e.g.: 0.30.1)."
  exit 1
fi

BINTRAY_OWNER=buchen
BINTRAY_REPO=downloads
PCK_NAME=portfolio-performance

BASE=$(pwd)/../../portfolio-product/target/products/

FILES=$(cat <<EOF
PortfolioPerformance-${PCK_VERSION}-linux.gtk.x86_64.tar.gz
PortfolioPerformance-${PCK_VERSION}-linux.gtk.x86.tar.gz
PortfolioPerformance-${PCK_VERSION}-win32.win32.x86_64.zip
PortfolioPerformance-${PCK_VERSION}-win32.win32.x86.zip
PortfolioPerformance-distro-${PCK_VERSION}-macosx.cocoa.x86_64.tar.gz
PortfolioPerformance-distro-${PCK_VERSION}-win32.win32.x86_64.zip
PortfolioPerformance-distro-${PCK_VERSION}-win32.win32.x86.zip
EOF
)

for f in $FILES;
do
  if [ ! -d $f ]; then
    echo "Processing $f file..."
    curl -X PUT -T $BASE/$f -u ${BINTRAY_USER}:${BINTRAY_API_KEY} https://api.bintray.com/content/${BINTRAY_OWNER}/${BINTRAY_REPO}/${PCK_NAME}/${PCK_VERSION}/${PCK_VERSION}/$f;publish=0
    echo ""
  fi
done