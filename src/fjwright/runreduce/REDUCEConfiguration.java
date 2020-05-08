package fjwright.runreduce;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RRPreferences {
    static final boolean windowsOS = System.getProperty("os.name").startsWith("Windows");
    static final Preferences prefs = Preferences.userRoot().node("/fjwright/runreduce");  // cf. package name
    // On Microsoft Windows the preferences for this application are stored in the registry under the key
    // Computer\HKEY_CURRENT_USER\Software\JavaSoft\Prefs\fjwright\runreduce
    // and on Ubuntu Linux they are stored in the XML file
    // ~/.java/.userPrefs/fjwright/runreduce/prefs.xml.

    // Preference keys:
    static final String FONTSIZE = "fontSize";
    static final String AUTORUNVERSION = "autoRunVersion";
    static final String BOLDPROMPTS = "boldPrompts";
    static final String COLOUREDIO = "colouredIO";
    static final String DISPLAYPANE = "displayPane";

    enum ColouredIO {NONE, MODAL, REDFRONT}

    enum DisplayPane {SINGLE, SPLIT, TABBED}

    static final String NONE = "None";

    static int fontSize = Math.max(prefs.getInt(FONTSIZE, 12), 5);
    // in case a very small font size gets saved accidentally!
    // Minimum value of 5 matches minimum value set for font size SpinnerModel.
    static String autoRunVersion = prefs.get(AUTORUNVERSION, NONE);
    static boolean boldPromptsState = prefs.getBoolean(BOLDPROMPTS, false);
    static ColouredIO colouredIOIntent =
            ColouredIO.valueOf(prefs.get(COLOUREDIO, ColouredIO.NONE.toString()));
    static DisplayPane displayPane =
            DisplayPane.valueOf(prefs.get(DISPLAYPANE, DisplayPane.SPLIT.toString())); // temporary!
    static ColouredIO colouredIOState = colouredIOIntent;

    static void save(String key, Object... values) {
        switch (key) {
            case AUTORUNVERSION:
                prefs.put(AUTORUNVERSION, autoRunVersion = (String) values[0]);
                break;
            case FONTSIZE:
                prefs.putInt(FONTSIZE, fontSize = (int) values[0]);
                break;
            case BOLDPROMPTS:
                prefs.putBoolean(BOLDPROMPTS, boldPromptsState);
                break;
            case COLOUREDIO:
                prefs.put(COLOUREDIO, (colouredIOIntent = (ColouredIO) values[0]).toString());
                // Update colouredIOState immediately unless switching to or from REDFRONT:
                if (colouredIOIntent != ColouredIO.REDFRONT && colouredIOState != ColouredIO.REDFRONT)
                    colouredIOState = colouredIOIntent;
                break;
            case DISPLAYPANE:
                prefs.put(DISPLAYPANE, (displayPane = (DisplayPane) values[0]).toString());
                break;
            default:
                System.err.println("Attempt to save unexpected preference key: " + key);
        }
    }
}

/**
 * This class defines a command to run REDUCE after checking it is executable.
 */
class REDUCECommand {
    String version = ""; // e.g. "CSL REDUCE" or "PSL REDUCE"
    String versionRootDir = ""; // version-specific reduceRootDir.
    String[] command = {"", "", "", "", "", ""}; // executable pathname followed by arguments

    REDUCECommand() {
    }

    REDUCECommand(String version, String versionRootDir, String... command) {
        this.version = version;
        this.versionRootDir = versionRootDir;
        this.command = command;
    }

    // Merge this method into the constructor?
    String[] buildCommand() {
        // Replace $REDUCE by versionRootDir if non-null else by reduceRootDir.
        Path reduceRootPath = Paths.get(
                !versionRootDir.equals("") ? versionRootDir : RunREDUCE.reduceConfiguration.reduceRootDir);
        String[] command = new String[this.command.length];
        for (int i = 0; i < this.command.length; i++) {
            String element = this.command[i];
            if (element.startsWith("$REDUCE/"))
                element = reduceRootPath.resolve(element.substring(8)).toString();
            command[i] = element;
        }
        if (!Files.isExecutable(Paths.get(command[0]))) {
            RunREDUCE.errorMessageDialog(
                    command[0] + " is not executable!",
                    "REDUCE Configuration Error");
            return null;
        }
        return command;
    }
}

