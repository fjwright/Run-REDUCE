package fjwright.runreduce.templates;

import fjwright.runreduce.RunREDUCE;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the base class for the specific template classes.
 */
public abstract class Template {

    protected RadioButton numRadioButton;
    @FXML
    protected Hyperlink symHyperlink, numHyperlink;

    /**
     * This method adds a pair of Symbolic/Numeric radio buttons to the top of the dialogue.
     */
    protected void addSymNumRadioButtons() {
        numRadioButton = new RadioButton("Numeric");
        var symRadioButton = new RadioButton("Symbolic");
        symRadioButton.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        symRadioButton.setSelected(true);
        new ToggleGroup().getToggles().addAll(symRadioButton, numRadioButton);
        var hBox = new HBox(symRadioButton, numRadioButton);
        hBox.setSpacing(10);
        hBox.setAlignment(Pos.CENTER);
        templateRoot.getChildren().add(0, hBox);
        if (symHyperlink != null)
            symHyperlink.visibleProperty().bind(numRadioButton.selectedProperty().not());
        if (numHyperlink != null)
            numHyperlink.visibleProperty().bind(numRadioButton.selectedProperty());
    }

// Add the button bar to the bottom of the dialogue box and
// set up the pop-up keyboard on the middle mouse button for all template dialogues ===============

    @FXML
    protected VBox templateRoot;

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
    protected void varCheckKeyTyped(KeyEvent keyEvent) {
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

    private static boolean getTextCheckNonEmpty;

    protected static String getTextCheckNonEmpty(final TextField textField) throws EmptyFieldException {
        final String text = textField.getText().trim();
        if (getTextCheckNonEmpty && text.isEmpty()) {
            RunREDUCE.errorMessageDialog("Template Error",
                    "A required field is empty.");
            throw new EmptyFieldException();
        } else
            return text;
    }

    private String preambleText;

    /**
     * This method may be called by a specific template class
     * to construct output that precedes the main result.
     */
    protected void preamble(String preambleText) {
        this.preambleText = preambleText;
    }

    /**
     * This method must be overridden by each specific template class
     * to construct the result.
     */
    abstract protected String result() throws EmptyFieldException;

    /**
     * This method processes the result returned by each specific template class to decode any
     * symbolic constants and precede and follow the result with any required switch settings.
     */
    private String processResult() throws EmptyFieldException {
        // result() must be called early because it sets the switch lists.
        String decodedResult = PopupKeyboard.decode(result());
        String switchOnOffString = null, switchOffOnString = null;
        if (!switchOnOffList.isEmpty()) {
            switchOnOffString = String.join(", ", switchOnOffList);
            switchOnOffList.clear();
        }
        if (!switchOffOnList.isEmpty()) {
            switchOffOnString = String.join(", ", switchOffOnList);
            switchOffOnList.clear();
        }
        StringBuilder result = new StringBuilder();
        if (preambleText != null) {
            result.append(preambleText);
            preambleText = null;
        }
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
        // No fields are required:
        getTextCheckNonEmpty = false;
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
        // Check required fields provided:
        getTextCheckNonEmpty = true;
        // Send to REDUCE if valid:
        try {
            RunREDUCE.reducePanel.sendInteractiveInputToREDUCE(processResult(), true);
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

// Hyperlink support ==============================================================================

    /**
     * Register this as the OnAction method for any Hyperlink to display
     * the URL specified as User Data.
     */
    @FXML
    protected void hyperlinkOnAction(ActionEvent actionEvent) {
        RunREDUCE.hostServices.showDocument(
                (String) ((Node) actionEvent.getTarget()).getUserData());
    }

    private static String redManIndex;
    private static Path redManRootDir;

    /**
     * Register this as the OnAction method for a Hyperlink to display
     * the local HTML REDUCE Manual section with the filename specified as User Data.
     */
    @FXML
    protected void redManHyperlinkOnAction(ActionEvent actionEvent) {
        // Cache redManIndex:
        if (redManIndex == null || !redManRootDir.equals(RunREDUCE.reduceConfiguration.redManRootDir)) {
            redManRootDir = RunREDUCE.reduceConfiguration.redManRootDir;
            try {
                redManIndex = Files.readString(redManRootDir.resolve("index.html"));
            } catch (IOException e) {
                RunREDUCE.errorMessageDialog(
                        "REDUCE Manual Hyperlink Error",
                        "Cannot read the index file at\n"
                                + redManRootDir.resolve("index.html"));
                return;
            }
            String searchText = "<div class=\"tableofcontents\">";
            int start = redManIndex.indexOf(searchText);
            if (start != -1) {
                start += searchText.length();
                int finish = redManIndex.indexOf("</div>", start);
                if (finish != -1) redManIndex = redManIndex.substring(start, finish);
            }
        }
        // Use redManIndex as a jump table for
        String searchText = (String) ((Node) actionEvent.getTarget()).getUserData();
        // Search for (e.g.)
        // <a href="manualse33.html#x44-760007.7" id="QQ2-44-77">CONTINUED_FRACTION Operator</a>
        Pattern pattern = Pattern.compile(
                String.format("<a\\s+href=\"(manual.+?\\.html).*?\"\\s+id=\".+?\">%s</a>", searchText));
        Matcher matcher = pattern.matcher(redManIndex);
        if (matcher.find())
            RunREDUCE.hostServices.showDocument(redManRootDir.resolve(matcher.group(1)).toString());
        else
            RunREDUCE.errorMessageDialog(
                    "REDUCE Manual Hyperlink Error",
                    "Link not found for\n" + searchText);
    }
}
