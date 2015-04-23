

# Scheduled for v1.0 #

  * GUI to choose elements to JNAerate and customize mappings (with batchable actions, filter-as-you-type...)
    * -choose creates a .jnaeratorChoices file
    * -choices test.jnaeratorChoices uses it

# Unscheduled #

  * fix file association of .jnaerator files on Mac OS X (register a listener to web start services so that the argument file is read... on Windows, it is passed with -open file as arguments, so it already works).
  * Support URLs in arguments, with automatic local caching, support of wildcards (list files at arbitrary http URL), integration with preprocessor...
  * More elaborate variable replacements in .jnaerator files : default values (w/ or w/o prompt to user), implicit download, script expressions...)
  * allow predefined source content to be prepared for each output class - raw text or velocity macro, post trigger w/ script api ?
  * expose all CLI jnaeration options through the GUI
  * define -visibility switch to control choice of exported functions : declspec(dllexport), not attribute((visibility=hiddden)), always... + warnings when no symbol is exported : "are you sure you didn't miss a '-visibility all' switch ?"
  * enhance the error reporting of parsing (work on antlr grammar) and of compilation