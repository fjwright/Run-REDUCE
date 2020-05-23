/*
 * RunREDUCE: A JavaFX GUI to run REDUCE
 * Compile and run from the PARENT directory of fjwright:
 * javac fjwright\runreduce\*.java
 * java fjwright.runreduce.RunREDUCE
 * (The above works in a Microsoft Windows cmd shell.)
 */

package fjwright.runreduce;

import javafx.application.Application;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * This is the main class that sets up and runs the application.
 **/
public class RunREDUCE extends Application {
    static RunREDUCEFrame runREDUCEFrame;
    static Stage primaryStage;
    static final String reduceFontFamilyName = "Consolas";
    static Font reduceFont, reduceFontBold;
    static SplitPane splitPane;
    static TabPane tabPane;
    static int tabLabelNumber = 1;
    static REDUCEPanel reducePanel; // the REDUCEPanel with current focus

    // Set the main window to 2/3 the linear dimension of the screen initially:
    static final Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
    static final double initialSceneFactor = 2.0 / 3.0;
    static Rectangle2D initialFrameSize = new Rectangle2D(
            primaryScreenBounds.getMinX(), primaryScreenBounds.getMinY(),
            primaryScreenBounds.getWidth() * initialSceneFactor,
            primaryScreenBounds.getHeight() * initialSceneFactor);

    static REDUCEConfigurationDefault reduceConfigurationDefault;
    static REDUCEConfiguration reduceConfiguration;

    @Override
    public void start(Stage primaryStage) throws Exception {
        RunREDUCE.primaryStage = primaryStage;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("RunREDUCEFrame.fxml"));
        Parent root = fxmlLoader.load();
        runREDUCEFrame = fxmlLoader.getController();
        primaryStage.setTitle("Run-REDUCE-FX");
        primaryStage.setScene(new Scene(root, initialFrameSize.getWidth(), initialFrameSize.getHeight()));
        primaryStage.show();

        // REDUCE I/O requires a monospaced font:
        // Only "system" fonts (in C:\Windows\Fonts) are found,
        // not "user" fonts (in C:\Users\franc\AppData\Local\Microsoft\Windows\Fonts).
        // ("DejaVu Sans Mono" is my only "user" font.)
        // ToDo Consider bundling a font as a resource.
        reduceFont = Font.font(reduceFontFamilyName, RRPreferences.fontSize);
        reduceFontBold = Font.font(reduceFontFamilyName, FontWeight.BOLD, RRPreferences.fontSize);
        if (debugPlatform) {
            System.err.println("reduceFont: " + reduceFont.toString());
            System.err.println("reduceFontBold: " + reduceFontBold.toString());
        }

        reducePanel = new REDUCEPanel();
        switch (RRPreferences.displayPane) {
            case SINGLE:
                runREDUCEFrame.frame.setCenter(reducePanel);
                break;
            case SPLIT:
                useSplitPane(true);
                break;
            case TABBED:
                useTabPane(true);
        }
    }

    static void useSplitPane(boolean enable) {
        if (enable) {
            REDUCEPanel reducePanel2 = new REDUCEPanel();
            splitPane = new SplitPane(reducePanel, reducePanel2);
            splitPane.setDividerPositions(0.5);
            runREDUCEFrame.frame.setCenter(splitPane);
            reducePanel.splitPane.setOnMouseClicked(RunREDUCE::useSplitPaneMouseClicked);
            reducePanel.inputTextArea.setOnMouseClicked(RunREDUCE::useSplitPaneMouseClicked);
            reducePanel2.splitPane.setOnMouseClicked(RunREDUCE::useSplitPaneMouseClicked);
            reducePanel2.inputTextArea.setOnMouseClicked(RunREDUCE::useSplitPaneMouseClicked);
            reducePanel2.setSelected(false);
        } else { // Revert to single pane.
            splitPane = null; // release resources
            reducePanel.splitPane.removeEventHandler(MouseEvent.MOUSE_CLICKED, RunREDUCE::useSplitPaneMouseClicked);
            reducePanel.inputTextArea.removeEventHandler(MouseEvent.MOUSE_CLICKED, RunREDUCE::useSplitPaneMouseClicked);
            // Retain the reducePanel from the selected tab if possible:
            runREDUCEFrame.frame.setCenter(reducePanel);
        }
    }

    private static void useSplitPaneMouseClicked(MouseEvent event) {
        Node node = (Node) event.getSource();
        while (!(node instanceof REDUCEPanel)) node = node.getParent();
        if (node == reducePanel) return;
        reducePanel.setSelected(false);
        reducePanel = (REDUCEPanel) node;
        reducePanel.inputTextArea.requestFocus();
        reducePanel.updateMenus();
        reducePanel.setSelected(true);
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
            tabPane = null; // release resources
            // Retain the reducePanel from the selected tab:
            runREDUCEFrame.frame.setCenter(reducePanel);
        }
    }

    static void addTab() {
        Tab tab = new Tab("Tab " + (++tabLabelNumber), reducePanel = new REDUCEPanel());
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
        }
    }

    static void errorMessageDialog(String message, String title) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle(title);
        alert.showAndWait();
    }

    // Run-time argument processing ***************************************************************

    static boolean debugPlatform, debugOutput;
    private static final String debugPlatformArg = "-debugPlatform";
    private static final String debugOutputArg = "-debugOutput";

    public static void main(String[] args) {
        for (String arg : args) {
            switch (arg) {
                case debugPlatformArg:
                    debugPlatform = true;
                    break;
                case debugOutputArg:
                    debugOutput = true;
                    break;
                default:
                    System.err.format("Unrecognised argument: %s.\nAllowed arguments are: %s and %s.",
                            arg, debugPlatformArg, debugOutputArg);
            }
        }

        reduceConfigurationDefault = new REDUCEConfigurationDefault();
        reduceConfiguration = new REDUCEConfiguration();

        // Launch the JavaFX GUI:
        launch(args);
    }
}

// ToDo Ensure that (Control+Tab) focused REDUCE panel is active in split pane.
