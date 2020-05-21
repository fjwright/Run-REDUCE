package fjwright.runreduce;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.Optional;

/**
 * This class controls the main RunREDUCE frame and menu bar.
 * Note that the current directory for REDUCE is the directory from
 * which this GUI was run, so filenames must be relative to that
 * directory or absolute; I currently use the latter.
 */
public class RunREDUCEFrame {
    // Fields defined in FXML must be public or @FXML!
    @FXML
    BorderPane frame;
    // File menu:
    @FXML
    private CheckMenuItem echoCheckMenuItem;
    @FXML
    MenuItem inputFileMenuItem;
    @FXML
    MenuItem inputPackageFileMenuItem;
    @FXML
    MenuItem outputNewFileMenuItem;
    @FXML
    MenuItem outputOpenFileMenuItem;
    @FXML
    MenuItem outputHereMenuItem;
    @FXML
    MenuItem shutFileMenuItem;
    @FXML
    MenuItem shutLastMenuItem;
    @FXML
    MenuItem loadPackagesMenuItem;
    // REDUCE menu:
    @FXML
    MenuItem stopREDUCEMenuItem;
    @FXML
    Menu runREDUCESubmenu;
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
    @FXML
    private RadioMenuItem singlePaneRadioButton;
    @FXML
    private RadioMenuItem splitPaneRadioButton;
    @FXML
    private RadioMenuItem tabbedPaneRadioButton;
    @FXML
    private MenuItem addTabMenuItem;
    @FXML
    private MenuItem removeTabMenuItem;
    // Help menu:
    @FXML
    private Menu helpMenu;

    private static final File USER_HOME_DIR = new File(System.getProperty("user.home"));
    private static final File PACKAGES_DIR = new File(RunREDUCE.reduceConfiguration.packagesRootDir, "packages");
    // ToDo Separate input and output file choosers?
    private static final FileChooser fileChooser = new FileChooser();
    private static final FileChooser.ExtensionFilter INPUT_FILE_FILTER =
            new FileChooser.ExtensionFilter("REDUCE Input Files (*.red, *.tst)", "*.red", "*.tst");
    private static final FileChooser.ExtensionFilter LOG_FILE_FILTER =
            new FileChooser.ExtensionFilter("REDUCE Log Files (*.log, *.rlg)", "*.log", "*.rlg");
    private static final FileChooser.ExtensionFilter TEXT_FILE_FILTER =
            new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt");
    private static final FileChooser.ExtensionFilter ALL_FILE_FILTER =
            new FileChooser.ExtensionFilter("All Files", "*.*");

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

        singlePaneRadioButton.setSelected(RRPreferences.displayPane == RRPreferences.DisplayPane.SINGLE);
        splitPaneRadioButton.setSelected(RRPreferences.displayPane == RRPreferences.DisplayPane.SPLIT);
        tabbedPaneRadioButton.setSelected(RRPreferences.displayPane == RRPreferences.DisplayPane.TABBED);
        addTabMenuItem.setDisable(RRPreferences.displayPane == RRPreferences.DisplayPane.SPLIT);
        removeTabMenuItem.setDisable(RRPreferences.displayPane != RRPreferences.DisplayPane.TABBED);

        /* ********* *
         * Help menu *
         * ********* */

        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();

            String[][] manuals = {
                    // {Manual name, Windows location, non-Windows location}
                    {"REDUCE Manual (HTML)", "lib/csl/reduce.doc/manual.html", "manual.html"},
                    {"REDUCE Manual (PDF)", "lib/csl/reduce.doc/manual.pdf", "manual.pdf.gz"},
                    {"Inside Reduce (PDF)", "doc/insidereduce.pdf", "insidereduce.pdf.gz"},
                    {"REDUCE Symbolic Mode Primer (PDF)", "doc/primer.pdf", "primer.pdf.gz"},
                    {"Standard Lisp Report (PDF)", "doc/sl.pdf", "sl.pdf.gz"}
            };

