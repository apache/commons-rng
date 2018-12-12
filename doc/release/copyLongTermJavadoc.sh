#!/bin/bash

set -e

# List of all modules paths for which the long-term Javadoc links must be copied
# We keep only the official distribution (i.e. _not_ "commons-rng-examples").
MODULES=(commons-rng-client-api \
             commons-rng-core \
             commons-rng-sampling \
             commons-rng-simple)

while getopts r:v: option
do
    case "${option}"
    in
        r) REVISION=${OPTARG};;
        v) VERSION=${OPTARG};;
    esac
done

if [ "$REVISION" == "" ]; then
    echo "Missing SVN revision: Specify '-r <svn commit id>'";
    exit 1;
fi

if [ "$VERSION" == "" ]; then
    echo "Missing component version: Specify '-v <component version id>'";
    exit 1;
fi

for mod in ${MODULES[@]}; do
    echo $mod
    CPLIST+=" cp $REVISION $mod/apidocs $mod/javadocs/api-$VERSION"
done

echo -n "Copying long-term links ... "
svnmucc -U https://svn.apache.org/repos/infra/websites/production/commons/content/proper/commons-rng \
        $CPLIST \
        -m "Commons RNG: Copying $RNG_RELEASE_VERSION apidocs to versioned directories for the long-term links."
echo "Done."
