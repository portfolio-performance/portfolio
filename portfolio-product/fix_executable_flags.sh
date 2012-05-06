#!/bin/sh

# Tycho drops executable flag... fixing tar file after build

version=0.5.4-SNAPSHOT
environments=( macosx.cocoa.x86_64 linux.gtk.x86_64 linux.gtk.x86 )

cp *.mtree target/products
cd target/products

for environment in ${environments[@]}
do
    name=PortfolioPerformance-${version}-${environment}
    echo "Processing ${name}"
	gunzip ${name}.tar.gz
	tar -c -f - --exclude portfolio/PortfolioPerformance.app/Contents/MacOS/PortfolioPerformance --exclude portfolio/PortfolioPerformance @${name}.tar | tar -c -f ${name}_fixed.tar @- @${environment}.mtree
	gzip ${name}_fixed.tar
	rm ${name}.tar
	mv ${name}_fixed.tar.gz ${name}.tar.gz
done
