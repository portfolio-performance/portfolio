#!/bin/bash

task=$1
if [ -z $task ]; then
  echo "Missing task: sync-terms, upload-new-terms, upload, download"
  exit 1
fi

api_token=$2
if [ -z $api_token ]; then
  echo "Missing POEditor.com API Token."
  exit 1
fi

project_id=$3
if [ -z $project_id ]; then
  echo "Missing POEditor.com Project ID."
  exit 1
fi

java -jar ../../../portfolio-releng/target/portfolio-releng-1.0-SNAPSHOT-jar-with-dependencies.jar $task $api_token $project_id $(pwd)/../.. $(pwd)/translation-config.xml
