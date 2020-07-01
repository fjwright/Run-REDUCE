package fjwright.runreduce.templates;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Popup;

import java.util.HashMap;
import java.util.Map;

/**
 * This class provides a pop-up keyboard on the middle mouse button for special symbols and Greek letters.
 */
class PopupKeyboard {
    private static Node target;
    private final static Popup popup = new Popup();

    static void middleMouseButtonClicked(MouseEvent mouseEvent) {
        if (!(mouseEvent.getButton() == MouseButton.MIDDLE &&
                ((((target = ((Node) mouseEvent.getTarget()).getParent()) instanceof TextField) || // on TextField
                        ((target = target.getParent()) instanceof TextField) || // on TextField content
                        ((target = target.getParent()) instanceof TextField))))) // on TextField content caret
            return;
        popup.show(((Node) mouseEvent.getSource()), mouseEvent.getScreenX(), mouseEvent.getScreenY());
        applyShiftButton(mouseEvent.isShiftDown());
    }

    // Array [0][...] is unshifted; [1][...] is shifted.
    // '\u200B' is a zero-width space used to distinguish characters to be decoded on input to REDUCE.
    private static final String[][] constants = new String[][]{
            {"\u200B∞", "\u200Bπ"},
            {"\u200Bγ", "\u200Bφ"}};
    private static final String[][] constTooltips = new String[][]{
            {"INFINITY: bigger than any real or natural number",
                    "PI: Archimedes' circle constant = 3.14159..."},
            {"EULER_GAMMA: Euler-Mascheroni constant = 0.57722...",
                    "GOLDEN_RATIO: (1+√5)/2 = 1.61803..."}};

    private static final String[][][] greekLetters = new String[2][2][12];
    //  Α α, Β β, Γ γ, Δ δ, Ε ε, Ζ ζ, Η η, Θ θ, Ι ι, Κ κ, Λ λ, Μ μ,
    //  Ν ν, Ξ ξ, Ο ο, Π π, Ρ ρ, Σ ς/σ, Τ τ, Υ υ, Φ φ, Χ χ, Ψ ψ, Ω ω

    static {
        char gl = '\u03B1'; // α
        for (int i = 0; i < greekLetters[0].length; i++)
            for (int j = 0; j < greekLetters[0][i].length; j++) {
                greekLetters[0][i][j] = String.valueOf(gl++);
                if (gl == '\u03C2' /* skip ς */) gl++;
            }
        gl = '\u0391'; // Α
        for (int i = 0; i < greekLetters[1].length; i++)
            for (int j = 0; j < greekLetters[1][i].length; j++) {
                greekLetters[1][i][j] = String.valueOf(gl++);
                if (gl == '\u03A2' /* skip empty UC ς code point */) gl++;
            }
    }

    private static final String GRIDPANE_TEXT_STYLE =
            "-fx-font-smoothing-type:lcd;-fx-font-size:6pt";

    private static final ToggleButton shiftButton = new ToggleButton();
    private static final GridPane[] gridPane0 = new GridPane[2];
    private static final GridPane[] gridPane1 = new GridPane[2];
    private static final Text[] gridPane1Text =
            {new Text("Greek Lower-Case Letters"), new Text("Greek Upper-Case Letters")};

    private static final double BUTTON_WIDTH = 30;

