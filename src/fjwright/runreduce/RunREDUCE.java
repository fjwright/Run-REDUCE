package fjwright.runreduce;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * This is the main class that sets up and runs the application.
 **/
public class RunREDUCE extends Application {
    static RunREDUCEFrame runREDUCEFrame;
    static Stage primaryStage;
    static REDUCEPanel reducePanel; // the REDUCEPanel with current focus

    static REDUCEConfigurationDefault reduceConfigurationDefault;
    static REDUCEConfiguration reduceConfiguration;

    @Override
    public void start(Stage primaryStage) throws Exception {
        RunREDUCE.primaryStage = primaryStage;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("RunREDUCEFrame.fxml"));
        Parent root = fxmlLoader.load();
        runREDUCEFrame = fxmlLoader.getController();
        primaryStage.setTitle("Run-REDUCE-FX");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();

        runREDUCEFrame.frame.setCenter(reducePanel = new REDUCEPanel());
        // Reset menu item status as appropriate when REDUCE is not running:
        runREDUCEFrame.reduceStopped();
    }

    static void errorMessageDialog(String message, String title) {
//        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE);
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle(title);
        alert.showAndWait();
    }

    // Run-time argument processing:
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
