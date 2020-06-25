package fjwright.runreduce.templates;

import fjwright.runreduce.RunREDUCE;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

import java.util.regex.Pattern;

public class SumProd {
    @FXML
    private ToggleButton sumToggleButton;
    @FXML
    private Label sumProdLabel;
    @FXML
    private TextField varTextField, lowLimTextField, upLimTextField, operandTextField;

    public void sumToggleButtonAction() {
        sumProdLabel.setText("∑");
    }

    public void prodToggleButtonAction() {
        sumProdLabel.setText("∏");
    }

    private static final Pattern VAR_PATTERN = Pattern.compile("(!|\\p{Alpha}).*");

    @FXML
    private void varCheckKeyTyped() {
        String text = varTextField.getText();
        if (!(text.isEmpty() || VAR_PATTERN.matcher(text).matches())) {
            RunREDUCE.errorMessageDialog("Sum or Product Template",
                    "The control variable must be an identifier.");
            varTextField.setText("");
        }
    }

    private static final Pattern NUM_OR_VAR_PATTERN = Pattern.compile("([+-]?\\d+)|(!|\\p{Alpha}).*");

    private void numOrVarCheckKeyTyped(TextField textField) {
        String text = textField.getText();
        if (!(text.isEmpty() || NUM_OR_VAR_PATTERN.matcher(text).matches())) {
            RunREDUCE.errorMessageDialog("Sum or Product Template",
                    "Each limit must be an integer or an identifier.");
            textField.setText("");
        }
    }

    @FXML
    private void upLimCheckKeyTyped() {
        numOrVarCheckKeyTyped(upLimTextField);
    }

    @FXML
    private void lowLimCheckKeyTyped() {
        numOrVarCheckKeyTyped(lowLimTextField);
    }

    private static class EmptyFieldException extends Exception {
    }

    private String getTextCheckNonEmpty(final TextField textField) throws EmptyFieldException {
        final String text = textField.getText().trim();
        if (text.isEmpty()) {
            RunREDUCE.errorMessageDialog("Sum or Product Template",
                    "All entries must be non-empty.");
            throw new EmptyFieldException();
        } else
            return text;
    }

    private String result() throws EmptyFieldException {
        var text = new StringBuilder();
        text.append("for ").append(getTextCheckNonEmpty(varTextField))
                .append(" := ").append(getTextCheckNonEmpty(lowLimTextField))
                .append(" : ").append(getTextCheckNonEmpty(upLimTextField));
        if (sumToggleButton.isSelected())
            text.append(" sum ");
        else
            text.append(" product ");
        text.append(getTextCheckNonEmpty(operandTextField));
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
