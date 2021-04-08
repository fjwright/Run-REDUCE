package fjwright.runreduce;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLElement;

import java.awt.Desktop; // only Desktop needed
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
    MenuItem outputHereMenuItem, shutFileMenuItem, shutLastMenuItem;
    // REDUCE menu:
    @FXML
    MenuItem loadPackagesMenuItem, stopREDUCEMenuItem, restartREDUCEMenuItem, killREDUCEMenuItem;
    @FXML
    Menu runREDUCESubmenu;
    @FXML
    private Menu autoRunREDUCESubmenu;
    // View menu:
    @FXML
    CheckMenuItem boldPromptsCheckMenuItem, colouredIOCheckMenuItem, typesetMathsCheckMenuItem, syncScrollCheckMenuItem;
    @FXML
    RadioMenuItem singlePaneRadioButton;
    @FXML
    private RadioMenuItem splitPaneRadioButton, tabbedPaneRadioButton;
    @FXML
    MenuItem addTabMenuItem;
    // Options menu:
    @FXML
    CheckMenuItem hideMenuHistoryCheckMenuItem, popupQueriesCheckMenuItem;
    // Templates and Functions menus:
    @FXML
    Menu templatesMenu, functionsMenu;
    // Help menu:
    @FXML
    private Menu helpMenu;
    // Debugging Menu:
    @FXML
    private Menu debuggingMenu;
    @FXML
    CheckMenuItem showTeXMarkupCheckMenuItem;

    private static final File PACKAGES_DIR = new File(RunREDUCE.reduceConfiguration.packagesDir);

    static final FileChooser fileChooser = new FileChooser();
    private static final FileChooser.ExtensionFilter INPUT_FILE_FILTER =
            new FileChooser.ExtensionFilter("REDUCE Input Files (*.red, *.tst)", "*.red", "*.tst");
    private static final FileChooser.ExtensionFilter LOG_FILE_FILTER =
            new FileChooser.ExtensionFilter("REDUCE Log Files (*.log, *.rlg)", "*.log", "*.rlg");
    private static final FileChooser.ExtensionFilter TEXT_FILE_FILTER =
            new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt");
    private static final FileChooser.ExtensionFilter ALL_FILE_FILTER =
            new FileChooser.ExtensionFilter("All Files", "*.*");
    private static final FileChooser.ExtensionFilter HTML_FILE_FILTER =
            new FileChooser.ExtensionFilter("HTML Files (*.html)", "*.html");

    static List<String> packageList;
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

        fileChooser.setInitialDirectory(new File(RunREDUCE.reduceConfiguration.workingDir));

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

        boldPromptsCheckMenuItem.setSelected(RRPreferences.boldPromptsState);
        colouredIOCheckMenuItem.setSelected(RRPreferences.colouredIOState);
        typesetMathsCheckMenuItem.setSelected(RRPreferences.typesetMathsState);
        singlePaneRadioButton.setSelected(RRPreferences.displayPane == RRPreferences.DisplayPane.SINGLE);
        splitPaneRadioButton.setSelected(RRPreferences.displayPane == RRPreferences.DisplayPane.SPLIT);
        tabbedPaneRadioButton.setSelected(RRPreferences.displayPane == RRPreferences.DisplayPane.TABBED);
        syncScrollCheckMenuItem.setDisable(RRPreferences.displayPane != RRPreferences.DisplayPane.SPLIT);
        addTabMenuItem.setDisable(RRPreferences.displayPane != RRPreferences.DisplayPane.TABBED);

        /* ********* *
         * Help menu *
         * ********* */

        // This code causes the following warning on Ubuntu 18.04.4 LTS:
        // (java:6572): Gdk-WARNING **: 17:06:21.689: XSetErrorHandler() called with a GDK error trap pushed. Don't do that.
        // It is avoided by using the JVM option -Djdk.gtk.version=2, which also makes dialogs behave better.
        if (!REDUCEConfiguration.windowsOS &&
                (!Desktop.isDesktopSupported()
                        || !(desktop = Desktop.getDesktop()).isSupported(Desktop.Action.OPEN)))
            desktop = null;

        String[][] manuals = { // FixMe Can this be handled more elegantly?
                // {Manual name, Windows filename, non-Windows filename}
                {"REDUCE Manual (HTML)", "manual.html", "manual.html"},
                {"REDUCE Manual (PDF)", "manual.pdf", "manual.pdf.gz"},
                {"Inside Reduce", "insidereduce.pdf", "insidereduce.pdf.gz"},
                {"REDUCE Symbolic Mode Primer", "primer.pdf", "primer.pdf.gz"},
                {"Standard Lisp Report", "sl.pdf", "sl.pdf.gz"}
        };

        int helpMenuItemIndex = 2;
        for (int i = 0; i < manuals.length; i++) {
            String[] manual = manuals[i];
            String dir = (i < 2) ? RunREDUCE.reduceConfiguration.manualDir :
                    RunREDUCE.reduceConfiguration.primersDir;
            if (i == 0 || REDUCEConfiguration.windowsOS) {
                MenuItem menuItem = new MenuItem(manual[0]);
                helpMenu.getItems().add(helpMenuItemIndex++, menuItem);
                menuItem.setOnAction(e -> RunREDUCE.hostServices.showDocument(
                        new File(dir,
                                manual[REDUCEConfiguration.windowsOS ? 1 : 2]).toString()));
            } else {
                if (desktop == null) break;
                MenuItem menuItem = new MenuItem(manual[0]);
                helpMenu.getItems().add(helpMenuItemIndex++, menuItem);
                menuItem.setOnAction(e -> documentOpen(
                        new File(dir, manual[2])));
            }
        }

        /* ************** *
         * Debugging menu *
         * ************** */

        debuggingMenu.setVisible(RunREDUCE.debugMenu);
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
        inputFile(true);
    }

    // Input from Package Files...
    @FXML
    private void inputPackageFileMenuItemAction() {
        fileChooser.setTitle("Input from Package Files...");
        File oldInitialDirectory = fileChooser.getInitialDirectory();
        fileChooser.setInitialDirectory(PACKAGES_DIR);
        inputFile(false);
        fileChooser.setInitialDirectory(oldInitialDirectory);
    }

    private void inputFile(boolean saveDir) {
        fileChooser.getExtensionFilters().setAll(INPUT_FILE_FILTER, TEXT_FILE_FILTER, ALL_FILE_FILTER);
        List<File> fileList = fileChooser.showOpenMultipleDialog(null);
        if (fileList != null) {
            StringBuilder text = new StringBuilder("in \"");
            text.append(fileList.get(0).toString()); // first file
            for (File file : fileList.subList(1, fileList.size())) { // remaining files
                text.append("\", \"");
                text.append(file.toString());
            }
            text.append(echoCheckMenuItem.isSelected() ? "\";\n" : "\"$\n");
            RunREDUCE.reducePanel.menuSendStringToREDUCEAndEcho(text.toString());
            if (saveDir) fileChooser.setInitialDirectory(fileList.get(0).getParentFile());
        }
    }

    // Output to New File...
    @FXML
    private void outputNewFileMenuItemAction() {
        fileChooser.setTitle("Output to File...");
        fileChooser.getExtensionFilters().setAll(LOG_FILE_FILTER, TEXT_FILE_FILTER, ALL_FILE_FILTER);
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            RunREDUCE.reducePanel.menuSendStringToREDUCEAndEcho("out \"" + file.toString() + "\"$\n");
            RunREDUCE.reducePanel.outputFileList.remove(file); // in case it was already open
            RunREDUCE.reducePanel.outputFileList.add(file);
            RunREDUCE.reducePanel.outputFileDisableMenuItems(false);
            fileChooser.setInitialDirectory(file.getParentFile());
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
                RunREDUCE.reducePanel.menuSendStringToREDUCEAndEcho("out \"" + file.toString() + "\"$\n");
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
        RunREDUCE.reducePanel.menuSendStringToREDUCEAndEcho("out t$\n");
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
            RunREDUCE.reducePanel.menuSendStringToREDUCEAndEcho(
                    "shut \"" + RunREDUCE.reducePanel.outputFileList.remove(last).toString() + "\"$\n");
        }
        RunREDUCE.reducePanel.outputFileDisableMenuItemsMaybe();
    }

    // Print Session Log...
    @FXML
    private void printLogMenuItemAction() {
        PrinterJob printerJob = PrinterJob.createPrinterJob();
        if (printerJob != null) {
            if (printerJob.showPrintDialog(frame.getScene().getWindow())) {
                RunREDUCE.reducePanel.webEngine.print(printerJob);
                if (printerJob.endJob()) {
                    RunREDUCE.alert(Alert.AlertType.INFORMATION,
                            "Print Session Log...",
                            "Session log sent to printer.");
                } else
                    RunREDUCE.alert(Alert.AlertType.ERROR,
                            "Print Session Log...",
                            "Failed to send session log to printer!");
            }
        } else
            RunREDUCE.alert(Alert.AlertType.ERROR, "Print Session Log...",
                    "Cannot create printer job. Check that you have a printer installed!");
    }

    // Save Session Log...
    @FXML
    private void saveLogMenuItemAction() {
        saveLog(false, false);
    }

    // Append Session Log...
    @FXML
    private void appendLogMenuItemAction() {
        saveLog(true, false);
    }

    private void saveLog(boolean append, boolean raw) {
        fileChooser.setInitialFileName(raw ? "raw-session-log.html" : "session.log");
        if (raw) fileChooser.getExtensionFilters().setAll(HTML_FILE_FILTER);
        else fileChooser.getExtensionFilters().setAll(LOG_FILE_FILTER, TEXT_FILE_FILTER, ALL_FILE_FILTER);
        File file;
        if (append && !raw) {
            fileChooser.setTitle("Append Session Log...");
            file = fileChooser.showOpenDialog(null);
        } else {
            fileChooser.setTitle(raw ? "Save Raw Session Log as HTML..." : "Save Session Log...");
            file = fileChooser.showSaveDialog(null);
        }
        if (file != null) {
            try (Writer out = new BufferedWriter(new FileWriter(file, append))) {
                if (raw) {
                    out.write((String)
                            RunREDUCE.reducePanel.webEngine.executeScript("document.documentElement.outerHTML"));
                } else {
                    /*
                     * The <body> content should look like repeats of this structure:
                     * <pre class=inputCSSClass><span class="prompt">Prompt</span>REDUCE input</pre>
                     * <pre class=outputCSSClass>REDUCE output</pre> if non-typeset, or
                     * <div class=outputCSSClass><span class="katex-display">KaTeX output</div> if typeset
                     */
                    NodeList nodeList = RunREDUCE.reducePanel.body.getChildNodes();
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        HTMLElement el = (HTMLElement) nodeList.item(i);
                        if (el.getTagName().equals("DIV")) {
                            // <div> containing MathML with TeX annotation.
                            out.write("\n");
                            out.write(el.getElementsByTagName("annotation").item(0).getTextContent());
                            out.write("\n\n");
                        } else
                            // Filter out ^A-^D in case redfront mode turned on then off.
                            out.write(el.getTextContent().replaceAll("[\u0001-\u0004]", "")); // FixMe Use an explicit matcher.
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            fileChooser.setInitialDirectory(file.getParentFile());
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
                            if (version.equals(cmd1.name)) {
                                RunREDUCE.reducePanel.run(cmd1);
                                break;
                            }
                        }
                    }
                }
            });
        }
    }

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

    @FXML
    private void stopREDUCEMenuItemAction() {
        RunREDUCE.reducePanel.stop();
    }

    @FXML
    private void clearDisplayMenuItemAction() {
        RunREDUCE.reducePanel.clearDisplay();
    }

    @FXML
    private void restartREDUCEMenuItemAction() {
        RunREDUCE.reducePanel.restart();
    }

    @FXML
    private void configureREDUCEMenuItemAction() {
        showDialogAndWait("Configure REDUCE Directories and Commands",
                "REDUCEConfigDialog.fxml");
    }

    @FXML
    private void killREDUCEMenuItemAction() {
        RunREDUCE.reducePanel.kill();
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
        RRPreferences.save(RRPreferences.BOLDPROMPTS, boldPromptsCheckMenuItem.isSelected());
    }

    @FXML
    private void colouredIOCheckBoxAction() {
        RRPreferences.save(RRPreferences.COLOUREDIO, colouredIOCheckMenuItem.isSelected());
    }

    @FXML
    private void fontColorsMenuItemAction() {
        showDialogAndWait("Font Colours...", "FontColorsDialog.fxml");
    }

    @FXML
    private void typesetMathsCheckBoxAction() {
        RRPreferences.save(RRPreferences.TYPESET_MATHS, typesetMathsCheckMenuItem.isSelected());
    }

    @FXML
    private void singlePaneRadioButtonAction() {
        switch (RRPreferences.displayPane) {
            case SINGLE:
                return;
            case SPLIT:
                RunREDUCE.useSplitPane(false, false);
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
        RunREDUCE.useSplitPane(true, false);
    }

    @FXML
    private void tabbedPaneRadioButtonAction() {
        if (RRPreferences.displayPane == RRPreferences.DisplayPane.SPLIT) RunREDUCE.useSplitPane(false, false);
        RRPreferences.save(RRPreferences.DISPLAYPANE, RRPreferences.DisplayPane.TABBED);
        RunREDUCE.useTabPane(true);
        addTabMenuItem.setDisable(false);
    }

    @FXML
    private void syncScrollCheckMenuItemAction() {
        RunREDUCE.setUseSplitPaneSyncScroll(syncScrollCheckMenuItem.isSelected());
    }

    @FXML
    private void addTabMenuItemAction() {
        RunREDUCE.addTab();
    }

    /* ************** *
     * Templates menu *
     * ************** */

    @FXML
    private void derivativeMenuItemAction() {
        showDialogAndWait("Evaluate a Multiple (Partial) Derivative", "templates/Derivative.fxml", Modality.NONE);
    }

    @FXML
    private void integralMenuItemAction() {
        showDialogAndWait("Evaluate a Multiple Integral", "templates/Integral.fxml", Modality.NONE);
    }

    @FXML
    private void limitMenuItemAction() {
        showDialogAndWait("Evaluate a Limit of a Function", "templates/Limit.fxml", Modality.NONE);
    }

    @FXML
    private void sumProdMenuItemAction() {
        showDialogAndWait("Evaluate a Sum or Product", "templates/SumProd.fxml", Modality.NONE);
    }

    @FXML
    private void matrixMenuItemAction() {
        showDialogAndWait("Create a Matrix", "templates/Matrix.fxml", Modality.NONE);
    }

    @FXML
    private void solveMenuItemAction() {
        showDialogAndWait("Solve Algebraic Equations", "templates/Solve.fxml", Modality.NONE);
    }

    @FXML
    private void forMenuItemAction() {
        showDialogAndWait("Execute a For Statement", "templates/For.fxml", Modality.NONE);
    }

    /* ************** *
     * Functions menu *
     * ************** */

    @FXML
    private void expLogEtcMenuItemAction() {
        showDialogAndWait("Exponentials, Logarithms, Powers, Roots, etc",
                "functions/ExpLogEtc.fxml", Modality.NONE);
    }

    @FXML
    private void gammaEtcMenuItemAction() {
        showDialogAndWait("Gamma, Beta and Related Functions",
                "functions/GammaEtc.fxml", Modality.NONE);
    }

    @FXML
    private void integralFunctionsMenuItemAction() {
        showDialogAndWait("Integral Functions",
                "functions/IntegralFunctions.fxml", Modality.NONE);
    }

    @FXML
    private void airyBesselMenuItemAction() {
        showDialogAndWait("Airy, Bessel and Related Functions",
                "functions/AiryBessel.fxml", Modality.NONE);
    }

    @FXML
    private void struveEtcMenuItemAction() {
        showDialogAndWait("Struve, Lommel, Kummer, Whittaker and Spherical Harmonic Functions",
                "functions/StruveEtc.fxml", Modality.NONE);
    }

    @FXML
    private void orthoPolyMenuItemAction() {
        showDialogAndWait("Classical Orthogonal Polynomials",
                "functions/OrthoPoly.fxml", Modality.NONE);
    }

    /* ********* *
     * Help menu *
     * ********* */

    private static final String USERGUIDE_FILENAME = "UserGuide.html";
    private static File userGuideTmpFile;

    @FXML
    private void userGuideMenuItemAction() {
        try {
            URL url = RunREDUCEFrame.class.getResource(USERGUIDE_FILENAME);
            // file:/C:/Users/franc/IdeaProjects/Run-REDUCE/out/production/Run-REDUCE/fjwright/runreduce/UserGuide.html
            // jar:file:/C:/Users/franc/IdeaProjects/Run-REDUCE/out/artifacts/Run_REDUCE_FX_jar/Run-REDUCE.jar!/fjwright/runreduce/UserGuide.html
            // JavaFX WebEngine accepts a jar URI but Firefox does not, so...
            if (url == null) {
                RunREDUCE.alert(Alert.AlertType.ERROR, "Run-REDUCE User Guide",
                        "Resource file \"" + USERGUIDE_FILENAME + "\" could not be located.");
            } else if (url.getProtocol().equals("file")) // Useful during development only!
                RunREDUCE.hostServices.showDocument(url.toString());
            else { // Normal case: when running a jar file the protocol is jar.
                if (userGuideTmpFile == null || !userGuideTmpFile.exists()) {
                    userGuideTmpFile = new File(getProperty("java.io.tmpdir"), USERGUIDE_FILENAME);
                    try (InputStream in = url.openStream()) {
                        Files.copy(in, userGuideTmpFile.toPath(), REPLACE_EXISTING);
                    }
                }
                RunREDUCE.hostServices.showDocument(userGuideTmpFile.toString());
            }
        } catch (IOException exc) {
            exc.printStackTrace();
        }
    }

    @FXML
    private void webSiteMenuItemAction() {
        RunREDUCE.hostServices.showDocument("https://reduce-algebra.sourceforge.io/");
    }

    @FXML
    private void sourceForgeMenuItemAction() {
        RunREDUCE.hostServices.showDocument("https://sourceforge.net/projects/reduce-algebra/");
    }

    @FXML
    private void aboutMenuItemAction() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setHeaderText(String.format(
                "Run REDUCE in a JavaFX GUI\n" +
                        "Version %s, %s\n" +
                        "%s", Version.VERSION, Version.DATE, Version.COPYRIGHT));
        alert.setContentText("Typeset maths by KaTeX.org\n\n" +
                Version.JAVA);
        alert.setTitle("About Run-REDUCE");
        ImageView img = new ImageView(RunREDUCE.RRicon128Image);
        img.setFitWidth(64);
        img.setPreserveRatio(true);
        img.setSmooth(true);
        img.setCache(true);
        alert.setGraphic(img);
        alert.showAndWait();
    }

    /* ************** *
     * Debugging menu *
     * ************** */

    // Save Raw Session Log...
    @FXML
    private void saveRawDisplayMenuItemAction() {
        saveLog(false, true);
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
        stage.setTitle(dialogTitle);
        stage.setScene(new Scene(root));
        stage.showAndWait();
    }
}
