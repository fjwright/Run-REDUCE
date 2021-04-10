package fjwright.runreduce;

import javafx.scene.control.Alert;

import java.io.BufferedReader;
import java.io.File;
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

import static java.lang.System.getProperty;
import static java.lang.System.getenv;

class RRPreferences {
    static final Preferences prefs = Preferences.userRoot().node("/fjwright/runreduce");  // cf. package name
    // On Microsoft Windows the preferences for this application are stored in the registry under the key
    // Computer\HKEY_CURRENT_USER\Software\JavaSoft\Prefs\fjwright\runreduce
    // and on Ubuntu Linux they are stored in the XML file
    // ~/.java/.userPrefs/fjwright/runreduce/prefs.xml.

    // Preference keys
    // REDUCE menu:
    static final String AUTO_RUN_VERSION = "autoRunVersion";
    // View menu:
    static final String FONT_SIZE = "fontSize";
    static final String BOLD_PROMPTS = "boldPrompts";
    static final String COLOURED_IO = "colouredIO";
    static final String TYPESET_MATHS = "typesetMaths";
    static final String DISPLAY_PANE = "displayPane";
    // Options menu:
    static final String SHOW_MENU_HISTORY = "showMenuHistory";
    static final String POPUP_QUERIES = "popupQueries";

    enum DisplayPane {SINGLE, SPLIT, TABBED}

    static final String NONE = "None";

    static String autoRunVersion = prefs.get(AUTO_RUN_VERSION, NONE);
    static int fontSize = Math.max(prefs.getInt(FONT_SIZE, 15), 5);
    // in case a very small font size gets saved accidentally!
    // Minimum value of 5 matches minimum value set for font size SpinnerModel.
    static boolean boldPromptsState = prefs.getBoolean(BOLD_PROMPTS, false);
    static boolean colouredIOState = prefs.getBoolean(COLOURED_IO, false);
    static boolean typesetMathsState = prefs.getBoolean(TYPESET_MATHS, false);
    static DisplayPane displayPane =
            DisplayPane.valueOf(prefs.get(DISPLAY_PANE, DisplayPane.SINGLE.toString()));
    static boolean showMenuHistory = prefs.getBoolean(SHOW_MENU_HISTORY, false);
    static boolean popupQueries = prefs.getBoolean(POPUP_QUERIES, true);

    static void save(String key, Object value) {
        switch (key) {
            case AUTO_RUN_VERSION:
                prefs.put(AUTO_RUN_VERSION, autoRunVersion = (String) value);
                break;
            case FONT_SIZE:
                prefs.putInt(FONT_SIZE, fontSize = (int) value);
                break;
            case BOLD_PROMPTS:
                prefs.putBoolean(BOLD_PROMPTS, boldPromptsState = (boolean) value);
                RunREDUCE.reducePanel.setBoldPrompts(boldPromptsState);
                break;
            case COLOURED_IO:
                prefs.putBoolean(COLOURED_IO, colouredIOState = (boolean) value);
                RunREDUCE.reducePanel.setColouredIO(colouredIOState);
                break;
            case TYPESET_MATHS:
                prefs.putBoolean(TYPESET_MATHS, typesetMathsState = (boolean) value);
                RunREDUCE.reducePanel.setTypesetMaths(typesetMathsState);
                break;
            case DISPLAY_PANE:
                prefs.put(DISPLAY_PANE, (displayPane = (DisplayPane) value).toString());
                break;
            case SHOW_MENU_HISTORY:
                prefs.putBoolean(SHOW_MENU_HISTORY, showMenuHistory = (boolean) value);
                break;
            case POPUP_QUERIES:
                prefs.putBoolean(POPUP_QUERIES, popupQueries = (boolean) value);
                break;
            default:
                System.err.println("Attempt to save unexpected preference key: " + key);
        }
    }
}

/**
 * This class defines the font colours used in the I/O Display pane.
 * They are represented as web colour strings.
 */
class FontColors {
    // Preference keys:
    private static final String FONT_COLORS = "fontColors";
    private static final String ALGEBRAIC_INPUT = "algebraicInput";
    private static final String SYMBOLIC_INPUT = "symbolicInput";
    private static final String ALGEBRAIC_OUTPUT = "algebraicOutput";
    private static final String SYMBOLIC_OUTPUT = "symbolicOutput";
    private static final String WARNING = "warning";
    private static final String ERROR = "error";
    // Default colour values:
    static final String algebraicInputDefault = "red";
    static final String symbolicInputDefault = "green";
    static final String algebraicOutputDefault = "blue";
    static final String symbolicOutputDefault = "brown";
    static final String warningDefault = "#ffa50040"; // orange, 1/4 opaque
    static final String errorDefault = "#ff000040";  // red, 1/4 opaque
    // Current colour values:
    static String algebraicInput;
    static String symbolicInput;
    static String algebraicOutput;
    static String symbolicOutput;
    static String warning;
    static String error;

    /*
     * Restore colours from preferences.
     */
    static {
        Preferences prefs = RRPreferences.prefs.node(FONT_COLORS);
        algebraicInput = prefs.get(ALGEBRAIC_INPUT, algebraicInputDefault);
        symbolicInput = prefs.get(SYMBOLIC_INPUT, symbolicInputDefault);
        algebraicOutput = prefs.get(ALGEBRAIC_OUTPUT, algebraicOutputDefault);
        symbolicOutput = prefs.get(SYMBOLIC_OUTPUT, symbolicOutputDefault);
        warning = prefs.get(WARNING, warningDefault);
        error = prefs.get(ERROR, errorDefault);
    }

    /**
     * Save colours as preferences.
     */
    static void save() {
        for (var reducePanel : RunREDUCE.reducePanelList) reducePanel.updateFontColours();
        Preferences prefs = RRPreferences.prefs.node(FONT_COLORS);
        prefs.put(ALGEBRAIC_INPUT, algebraicInput);
        prefs.put(SYMBOLIC_INPUT, symbolicInput);
        prefs.put(ALGEBRAIC_OUTPUT, algebraicOutput);
        prefs.put(SYMBOLIC_OUTPUT, symbolicOutput);
        prefs.put(WARNING, warning);
        prefs.put(ERROR, error);
    }
}

/**
 * This class defines a command to run REDUCE after checking it is executable.
 */
class REDUCECommand {
    String name = ""; // e.g. "CSL REDUCE" or "PSL REDUCE"
    boolean useShell = false;
    boolean checkCommand = true;
    String rootDir = ""; // command-specific reduceRootDir.
    String[] command = {"", "", "", "", "", ""}; // executable pathname followed by 5 arguments

    REDUCECommand() {
    }

    REDUCECommand(String name) {
        this.name = name;
    }

    REDUCECommand(String name, boolean useShell, boolean checkCommand, String rootDir, String... command) {
        this.name = name;
        this.useShell = useShell;
        this.checkCommand = checkCommand;
        this.rootDir = rootDir;
        this.command = command;
    }

    // Merge this method into the constructor?
    String[] buildCommand() {
        // Replace $REDUCE by local command rootDir if non-null else by global reduceRootDir.
        Path reduceRootPath = Paths.get(
                !rootDir.isEmpty() ? rootDir : RunREDUCE.reduceConfiguration.reduceRootDir);
        String[] command = new String[this.command.length];
        for (int i = 0; i < this.command.length; i++) {
            String element = this.command[i];
            if (element.startsWith("$REDUCE")) // replace $REDUCE/ or $REDUCE\
                element = reduceRootPath.resolve(element.substring(8)).toString();
            command[i] = element;
        }
        if (useShell) {
            if (REDUCEConfigurationType.windowsOS) {
                // Microsoft Windows
                StringBuilder reduceCommand = new StringBuilder();
                // Double-quote the command name/pathname element ONLY:
                reduceCommand.append("\"").append(command[0]).append("\"");
                for (int i = 1; i < command.length; i++) {
                    reduceCommand.append(" ").append(command[i]);
                }
                return new String[]{getenv("ComSpec"), "/c", reduceCommand.toString()};
            } else {
                // Linux, etc.
                return new String[]{getenv("SHELL"), "-c", String.join(" ", command)};
            }
        }
        if (!Files.isExecutable(Paths.get(command[0]))) {
            RunREDUCE.alert(Alert.AlertType.ERROR, "REDUCE Configuration",
                    "The command path name\n" + command[0] + "\ndoes not exist or is not executable.");
            return null;
        }
        return command;
    }
}

/**
 * This class defines a list of commands to run REDUCE.
 */
class REDUCECommandList extends ArrayList<REDUCECommand> {
    REDUCECommandList copy() {
        REDUCECommandList reduceCommandList = new REDUCECommandList();
        for (REDUCECommand cmd : this) // Build a deep copy of cmd
            reduceCommandList.add(new REDUCECommand(cmd.name, cmd.useShell, cmd.checkCommand, cmd.rootDir, cmd.command));
        return reduceCommandList;
    }
}

/**
 * This class defines a template for REDUCEConfigurationDefaults and REDUCEConfiguration.
 */
abstract class REDUCEConfigurationType {
    public static final boolean windowsOS = System.getProperty("os.name").startsWith("Windows");
    String reduceRootDir, packagesDir;
    public String manualDir, primersDir;
    String workingDir;
    REDUCECommandList reduceCommandList;
}

/**
 * This class represents the application default REDUCE directory and command configuration.
 * It is initialised when the application starts.
 * **Note that no value can be null because preference values cannot be null.**
 */
class REDUCEConfigurationDefault extends REDUCEConfigurationType {
    private static final String CSL_REDUCE = "CSL REDUCE";
    private static final String PSL_REDUCE = "PSL REDUCE";

    REDUCEConfigurationDefault() {
        if (RunREDUCE.debugPlatform) System.err.println("OS name: " + System.getProperty("os.name"));

        reduceCommandList = new REDUCECommandList();
        if (windowsOS) {
            // On Windows, all REDUCE directories should be found automatically in "/Program Files/Reduce".
            reduceRootDir = findREDUCERootDir();
            packagesDir = Path.of(reduceRootDir, "packages").toString();
            manualDir = Path.of(reduceRootDir, "lib\\csl\\reduce.doc").toString();
            primersDir = Path.of(reduceRootDir, "doc").toString();
            // $REDUCE below will be replaced by versionRootDir if set or reduceRootDir otherwise
            // before attempting to run REDUCE.
            reduceCommandList.add(new REDUCECommand(CSL_REDUCE,
                    true, // false,
                    true,
                    "",
                    "$REDUCE\\bin\\redcsl.bat", // "$REDUCE\\lib\\csl\\reduce.exe",
                    "--nogui"));
            reduceCommandList.add(new REDUCECommand(PSL_REDUCE,
                    true, // false,
                    true,
                    "",
                    "$REDUCE\\bin\\redpsl.bat")); // "$REDUCE\\lib\\psl\\psl\\bpsl.exe",
                    // "-td", "1000", "-f", "$REDUCE\\lib\\psl\\red\\reduce.img"));
        } else {
            // Linux, etc.: use shell and search path
            reduceRootDir = "/usr/bin"; // "/usr/lib/reduce";
            packagesDir = "/usr/share/reduce/packages";
            primersDir = manualDir = "/usr/share/doc/reduce";
            reduceCommandList.add(new REDUCECommand(CSL_REDUCE,
                    true, // false,
                    false,
                    "",
                    "redcsl", // "$REDUCE/cslbuild/csl/reduce",
                    "--nogui"));
            reduceCommandList.add(new REDUCECommand(PSL_REDUCE,
                    true, // false,
                    false,
                    "",
                    "redpsl")); // "$REDUCE/pslbuild/psl/bpsl",
                    // "-td", "1000", "-f", "$REDUCE/pslbuild/red/reduce.img"));
        }
        workingDir = getProperty("user.home");
    }

    /**
     * This method attempts to locate the REDUCE installation directory on Windows (only).
     * If not found then it returns an empty string.
     */
    private static String findREDUCERootDir() {
        Path targetPath = Paths.get("Program Files", "Reduce");
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            Path reduceRootPath = root.resolve(targetPath);
            if (Files.exists(reduceRootPath)) return reduceRootPath.toString();
        }
        return "";
    }
}

/**
 * This class represents the current REDUCE directory and command configuration.
 * It is initialised when the application starts and can be updated and saved using the REDUCEConfigDialog class.
 */
public class REDUCEConfiguration extends REDUCEConfigurationType {
    // Preference keys:
    static final String REDUCE_ROOT_DIR = "reduceRootDir";
    static final String PACKAGES_DIR = "packagesDir";
    static final String MANUAL_DIR = "manualDir";
    static final String PRIMERS_DIR = "primersDir";
    static final String WORKING_DIR = "workingDir";
    static final String REDUCE_VERSIONS = "reduceVersions"; // FixMe delete this obsolete temporary compatibility code
    static final String REDUCE_COMMANDS = "reduceCommands";
    static final String COMMAND_INDEX = "commandIndex";
    static final String USE_SHELL = "useShell";
    static final String CHECK_COMMAND = "checkCommand";
    static final String COMMAND_ROOT_DIR = "commandRootDir";
    static final String COMMAND_LENGTH = "commandLength";
    static final String COMMAND = "command";
    static final String ARG = "arg";

    /**
     * Initialise the *RootDir and REDUCECommandList fields from saved preferences
     * or platform defaults.
     */
    REDUCEConfiguration() {
        Preferences prefs = RRPreferences.prefs;
        reduceRootDir = prefs.get(REDUCE_ROOT_DIR, RunREDUCE.reduceConfigurationDefault.reduceRootDir);
        packagesDir = prefs.get(PACKAGES_DIR, RunREDUCE.reduceConfigurationDefault.packagesDir);
        manualDir = prefs.get(MANUAL_DIR, RunREDUCE.reduceConfigurationDefault.manualDir);
        primersDir = prefs.get(PRIMERS_DIR, RunREDUCE.reduceConfigurationDefault.primersDir);
        workingDir = prefs.get(WORKING_DIR, RunREDUCE.reduceConfigurationDefault.workingDir);
        reduceCommandList = new REDUCECommandList();

        try {
            if (prefs.nodeExists(REDUCE_COMMANDS)) {
                prefs = prefs.node(REDUCE_COMMANDS);
                String[] childrenNames = prefs.childrenNames();
                // Build a list with the right number of null REDUCECommand elements
                // so that they can be set in the right order below:
                reduceCommandList.addAll(Collections.nCopies(childrenNames.length, null));
                for (String commandName : childrenNames) {
                    // Get defaults:
                    REDUCECommand cmdDefault = null;
                    for (REDUCECommand cmd : RunREDUCE.reduceConfigurationDefault.reduceCommandList)
                        if (commandName.equals(cmd.name)) {
                            cmdDefault = cmd;
                            break;
                        }
                    if (cmdDefault == null) cmdDefault = new REDUCECommand(); // all fields ""
                    prefs = prefs.node(commandName);
                    boolean useShell = prefs.getBoolean(USE_SHELL, cmdDefault.useShell);
                    boolean checkCommand = prefs.getBoolean(CHECK_COMMAND, cmdDefault.checkCommand);
                    String commandRootDir = prefs.get(COMMAND_ROOT_DIR, cmdDefault.rootDir);
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
                    int commandIndex = prefs.getInt(COMMAND_INDEX, -1);
                    if (commandIndex == -1) // append, but this should not happen!
                        reduceCommandList.add(new REDUCECommand(commandName, useShell, checkCommand, commandRootDir, command));
                    else
                        reduceCommandList.set(commandIndex,
                                new REDUCECommand(commandName, useShell, checkCommand, commandRootDir, command));
                    prefs = prefs.parent();
                }
            } else if (prefs.nodeExists(REDUCE_VERSIONS)) { // FixMe delete this obsolete temporary compatibility code
                prefs = prefs.node(REDUCE_VERSIONS);
                for (String version : prefs.childrenNames()) {
                    // Get defaults:
                    REDUCECommand cmdDefault = null;
                    for (REDUCECommand cmd : RunREDUCE.reduceConfigurationDefault.reduceCommandList)
                        if (version.equals(cmd.name)) {
                            cmdDefault = cmd;
                            break;
                        }
                    if (cmdDefault == null) cmdDefault = new REDUCECommand(); // all fields ""
                    prefs = prefs.node(version);
                    String versionRootDir = prefs.get(REDUCE_ROOT_DIR, cmdDefault.rootDir);
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
                    reduceCommandList.add(new REDUCECommand(version, false, true, versionRootDir, command));
                    prefs = prefs.parent();
                }
            } else
                reduceCommandList = RunREDUCE.reduceConfigurationDefault.reduceCommandList.copy();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method saves the generic directory and REDUCECommandList fields as preferences.
     */
    void save() {
        Preferences prefs = RRPreferences.prefs;
        prefs.put(REDUCE_ROOT_DIR, reduceRootDir);
        prefs.put(PACKAGES_DIR, packagesDir);
        prefs.put(MANUAL_DIR, manualDir);
        prefs.put(PRIMERS_DIR, primersDir);
        prefs.put(WORKING_DIR, workingDir);
        RunREDUCEFrame.fileChooser.setInitialDirectory(new File(workingDir));
        // Remove all saved REDUCE versions before saving the current REDUCE versions:
        try { // FixMe delete this obsolete temporary compatibility code
            prefs.node(REDUCE_VERSIONS).removeNode();
        } catch (BackingStoreException ignored) {
        }
        try {
            prefs.node(REDUCE_COMMANDS).removeNode();
        } catch (BackingStoreException ignored) {
        }
        prefs = prefs.node(REDUCE_COMMANDS);
        int commandIndex = 0;
        for (REDUCECommand cmd : reduceCommandList) {
            prefs = prefs.node(cmd.name);
            prefs.putInt(COMMAND_INDEX, commandIndex++);
            prefs.putBoolean(USE_SHELL, cmd.useShell);
            prefs.putBoolean(CHECK_COMMAND, cmd.checkCommand);
            prefs.put(COMMAND_ROOT_DIR, cmd.rootDir);
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
 * The list excludes preloaded packages and is sorted alphabetically.
 */
class REDUCEPackageList extends ArrayList<String> {
    // FixMe Remove fmprint and tmprint?

    REDUCEPackageList() {
        Path packagesDirPath = Paths.get(RunREDUCE.reduceConfiguration.packagesDir);
        Path packageMapFile = packagesDirPath.resolve("package.map");
        if (!Files.isReadable(packageMapFile)) {
            RunREDUCE.alert(Alert.AlertType.ERROR, "Packages",
                    "The REDUCE package map file is not available!" +
                            "\nPlease correct 'REDUCE Packages Directory' in the 'Configure REDUCE...' dialogue," +
                            "\nwhich will open automatically when you close this dialogue.");
            RunREDUCE.runREDUCEFrame.showDialogAndWait("Configure REDUCE Directories and Commands",
                    "REDUCEConfigDialog.fxml");
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
    }
}
