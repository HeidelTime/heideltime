#!/bin/bash

if [ "$HEIDELTIME_HOME" = "" ] ; then
	echo "please set \$HEIDELTIME_HOME"
	exit -1
fi

PWD=$(pwd)
cd $HEIDELTIME_HOME/resources

echo "Writing used_resources.txt"
find ./ -name "*.txt"  > used_resources.txt

echo "Copying resources..."
cp -r $HEIDELTIME_HOME/resources/* $HEIDELTIME_HOME/class/ 

echo "done."
cd $PWD
