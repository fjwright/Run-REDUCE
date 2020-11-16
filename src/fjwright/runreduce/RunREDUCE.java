/*
 * RunREDUCE: A JavaFX GUI to run REDUCE
 * Compile and run from the PARENT directory of fjwright:
 * javac fjwright\runreduce\*.java
 * java fjwright.runreduce.RunREDUCE
 * (The above works in a Microsoft Windows cmd shell.)
 */

package fjwright.runreduce;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is the main class that sets up and runs the application.
 */
public class RunREDUCE extends Application {
    static RunREDUCEFrame runREDUCEFrame;
    static String reduceFontFamilyName;
    static SplitPane splitPane;
    static TabPane tabPane;
    static int tabLabelNumber = 1;
    public static REDUCEPanel reducePanel; // the REDUCEPanel with current focus
    static List<REDUCEPanel> reducePanelList = new ArrayList<>();

    // Set the main window to 2/3 the linear dimension of the screen initially:
    static final Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
    static final double initialSceneFactor = 2.0 / 3.0;
    static Rectangle2D initialFrameSize = new Rectangle2D(
            primaryScreenBounds.getMinX(), primaryScreenBounds.getMinY(),
            primaryScreenBounds.getWidth() * initialSceneFactor,
            primaryScreenBounds.getHeight() * initialSceneFactor);

    static REDUCEConfigurationDefault reduceConfigurationDefault;
    public static REDUCEConfiguration reduceConfiguration;

