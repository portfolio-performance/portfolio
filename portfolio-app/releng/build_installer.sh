#!/bin/bash

version=$1
if [ -z $version ]; then
  echo "There is no valid compilation version available (e.g.: 0.30.1)."
  exit 1
fi

rm ../../portfolio-product/target/products/*.exe

makensis -DARCHITECTURE=x32 -DSOFTWARE_VERSION=$version ./setup.nsi
makensis -DARCHITECTURE=x64 -DSOFTWARE_VERSION=$version ./setup.nsi
