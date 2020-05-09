package fjwright.runreduce;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * This class controls the main RunREDUCE frame and menu bar.
 * Note that the current directory for REDUCE is the directory from
 * which this GUI was run, so filenames must be relative to that
 * directory or absolute; I currently use the latter.
 */
public class RunREDUCEFrame {
    // ToDo Menu ToolTips
    // Fields defined in FXML must be public or @FXML!
    @FXML
    BorderPane frame;
    // File menu:
    @FXML
    private CheckMenuItem echoCheckMenuItem;
    @FXML
    private MenuItem inputFileMenuItem;
    @FXML
    private MenuItem inputPackageFileMenuItem;
    @FXML
    private MenuItem outputNewFileMenuItem;
    @FXML
    private MenuItem outputOpenFileMenuItem;
    @FXML
    private MenuItem outputHereMenuItem;
    @FXML
    private MenuItem shutFileMenuItem;
    @FXML
    private MenuItem shutLastMenuItem;
    @FXML
    private MenuItem loadPackagesMenuItem;
    // REDUCE menu:
    @FXML
    private MenuItem stopREDUCEMenuItem;
    @FXML
    private Menu runREDUCESubmenu;
    @FXML
    private Menu autoRunREDUCESubmenu;
    // View menu:
    @FXML
    private CheckMenuItem boldPromptsCheckBox;
    @FXML
    private RadioMenuItem noColouredIORadioButton;
    @FXML
    private RadioMenuItem modeColouredIORadioButton;
    @FXML
    private RadioMenuItem redfrontColouredIORadioButton;
    // Help menu:
    @FXML
    private Menu helpMenu;

    // ToDo Separate input and output file choosers?
    private static final File USER_HOME_DIR = new File(System.getProperty("user.home"));
    private static final File PACKAGES_DIR = new File(RunREDUCE.reduceConfiguration.packagesRootDir, "packages");
    private static final FileChooser.ExtensionFilter INPUT_FILE_FILTER =
            new FileChooser.ExtensionFilter("REDUCE Input Files (*.red, *.tst)", "*.red", "*.tst");
    private static final FileChooser fileChooser = new FileChooser();
    private static final FileChooser.ExtensionFilter LOG_FILE_FILTER =
            new FileChooser.ExtensionFilter("REDUCE Log Files (*.log, *.rlg)", "*.log", "*.rlg");
    private static final FileChooser.ExtensionFilter TEXT_FILE_FILTER =
            new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt");
    private static final FileChooser.ExtensionFilter ALL_FILE_FILTER =
            new FileChooser.ExtensionFilter("All Files", "*.*");

    //    static ShutOutputFilesDialog shutOutputFilesDialog;
    //    static LoadPackagesDialog loadPackagesDialog;
    static List<String> packageList;

