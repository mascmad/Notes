import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.InputMismatchException;

public class Notes {

	private static final String usr = System.getenv("USER");

	private static final String notesFile = "notes.txt";
	private static final String notesPath = "/home/" + usr + "/.notes/";
	private static final String wholePath = notesPath + notesFile;
	private static final File f = new File(wholePath);
	private static final boolean dbg = false;
	private static boolean loaded = false;
	private static boolean firstRun = true;

	private static final Console c = System.console();

	private static ArrayList<Note> noteArray = new ArrayList<Note>();
	private static Scanner scan = new Scanner(System.in);

	private static final String sep = "---------------";

	public static void main(String args[]) {

		if (c == null) {
			System.err.println("ERROR: Console not found.");
			return;
		} else {
			if (dbg)
				c.printf("[i] Console found, continuing.\n");
		}

		if (args.length == 1) {
			if (args[0].equalsIgnoreCase("-l")) {			// list
				listNotes();
			} else if (args[0].equalsIgnoreCase("-a")) {	// add
				addNote();
			} else if (args[0].equalsIgnoreCase("-d")) {	// delete
				deleteNote();
			} else if (args[0].equalsIgnoreCase("-p")) {	// purge
				purgeNotes(false);
			} else if (args[0].equalsIgnoreCase("-h")) {
				listHelp(false);
			} else {
				c.printf("Invalid answer.\n");
				listHelp(true);
			}
			return;
		}

		if (!(usableFile(f, dbg))) {
			c.printf("[!!] Error in main()");
		} else {
			loadNotesFile();
			firstRun = false;

			/* Begin listing the menu:
			 * 1.	List all notes
			 * 2.	Add a note
			 * 3.	Delete a note
			 * 4.	Purge notes
			 * 0.	Exit
			 */
			while (true) {
				c.printf("%s\n", sep);
				int choice = listMenu();
				switch (choice) {
				case 1:
					listNotes();
					break;
				case 2:
					addNote();
					break;
				case 3:
					deleteNote();
					break;
				case 4:
					purgeNotes(false);
					break;
				case 0:
					cleanup();
					c.printf("Goodbye!\n");
					return;
				default:
					c.printf("Invalid answer.\n");
					break;
				}
			}
		}
	}

