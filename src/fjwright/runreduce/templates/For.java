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

import java.util.regex.Pattern;

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

    private static final Pattern VAR_PATTERN = Pattern.compile("(!|\\p{Alpha}).*");

    private void varCheckKeyTyped(TextField varTextField) {
        String text = varTextField.getText();
        if (!(text.isEmpty() || VAR_PATTERN.matcher(text).matches())) {
            RunREDUCE.errorMessageDialog("For Template",
                    "A 'var' entry must be an identifier.");
            varTextField.setText("");
        }
    }

    @FXML
    private void forVarCheckKeyTyped() {
        varCheckKeyTyped(forVarTextField);
    }

    @FXML
    private void foreachVarCheckKeyTyped() {
        varCheckKeyTyped(foreachVarTextField);
    }

    private static final Pattern LIST_OR_VAR_PATTERN = Pattern.compile("([{!]|\\p{Alpha}).*");

    @FXML
    private void listOrVarCheckKeyTyped() {
        String text = foreachListTextField.getText();
        if (!(text.isEmpty() || LIST_OR_VAR_PATTERN.matcher(text).matches())) {
            RunREDUCE.errorMessageDialog("For Template",
                    "The 'list' entry must be a list or a variable that evaluates to a list.");
            foreachListTextField.setText("");
        }
    }

    private static class EmptyFieldException extends Exception {
    }

    private String getTextCheckNonEmpty(final TextField textField) throws EmptyFieldException {
        final String text = textField.getText().trim();
        if (text.isEmpty()) {
            RunREDUCE.errorMessageDialog("For Template",
                    "All entries except the step size must be non-empty.");
            throw new EmptyFieldException();
        } else
            return text;
    }

    private String result() throws EmptyFieldException {
        var text = new StringBuilder();
        if (tabPane.getSelectionModel().getSelectedIndex() == 0) {
            // Iterate over a numerical range:
            text.append("for ").append(getTextCheckNonEmpty(forVarTextField))
                    .append(" := ").append(getTextCheckNonEmpty(fromTextField));
            String step = stepTextField.getText().trim();
            if (step.isEmpty() || step.equals("1"))
                text.append(" : ");
            else
                text.append(" step ").append(step).append(" until ");
            text.append(getTextCheckNonEmpty(untilTextField));
        } else  // Iterate over a list:
            text.append("foreach ").append(getTextCheckNonEmpty(foreachVarTextField))
                    .append(" ").append(foreachChoiceBox.getSelectionModel().getSelectedItem())
                    .append(" ").append(getTextCheckNonEmpty(foreachListTextField));
        text.append(" ").append(actionChoiceBox.getSelectionModel().getSelectedItem())
                .append(" ").append(getTextCheckNonEmpty(exprnTextField));
        return text.toString();
    }

    @FXML
    private void editButtonAction(ActionEvent actionEvent) {
        // Insert in input editor if valid:
        final TextArea textArea = RunREDUCE.reducePanel.inputTextArea;
        try {
            textArea.insertText(textArea.getCaretPosition(), result());
            // Close dialogue:
            cancelButtonAction(actionEvent);
        } catch (EmptyFieldException ignored) {
        }
    }

    @FXML
    private void evaluateButtonAction(ActionEvent actionEvent) {
        // Send to REDUCE if valid:
        try {
            RunREDUCE.reducePanel.sendStringToREDUCEAndEcho(result() + ";\n");
            // Close dialogue:
            cancelButtonAction(actionEvent);
        } catch (EmptyFieldException ignored) {
        }
    }

    @FXML
    private void cancelButtonAction(ActionEvent actionEvent) {
        // Close dialogue:
        Node source = (Node) actionEvent.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
