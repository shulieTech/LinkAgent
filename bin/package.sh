#!/bin/bash

HOME=$(dirname $(pwd))

cd ${HOME}/instrument-simulator/bin/
sh simulator-packages.sh

cd ${HOME}/instrument-modules/bin/
sh packages.sh


cd ${HOME}/simulator-agent/bin/
sh agent-packages.sh