	public static void purgeNotes(boolean quiet) {
		String answer;
		if (!quiet) {
			if (noteArray.size() == 0) {
				c.printf("[*] There are no notes to purge. ");
				answer = c.readLine("Delete anyway? (y/N)\n> ");
			} else {
				answer = c.readLine("Are you sure you want to purge your notes? (y/N)\n(This cannot be undone)\n> ");
			}
		} else {
			answer = "yes";
		}
		if (answer.equals("")) {
			space();
			return;
		} else if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("ye") || answer.equalsIgnoreCase("yes")) {
			space();
			if (!(usableFile(f, dbg)) && !quiet) {
				c.printf("[!!] Error in purgeNotes()\n");
			} else {
				loadNotesFile();
				noteArray.clear();
				try {
					String removeCommand = "rm " + wholePath;
					if (!quiet)
						removeCommand += " && rm ." + wholePath + ".bak";
					Process rm = Runtime.getRuntime().exec(removeCommand);
					loaded = false;
					if (!(usableFile(f, (quiet ? false : dbg))))
						loadNotesFile();
					return;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else if (answer.equalsIgnoreCase("n") || answer.equalsIgnoreCase("no")) {
			space();
			return;
		} else {
			c.printf("Invalid answer, not deleting.\n");
			space();
			return;
		}
	}

	public static void deleteNote() {
		if (noteArray.size() == 0) {
			c.printf("[*] There are no notes to delete.\n");
			return;
		}
		if (!(usableFile(f, dbg))) {
			c.printf("[!!] Error in deleteNote()\n");
		} else {
			loadNotesFile();
			for (Note n: noteArray) {
				c.printf("%d -- %s:%s\n\n", noteArray.indexOf(n) + 1, n.getTitle(), n.getContent());
			}
			try {
				int choice = Integer.valueOf(c.readLine("Which to delete?\n> ")) - 1;
				Note deletion = noteArray.get(choice);
				String lookup = deletion.getTitle() + ":" + deletion.getContent();// + "\n";
				c.printf("\nRemove note:\n%s:%s\nat index: %d\n", 
						deletion.getTitle(), deletion.getContent(), noteArray.indexOf(deletion));
				space();
				// begin looking for that string in notes.txt
				BufferedReader inStream = null;
				try {
					inStream = new BufferedReader(new FileReader(f));
					String str;
					while ((str = inStream.readLine()) != null) {
						for (String lineSep: str.split("\n")) {
							if (lineSep.equals(lookup)) {
								noteArray.remove(deletion);
							}
						}
					}
					if (noteArray.size() == 0) {
						purgeNotes(true);
					}
					Process cp = Runtime.getRuntime().exec("cp " + wholePath 
							+ " ." + wholePath + ".bak"); // create the backup
					int i = 0;
					for (Note n: noteArray) {
						if (i == 0) {
							 // the first iteration should not append the file, thus wiping it
							printToFile(f, false, n);
						} else {
							 // the subsequent iterations SHOULD append, adding data to the file
							printToFile(f, true, n);
						}
						i++;
					}
				} catch (IOException e) {
					e.printStackTrace();
					return;
				} finally {
					if (inStream != null)
						inStream.close();
				}
			} catch (Exception e) {
				c.printf("[!!] Invalid answer.\n");
				return;
			}
		}
	}

	public static void loadNotesFile() {
		if (!(loaded)) {
			for (Note n :readFile(f)) {
				noteArray.add(n);
			}
			loaded = true;
		}
	}

	public static void listNotes() {
		if (!(usableFile(f, dbg))) {
			c.printf("[!!] Error in listNotes()\n");
		} else {
			loadNotesFile();
			if (noteArray.size() == 0) {
				c.printf("[*] There are no notes to show.\n");
				return;
			}
			for (Note n: noteArray) {
				c.printf("%d -- %s\n\n", noteArray.indexOf(n) + 1, n.toString());
			}
		}
	}

	public static void addNote() {
		if (!(usableFile(f, dbg))) {
			c.printf("[!!] Error in addNote()\n");
		} else {
			loadNotesFile();
			String inContent = c.readLine("Enter the new note:\n");
			if (inContent.equals("")) {
				c.printf("No content given, no note created.\n");
				return;
			}
			space();
			String inTitle = c.readLine("Please enter the title of the note: ");
			Note addition = new Note(inTitle, inContent);
			printToFile(f, true, addition);
			noteArray.add(addition);
			space();
		}
	}
	
	public static void listHelp(boolean usage) {
		String msg = (usage ? "Usage" : "Help");
		space();
		c.printf(msg + ":\n\n"
				+ "  -a\tAdd a note\n"
				+ "  -l\tList notes\n"
				+ "  -d\tDelete a note\n"
				+ "  -p\tPurge notes\n"
				+ "  -h\tShow this help menu\n"
				+ "\n"
				+ "All of these options can be used either capitalized or lowercase.\n\n");
	}

	public static int listMenu() {
		int answer;
		c.printf("Menu:\n"
				+ "1.\tList all notes (-l)\n"
				+ "2.\tAdd a note (-a)\n"
				+ "3.\tDelete a note (-d)\n"
				+ "4.\tPurge notes (-p)\n"
				+ "0.\tExit\n");
		try {
			c.printf("> ");
			answer = scan.nextInt();
			space();
			return answer;
		} catch (Exception e) {
			c.printf("\n[!!] Invalid response.\n");
			return listMenu();
		}
	}

	public static void printToFile(File out, boolean append, Note input) {
		if (!(usableFile(out, dbg))) {
			c.printf("[!!] Error in printToFile()\n");
			return;
		}

		FileWriter outStream = null;
		try {
			outStream = new FileWriter(out, append);

			outStream.write(input.getTitle() + ":" + input.getContent() + "\n");
		} catch (IOException e) { 
			e.printStackTrace();
		} finally {
			if (outStream != null) {
				try {
					outStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static boolean usableFile(File in, boolean debug) { 
		if (Files.notExists(in.toPath(), LinkOption.NOFOLLOW_LINKS)) {
			try {
				if (debug || firstRun)
					c.printf("[i] Created path to file.\n");
				Process mkdir = Runtime.getRuntime().exec("mkdir -p " + notesPath);
			} catch (IOException e) {
				c.printf("[!!] Error in creating path.\n");
				return false;
			}
		}
		
		if (!(in.exists())) {
			if (debug)
				c.printf("[!!] File does not exist.\n");
			try {
				if (in.createNewFile()) {
					if (debug || firstRun)
						c.printf("[i] File created.\n");
				} else {
					c.printf("[!!] Error in creating file.\n");
					return false;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		if (!(in.canWrite()) || !(in.canRead())) {
			c.printf("[!!] File permissions not set.\n");
			try {
				if (in.setReadable(true, true)) {
					if (debug || firstRun)
						c.printf("[i] Set read permissions...\n");
				} else {
					c.printf("[!!] Could not set read permissions.\n");
				}

				if (in.setWritable(true, true)) {
					if (debug || firstRun)
						c.printf("[i] Set write permissions...\n");
				} else {
					c.printf("[!!] Could not set write permissions.\n");
				}
			} catch (Exception e) {
				return false;
			}
		}
		return true;
	}

	public static ArrayList<Note> readFile(File in) {
		ArrayList<Note> out = new ArrayList<Note>();
		BufferedReader inStream = null;
		if (!(usableFile(in, dbg))) {
			c.printf("[!!] File error in readFile()\n");
			return null;
		}
		try {
			inStream = new BufferedReader(new FileReader(in));

			String str;
			while ((str = inStream.readLine()) != null) {
				for (String splitNewline: str.split("\n")) { // separates by line
					String splitContent = splitNewline.substring(splitNewline.indexOf(":") + 1);
					String splitTitle = splitNewline.substring(0, splitNewline.indexOf(":"));
					out.add(new Note(splitTitle, splitContent));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return out;
	}

	public static void cleanup() {
		scan.close();
		c.flush();
	}

	private static void space() {
		c.printf("%s", "\n");
	}

	public static class Note {
		private String name, description;

		public Note(String title, String content) {
			if (title.equals(""))
				this.setTitle("(untitled)");
			else
				this.name = title;
			this.description = content;
		}

		public String getTitle() {
			return this.name;
		}

		public void setTitle(String title) {
			this.name = title;
		}

		public String getContent() {
			return this.description;
		}

		@Override
		public String toString() {
			return this.getTitle() + ":" + this.getContent();
		}
	}
}
