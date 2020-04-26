package fjwright.runreduce;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

/*
 * This class provides the panel that displays REDUCE input and output.
 * The run method runs REDUCE as a sub-process.
 */
public class REDUCEPanel extends BorderPane {

    public REDUCEPanel() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("REDUCEPanel.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
