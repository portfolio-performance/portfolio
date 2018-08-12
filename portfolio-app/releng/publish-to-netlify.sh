#!/bin/bash

PCK_VERSION=$1
if [ -z $PCK_VERSION ]; then
  echo "There is no valid version available (e.g.: 0.30.1)."
  exit 1
fi

BASE=$(pwd)/../../portfolio-product/target/repository

rm -R updatesite
mkdir updatesite
mkdir updatesite/portfolio
cp -R ${BASE}/* updatesite/portfolio
cp ${BASE}/index.html updatesite
sed -i -e 's/css\/styles.css/portfolio\/css\/styles.css/g' updatesite/index.html
sed -i -e 's/images\/pp_16.gif/portfolio\/images\/pp_16.gif/g' updatesite/index.html
sed -i -e "s/<body>/<body><!-- ${PCK_VERSION} -->/g" updatesite/index.html

netlifyctl deploy --publish-directory updatesite --message ${PCK_VERSION} --site-id 963dc23a-4a77-4591-bcd0-b680a493e2d3
