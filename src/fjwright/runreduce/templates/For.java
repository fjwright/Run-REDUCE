package fjwright.runreduce.templates;

import fjwright.runreduce.RunREDUCE;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class For {
    @FXML
    private TextField forVarTextField, fromTextField, stepTextField, untilTextField;
    @FXML
    private TextField foreachVarTextField, foreachListTextField, exprnTextField;
    @FXML
    private ChoiceBox<String> foreachChoiceBox, actionChoiceBox;
    @FXML
    private TabPane tabPane;

    @FXML
    private void initialize() {
        foreachChoiceBox.getSelectionModel().selectFirst();
        actionChoiceBox.getSelectionModel().selectFirst();
    }

    private String result() {
        var text = new StringBuilder();
        String forVar = forVarTextField.getText().trim(),
                foreachVar = foreachVarTextField.getText().trim(),
                step = stepTextField.getText().trim();
        if (tabPane.getSelectionModel().getSelectedIndex() == 0) {
            // Iterate over a numerical range:
            text.append("for ").append(forVar).append(" := ").append(fromTextField.getText());
            if (step.isEmpty() || step.equals("1"))
                text.append(" : ");
            else
                text.append(" step ").append(step).append(" until ");
            text.append(untilTextField.getText());
        } else  // Iterate over a list:
            text.append("foreach ").append(foreachVar)
                    .append(" ").append(foreachChoiceBox.getSelectionModel().getSelectedItem())
                    .append(" ").append(foreachListTextField.getText());
        text.append(" ").append(actionChoiceBox.getSelectionModel().getSelectedItem())
                .append(" ").append(exprnTextField.getText());
        return text.toString();
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