/**
 * This class defines a list of commands to run different versions of REDUCE.
 */
class REDUCECommandList extends ArrayList<REDUCECommand> {
    REDUCECommandList copy() {
        REDUCECommandList reduceCommandList = new REDUCECommandList();
        for (REDUCECommand cmd : this) // Build a deep copy of cmd
            reduceCommandList.add(new REDUCECommand(cmd.version, cmd.versionRootDir, cmd.command));
        return reduceCommandList;
    }
}

/**
 * This class defines a template for REDUCEConfigurationDefaults and REDUCEConfiguration.
 */
abstract class REDUCEConfigurationType {
    String reduceRootDir;
    String packagesRootDir;
    REDUCECommandList reduceCommandList;
}

/**
 * This class represents the application default REDUCE directory and command configuration.
 * It is initialised when the application starts.
 * **Note that no value can be null because preference values cannot be null.**
 */
class REDUCEConfigurationDefault extends REDUCEConfigurationType {
    static final String CSL_REDUCE = "CSL REDUCE";
    static final String PSL_REDUCE = "PSL REDUCE";

    REDUCEConfigurationDefault() {
        if (RunREDUCE.debugPlatform) System.err.println("OS name: " + System.getProperty("os.name"));

        reduceCommandList = new REDUCECommandList();
        reduceRootDir = System.getenv("REDUCE");
        // $REDUCE below will be replaced by versionRootDir if set or reduceRootDir otherwise
        // before attempting to run REDUCE.
        if (RRPreferences.windowsOS) {
            // On Windows, all REDUCE directories should be found automatically in "/Program Files/Reduce".
            if (reduceRootDir == null) reduceRootDir = findREDUCERootDir();
            if (reduceRootDir == null) reduceRootDir = "";
            packagesRootDir = reduceRootDir;
            reduceCommandList.add(new REDUCECommand(CSL_REDUCE,
                    "",
                    "$REDUCE/lib/csl/reduce.exe",
                    "--nogui"));
            reduceCommandList.add(new REDUCECommand(PSL_REDUCE,
                    "",
                    "$REDUCE/lib/psl/psl/bpsl.exe",
                    "-td", "1000", "-f",
                    "$REDUCE/lib/psl/red/reduce.img"));
        } else {
            // This is appropriate for Ubuntu:
            reduceRootDir = "/usr/lib/reduce";
            packagesRootDir = "/usr/share/reduce";
            reduceCommandList.add(new REDUCECommand(CSL_REDUCE,
                    "",
                    "$REDUCE/cslbuild/csl/reduce",
                    "--nogui"));
            reduceCommandList.add(new REDUCECommand(PSL_REDUCE,
                    "",
                    "$REDUCE/pslbuild/psl/bpsl",
                    "-td", "1000", "-f",
                    "$REDUCE/pslbuild/red/reduce.img"));
        }
    }

    /**
     * This method attempts to locate the REDUCE installation directory on Windows (only).
     */
    private static String findREDUCERootDir() {
        Path targetPath = Paths.get("Program Files", "Reduce");
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            Path reduceRootPath = root.resolve(targetPath);
            if (Files.exists(reduceRootPath)) return reduceRootPath.toString();
        }
        return null;
    }
}

/**
 * This class represents the current REDUCE directory and command configuration.
 * It is initialised when the application starts and can be updated and saved using the REDUCEConfigDialog class.
 */
class REDUCEConfiguration extends REDUCEConfigurationType {
    // Preference keys:
    static final String REDUCE_ROOT_DIR = "reduceRootDir";
    static final String PACKAGES_ROOT_DIR = "packagesRootDir";
    static final String REDUCE_VERSIONS = "reduceVersions";
    static final String COMMAND_LENGTH = "commandLength";
    static final String COMMAND = "command";
    static final String ARG = "arg";

