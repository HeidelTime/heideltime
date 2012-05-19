#!/bin/bash

echo "Copying resources..."
cp -r * ../class/ 

echo "Writing used_resources.txt"
find ./ -name "*.txt"  > ../class/used_resources.txt

echo "done."
