#!/bin/bash

HOME=$(dirname $(pwd))


INSTRUMENT_MODULES_TARGET=${HOME}/instrument-modules/target
INSTRUMENT_SIMULATOR_TARGET=${HOME}/instrument-simulator/target
SIMULATOR_TARGET=${HOME}/simulator-agent/target/simulator-agent

cp -r $INSTRUMENT_MODULES_TARGET/bootstrap/* $INSTRUMENT_SIMULATOR_TARGET/simulator/bootstrap/
cp -r $INSTRUMENT_MODULES_TARGET/modules/* $INSTRUMENT_SIMULATOR_TARGET/simulator/modules/
cp -r $INSTRUMENT_MODULES_TARGET/biz-classloader-jars/* $INSTRUMENT_SIMULATOR_TARGET/simulator/biz-classloader-jars/

rm -rf $SIMULATOR_TARGET/agent
mkdir $SIMULATOR_TARGET/agent
cp -r $INSTRUMENT_SIMULATOR_TARGET/simulator $SIMULATOR_TARGET/agent/

mkdir ${HOME}/deploy
rm -rf $HOME/deploy/simulator-agent

cp -r $SIMULATOR_TARGET ${HOME}/deploy/

cd $INSTRUMENT_SIMULATOR_TARGET/

cp ${HOME}/deploy/license.lic simulator/

zip -r simulator.zip simulator
tar -zcvf simulator.tar.gz simulator

cd $HOME

mv $INSTRUMENT_SIMULATOR_TARGET/simulator.zip ${HOME}/deploy/
mv $INSTRUMENT_SIMULATOR_TARGET/simulator.tar.gz  ${HOME}/deploy/


tar -zcvf deploy/simulator-agent.tar.gz ${HOME}/deploy/simulator-agent



