#!/bin/bash

PCK_VERSION=$1
if [ -z $PCK_VERSION ]; then
  echo "There is no valid version available (e.g.: 0.30.1)."
  exit 1
fi

github-release release \
    --user buchen \
    --repo portfolio \
    --tag ${PCK_VERSION} \
    --name ${PCK_VERSION} \
    --pre-release

BASE=$(pwd)/../../portfolio-product/target/products/
BASE_GPG=$(pwd)/../../portfolio-product/target/gpg/target/products/

FILES=$(cat <<EOF
PortfolioPerformance-${PCK_VERSION}-setup.exe
PortfolioPerformance-${PCK_VERSION}-linux.gtk.x86_64.tar.gz
PortfolioPerformance-${PCK_VERSION}-win32.win32.x86_64.zip
PortfolioPerformance-distro-${PCK_VERSION}-win32.win32.x86_64.zip
PortfolioPerformance-${PCK_VERSION}-x86_64.dmg
PortfolioPerformance-${PCK_VERSION}-aarch64.dmg
EOF
)

for f in $FILES;
do
  if [ ! -d $f ]; then
    echo "Processing $f file..."
    gh release upload --repo buchen/portfolio ${PCK_VERSION} $BASE/$f
    gh release upload --repo buchen/portfolio ${PCK_VERSION} $BASE_GPG/$f.asc
    echo ""
  fi
done
