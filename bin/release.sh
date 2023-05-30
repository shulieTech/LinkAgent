#!/bin/bash

HOME=$(dirname $(pwd))

echo "start package..."
sh ${HOME}/bin/package.sh
echo "end package..."

echo "start deploy ..."
sh ${HOME}/bin/deploy.sh
echo "end deploy ..."

cd ${HOME}/instrument-modules/user-modules/module-pradar-core
mvn io.shulie.instrument.module:dependency-processor:1.1:DependencyJarCollect

echo "release finish ..."
echo "release home package: ${HOME}/deploy"
