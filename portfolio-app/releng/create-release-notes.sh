#!/bin/bash

task=$1
if [ -z $task ]; then
  echo "Missing task: plain, html"
  exit 1
fi

java -jar ../../../portfolio-releng/target/portfolio-releng-1.0-SNAPSHOT-jar-with-dependencies.jar release-notes $task $(pwd)/../../portfolio-product/metainfo/info.portfolio_performance.PortfolioPerformance.metainfo.xml
