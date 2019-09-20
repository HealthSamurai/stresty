#!/bin/bash

# bump version

EXPECTED_GIT_STATUS_MESSAGE="On branch master
Your branch is up-to-date with 'origin/master'.
nothing to commit, working tree clean"

RELEASED_VERSION=`curl -w '%{url_effective}\n' -I -L -s -S https://github.com/Aidbox/stresty/releases/latest -o /dev/null | sed 's:.*/::'`
VERSION=`cat VERSION`

function version_gt() {
    test "$(printf '%s\n' "$@" | sort -V | head -n 1)" != "$1";
}

function validate_version() {
    # validate that version matches the pattern x.x.x

    if version_gt $RELEASED_VERSION $VERSION
    then
        echo "Error: New version must be greater than lastest released."
        echo "Latest released version is $RELEASED_VERSION"
        exit 1
    fi
}

if [[ $EXPECTED_GIT_STATUS_MESSAGE != `git status` ]]
then
    echo "Error: firstly commit & push your changes to master."
    exit 1
fi

validate_version

echo "Creating tag $VERSION"
git tag $VERSION || exit 1
echo "Pushing to orgin $VERSION"
git push origin $VERSION
