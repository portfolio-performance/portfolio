#!/bin/bash

REPO_PATH=$1
if [ -z $REPO_PATH ]; then
  echo "There is no valid path available (e.g.: 2018/0.30.1)."
  exit 1
fi

BASE=$(pwd)/../../portfolio-product/target/products/
BASE_GPG=$(pwd)/../../portfolio-product/target/gpg/products/

FILES=${BASE}/PortfolioPerformance-*
FILES_GPG=${BASE_GPG}/PortfolioPerformance-*

mkdir $(pwd)/../../../Archive/$REPO_PATH

for f in $FILES;
do
  if [ ! -d $f ]; then
    filename=$(basename -- "$f")
    echo "Processing $filename file..."
    mv $f $(pwd)/../../../Archive/$REPO_PATH
    echo ""
  fi
done

for f in $FILES_GPG;
do
  if [ ! -d $f ]; then
    filename=$(basename -- "$f")
    echo "Processing $filename file..."
    mv $f $(pwd)/../../../Archive/$REPO_PATH
    echo ""
  fi
done