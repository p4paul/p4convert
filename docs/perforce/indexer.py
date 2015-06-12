#!/usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import division
import sys, getopt, json, os.path
from pprint import pprint as pp

# import BeautifulSoup relative to where this script exists
include_path = os.path.dirname(os.path.abspath(os.path.dirname(sys.argv[0])))
sys.path.append(include_path + "/beautifulsoup4-4.3.2")
from bs4 import BeautifulSoup

def usage(level):
    print """
indexer.py [-h] -d <document file> -i <index file>
-h: this help
-d: the document file (HTML, with content in the '#content' id) to index
-i: the index file, in JSON format. Will be created if it does not exist.
"""
    sys.exit(level)


class IndexerException(Exception):
    def __init__(self, value):
        self.value = value
    def __str__(self):
        return repr(self.value)

# ========================================================================

filenames  = []
documents  = []
index_file = ''
index      = {}

try:
    options, args = getopt.getopt(sys.argv[1:], "hi:")
except getopt.GetoptError:
    usage(2)

for opt, arg in options:
    if opt == "-h":
        usage(0)
    elif opt == "-i":
        index_file = arg

for arg in args:
    filename = os.path.basename(arg.strip("/\\"))
    if filename in filenames:
        raise IndexerException(
            "{} already specified".format(filename)
        )
    filenames.append(filename)
    documents.append({ "name": filename, "path": arg })

if len(documents) < 1:
    print "Error: No document filename specified"
    usage(1)

if len(index_file) < 1:
    print "Error: No index filename specified"
    usage(1)

strip_chars = " .,:;(){}[]?|" + u"\u201c" + u"\u201d" + u"\xa9"

for d, doc in enumerate(documents):
    print("Indexing {}".format(doc["path"]))

    soup = BeautifulSoup(open(doc["path"]))
    doc["title"] = soup.title.string
    text = soup.find(id="content").get_text()
    if len(text) < 1:
        raise IndexerException("{} has no content".format(doc["path"]))

    for pos, w in enumerate(text.lower().split()):
        word = w.strip(strip_chars).replace('"', '').replace("'", "")
        if len(word) < 1:
            continue

        if word not in index:
            index[word] = {}

        if d not in index[word]:
            index[word][d] = []

        index[word][d].append(pos)

hash = {
    "f": documents,
    "i": index,
}
with open(index_file, "w") as zFile:
    zFile.write(json.dumps(hash))

# print("\nIndex:")
#print("final index:")
#pp(index)
# print("files:")
# pp(documents)
