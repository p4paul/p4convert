#!/bin/bash

DIR="$(readlink -f 'symlink-target')"

rm -rf "${DIR}" "${DIR}-co"

svnadmin create "${DIR}"

svn co "file://${DIR}" "${DIR}-co"

cd "${DIR}-co"


mkdir foo
touch foo/file1
touch foo/file2

svn add foo

svn commit -m "Create foo"

ln -s foo bar

svn add bar

echo "Contents" > foo/file1

svn commit -m "Create bar as symlink to foo, then edit file in foo"

svnadmin dump "${DIR}" > 'symlink-target.dump'