    public static HostServices hostServices;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("RunREDUCEFrame.fxml"));
        Parent root = fxmlLoader.load();
        runREDUCEFrame = fxmlLoader.getController();
        primaryStage.setTitle(String.format("Run-REDUCE (v%s)", RunREDUCEFrame.VERSION));
        primaryStage.setScene(new Scene(root, initialFrameSize.getWidth(), initialFrameSize.getHeight()));
        primaryStage.show();
        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(375);
        primaryStage.getIcons().addAll(
                new Image(getClass().getResourceAsStream("resources/images/icons8-r-64.png")));

        // REDUCE I/O requires a monospaced font:
        // Only "system" fonts (in C:\Windows\Fonts) are found, not
        // "user" fonts (in C:\Users\franc\AppData\Local\Microsoft\Windows\Fonts).
        // ("DejaVu Sans Mono" is my only "user" font.)
        // ToDo Consider bundling a font as a resource.
        reduceFontFamilyName = REDUCEConfiguration.windowsOS ? "Consolas" : "DejaVu Sans Mono";
        Font reduceFont = Font.font(reduceFontFamilyName, RRPreferences.fontSize);
        if (!reduceFont.getFamily().equals(reduceFontFamilyName))
            alert(Alert.AlertType.WARNING, "REDUCE I/O Font",
                    String.format(
                            "Specified font '%s' not found."
                                    + "\nReplacement font '%s' used instead."
                                    + "\nBeware that REDUCE output may be mangled!",
                            reduceFontFamilyName, reduceFont.getFamily()));
        if (debugPlatform) System.err.println("reduceFont: " + reduceFont.toString());

        reducePanel = new REDUCEPanel();
        reducePanelList.add(reducePanel);
        switch (RRPreferences.displayPane) {
            case SINGLE:
                runREDUCEFrame.frame.setCenter(reducePanel);
                break;
            case SPLIT:
                useSplitPane(true, true);
                break;
            case TABBED:
                useTabPane(true);
        }

        hostServices = getHostServices();
    }

    static void useSplitPane(boolean enable, boolean startup) {
        if (enable) {
            REDUCEPanel reducePanel2 = new REDUCEPanel();
            reducePanelList.add(reducePanel2);
            splitPane = new SplitPane(reducePanel, reducePanel2);
            splitPane.setDividerPositions(0.5);
            runREDUCEFrame.frame.setCenter(splitPane);
            reducePanel.addEventFilter(MouseEvent.MOUSE_CLICKED, RunREDUCE::useSplitPaneMouseClicked);
            reducePanel2.addEventFilter(MouseEvent.MOUSE_CLICKED, RunREDUCE::useSplitPaneMouseClicked);
            if (startup)
                reducePanel.setSelected(true);
            else {
                reducePanel.setSelected(false); // old panel
                reducePanel = reducePanel2; // new panel
                reducePanel.setSelected(true);
                reducePanel.updateMenus();
                reducePanel.inputTextArea.requestFocus();
            }
        } else { // Revert to single pane.
            reducePanelList.removeIf(x -> x != reducePanel);
            splitPane = null; // release resources
            reducePanel.removeEventFilter(MouseEvent.MOUSE_CLICKED, RunREDUCE::useSplitPaneMouseClicked);
            // Retain the reducePanel from the selected tab:
            runREDUCEFrame.frame.setCenter(reducePanel);
            reducePanel.activeLabel.setVisible(false);
        }
    }

    private static void useSplitPaneMouseClicked(MouseEvent event) {
        Node node = (Node) event.getSource();
        if (node == reducePanel) return;
        reducePanel.setSelected(false); // other panel
        reducePanel = (REDUCEPanel) node; // this panel
        reducePanel.setSelected(true);
        reducePanel.updateMenus();
        reducePanel.inputTextArea.requestFocus();
    }

    static void useTabPane(boolean enable) {
        if (enable) {
            tabPane = new TabPane();
            runREDUCEFrame.frame.setCenter(tabPane);
            tabLabelNumber = 1;
            Tab tab = new Tab(reducePanel.title != null ? reducePanel.title : "Tab 1", reducePanel);
            tabPane.getTabs().add(tab);
            tab.setOnSelectionChanged(RunREDUCE::tabOnSelectionChanged);
            tab.setOnClosed(RunREDUCE::tabOnClosed);
            if (reducePanel.runningREDUCE) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Platform.runLater(() -> reducePanel.inputTextArea.requestFocus());
            }
        } else { // Revert to single pane.
            reducePanelList.removeIf(x -> x != reducePanel);
            tabPane = null; // release resources
            // Retain the reducePanel from the selected tab:
            runREDUCEFrame.frame.setCenter(reducePanel);
        }
    }

    static void addTab() {
        Tab tab = new Tab("Tab " + (++tabLabelNumber), reducePanel = new REDUCEPanel());
        reducePanelList.add(reducePanel);
        tab.setOnSelectionChanged(RunREDUCE::tabOnSelectionChanged);
        tab.setOnClosed(RunREDUCE::tabOnClosed);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    private static void tabOnSelectionChanged(Event event) {
        Tab tab = (Tab) event.getTarget();
        if (tab.isSelected()) {
            reducePanel = (REDUCEPanel) tab.getContent();
            reducePanel.updateMenus();
            if (reducePanel.runningREDUCE)
                Platform.runLater(() -> reducePanel.inputTextArea.requestFocus());
        }
    }

    private static void tabOnClosed(Event event) {
        if (tabPane.getTabs().isEmpty()) {
            tabPane = null; // release resources
            // Retain the reducePanel from the selected tab:
            runREDUCEFrame.frame.setCenter(reducePanel);
            RRPreferences.save(RRPreferences.DISPLAYPANE, RRPreferences.DisplayPane.SINGLE);
            runREDUCEFrame.singlePaneRadioButton.setSelected(true);
            runREDUCEFrame.addTabMenuItem.setDisable(true);
        } else reducePanelList.remove(((Tab) event.getSource()).getContent());
    }

    /**
     * Display a standard modal JavaFX pop-up alert dialogue and wait for a response.
     * alertType can be Alert.AlertType.CONFIRMATION / ERROR / INFORMATION / NONE / WARNING.
     * title is most useful for INFORMATION / NONE.
     */
    public static void alert(Alert.AlertType alertType, String headerText, String contentText, String... title) {
        Alert alert = new Alert(alertType);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        if (title.length > 0) alert.setTitle(title[0]);
        alert.showAndWait();
    }

    // Run-time argument processing ***************************************************************

    static boolean debugPlatform, debugOutput, debugMenu;
    private static final String debugPlatformArg = "-debugPlatform";
    private static final String debugOutputArg = "-debugOutput";
    private static final String debugMenuArg = "-debugMenu";
    private static final String[] debugArgs = {debugPlatformArg, debugOutputArg, debugMenuArg};

    public static void main(String[] args) {
        for (String arg : args) {
            switch (arg) {
                case debugPlatformArg:
                    debugPlatform = true;
                    break;
                case debugOutputArg:
                    debugOutput = true;
                    break;
                case debugMenuArg:
                    debugMenu = true;
                    break;
                default:
                    System.err.format("Unrecognised argument: %s.\nAllowed arguments are: %s.",
                            arg, Arrays.toString(debugArgs));
            }
        }

        reduceConfigurationDefault = new REDUCEConfigurationDefault();
        reduceConfiguration = new REDUCEConfiguration();

        // Launch the JavaFX GUI:
        launch(args);
    }
}
