package fjwright.runreduce.templates;

import fjwright.runreduce.RunREDUCE;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class For {
    @FXML
    private TextField forVarTextField, fromTextField, stepTextField, untilTextField;
    @FXML
    private TextField foreachVarTextField, foreachListTextField, exprnTextField;
    @FXML
    private ChoiceBox foreachChoiceBox, actionChoiceBox;

    @FXML
    private void initialize() {
    }

    private String result() {
        return "For";
    }

    @FXML
    private void editButtonAction(ActionEvent actionEvent) {
        // Insert in input editor if valid:
        final TextArea textArea = RunREDUCE.reducePanel.inputTextArea;
        textArea.insertText(textArea.getCaretPosition(), result());
        // Close dialogue:
        cancelButtonAction(actionEvent);
    }

    @FXML
    private void evaluateButtonAction(ActionEvent actionEvent) {
        // Send to REDUCE if valid:
        RunREDUCE.reducePanel.sendStringToREDUCEAndEcho(result() + ";\n");
        // Close dialogue:
        cancelButtonAction(actionEvent);
    }

    @FXML
    private void cancelButtonAction(ActionEvent actionEvent) {
        // Close dialogue:
        Node source = (Node) actionEvent.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
