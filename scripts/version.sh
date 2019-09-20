#!/bin/bash

version=$1
major=0
minor=0
patch=0

# break down the version number into it's components
regex="([0-9]+).([0-9]+).([0-9]+)"
if [[ $version =~ $regex ]]; then
    major="${BASH_REMATCH[1]}"
    minor="${BASH_REMATCH[2]}"
    patch="${BASH_REMATCH[3]}"
fi

# check paramater to see which number to increment
if [[ "$2" == "patch" ]]; then
    patch=$(echo $patch + 1 | bc)
elif [[ "$2" == "minor" ]]; then
    minor=$(echo $minor + 1 | bc)
    patch=0
elif [[ "$2" == "major" ]]; then
    major=$(echo $major + 1 | bc)
    minor=0
    patch=0
else
    patch=$(echo $patch + 1 | bc)
fi

# echo the new version number
echo "${major}.${minor}.${patch}"
