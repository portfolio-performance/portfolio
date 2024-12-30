#!/bin/bash

PCK_VERSION=$1
if [ -z $PCK_VERSION ]; then
  echo "There is no valid version available (e.g.: 0.30.1)."
  exit 1
fi

BASE=$(pwd)/../../portfolio-product/target/repository
E430BASE=$(pwd)/../../../portfolio-eclipse430/portfolio-app/releng/updatesite/portfolio

mkdir updatesite
rm updatesite/CNAME
rm updatesite/index.html
rm updatesite/index.html-e
rm -rf updatesite/portfolio
mkdir updatesite/portfolio
rm -rf updatesite/portfolio-e430
mkdir updatesite/portfolio-e430
cp -R ${BASE}/* updatesite/portfolio
cp -R ${E430BASE}/* updatesite/portfolio-e430
cp ${BASE}/index.html updatesite
sed -i -e 's/css\/styles.css/portfolio\/css\/styles.css/g' updatesite/index.html
sed -i -e 's/images\/pp_16.gif/portfolio\/images\/pp_16.gif/g' updatesite/index.html
sed -i -e "s/<body>/<body><!-- ${PCK_VERSION} -->/g" updatesite/index.html

cd updatesite

cat >CNAME <<EOF
updates.portfolio-performance.info
EOF

# git init
git add -A
git commit -m "Version ${PCK_VERSION}"
# git branch -m main gh-pages
# git remote add origin https://github.com/portfolio-performance/portfolio-updatesite.git
# git config http.postBuffer 524288000
