#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Stop server
p4 -H localhost -p 5956 admin stop

sleep 3

# Make empty server root and workspace
rm -rf "${DIR}/p4root4" "${DIR}/workspace-st"
mkdir -p "${DIR}/p4root4" "${DIR}/workspace-st"

# Start server
"${DIR}/p4d.2010.2.312470" -r "${DIR}/p4root4" -p 127.0.0.1:5956 -d

# Create config file from template
sed -e "s!%DIR%!${DIR}!" "${DIR}/symlink-target.tmpl.cfg" > "${DIR}/symlink-target.cfg"

# Run conversion
java -Dlog4j.configuration="file:${DIR}/log4j.xml" -jar "${DIR}/p4convert-svn.jar" "${DIR}/symlink-target.cfg"

echo "Results are in ${DIR}"
