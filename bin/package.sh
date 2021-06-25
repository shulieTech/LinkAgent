#!/bin/bash

HOME=$(dirname $(pwd))

cd ${HOME}/instrument-simulator/bin/
./simulator-packages.sh

cd ${HOME}/instrument-modules/bin/
./packages.sh


cd ${HOME}t/simulator-agent/bin/
sh agent-packages.sh


