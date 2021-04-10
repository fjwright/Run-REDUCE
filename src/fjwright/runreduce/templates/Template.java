package fjwright.runreduce.templates;

import fjwright.runreduce.PopupKeyboard;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
        hBox.setSpacing(20);
        var label = new Label("Control+Click for the Pop-Up Keyboard");
        label.setTooltip(new Tooltip("""
                Hold down the Control key and click your primary mouse button
                or click your middle (tertiary) mouse button on any text field
                to access special symbols etc. using the pop-up keyboard."""));
        hBox.getChildren().add(label);
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        hBox.getChildren().add(spacer);
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

        // Register the pop-up keyboard filter on the template:
        templateRoot.addEventFilter(MouseEvent.MOUSE_CLICKED, PopupKeyboard::showPopupKbdOnTemplate);
    }

// Check field entries dynamically ================================================================

    protected static final Pattern VAR_PATTERN =
            Pattern.compile("(?:!|\\p{Alpha}).*");  // non-capturing group

    @FXML
    protected void varCheckKeyTyped(KeyEvent keyEvent) {
        final TextField textField = (TextField) keyEvent.getTarget();
        final String text = textField.getText();
        if (!(text.isEmpty() || VAR_PATTERN.matcher(text).matches())) {
            RunREDUCE.alert(Alert.AlertType.ERROR, "Template Error",
                    "This field must be, or begin with, an identifier.");
            textField.setText("");
        }
    }

    private static final Pattern INT_OR_VAR_PATTERN =
            Pattern.compile("[+-]?\\d*|(?:!|\\p{Alpha}).*");  // non-capturing groups

    @FXML
    private void intOrVarCheckKeyTyped(KeyEvent keyEvent) {
        final TextField textField = (TextField) keyEvent.getTarget();
        final String text = textField.getText();
        if (!(text.isEmpty() || INT_OR_VAR_PATTERN.matcher(text).matches())) {
            RunREDUCE.alert(Alert.AlertType.ERROR, "Template Error",
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
            RunREDUCE.alert(Alert.AlertType.ERROR, "Template Error",
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
    private String processResult(boolean decode) throws EmptyFieldException {
        // result() must be called early because it sets the switch lists.
        String decodedResult = decode ? PopupKeyboard.decode(result()) : result();
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
            // Pop-up keyboard input will be decoded later before sending to REDUCE:
            textArea.insertText(textArea.getCaretPosition(), processResult(false));
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
            // Decode pop-up keyboard input before sending to REDUCE:
            RunREDUCE.reducePanel.templateSendInteractiveInputToREDUCE(processResult(true));
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

    static class REDUCEManual {
        private static String manualIndex;
        private static Path manualDirPath;

        static class REDUCEManualException extends IOException {
            REDUCEManualException() {
                super("REDUCE Manual Error");
            }
        }

        /**
         * Get the cached HTML REDUCE Manual index, creating it if necessary.
         */
        static String getIndex() throws IOException {
            // Cache manualIndex:
            Path manualDirPathNew = Path.of(RunREDUCE.reduceConfiguration.manualDir);
            if (manualIndex == null || !manualDirPath.equals(manualDirPathNew)) {
                manualDirPath = manualDirPathNew;
                Path manualIndexFileName = manualDirPath.resolve("manual.html");
                try {
                    manualIndex = Files.readString(manualIndexFileName);
                } catch (IOException e) {
                    RunREDUCE.alert(Alert.AlertType.ERROR,
                            "REDUCE Manual Error",
                            "Cannot read HTML index file at\n"
                                    + manualIndexFileName);
                    throw e;
                }
                String searchText = "<div class=\"tableofcontents\">";
                int start = manualIndex.indexOf(searchText);
                if (start != -1) {
                    start += searchText.length();
                    int finish = manualIndex.indexOf("</div>", start);
                    if (finish != -1) manualIndex = manualIndex.substring(start, finish);
                } else {
                    RunREDUCE.alert(Alert.AlertType.ERROR,
                            "REDUCE Manual Error",
                            "Cannot find table of contents in HTML index file at\n"
                                    + manualIndexFileName);
                    throw new REDUCEManualException();
                }
            }
            return manualIndex;
        }

        static Path getDirPath() throws IOException {
            if (manualDirPath == null) throw new REDUCEManualException();
            return manualDirPath;
        }
    }

    /**
     * Register this as the OnAction method for a Hyperlink to display the
     * local HTML REDUCE Manual section with the filename specified as User Data.
     */
    @FXML
    protected void redManHyperlinkOnAction(ActionEvent actionEvent) {
        // Use manualIndex as a jump table for
        String searchText = (String) ((Node) actionEvent.getTarget()).getUserData();
        // Search for (e.g.)
        // <a href="manualse33.html#x44-760007.7" id="QQ2-44-77">CONTINUED_FRACTION Operator</a>
        Pattern pattern = Pattern.compile(
                String.format("<a\\s+href=\"(manual.+?\\.html).*?\"\\s+id=\".+?\">%s</a>", searchText));
        try {
            Matcher matcher = pattern.matcher(REDUCEManual.getIndex());
            if (matcher.find())
                RunREDUCE.hostServices.showDocument(
                        REDUCEManual.getDirPath().resolve(matcher.group(1)).toString());
            else
                RunREDUCE.alert(Alert.AlertType.ERROR,
                        "REDUCE Manual Hyperlink Error",
                        "Link not found for\n" + searchText);
        } catch (IOException ignored) {
        }
    }
}