            int helpMenuItemIndex = 2;
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                for (String[] manual : manuals) {
                    MenuItem menuItem = new MenuItem(manual[0]);
                    helpMenu.getItems().add(helpMenuItemIndex++, menuItem);
                    menuItem.setOnAction(e -> {
                        try {
                            desktop.open(new File(RunREDUCE.reduceConfiguration.docRootDir,
                                    manual[RRPreferences.windowsOS ? 1 : 2]));
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
    private void inputFileMenuItemAction() {
        fileChooser.setTitle("Input from Files...");
        inputFile();
    }

    // Input from Package Files...
    @FXML
    private void inputPackageFileMenuItemAction() {
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
    private void outputNewFileMenuItemAction() {
        fileChooser.setTitle("Output to File...");
        fileChooser.getExtensionFilters().setAll(LOG_FILE_FILTER, TEXT_FILE_FILTER, ALL_FILE_FILTER);
        File file = fileChooser.showSaveDialog(RunREDUCE.primaryStage);
        if (file != null) {
            RunREDUCE.reducePanel.sendStringToREDUCEAndEcho("out \"" + file.toString() + "\"$\n");
            RunREDUCE.reducePanel.outputFileList.remove(file); // in case it was already open
            RunREDUCE.reducePanel.outputFileList.add(file);
            RunREDUCE.reducePanel.outputFileDisableMenuItems(false);
        }
    }

    // Output to Open File...
    @FXML
    private void outputOpenFileMenuItemAction() {
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
                RunREDUCE.reducePanel.outputFileDisableMenuItems(false);
            }
        }
    }

    // Output Here
    @FXML
    private void outputHereMenuItemAction() {
        RunREDUCE.reducePanel.sendStringToREDUCEAndEcho("out t$\n");
        RunREDUCE.reducePanel.outputHereDisableMenuItemsMaybe();
    }

    // Shut Output Files...
    @FXML
    private void shutFileMenuItemAction() {
        if (!RunREDUCE.reducePanel.outputFileList.isEmpty()) // not strictly necessary
            // Select output file(s) to shut:
            showDialogAndWait("Shut Output Files...", "ShutOutputFilesDialog.fxml");
        RunREDUCE.reducePanel.outputFileDisableMenuItemsMaybe();
    }

    // Shut Last Output File
    @FXML
    private void shutLastMenuItemAction() {
        if (!RunREDUCE.reducePanel.outputFileList.isEmpty()) { // not strictly necessary
            int last = RunREDUCE.reducePanel.outputFileList.size() - 1;
            RunREDUCE.reducePanel.sendStringToREDUCEAndEcho(
                    "shut \"" + RunREDUCE.reducePanel.outputFileList.remove(last).toString() + "\"$\n");
        }
        RunREDUCE.reducePanel.outputFileDisableMenuItemsMaybe();
    }

    // Load Packages...
    @FXML
    private void loadPackagesMenuItemAction() {
        if (packageList == null) packageList = new REDUCEPackageList();
        if (packageList.isEmpty()) {
            // Allow the user to correct the packages directory and try again:
            packageList = null;
            return;
        }
        // Select packages to load:
        showDialogAndWait("Load Packages...", "LoadPackagesDialog.fxml");
    }

    // Save Session Log...
    @FXML
    private void saveLogMenuItemAction() {
        saveLog(false);
    }

    // Append Session Log...
    @FXML
    private void appendLogMenuItemAction() {
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
    private void exitMenuItemAction() {
        Platform.exit();
    }

    /* *********** *
     * REDUCE menu *
     * *********** */

    private void runREDUCESubmenuBuild() {
        ObservableList<MenuItem> menuItems = runREDUCESubmenu.getItems();
        menuItems.clear();
        for (REDUCECommand cmd : RunREDUCE.reduceConfiguration.reduceCommandList) {
            MenuItem item = new MenuItem(cmd.name);
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
            RadioMenuItem item = new RadioMenuItem(cmd.name);
            if (RRPreferences.autoRunVersion.equals(cmd.name)) item.setSelected(true);
            menuItems.add(item);
            item.setToggleGroup(autoRunToggleGroup);
            item.setOnAction(e -> {
                RadioMenuItem radioMenuItem = (RadioMenuItem) e.getSource();
                if (radioMenuItem.isSelected()) {
                    String version = radioMenuItem.getText();
                    RRPreferences.save(RRPreferences.AUTORUNVERSION, version);
                    if (!RunREDUCE.reducePanel.runningREDUCE) {
                        for (REDUCECommand cmd1 : RunREDUCE.reduceConfiguration.reduceCommandList) {
                            if (version.equals(cmd1.name))
                                RunREDUCE.reducePanel.run(cmd1);
                            break;
                        }
                    }
                }
            });
        }
    }

    @FXML
    private void stopREDUCEMenuItemAction() {
        RunREDUCE.reducePanel.sendStringToREDUCEAndEcho("bye;\n");
        RunREDUCE.reducePanel.sendButton.setDisable(true);
        RunREDUCE.reducePanel.runningREDUCE = false;
        RunREDUCE.reducePanel.outputFileList.clear();
        // Reset enabled status of menu items:
        RunREDUCE.reducePanel.reduceStopped();
    }

    @FXML
    private void clearDisplayMenuItemAction() {
        RunREDUCE.reducePanel.outputTextFlow.getChildren().clear();
    }

    @FXML
    private void configureREDUCEMenuItemAction() {
        showDialogAndWait("Configure REDUCE Directories and Commands",
                "REDUCEConfigDialog.fxml");
    }

    /* ********* *
     * View menu *
     * ********* */

    @FXML
    private void fontSizeMenuItemAction() {
        showDialogAndWait("Font Size...", "FontSizeDialog.fxml");
    }

    @FXML
    private void boldPromptsCheckBoxAction() {
        RRPreferences.boldPromptsState = boldPromptsCheckBox.isSelected();
        RRPreferences.save(RRPreferences.BOLDPROMPTS);
    }

    @FXML
    private void noColouredIORadioButtonAction() {
        RRPreferences.save(RRPreferences.COLOUREDIO, RRPreferences.ColouredIO.NONE);
    }

    @FXML
    private void modeColouredIORadioButtonAction() {
        RRPreferences.save(RRPreferences.COLOUREDIO, RRPreferences.ColouredIO.MODAL);
    }

    @FXML
    private void redfrontColouredIORadioButtonAction() {
        RRPreferences.save(RRPreferences.COLOUREDIO, RRPreferences.ColouredIO.REDFRONT);
    }

    @FXML
    private void singlePaneRadioButtonAction() {
        switch (RRPreferences.displayPane) {
            case SPLIT:
                RunREDUCE.useSplitPane(false);
//                addTabMenuItem.setEnabled(true);
                break;
//            case TABBED:
//                RunREDUCE.useTabbedPane(false);
//                removeTabMenuItem.setEnabled(false);
        }
        RRPreferences.save(RRPreferences.DISPLAYPANE, RRPreferences.DisplayPane.SINGLE);
    }

    @FXML
    private void splitPaneRadioButtonAction() {
//        if (RRPreferences.displayPane == RRPreferences.DisplayPane.TABBED) RunREDUCE.useTabbedPane(false);
//        addTabMenuItem.setEnabled(false);
//        removeTabMenuItem.setEnabled(false);
        RRPreferences.save(RRPreferences.DISPLAYPANE, RRPreferences.DisplayPane.SPLIT);
        RunREDUCE.useSplitPane(true);
    }

    @FXML
    private void tabbedPaneRadioButtonAction() {
//        if (RRPreferences.displayPane == RRPreferences.DisplayPane.SPLIT) RunREDUCE.useSplitPane(false);
//        RRPreferences.save(RRPreferences.DISPLAYPANE, RRPreferences.DisplayPane.TABBED);
//        RunREDUCE.useTabbedPane(true);
//        addTabMenuItem.setEnabled(true);
//        removeTabMenuItem.setEnabled(true);
    }

    @FXML
    private void addTabMenuItemAction() {
//        RunREDUCE.addTab();
    }

    @FXML
    private void removeTabMenuItemAction() {
//        RunREDUCE.removeTab();
    }

    /* ********* *
     * Help menu *
     * ********* */

    @FXML
    private void userGuideMenuItemAction() {
        Stage stage = new Stage();
        stage.setTitle("Run-REDUCE User Guide");
        Scene scene = new Scene(new Browser());
        stage.setScene(scene);
//            scene.getStylesheets().add("webviewsample/BrowserToolbar.css");
        stage.show();
    }

    @FXML
    private void aboutMenuItemAction() {
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

    void showDialogAndWait(String dialogTitle, String fxmlFileName) {
        Parent root;
        try {
            root = FXMLLoader.load(getClass().getResource(fxmlFileName));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(dialogTitle);
        stage.setScene(new Scene(root));
        stage.showAndWait();
    }

    private static class Browser extends Region {
        final WebView browser = new WebView();
        final WebEngine webEngine = browser.getEngine();

        Browser() {
            // add the web view to the scene
            getChildren().add(browser);
            // apply the styles
//                getStyleClass().add("browser");
            setStyle("-fx-padding: 5;");
            // load the web page
//            webEngine.load("https://fjwright.github.io/Run-REDUCE/UserGuide.html"); // Works!
            webEngine.load(RunREDUCEFrame.class.getResource("UserGuide.html").toExternalForm());
        }

        @Override
        protected void layoutChildren() {
            layoutInArea(browser, 0, 0, getWidth(), getHeight(), 0, HPos.CENTER, VPos.CENTER);
        }
    }
}
