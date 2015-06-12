#!/usr/bin/env ruby
#
# ad_generator.rb: a simple script that takes two arguments:
#  1) the path to an AsciiDoc file to be converted to DocBook
#  2) the path where the converted DocBook should be written
#
# Most of the work is handled by AsciiDoctor, which this script
# depends on. A few minor touchups to the DocBook XML source
# to make our doc build toolchain happy also occur.

=begin
# Requirements:
brew cask install java
brew install ant
brew install source-highlight
gem install asciidoctor
=end

require 'optparse'

def emit(*message)
  return unless $options[:verbose] and !message.empty?

  p message
end

# handle options
$options = {}
OptionParser.new do |opts|
  opts.banner = "Usage: ad_generator.rb [options]"

  opts.on("-a", "--asciidoc PATH", "path to AsciiDoc file to convert") do |a|
    $options[:ad] = a
  end

  opts.on("-p", "--p4build PATH", "path to doc build infrastructure") do |p|
    $options[:p4] = p
  end

  opts.on("-t", "--type TYPE", "specify asset type") do |t|
    $options[:type] = t
  end

  opts.on("-v", "--verbose", "enable verbose output") do |v|
    $options[:verbose] = v
  end

  opts.on("-x", "--xml PATH", "path to DocBook XML to write") do |x|
    $options[:xml] = x
  end

  opts.on("-h", "--help", "This help") do
    puts opts
    exit
  end
end.parse!

# startup checks
abort("Bad AsciiDoc path #{$options[:ad]}") unless File.file?($options[:ad])

emit "Starting DocBook generation..."
# read the specified AsciiDoc file to learn its title/subtitle
title = ""
subtitle = ""
File.open($options[:ad]).each_line do |line|
  line.chomp!
  break if line.empty?
  title = (line[/^=\s*(.+)$/, 1] || "") if title.empty?
  subtitle = (line[/^:subtitle:\s*(.+)$/, 1] || "") if subtitle.empty?
end

abort "'#{$options[:ad]}' has no title!" if title.empty?
abort "'#{$options[:ad]}' has no subtitle!" if subtitle.empty?

# convert AsciiDoc to DocBook5 with AsciiDoctor
# note: common_adoc here is an attribute for AsciiDoc includes
arguments = [
  "-b docbook5",
  "-a common_adoc=#{$options[:p4]}/perforce/common_adoc",
  "-a assettype=#{$options[:type]}",
  "-o #{$options[:xml]}",
  $options[:ad],
  "2>&1"
]
arguments.unshift "-v" if $options[:verbose]
arguments = arguments.join(" ")
emit "asciidoctor #{arguments}"

results = `asciidoctor #{arguments}`
abort "AsciiDoctor invocation failed with:\n#{results}" unless results.empty?

# post-conversion processing
emit "post-processing..."
db = File.read($options[:xml])

# apply tagged directory replacements, to handle commonly included files
# note: common-adoc here is for the docinfo file, which is XML.
db = db.gsub("@common-xml@", "#{$options[:p4]}/perforce/common_xml")
db = db.gsub("@common-adoc@", "#{$options[:p4]}/perforce/common_adoc")
db = db.gsub("@guides-dir@", "#{$options[:p4]}/..")

# fixup line spacing for preformatted blocks
db = db.gsub(/<screen([^>]*)>/, "<screen\\1>\n")
db = db.gsub(/<programlisting([^>]*)>/, "<programlisting\\1>\n")

# place the title/subtitle into the correct XML dom location.
db = db.gsub("<title>#{title}</title>", "")
#db = db.gsub("<subtitle>#{subtitle}</subtitle>", "")

# if a dbhtml processing instruction exists, the document is likely trying to
# assert which content appears in index.html. That means that the "book" content
# needs to move out of the way, into copyright.html.
dbhtml = (db =~ /<\?dbhtml\s+filename="[^"]+"\?>/) ? '<?dbhtml filename="copyright.html"?>' : ""
db = db.gsub(/\<book (.+?)\>/, "<book \\1>#{dbhtml}<title>#{title}</title><subtitle>#{subtitle}</subtitle>")

# output post-processed file
emit "writing file..."
File.write($options[:xml], db)

emit "DocBook generation complete."

# vim: set ts=2 sw=2 tw=80 ai si:
