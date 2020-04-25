package fjwright.runreduce;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/*
 * This class controls the RunREDUCE menu bar.
 * Note that the current directory for REDUCE is the directory from
 * which this GUI was run, so filenames must be relative to that
 * directory or absolute; I currently use the latter.
 */
public class RunREDUCEController implements Initializable {
    // ToDo Menu ToolTips
    // Fields defined in FXML must be public!
    // File menu:
    public CheckMenuItem echoCheckMenuItem;
    public MenuItem inputFileMenuItem;
    public MenuItem inputPackageFileMenuItem;
    public MenuItem outputNewFileMenuItem;
    public MenuItem outputOpenFileMenuItem;
    public MenuItem outputHereMenuItem;
    public MenuItem shutFileMenuItem;
    public MenuItem shutLastMenuItem;
    public MenuItem loadPackagesMenuItem;
    // Help menu:
    public Menu helpMenu;

    static final FileChooser fileChooser = new FileChooser();
    // ToDo Separate input and output file choosers?
    static final FileChooser.ExtensionFilter INPUT_FILE_FILTER =
            new FileChooser.ExtensionFilter("REDUCE Input Files (*.red, *.tst)", "*.red", "*.tst");
    static final FileChooser.ExtensionFilter LOG_FILE_FILTER =
            new FileChooser.ExtensionFilter("REDUCE Log Files (*.log, *.rlg)", "*.log", "*.rlg");
    static final FileChooser.ExtensionFilter TEXT_FILE_FILTER =
            new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt");
    static final FileChooser.ExtensionFilter ALL_FILE_FILTER =
            new FileChooser.ExtensionFilter("All Files", "*.*");

    //    static ShutOutputFilesDialog shutOutputFilesDialog;
    static final List<File> outputFileList = new ArrayList<>();
    //    static LoadPackagesDialog loadPackagesDialog;
    static List<String> packageList;