    static {
        final var hBox = new HBox();
        popup.getContent().add(hBox);
        hBox.setSpacing(5);
        hBox.setAlignment(Pos.CENTER);
        hBox.setStyle("-fx-background-color:lightgray;-fx-border-color:darkgray;-fx-border-width:2px;" +
                "-fx-background-radius:3px;-fx-border-radius:3px;");

        // Shift button
        final var shiftText = new Text("Shift");
        shiftText.setRotate(-90);
        shiftButton.setGraphic(shiftText);
        hBox.getChildren().add(shiftButton);
        shiftButton.setMaxHeight(Double.MAX_VALUE);
        shiftButton.setPrefWidth(BUTTON_WIDTH);
        shiftButton.setOnAction(actionEvent -> applyShift(shiftButton.isSelected()));
        shiftButton.setTooltip(new Tooltip(
                "Click, or hold the Shift key, to show the secondary keyboard." +
                        "\nClick again, or release the Shift key, to show the primary keyboard."));

        // Consts button grid
        // Ths unshifted and shifted button grids are superimposed, but one is hidden.
        final var gridPane0Text = new Text("Consts");
        gridPane0Text.setStyle(GRIDPANE_TEXT_STYLE);
        final var gridStackPane0 = new StackPane();
        final var vBox0 = new VBox(gridPane0Text, gridStackPane0);
        vBox0.setAlignment(Pos.CENTER);
        hBox.getChildren().add(vBox0);
        for (int s = 0; s < 2; s++) {
            gridStackPane0.getChildren().add(gridPane0[s] = new GridPane());
            for (int i = 0; i < constants[s].length; i++) {
                String constant = constants[s][i];
                Button button = new Button(constant);
                gridPane0[s].add(button, 0, i);
                button.setPrefWidth(BUTTON_WIDTH);
                button.setTooltip(new Tooltip(constTooltips[s][i]));
                button.setOnAction(actionEvent -> charButtonAction(constant));
            }
        }

        // Greek letters button grid
        // Ths unshifted and shifted button grids are superimposed, but one is hidden.
        final var textStackPane1 = new StackPane();
        final var gridStackPane1 = new StackPane();
        final var vBox1 = new VBox(textStackPane1, gridStackPane1);
        vBox1.setAlignment(Pos.CENTER);
        hBox.getChildren().add(vBox1);
        for (int s = 0; s < 2; s++) {
            gridStackPane1.getChildren().add(gridPane1[s] = new GridPane());
            textStackPane1.getChildren().add(gridPane1Text[s]);
            gridPane1Text[s].setStyle(GRIDPANE_TEXT_STYLE);
            for (int i = 0; i < greekLetters[s].length; i++)
                for (int j = 0; j < greekLetters[s][i].length; j++) {
                    String greekLetter = greekLetters[s][i][j];
                    Button button = new Button(greekLetter);
                    gridPane1[s].add(button, j, i);
                    button.setPrefWidth(BUTTON_WIDTH);
                    button.setOnAction(actionEvent -> charButtonAction(greekLetter));
                }
        }

        // Close button
        final var closeText = new Text("Close");
        closeText.setRotate(90);
        final var closeButton = new Button();
        closeButton.setGraphic(closeText);
        hBox.getChildren().add(closeButton);
        closeButton.setMaxHeight(Double.MAX_VALUE);
        closeButton.setPrefWidth(BUTTON_WIDTH);
        closeButton.setOnAction(actionEvent -> popup.hide());
        closeButton.setTooltip(new Tooltip(
                "Close the pop-up.\nPressing the Escape key also closes the pop-up."));

        // Initialise the shift state
        gridPane0[1].setVisible(false);
        gridPane1[1].setVisible(false);
        gridPane1Text[1].setVisible(false);
    }

    /**
     * Overwrite selected text in target TextField or insert character at caret.
     */
    private static void charButtonAction(String character) {
        TextField textField = (TextField) target;
        if (textField.getSelectedText() == null)
            textField.insertText(textField.getCaretPosition(), character);
        else
            textField.replaceSelection(character);
        popup.hide();
    }

// Handle shifting ================================================================================

    private static void applyShift(boolean shifted) {
        gridPane0[1].setVisible(shifted);
        gridPane0[0].setVisible(!shifted);
        gridPane1[1].setVisible(shifted);
        gridPane1[0].setVisible(!shifted);
        gridPane1Text[1].setVisible(shifted);
        gridPane1Text[0].setVisible(!shifted);
    }

    private static void applyShiftButton(boolean shifted) {
        applyShift(shifted);
        shiftButton.setSelected(shifted);
    }

    static {
        popup.addEventHandler(KeyEvent.ANY, keyEvent -> {
            if (keyEvent.getCode() == KeyCode.SHIFT)
                applyShiftButton(keyEvent.isShiftDown());
        });
    }

// Decoding special symbols to the names used in REDUCE ===========================================

    static Map<Character, String> map = new HashMap<>();

    static {
        map.put('∞', "INFINITY");
        map.put('π', "PI");
        map.put('γ', "EULER_GAMMA");
        map.put('φ', "GOLDEN_RATIO");
    }

    static String decode(final String text) {
        final StringBuilder builder = new StringBuilder();
        boolean found = false;
        for (int i = 0; i < text.length(); i++) {
            char a = text.charAt(i);
            if (a == '\u200B') {
                found = true;
                builder.append(map.get(text.charAt(++i)));
            } else
                builder.append(a);
        }
        return found ? builder.toString() : text;
    }
}
