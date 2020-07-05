package fjwright.runreduce.templates;

import fjwright.runreduce.RunREDUCE;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This is the base class for the specific template classes.
 */
public abstract class Template {

// Add the button bar to the bottom of the dialogue box and
// set up the pop-up keyboard on the middle mouse button for all template dialogues ===============

    @FXML
    private VBox templateRoot;

    @FXML
    protected void initialize() {
        // Add the button bar to the bottom of the dialogue box:
        var hBox = new HBox();
        templateRoot.getChildren().add(hBox);
        hBox.setAlignment(Pos.CENTER_RIGHT);
        hBox.setSpacing(20);
        var button = new Button("Edit");
        hBox.getChildren().add(button);
        button.setOnAction(this::editButtonAction);
        button.setTooltip(new Tooltip("Insert into input editor"));
        button = new Button("Evaluate");
        hBox.getChildren().add(button);
        button.setOnAction(this::evaluateButtonAction);
        button.setTooltip(new Tooltip("Send to REDUCE"));
        button = new Button("Close");
        hBox.getChildren().add(button);
        button.setOnAction(this::closeButtonAction);

        // Register the pop-up handler on the template:
        templateRoot.addEventFilter(MouseEvent.MOUSE_CLICKED, PopupKeyboard::showPopupKeyboard);
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
                    "This field must be, or begin with, an identifier.");
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

// Process switches ===============================================================================

    private final List<String> switchOnOffList = new ArrayList<>();
    private final List<String> switchOffOnList = new ArrayList<>();

    /**
     * Call this method in result() in each specific template class
     * for switch CheckBoxes that are off by default to turn them on
     * and off again after evaluating the template output.
     */
    protected void switchCheckBoxesOnOff(CheckBox... switchCheckBoxes) {
        for (var switchCheckBox : switchCheckBoxes)
            if (switchCheckBox.isSelected()) switchOnOffList.add(switchCheckBox.getText());
    }

    /**
     * Call this method in result() in each specific template class
     * for switch Names that are off by default to turn them on
     * and off again after evaluating the template output.
     */
    protected void switchNameOnOff(String switchName) {
        switchOnOffList.add(switchName);
    }

    /**
     * Call this method in result() in each specific template class
     * for switch CheckBoxes that are on by default to turn them off
     * and on again after evaluating the template output.
     */
    protected void switchCheckBoxesOffOn(CheckBox... switchCheckBoxes) {
        for (var switchCheckBox : switchCheckBoxes) {
            if (!switchCheckBox.isSelected()) {
                switchOffOnList.add(switchCheckBox.getText());
            }
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

    /**
     * This method must be overridden by each specific template class
     * to construct the result.
     */
    abstract String result() throws EmptyFieldException;

    /**
     * This method may be overridden by a specific template class
     * to construct output that precedes the main result.
     */
    String resultPreamble() {
        return "";
    }

    /**
     * This method processes the result returned by each specific template class to decode any
     * symbolic constants and precede and follow the result with any required switch settings.
     */
    private String processResult() throws EmptyFieldException {
        // result() must be called early because it sets the switch lists.
        String decodedResult = PopupKeyboard.decode(result());
        if (switchOnOffList.isEmpty() && switchOffOnList.isEmpty())
            return decodedResult;
        String switchOnOffString = null, switchOffOnString = null;
        if (!switchOnOffList.isEmpty())
            switchOnOffString = String.join(", ", switchOnOffList);
        if (!switchOffOnList.isEmpty())
            switchOffOnString = String.join(", ", switchOffOnList);
        switchOnOffList.clear();
        switchOffOnList.clear();
        StringBuilder result = new StringBuilder(resultPreamble());
        if (switchOnOffString != null) result.append("on ").append(switchOnOffString).append(";\n");
        if (switchOffOnString != null) result.append("off ").append(switchOffOnString).append(";\n");
        result.append(decodedResult);
        if (switchOffOnString != null) result.append(";\non ").append(switchOffOnString);
        if (switchOnOffString != null) result.append(";\noff ").append(switchOnOffString);
        return result.toString();
    }

    // Could apply PopupKeyboard.decode() below more efficiently only to appropriate user input.
    // But it seems fast enough as it is.

    @FXML
    private void editButtonAction(ActionEvent actionEvent) {
        // Insert in input editor if valid:
        try {
            final TextArea textArea = RunREDUCE.reducePanel.inputTextArea;
            textArea.insertText(textArea.getCaretPosition(), processResult());
            // Close dialogue:
//            cancelButtonAction(actionEvent);
        } catch (EmptyFieldException ignored) {
        }
    }

    @FXML
    private void evaluateButtonAction(ActionEvent actionEvent) {
        // Send to REDUCE if valid:
        try {
            RunREDUCE.reducePanel.sendStringToREDUCEAndEcho(processResult() + ";\n");
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
