package fjwright.runreduce;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static java.lang.System.getProperty;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

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
    MenuItem inputFileMenuItem, inputPackageFileMenuItem, outputNewFileMenuItem, outputOpenFileMenuItem;
    @FXML
    MenuItem outputHereMenuItem, shutFileMenuItem, shutLastMenuItem, loadPackagesMenuItem;
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
    private RadioMenuItem noColouredIORadioButton, modeColouredIORadioButton, redfrontColouredIORadioButton;
    @FXML
    RadioMenuItem singlePaneRadioButton;
    @FXML
    private RadioMenuItem splitPaneRadioButton, tabbedPaneRadioButton;
    @FXML
    MenuItem addTabMenuItem;
    // Templates menu:
    @FXML
    Menu templatesMenu;
    // Help menu:
    @FXML
    private Menu helpMenu;

    private static final File USER_HOME_DIR = new File(getProperty("user.home"));
    private static final File PACKAGES_DIR = new File(RunREDUCE.reduceConfiguration.packagesRootDir, "packages");

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
    static HostServices hostServices;
    static Desktop desktop;

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
        addTabMenuItem.setDisable(RRPreferences.displayPane != RRPreferences.DisplayPane.TABBED);

        /* ********* *
         * Help menu *
         * ********* */

        // This code causes the following warning on Ubuntu 18.04.4 LTS:
        // (java:6572): Gdk-WARNING **: 17:06:21.689: XSetErrorHandler() called with a GDK error trap pushed. Don't do that.
        // It is avoided by using the JVM option -Djdk.gtk.version=2, which also makes dialogs behave better.
        if (!RRPreferences.windowsOS &&
                (!Desktop.isDesktopSupported()
                        || !(desktop = Desktop.getDesktop()).isSupported(Desktop.Action.OPEN)))
            desktop = null;

        String[][] manuals = {
                // {Manual name, Windows location, non-Windows location}
                {"REDUCE Manual (HTML)", "lib/csl/reduce.doc/manual.html", "manual.html"},
                {"REDUCE Manual (PDF)", "lib/csl/reduce.doc/manual.pdf", "manual.pdf.gz"},
                {"Inside Reduce", "doc/insidereduce.pdf", "insidereduce.pdf.gz"},
                {"REDUCE Symbolic Mode Primer", "doc/primer.pdf", "primer.pdf.gz"},
                {"Standard Lisp Report", "doc/sl.pdf", "sl.pdf.gz"}
        };

        int helpMenuItemIndex = 2;
        for (int i = 0; i < manuals.length; i++) {
            String[] manual = manuals[i];
            if (i == 0 || RRPreferences.windowsOS) {
                MenuItem menuItem = new MenuItem(manual[0]);
                helpMenu.getItems().add(helpMenuItemIndex++, menuItem);
                menuItem.setOnAction(e -> hostServices.showDocument(
                        new File(RunREDUCE.reduceConfiguration.docRootDir,
                                manual[RRPreferences.windowsOS ? 1 : 2]).toString()));
            } else {
                if (desktop == null) break;
                MenuItem menuItem = new MenuItem(manual[0]);
                helpMenu.getItems().add(helpMenuItemIndex++, menuItem);
                menuItem.setOnAction(e -> documentOpen(
                        new File(RunREDUCE.reduceConfiguration.docRootDir,
                                manual[2])));
            }
        }
    }

    /*
    * This method is based on https://www.tfzx.net/article/9082893.html
    * Thanks to Rainer Schöpf for the reference.
    */
    private void documentOpen(File manual) {
        final Task<Void> openFile = new Task<>() {
            @Override
            protected Void call() {
                try {
                    desktop.open(manual);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }
        };
        final Thread thread = new Thread(openFile);
        thread.setDaemon(true);
        thread.start();
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
            choiceDialog.initOwner(RunREDUCE.primaryStage);
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

    void runREDUCESubmenuBuild() {
        ObservableList<MenuItem> menuItems = runREDUCESubmenu.getItems();
        menuItems.clear();
        for (REDUCECommand cmd : RunREDUCE.reduceConfiguration.reduceCommandList) {
            MenuItem item = new MenuItem(cmd.name);
            menuItems.add(item);
            item.setOnAction(e -> RunREDUCE.reducePanel.run(cmd));
        }
    }

    void autoRunREDUCESubmenuBuild() {
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
        RunREDUCE.reducePanel.runningREDUCE = false;
        RunREDUCE.reducePanel.outputFileList.clear();
        // Reset enabled status of controls:
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
            case SINGLE:
                return;
            case SPLIT:
                RunREDUCE.useSplitPane(false);
                break;
            case TABBED:
                RunREDUCE.useTabPane(false);
                addTabMenuItem.setDisable(true);
        }
        RRPreferences.save(RRPreferences.DISPLAYPANE, RRPreferences.DisplayPane.SINGLE);
    }

    @FXML
    private void splitPaneRadioButtonAction() {
        if (RRPreferences.displayPane == RRPreferences.DisplayPane.TABBED) RunREDUCE.useTabPane(false);
        RRPreferences.save(RRPreferences.DISPLAYPANE, RRPreferences.DisplayPane.SPLIT);
        RunREDUCE.useSplitPane(true);
    }

    @FXML
    private void tabbedPaneRadioButtonAction() {
        if (RRPreferences.displayPane == RRPreferences.DisplayPane.SPLIT) RunREDUCE.useSplitPane(false);
        RRPreferences.save(RRPreferences.DISPLAYPANE, RRPreferences.DisplayPane.TABBED);
        RunREDUCE.useTabPane(true);
        addTabMenuItem.setDisable(false);
    }

    @FXML
    private void addTabMenuItemAction() {
        RunREDUCE.addTab();
    }

    /* ************** *
     * Templates menu *
     * ************** */

    @FXML
    private void expLogEtcMenuItemAction() {
        showDialogAndWait("Create an Exponential, Logarithm, Power, etc", "templates/ExpLogEtc.fxml", Modality.NONE);
    }

    @FXML
    private void derivativeMenuItemAction() {
        showDialogAndWait("Create a (Partial) Derivative", "templates/Derivative.fxml", Modality.NONE);
    }

    @FXML
    private void integralMenuItemAction() {
        showDialogAndWait("Create an Integral", "templates/Integral.fxml", Modality.NONE);
    }

    @FXML
    private void limitMenuItemAction() {
        showDialogAndWait("Create a Limit", "templates/Limit.fxml", Modality.NONE);
    }

    @FXML
    private void sumProdMenuItemAction() {
        showDialogAndWait("Create a Sum or Product", "templates/SumProd.fxml", Modality.NONE);
    }

    @FXML
    private void matrixMenuItemAction() {
        showDialogAndWait("Create a Matrix", "templates/Matrix.fxml", Modality.NONE);
    }

    @FXML
    private void solveMenuItemAction() {
        showDialogAndWait("Solve Equation(s)", "templates/Solve.fxml", Modality.NONE);
    }

    @FXML
    private void forMenuItemAction() {
        showDialogAndWait("Create a For Statement", "templates/For.fxml", Modality.NONE);
    }

    /* ********* *
     * Help menu *
     * ********* */

    private static final String USERGUIDE_FILENAME = "UserGuide.html";
    private static File file;

    @FXML
    private void userGuideMenuItemAction() {
        try {
            URL url = RunREDUCEFrame.class.getResource(USERGUIDE_FILENAME);
            // file:/C:/Users/franc/IdeaProjects/Run-REDUCE-FX/out/production/Run-REDUCE-FX/fjwright/runreduce/UserGuide.html
            // jar:file:/C:/Users/franc/IdeaProjects/Run-REDUCE-FX/out/artifacts/Run_REDUCE_FX_jar/Run-REDUCE-FX.jar!/fjwright/runreduce/UserGuide.html
            // JavaFX WebEngine accepts a jar URI but Firefox does not, so...
            if (url == null) {
                RunREDUCE.errorMessageDialog("Run-REDUCE-FX User Guide",
                        "Resource file \"" + USERGUIDE_FILENAME + "\" could not be located.");
            } else if (url.getProtocol().equals("file")) // Useful during development only!
                hostServices.showDocument(url.toString());
            else { // Normal case: when running a jar file the protocol is jar.
                if (file == null || !file.exists()) {
                    file = new File(getProperty("java.io.tmpdir"), USERGUIDE_FILENAME);
                    try (InputStream in = url.openStream()) {
                        Files.copy(in, file.toPath(), REPLACE_EXISTING);
                    }
                }
                hostServices.showDocument(file.toString());
            }
        } catch (IOException exc) {
            exc.printStackTrace();
        }
    }

    @FXML
    private void aboutMenuItemAction() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Version 1.32, July 2020\n" +
                        "\u00A9 2020 Francis Wright");
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.initOwner(RunREDUCE.primaryStage);
        alert.setTitle("About Run-REDUCE-FX");
        alert.setHeaderText("Run REDUCE in a JavaFX GUI");
        alert.showAndWait();
    }

    /* *************** *
     * Support methods *
     * *************** */

    void showDialogAndWait(String dialogTitle, String fxmlFileName, Modality... modality) {
        Parent root;
        try {
            root = FXMLLoader.load(getClass().getResource(fxmlFileName));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Stage stage = new Stage();
        if (modality.length == 0) {
            stage.initModality(Modality.APPLICATION_MODAL);
        } else {
            stage.initModality(modality[0]);
        }
        stage.initOwner(RunREDUCE.primaryStage);
        stage.setTitle(dialogTitle);
        stage.setScene(new Scene(root));
        stage.showAndWait();
    }
}
