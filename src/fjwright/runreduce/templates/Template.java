package fjwright.runreduce.templates;

import fjwright.runreduce.RunREDUCE;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.util.regex.Pattern;

/**
 * This is the base class for the specific template classes.
 */
abstract class Template {

// Pop-up keyboard for template dialogues

    @FXML
    private Node templateRoot;

    @FXML
    protected void initialize() {
        templateRoot.addEventFilter(MouseEvent.MOUSE_CLICKED, this::middleMouseButtonClicked);
    }

    private static final String[] symbols = new String[]{"∞", "π"};

    private static final String[][] greekLetters = new String[2][12];
//  Α α, Β β, Γ γ, Δ δ, Ε ε, Ζ ζ, Η η, Θ θ, Ι ι, Κ κ, Λ λ, Μ μ,
//  Ν ν, Ξ ξ, Ο ο, Π π, Ρ ρ, Σ ς/σ, Τ τ, Υ υ, Φ φ, Χ χ, Ψ ψ, Ω ω

    static {
        char gl = '\u03B1'; // α
        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 12; j++) {
                greekLetters[i][j] = String.valueOf(gl++);
                if (gl == '\u03C2' /* skip ς */) gl++;
            }
    }

    @FXML
    private void middleMouseButtonClicked(MouseEvent mouseEvent) {
        Node target;
        if (!(mouseEvent.getButton() == MouseButton.MIDDLE &&
                ((target = ((Node) mouseEvent.getTarget()).getParent())
                        instanceof TextField)))
            return;
        final var popup = new Popup();
        final var hBox = new HBox();
        popup.getContent().add(hBox);
        hBox.setAlignment(Pos.CENTER);
        hBox.setStyle("-fx-background-color: gray; -fx-border-color: darkgray; -fx-border-insets: 2px; -fx-border-radius: 2px;");

        final var gridPane0 = new GridPane();
        hBox.getChildren().add(gridPane0);
        for (int i = 0; i < symbols.length; i++) {
            String symbol = symbols[i];
            Button button = new Button(symbol);
            gridPane0.add(button,0, i);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(actionEvent -> {
                ((TextField) target).setText(symbol);
                popup.hide();
            });
        }

        hBox.getChildren().add(new Separator(Orientation.VERTICAL));

        final var gridPane = new GridPane();
        hBox.getChildren().add(gridPane);
        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 12; j++) {
                String gl = greekLetters[i][j];
                Button button = new Button(gl);
                gridPane.add(button, j, i);
                button.setMaxWidth(Double.MAX_VALUE);
                button.setOnAction(actionEvent -> {
                    ((TextField) target).setText(gl);
                    popup.hide();
                });
            }

        hBox.getChildren().add(new Separator(Orientation.VERTICAL));

        final var closeText = new Text("Close");
        closeText.setRotate(-90);
        final var closeButton = new Button();
        closeButton.setGraphic(closeText);
        hBox.getChildren().add(closeButton);
        closeButton.setMaxHeight(Double.MAX_VALUE);
        closeButton.setPrefWidth(30);
        closeButton.setMinWidth(30);
        closeButton.setOnAction(actionEvent -> popup.hide());

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