    /**
     * Called to initialize a controller after its root element has been completely processed.
     */
    @FXML
    private void initialize() {
        // Finish building menus dynamically.

        /* ********* *
         * File menu *
         * ********* */

        // Default initial directory on Windows 10 is "This PC", so...
        fileChooser.setInitialDirectory(USER_HOME_DIR);

        /* *********** *
         * REDUCE menu *
         * *********** */

        // Create a submenu to run the selected version of REDUCE:
        runREDUCESubmenuBuild();
        // Create a submenu to select the version of REDUCE to auto-run (or none):
        autoRunREDUCESubmenuBuild();

        /* ********* *
         * View menu *
         * ********* */

        boldPromptsCheckBox.setSelected(RRPreferences.boldPromptsState);

        switch (RRPreferences.colouredIOIntent) {
            case NONE:
            default:
                noColouredIORadioButton.setSelected(true);
                break;
            case MODAL:
                modeColouredIORadioButton.setSelected(true);
                break;
            case REDFRONT:
                redfrontColouredIORadioButton.setSelected(true);
        }

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
                    menuItem.setOnAction(e -> {
                        try {
                            desktop.open(RRPreferences.windowsOS ?
                                    // ToDo Make the directory used below configurable?
                                    new File(RunREDUCE.reduceConfiguration.packagesRootDir, manual[1]) :
                                    new File("/usr/share/doc/reduce", manual[2]));
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
    @FXML
    private void inputFileMenuItemAction(/*ActionEvent actionEvent*/) {
        fileChooser.setTitle("Input from Files...");
        inputFile();
    }

    // Input from Package Files...
    @FXML
    private void inputPackageFileMenuItemAction(/*ActionEvent actionEvent*/) {
        fileChooser.setTitle("Input from Package Files...");
        fileChooser.setInitialDirectory(PACKAGES_DIR);
        inputFile();
        fileChooser.setInitialDirectory(USER_HOME_DIR);
    }

    private void inputFile() {
        fileChooser.getExtensionFilters().setAll(INPUT_FILE_FILTER, TEXT_FILE_FILTER, ALL_FILE_FILTER);
        List<File> fileList = fileChooser.showOpenMultipleDialog(RunREDUCE.primaryStage);
        if (fileList != null) {
            StringBuilder text = new StringBuilder("in \"");
            text.append(fileList.get(0).toString());
            for (File file : fileList.subList(1, fileList.size())) {
                text.append("\", \"");
                text.append(file.toString());
            }
            text.append(echoCheckMenuItem.isSelected() ? "\";\n" : "\"$\n");
            RunREDUCE.reducePanel.sendStringToREDUCEAndEcho(text.toString());
        }
    }

    // Output to New File...
    @FXML
    private void outputNewFileMenuItemAction(/*ActionEvent actionEvent*/) {
        fileChooser.setTitle("Output to File...");
        fileChooser.getExtensionFilters().setAll(LOG_FILE_FILTER, TEXT_FILE_FILTER, ALL_FILE_FILTER);
        File file = fileChooser.showSaveDialog(RunREDUCE.primaryStage);
        if (file != null) {
            RunREDUCE.reducePanel.sendStringToREDUCEAndEcho("out \"" + file.toString() + "\"$\n");
            RunREDUCE.reducePanel.outputFileList.remove(file); // in case it was already open
            RunREDUCE.reducePanel.outputFileList.add(file);
            outputFileSetDisableMenuItems(false);
        }
    }

    // Output to Open File...
    @FXML
    private void outputOpenFileMenuItemAction(/*ActionEvent actionEvent*/) {
        if (!RunREDUCE.reducePanel.outputFileList.isEmpty()) { // not strictly necessary
            // Select output file to shut:
            ChoiceDialog<File> choiceDialog = new ChoiceDialog<>(RunREDUCE.reducePanel.outputFileList.get(0),
                    RunREDUCE.reducePanel.outputFileList);
            Optional<File> result = choiceDialog.showAndWait();
            if (result.isPresent()) {
                File file = result.get();
                RunREDUCE.reducePanel.sendStringToREDUCEAndEcho("out \"" + file.toString() + "\"$\n");
                // Make this the last file used for output:
                RunREDUCE.reducePanel.outputFileList.remove(file);
                RunREDUCE.reducePanel.outputFileList.add(file);
                outputFileSetDisableMenuItems(false);
            }
        }
    }

    // Output Here
    @FXML
    private void outputHereMenuItemAction(/*ActionEvent actionEvent*/) {
        RunREDUCE.reducePanel.sendStringToREDUCEAndEcho("out t$\n");
        outputHereMenuItem.setDisable(RunREDUCE.reducePanel.outputHereMenuItemDisabled = true);
    }

    // Shut Output Files...
    @FXML
    private void shutFileMenuItemAction(/*ActionEvent actionEvent*/) {
        if (!RunREDUCE.reducePanel.outputFileList.isEmpty()) { // not strictly necessary
            // Select output file(s) to shut:
            Parent root;
            try {
                root = FXMLLoader.load(getClass().getResource("ShutOutputFilesDialog.fxml"));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Shut Output Files...");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        }
        if (RunREDUCE.reducePanel.outputFileList.isEmpty()) outputFileSetDisableMenuItems(true);
    }

    // Shut Last Output File
    @FXML
    private void shutLastMenuItemAction(/*ActionEvent actionEvent*/) {
        if (!RunREDUCE.reducePanel.outputFileList.isEmpty()) { // not strictly necessary
            int last = RunREDUCE.reducePanel.outputFileList.size() - 1;
            RunREDUCE.reducePanel.sendStringToREDUCEAndEcho(
                    "shut \"" + RunREDUCE.reducePanel.outputFileList.remove(last).toString() + "\"$\n");
        }
        if (RunREDUCE.reducePanel.outputFileList.isEmpty()) outputFileSetDisableMenuItems(true);
    }

    private void outputFileSetDisableMenuItems(boolean disable) {
        outputOpenFileMenuItem.setDisable(RunREDUCE.reducePanel.outputOpenMenuItemDisabled = disable);
        outputHereMenuItem.setDisable(RunREDUCE.reducePanel.outputHereMenuItemDisabled = disable);
        shutFileMenuItem.setDisable(RunREDUCE.reducePanel.shutFileMenuItemDisabled = disable);
        shutLastMenuItem.setDisable(RunREDUCE.reducePanel.shutLastMenuItemDisabled = disable);
    }

    // Load Packages...
    @FXML
    private void loadPackagesMenuItemAction(/*ActionEvent actionEvent*/) { // FixMe
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
    @FXML
    private void saveLogMenuItemAction(/*ActionEvent actionEvent*/) {
        saveLog(false);
    }

    // Append Session Log...
    @FXML
    private void appendLogMenuItemAction(/*ActionEvent actionEvent*/) {
        saveLog(true);
    }

    private void saveLog(boolean append) {
        fileChooser.getExtensionFilters().setAll(LOG_FILE_FILTER, TEXT_FILE_FILTER, ALL_FILE_FILTER);
        File file;
        if (append) {
            fileChooser.setTitle("Append Session Log...");
            file = fileChooser.showOpenDialog(RunREDUCE.primaryStage);
        } else {
            fileChooser.setTitle("Save Session Log...");
            file = fileChooser.showSaveDialog(RunREDUCE.primaryStage);
        }
        if (file != null) {
            try (Writer out = new BufferedWriter(new FileWriter(file, append))) {
                RunREDUCE.reducePanel.outputTextFlow.getChildren().forEach(e -> {
                    try {
                        out.write(((Text) e).getText());
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                });
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    // Exit
    @FXML
    private void exitMenuItemAction(/*ActionEvent actionEvent*/) {
        Platform.exit();
    }

    /* *********** *
     * REDUCE menu *
     * *********** */

    private void runREDUCESubmenuBuild() {
        ObservableList<MenuItem> menuItems = runREDUCESubmenu.getItems();
        menuItems.clear();
        for (REDUCECommand cmd : RunREDUCE.reduceConfiguration.reduceCommandList) {
            MenuItem item = new MenuItem(cmd.version);
            menuItems.add(item);
            item.setOnAction(e -> RunREDUCE.reducePanel.run(cmd));
        }
    }

    private void autoRunREDUCESubmenuBuild() {
        ObservableList<MenuItem> menuItems = autoRunREDUCESubmenu.getItems();
        menuItems.clear();
        ToggleGroup autoRunToggleGroup = new ToggleGroup();
        RadioMenuItem noAutoRunRadioMenuItem = new RadioMenuItem(RRPreferences.NONE);
        menuItems.add(noAutoRunRadioMenuItem);
        noAutoRunRadioMenuItem.setToggleGroup(autoRunToggleGroup);
        if (RRPreferences.autoRunVersion.equals(RRPreferences.NONE)) noAutoRunRadioMenuItem.setSelected(true);
        noAutoRunRadioMenuItem.setOnAction(e -> {
            if (((RadioMenuItem) e.getSource()).isSelected())
                RRPreferences.save(RRPreferences.AUTORUNVERSION, RRPreferences.NONE);
        });
        for (REDUCECommand cmd : RunREDUCE.reduceConfiguration.reduceCommandList) {
            RadioMenuItem item = new RadioMenuItem(cmd.version);
            if (RRPreferences.autoRunVersion.equals(cmd.version)) item.setSelected(true);
            menuItems.add(item);
            item.setToggleGroup(autoRunToggleGroup);
            item.setOnAction(e -> {
                RadioMenuItem radioMenuItem = (RadioMenuItem) e.getSource();
                if (radioMenuItem.isSelected()) {
                    String version = radioMenuItem.getText();
                    RRPreferences.save(RRPreferences.AUTORUNVERSION, version);
                    if (!RunREDUCE.reducePanel.runningREDUCE) {
                        for (REDUCECommand cmd1 : RunREDUCE.reduceConfiguration.reduceCommandList) {
                            if (version.equals(cmd1.version))
                                RunREDUCE.reducePanel.run(cmd1);
                            break;
                        }
                    }
                }
            });
        }
    }

    @FXML
    private void stopREDUCEMenuItemAction(/*ActionEvent actionEvent*/) {
        RunREDUCE.reducePanel.sendStringToREDUCEAndEcho("bye;\n");
        RunREDUCE.reducePanel.sendButton.setDisable(true);
        RunREDUCE.reducePanel.runningREDUCE = false;
        RunREDUCE.reducePanel.outputFileList.clear();
        // Reset enabled status of menu items:
        RunREDUCE.runREDUCEFrame.reduceStopped();
    }

    @FXML
    private void clearDisplayMenuItemAction(/*ActionEvent actionEvent*/) {
        RunREDUCE.reducePanel.outputTextFlow.getChildren().clear();
    }

    @FXML
    private void configureREDUCEMenuItemAction(/*ActionEvent actionEvent*/) {
    }

    /* ********* *
     * View menu *
     * ********* */

    @FXML
    private void fontSizeMenuItemAction(/*ActionEvent actionEvent*/) {
        Parent root;
        try {
            root = FXMLLoader.load(getClass().getResource("FontSizeDialog.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Font Size...");
        stage.setScene(new Scene(root));
        stage.showAndWait();
    }

    @FXML
    private void boldPromptsCheckBoxAction(/*ActionEvent actionEvent*/) {
        RRPreferences.boldPromptsState = boldPromptsCheckBox.isSelected();
        RRPreferences.save(RRPreferences.BOLDPROMPTS);
    }

    @FXML
    private void noColouredIORadioButtonAction(/*ActionEvent actionEvent*/) {
        RRPreferences.save(RRPreferences.COLOUREDIO, RRPreferences.ColouredIO.NONE);
    }

    @FXML
    private void modeColouredIORadioButtonAction(/*ActionEvent actionEvent*/) {
        RRPreferences.save(RRPreferences.COLOUREDIO, RRPreferences.ColouredIO.MODAL);
    }

    @FXML
    private void redfrontColouredIORadioButtonAction(/*ActionEvent actionEvent*/) {
        RRPreferences.save(RRPreferences.COLOUREDIO, RRPreferences.ColouredIO.REDFRONT);
    }

    @FXML
    private void singlePaneRadioButtonAction(/*ActionEvent actionEvent*/) {
    }

    @FXML
    private void splitPaneRadioButtonAction(/*ActionEvent actionEvent*/) {
    }

    @FXML
    private void tabbedPaneRadioButtonAction(/*ActionEvent actionEvent*/) {
    }

    @FXML
    private void addTabMenuItemAction(/*ActionEvent actionEvent*/) {
    }

    @FXML
    private void removeTabMenuItemAction(/*ActionEvent actionEvent*/) {
    }

    /* ********* *
     * Help menu *
     * ********* */

    @FXML
    private void aboutMenuItemAction(/*ActionEvent actionEvent*/) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Prototype version 0.1\n" +
                        "\u00A9 Francis Wright, May 2020");
        alert.setTitle("About Run-REDUCE");
        alert.setHeaderText("Run REDUCE in a JavaFX GUI");
        alert.showAndWait();
    }

    /* *************** *
     * Support methods *
     * *************** */

    /**
     * Reset menu item status as appropriate when REDUCE is not running.
     */
    void reduceStopped() {
        startingOrStoppingREDUCE(false);
    }

    /**
     * Reset menu item status as appropriate when REDUCE has just started.
     */
    void reduceStarted() {
        startingOrStoppingREDUCE(true);
    }

    private void startingOrStoppingREDUCE(boolean starting) {
        // Items to enable/disable when REDUCE starts/stops running:
        inputFileMenuItem.setDisable(RunREDUCE.reducePanel.inputFileMenuItemDisabled = !starting);
        inputPackageFileMenuItem.setDisable(RunREDUCE.reducePanel.inputFileMenuItemDisabled = !starting);
        outputNewFileMenuItem.setDisable(RunREDUCE.reducePanel.outputFileMenuItemDisabled = !starting);
        loadPackagesMenuItem.setDisable(RunREDUCE.reducePanel.loadPackagesMenuItemDisabled = !starting);
        stopREDUCEMenuItem.setDisable(RunREDUCE.reducePanel.stopREDUCEMenuItemDisabled = !starting);

        // Items to disable/enable when REDUCE starts/stops running:
        runREDUCESubmenu.setDisable(RunREDUCE.reducePanel.runREDUCESubmenuDisabled = starting);

        // Items to always disable when REDUCE starts or stops running:
        outputOpenFileMenuItem.setDisable(RunREDUCE.reducePanel.outputFileMenuItemDisabled = true);
        outputHereMenuItem.setDisable(RunREDUCE.reducePanel.outputHereMenuItemDisabled = true);
        shutFileMenuItem.setDisable(RunREDUCE.reducePanel.shutFileMenuItemDisabled = true);
        shutLastMenuItem.setDisable(RunREDUCE.reducePanel.shutLastMenuItemDisabled = true);
    }

    /**
     * Update the enabled status of the menus.
     */
    void updateMenus() {
        inputFileMenuItem.setDisable(RunREDUCE.reducePanel.inputFileMenuItemDisabled);
        inputPackageFileMenuItem.setDisable(RunREDUCE.reducePanel.inputFileMenuItemDisabled);
        outputNewFileMenuItem.setDisable(RunREDUCE.reducePanel.outputFileMenuItemDisabled);
        outputOpenFileMenuItem.setDisable(RunREDUCE.reducePanel.outputFileMenuItemDisabled);
        loadPackagesMenuItem.setDisable(RunREDUCE.reducePanel.loadPackagesMenuItemDisabled);
        stopREDUCEMenuItem.setDisable(RunREDUCE.reducePanel.stopREDUCEMenuItemDisabled);
        runREDUCESubmenu.setDisable(RunREDUCE.reducePanel.runREDUCESubmenuDisabled);
        outputHereMenuItem.setDisable(RunREDUCE.reducePanel.outputHereMenuItemDisabled);
        shutFileMenuItem.setDisable(RunREDUCE.reducePanel.shutFileMenuItemDisabled);
        shutLastMenuItem.setDisable(RunREDUCE.reducePanel.shutLastMenuItemDisabled);
    }
}
