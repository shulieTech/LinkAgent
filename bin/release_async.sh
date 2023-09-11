#!/bin/bash

HOME=$(dirname $(pwd))

echo "start package..."
seq 3|xargs -P 3 -I {} sh ${HOME}/bin/package_async.sh {}
echo "end package..."

echo "start deploy ..."
sh ${HOME}/bin/deploy.sh
echo "end deploy ..."


echo "release finish ..."
echo "release home package: ${HOME}/deploy"