    /**
     * Called to initialize a controller after its root element has been
     * completely processed.
     *
     * @param location  The location used to resolve relative paths for the root object, or
     *                  <tt>null</tt> if the location is not known.
     * @param resources The resources used to localize the root object, or <tt>null</tt> if
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Dynamically finish building menus.

        /* ********* *
         * File menu *
         * ********* */

        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        /* ********* *
         * Help menu *
         * ********* */

        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            int helpMenuItemIndex = 0;

            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                MenuItem userGuideMenuItem = new MenuItem("Run-REDUCE User Guide (HTML)");
                helpMenu.getItems().add(helpMenuItemIndex++, userGuideMenuItem);
//                userGuideMenuItem.setToolTipText("Open the Run-REDUCE User Guide in the default web browser.");
                userGuideMenuItem.setOnAction(e ->
                {
                    try {
                        desktop.browse(new URI("https://fjwright.github.io/Run-REDUCE/UserGuide.html"));
                    } catch (IOException | URISyntaxException ex) {
                        ex.printStackTrace();
                    }
                });
            }
            helpMenu.getItems().add(helpMenuItemIndex++, new SeparatorMenuItem());

            String[][] manuals = {
                    {"REDUCE Manual (HTML)", "lib/csl/reduce.doc/manual.html", "manual.html"},
                    {"REDUCE Manual (PDF)", "lib/csl/reduce.doc/manual.pdf", "manual.pdf.gz"},
                    {"Inside Reduce (PDF)", "doc/insidereduce.pdf", "insidereduce.pdf.gz"},
                    {"REDUCE Symbolic Mode Primer (PDF)", "doc/primer.pdf", "primer.pdf.gz"},
                    {"Standard Lisp Report (PDF)", "doc/sl.pdf", "sl.pdf.gz"}
            };

            if (desktop.isSupported(Desktop.Action.OPEN)) {
                for (String[] manual : manuals) {
                    MenuItem menuItem = new MenuItem(manual[0]);
                    helpMenu.getItems().add(helpMenuItemIndex++, menuItem);
//                    menuItem.setToolTipText("Open this manual in the default application.");
                    menuItem.setOnAction(e ->
                    {
                        try { // FixMe
//                            desktop.open(RRPreferences.windowsOS ?
//                                    // ToDo Make the directory used below configurable?
//                                    new File(RunREDUCE.reduceConfiguration.packagesRootDir, manual[1]) :
//                                    new File("/usr/share/doc/reduce", manual[2]));
                            desktop.open(new File("D:/Program Files/Reduce", manual[1]));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    });
                }
            }
            helpMenu.getItems().add(helpMenuItemIndex, new SeparatorMenuItem());
        }

    }

    /* ********* *
     * File menu *
     * ********* */

    // Input from Files...
    public void inputFileMenuItemAction(ActionEvent actionEvent) {
        fileChooser.setTitle("Input from Files...");
        inputFile();
    }

    // Input from Package Files...
    public void inputPackageFileMenuItemAction(ActionEvent actionEvent) {
        fileChooser.setTitle("Input from Package Files...");
        fileChooser.setInitialDirectory(new File("D:/Program Files/Reduce/packages")); // FixMe
        inputFile();
    }

    private void inputFile() {
        fileChooser.getExtensionFilters().clear();
        fileChooser.getExtensionFilters().addAll(INPUT_FILE_FILTER, TEXT_FILE_FILTER, ALL_FILE_FILTER);
        List<File> fileList = fileChooser.showOpenMultipleDialog(RunREDUCE.primaryStage);
        if (fileList != null) {
            StringBuilder text = new StringBuilder("in \"");
            text.append(fileList.get(0).toString());
            for (File file : fileList.subList(1, fileList.size())) {
                text.append("\", \"");
                text.append(file.toString());
            }
            text.append(echoCheckMenuItem.isSelected() ? "\";\n" : "\"$\n");
//            RunREDUCE.reducePanel.sendStringToREDUCEAndEcho(text.toString());
            System.err.println(text);
        }

    }

    // Output to New File...
    public void outputNewFileMenuItemAction(ActionEvent actionEvent) {
        fileChooser.setTitle("Output to File...");
        fileChooser.getExtensionFilters().clear();
        fileChooser.getExtensionFilters().addAll(LOG_FILE_FILTER, TEXT_FILE_FILTER, ALL_FILE_FILTER);
        File file = fileChooser.showSaveDialog(RunREDUCE.primaryStage);
        if (file != null) {
            System.err.println("out \"" + file + "\"$\n"); // FixMe
            outputFileList.remove(file); // in case it was already open
            outputFileList.add(file);
            outputFileSetEnabledMenuItems(true);
        }
    }

    // Output to Open File...
    public void outputOpenFileMenuItemAction(ActionEvent actionEvent) {
        if (!outputFileList.isEmpty()) { // not strictly necessary
            // Select output file to shut:
            ChoiceDialog<File> choiceDialog = new ChoiceDialog<>(outputFileList.get(0), outputFileList);
            Optional<File> result = choiceDialog.showAndWait();
            if (result.isPresent()) {
                File file = result.get();
                System.err.println("out \"" + file + "\"$\n"); // FixMe
                // Make this the last file used for output:
                outputFileList.remove(file);
                outputFileList.add(file);
            }
        }
    }

    // Output Here
    public void outputHereMenuItemAction(ActionEvent actionEvent) {
//        RunREDUCE.reducePanel.sendStringToREDUCEAndEcho("out t$\n");
//        outputHereMenuItem.setEnabled(
//                RunREDUCE.reducePanel.menuItemStatus.outputHereMenuItem = false);
    }

    // Shut Output Files...
    // FixMe Shut multiple files
    public void shutFileMenuItemAction(ActionEvent actionEvent) {
        if (!outputFileList.isEmpty()) { // not strictly necessary
            // Select output file to shut:
            ChoiceDialog<File> choiceDialog = new ChoiceDialog<>(outputFileList.get(0), outputFileList);
            Optional<File> result = choiceDialog.showAndWait();
            if (result.isPresent()) {
                File file = result.get();
                System.err.println("shut \"" + file + "\"$\n"); // FixMe
                outputFileList.remove(file);
            }
        }
        if (outputFileList.isEmpty()) outputFileSetEnabledMenuItems(false);
    }

    // Shut Last Output File
    public void shutLastMenuItemAction(ActionEvent actionEvent) {
//        if (!outputFileList.isEmpty()) { // not strictly necessary
//            int last = outputFileList.size() - 1;
//            RunREDUCE.reducePanel.sendStringToREDUCEAndEcho("shut \"" + outputFileList.remove(last).toString() + "\"$\n");
//        }
//        if (outputFileList.isEmpty()) outputFileSetEnabledMenuItems(false);
    }

    // Load Packages...
    public void loadPackagesMenuItemAction(ActionEvent actionEvent) {
//        if (loadPackagesDialog == null) loadPackagesDialog = new LoadPackagesDialog(frame);
//        if (packageList == null) packageList = new REDUCEPackageList();
//        if (packageList.isEmpty()) {
//            // Allow the user to correct the packages directory and try again:
//            packageList = null;
//            return;
//        }
//        // Select packages to load:
//        List<String> selectedPackages = loadPackagesDialog.showDialog(packageList);
//        if (!selectedPackages.isEmpty()) {
//            StringBuilder text = new StringBuilder("load_package ");
//            text.append(selectedPackages.get(0));
//            for (int i = 1; i < selectedPackages.size(); i++) {
//                text.append(", ");
//                text.append(selectedPackages.get(i));
//            }
//            text.append(";\n");
//            RunREDUCE.reducePanel.sendStringToREDUCEAndEcho(text.toString());
//        }
    }

    // Save Session Log...
    public void saveLogMenuItemAction(ActionEvent actionEvent) {
        fileChooser.setTitle("Save Session Log...");
        saveLog(false);
    }

    // Append Session Log...
    public void appendLogMenuItemAction(ActionEvent actionEvent) {
        fileChooser.setTitle("Append Session Log...");
        saveLog(true);
    }

    private void saveLog(boolean append) {
        fileChooser.getExtensionFilters().clear();
        fileChooser.getExtensionFilters().addAll(LOG_FILE_FILTER, TEXT_FILE_FILTER, ALL_FILE_FILTER);
        File file = fileChooser.showSaveDialog(RunREDUCE.primaryStage);
        if (file != null) { // FixMe
            System.err.println((append ? "Append" : "Save") + " Session Log to " + file);
//            try (Writer out = new BufferedWriter
//                    (new FileWriter(file, append))) {
//                RunREDUCE.reducePanel.outputTextPane.write(out);
//            } catch (IOException ioe) {
//                ioe.printStackTrace();
//            }
        }
    }

    // Exit
    public void exitMenuItemAction(ActionEvent actionEvent) {
        System.exit(0);
    }

    /* *********** *
     * REDUCE menu *
     * *********** */

    public void stopREDUCEMenuItemAction(ActionEvent actionEvent) {
    }

    public void clearDisplayMenuItemAction(ActionEvent actionEvent) {
    }

    public void configureREDUCEMenuItemAction(ActionEvent actionEvent) {
    }

    /* ********* *
     * View menu *
     * ********* */

    public void javaLFRadioButtonMenuItemAction(ActionEvent actionEvent) {
    }

    public void nativeLFRadioButtonMenuItemAction(ActionEvent actionEvent) {
    }

    public void motifLFRadioButtonMenuItemAction(ActionEvent actionEvent) {
    }

    public void fontSizeMenuItemAction(ActionEvent actionEvent) {
    }

    public void boldPromptsCheckBoxAction(ActionEvent actionEvent) {
    }

    public void noColouredIORadioButtonAction(ActionEvent actionEvent) {
    }

    public void modeColouredIORadioButtonAction(ActionEvent actionEvent) {
    }

    public void redfrontColouredIORadioButtonAction(ActionEvent actionEvent) {
    }

    public void singlePaneRadioButtonAction(ActionEvent actionEvent) {
    }

    public void splitPaneRadioButtonAction(ActionEvent actionEvent) {
    }

    public void tabbedPaneRadioButtonAction(ActionEvent actionEvent) {
    }

    public void addTabMenuItemAction(ActionEvent actionEvent) {
    }

    public void removeTabMenuItemAction(ActionEvent actionEvent) {
    }

    /* ********* *
     * Help menu *
     * ********* */

    public void aboutMenuItemAction(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Prototype version 0.1\n" +
                        "\u00A9 Francis Wright, April 2020");
        alert.setTitle("About Run-REDUCE");
        alert.setHeaderText("Run REDUCE in a JavaFX GUI");
        alert.showAndWait();
    }

    /* *************** *
     * Support methods *
     * *************** */

    private void outputFileSetEnabledMenuItems(boolean enabled) {
        // FixMe Uncomment
        outputHereMenuItem.setDisable(/*RunREDUCE.reducePanel.menuItemStatus.outputHereMenuItem =*/ !enabled);
        shutFileMenuItem.setDisable(/*RunREDUCE.reducePanel.menuItemStatus.shutFileMenuItem =*/ !enabled);
        shutLastMenuItem.setDisable(/*RunREDUCE.reducePanel.menuItemStatus.shutLastMenuItem = */!enabled);
    }
}
