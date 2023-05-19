#!/bin/bash

HOME=$(dirname $(pwd))

echo "start package..."
sh ${HOME}/bin/package.sh
echo "end package..."

echo "start deploy ..."
sh ${HOME}/bin/deploy.sh
echo "end deploy ..."

java -cp ${HOME}/instrument-modules/user-modules/module-pradar-core/target/module-pradar-core-2.0.2.0.jar com.shulie.instrument.module.pradar.core.dependency.PradarCoreModuleDependenciesCollector

echo "release finish ..."
echo "release home package: ${HOME}/deploy"
