#!/bin/bash

echo "Writing used_resources.txt"
find ./ -name "*.txt"  > used_resources.txt

echo "Copying resources..."
cp -r * ../class/ 

echo "done."
