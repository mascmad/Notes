# Notes
A Java application for note-taking on the Linux command line.


This can be ran with no arguments, and thus will run interactively.
Or, it can be ran with one of four arguments, which will run then quit:
  * -a    Add a note
  * -l    List all notes
  * -d    Delete a note
  * -p    Purge all notes

Any other argument will cause the program to return with an error message.

The file the notes are stored in (notes.txt) is stored in plaintext in the ~/Documents folder.
