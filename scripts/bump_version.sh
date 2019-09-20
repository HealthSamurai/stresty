#!/bin/bash

current_version=`cat VERSION`
label=$1

if [[ -z $label ]]
then
    label=patch
fi

new_version=`./scripts/version.sh $current_version $1`

echo $new_version > VERSION
echo "Bumped new version $current_version -> $new_version"
echo "Don't forget to commit & push your changes. ;)"
