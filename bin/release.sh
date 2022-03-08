#!/bin/bash

HOME=$(dirname $(pwd))

echo "start package..."
sh ${HOME}/bin/package.sh
echo "end package..."

echo "start deploy ..."
sh ${HOME}/bin/deploy.sh
echo "end deploy ..."



echo "release finish ..."
echo "release home package: ${HOME}/deploy"
