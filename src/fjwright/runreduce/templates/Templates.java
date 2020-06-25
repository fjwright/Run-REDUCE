package fjwright.runreduce.templates;

import fjwright.runreduce.RunREDUCE;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.regex.Pattern;

/**
 * This class provides common facilities for the specific template classes.
 */
class Templates {

// Check field entries dynamically ================================================================

    private static final Pattern VAR_PATTERN = Pattern.compile("(?:!|\\p{Alpha}).*");  // non-capturing group

    static void varCheckKeyTyped(TextField textField) {
        String text = textField.getText();
        if (!(text.isEmpty() || VAR_PATTERN.matcher(text).matches())) {
            RunREDUCE.errorMessageDialog("Template Error",
                    "This field must be an identifier.");
            textField.setText("");
        }
    }

    private static final Pattern INT_OR_VAR_PATTERN = Pattern.compile("(?:[+-]?\\d+)|(?:!|\\p{Alpha}).*");  // non-capturing groups

    static void intOrVarCheckKeyTyped(TextField textField) {
        String text = textField.getText();
        if (!(text.isEmpty() || INT_OR_VAR_PATTERN.matcher(text).matches())) {
            RunREDUCE.errorMessageDialog("Template Error",
                    "This field must be an integer or an identifier.");
            textField.setText("");
        }
    }

// Process the result =============================================================================

    static class EmptyFieldException extends Exception {
    }

    static String getTextCheckNonEmpty(final TextField textField) throws EmptyFieldException {
        final String text = textField.getText().trim();
        if (text.isEmpty()) {
            RunREDUCE.errorMessageDialog("Template Error",
                    "All fields must be non-empty.");
            throw new EmptyFieldException();
        } else
            return text;
    }

    static void editButtonAction(ActionEvent actionEvent, String result) {
        // Insert in input editor if valid:
        final TextArea textArea = RunREDUCE.reducePanel.inputTextArea;
        textArea.insertText(textArea.getCaretPosition(), result);
        // Close dialogue:
        cancelButtonAction(actionEvent);
    }

    static void evaluateButtonAction(ActionEvent actionEvent, String result) {
        // Send to REDUCE if valid:
        RunREDUCE.reducePanel.sendStringToREDUCEAndEcho(result + ";\n");
        // Close dialogue:
        cancelButtonAction(actionEvent);
    }

    static void cancelButtonAction(ActionEvent actionEvent) {
        // Close dialogue:
        Node source = (Node) actionEvent.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
