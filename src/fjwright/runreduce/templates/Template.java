package fjwright.runreduce.templates;

import fjwright.runreduce.RunREDUCE;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.util.regex.Pattern;

/**
 * This is the base class for the specific template classes.
 */
abstract class Template {
    @FXML
    private Node templateRoot;

    @FXML
    protected void initialize() {
        templateRoot.addEventFilter(MouseEvent.MOUSE_CLICKED, this::middleMouseButtonClicked);
    }

    @FXML
    private void middleMouseButtonClicked(MouseEvent mouseEvent) {
        Node target;
        if (!(mouseEvent.getButton() == MouseButton.MIDDLE &&
                ((target = ((Node) mouseEvent.getTarget()).getParent())
                        instanceof TextField)))
            return;

        final var popup = new Popup();
        final BorderPane borderPane = new BorderPane();
        borderPane.setStyle("-fx-background-color: blue");
        popup.getContent().add(borderPane);

        final Button button = new Button("∞");
        button.setOnAction(actionEvent -> {
            ((TextField) target).setText("infinity");
            popup.hide();
        });
        borderPane.setCenter(button);

        popup.show(templateRoot.getScene().getWindow());
    }

// Check field entries dynamically ================================================================

    protected static final Pattern VAR_PATTERN =
            Pattern.compile("(?:!|\\p{Alpha}).*");  // non-capturing group

    @FXML
    private void varCheckKeyTyped(KeyEvent keyEvent) {
        final TextField textField = (TextField) keyEvent.getTarget();
        final String text = textField.getText();
        if (!(text.isEmpty() || VAR_PATTERN.matcher(text).matches())) {
            RunREDUCE.errorMessageDialog("Template Error",
                    "This field must be an identifier.");
            textField.setText("");
        }
    }

    private static final Pattern INT_OR_VAR_PATTERN =
            Pattern.compile("(?:[+-]?\\d*)|(?:!|\\p{Alpha}).*");  // non-capturing groups

    @FXML
    private void intOrVarCheckKeyTyped(KeyEvent keyEvent) {
        final TextField textField = (TextField) keyEvent.getTarget();
        final String text = textField.getText();
        if (!(text.isEmpty() || INT_OR_VAR_PATTERN.matcher(text).matches())) {
            RunREDUCE.errorMessageDialog("Template Error",
                    "This field must be an integer or an identifier.");
            textField.setText("");
        }
    }

// Process the result =============================================================================

    protected static class EmptyFieldException extends Exception {
    }

    protected static String getTextCheckNonEmpty(final TextField textField) throws EmptyFieldException {
        final String text = textField.getText().trim();
        if (text.isEmpty()) {
            RunREDUCE.errorMessageDialog("Template Error",
                    "A required field is empty.");
            throw new EmptyFieldException();
        } else
            return text;
    }

    abstract String result() throws EmptyFieldException;

    @FXML
    private void editButtonAction(ActionEvent actionEvent) {
        // Insert in input editor if valid:
        try {
            final TextArea textArea = RunREDUCE.reducePanel.inputTextArea;
            textArea.insertText(textArea.getCaretPosition(), result());
            // Close dialogue:
//            cancelButtonAction(actionEvent);
        } catch (EmptyFieldException ignored) {
        }
    }

    @FXML
    private void evaluateButtonAction(ActionEvent actionEvent) {
        // Send to REDUCE if valid:
        try {
            RunREDUCE.reducePanel.sendStringToREDUCEAndEcho(result() + ";\n");
            // Close dialogue:
//            cancelButtonAction(actionEvent);
        } catch (EmptyFieldException ignored) {
        }
    }

    @FXML
    private void closeButtonAction(ActionEvent actionEvent) {
        // Close dialogue:
        final Node source = (Node) actionEvent.getSource();
        final Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