    /**
     * This method initialises the reduceRootDir, packagesRootDir and runREDUCECommands fields from saved preferences
     * or application defaults.
     */
    REDUCEConfiguration() {
        reduceCommandList = new REDUCECommandList();
        Preferences prefs = RRPreferences.prefs;
        reduceRootDir = prefs.get(REDUCE_ROOT_DIR, RunREDUCE.reduceConfigurationDefault.reduceRootDir);
        packagesRootDir = prefs.get(PACKAGES_ROOT_DIR, RunREDUCE.reduceConfigurationDefault.packagesRootDir);
        if (packagesRootDir.isEmpty()) packagesRootDir = RunREDUCE.reduceConfigurationDefault.packagesRootDir;

        try {
            if (prefs.nodeExists(REDUCE_VERSIONS)) {
                prefs = prefs.node(REDUCE_VERSIONS);
                for (String version : prefs.childrenNames()) {
                    // Get defaults:
                    REDUCECommand cmdDefault = null;
                    for (REDUCECommand cmd : RunREDUCE.reduceConfigurationDefault.reduceCommandList)
                        if (version.equals(cmd.version)) {
                            cmdDefault = cmd;
                            break;
                        }
                    if (cmdDefault == null) cmdDefault = new REDUCECommand(); // all fields ""
                    prefs = prefs.node(version);
                    String versionRootDir = prefs.get(REDUCE_ROOT_DIR, cmdDefault.versionRootDir);
                    int commandLength = prefs.getInt(COMMAND_LENGTH, cmdDefault.command.length);
                    String[] command;
                    if (commandLength == 0) {
                        command = new String[]{""};
                    } else {
                        command = new String[commandLength];
                        command[0] = prefs.get(COMMAND, cmdDefault.command[0]);
                        for (int i = 1; i < commandLength; i++) {
                            command[i] = prefs.get(ARG + i,
                                    i < cmdDefault.command.length ? cmdDefault.command[i] : "");
                        }
                    }
                    reduceCommandList.add(new REDUCECommand(version, versionRootDir, command));
                    prefs = prefs.parent();
                }
            } else
                reduceCommandList = RunREDUCE.reduceConfigurationDefault.reduceCommandList.copy();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method saves the reduceRootDir, packagesRootDir and runREDUCECommands fields as preferences.
     */
    void save() {
        Preferences prefs = RRPreferences.prefs;
        prefs.put(REDUCE_ROOT_DIR, reduceRootDir);
        prefs.put(PACKAGES_ROOT_DIR, packagesRootDir);
        // Remove all saved versions before saving current versions:
        try {
            prefs.node(REDUCE_VERSIONS).removeNode();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
        prefs = prefs.node(REDUCE_VERSIONS);
        for (REDUCECommand cmd : reduceCommandList) {
            prefs = prefs.node(cmd.version);
            prefs.put(REDUCE_ROOT_DIR, cmd.versionRootDir);
            int commandLength = cmd.command.length;
            prefs.putInt(COMMAND_LENGTH, commandLength);
            prefs.put(COMMAND, commandLength > 0 ? cmd.command[0] : "");
            for (int i = 1; i < cmd.command.length; i++)
                prefs.put(ARG + i, cmd.command[i]);
            prefs = prefs.parent();
        }
    }
}

/**
 * This class provides a list of all REDUCE packages by parsing the package.map file.
 * The list excludes preloaded packages, and it is sorted alphabetically.
 */
class REDUCEPackageList extends ArrayList<String> {

    REDUCEPackageList() {
        Path packagesRootPath = Paths.get(RunREDUCE.reduceConfiguration.packagesRootDir);
        Path packageMapFile = packagesRootPath.resolve("packages/package.map");
        if (!Files.isReadable(packageMapFile)) {
            RunREDUCE.errorMessageDialog(
                    "The REDUCE package map file is not available!" +
                            "\nPlease correct 'Packages Root Dir' in the 'Configure REDUCE...' dialogue," +
                            "\nwhich will open automatically when you close this dialogue.",
                    "REDUCE Package Error");
// FixMe           RRMenuBar.showREDUCEConfigDialog();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(packageMapFile)) {
            String line;
            Pattern pattern = Pattern.compile("\\s*\\((\\w+)");
            // The preloaded packages are these (using non-capturing groups):
            Pattern exclude = Pattern.compile("(?:alg)|(?:arith)|(?:mathpr)|(?:poly)|(?:rlisp)");
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.lookingAt()) {
                    String pkg = matcher.group(1);
                    if (!exclude.matcher(pkg).matches()) this.add(pkg);
                }
            }
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }

        Collections.sort(this);

        // For testing only:
//        for (String s : this) {
//            System.out.print(s);
//            System.out.print(" ");
//        }
    }
}
