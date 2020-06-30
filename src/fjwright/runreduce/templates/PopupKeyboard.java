package fjwright.runreduce.templates;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
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
    }

    private static final String[] constsLc = new String[]{"∞", "π"};
    private static final String[] constsLcTooltips = new String[]{
            "INFINITY: bigger than any real or natural number",
            "PI: Archimedes' circle constant = 3.14159..."};
    private static final String[] constsUc = new String[]{"γ", "φ"};
    private static final String[] constsUcTooltips = new String[]{
            "EULER_GAMMA: Euler-Mascheroni constant = 0.57722...",
            "GOLDEN_RATIO: (1+√5)/2 = 1.61803..."};

    private static final String[][] greekLettersUc = new String[2][12];
    private static final String[][] greekLettersLc = new String[2][12];
    //  Α α, Β β, Γ γ, Δ δ, Ε ε, Ζ ζ, Η η, Θ θ, Ι ι, Κ κ, Λ λ, Μ μ,
    //  Ν ν, Ξ ξ, Ο ο, Π π, Ρ ρ, Σ ς/σ, Τ τ, Υ υ, Φ φ, Χ χ, Ψ ψ, Ω ω

    static {
        char gl = '\u0391'; // Α
        for (int i = 0; i < greekLettersUc.length; i++)
            for (int j = 0; j < greekLettersUc[i].length; j++) {
                greekLettersUc[i][j] = String.valueOf(gl++);
                if (gl == '\u03A2' /* skip empty UC ς code point */) gl++;
            }
        gl = '\u03B1'; // α
        for (int i = 0; i < greekLettersLc.length; i++)
            for (int j = 0; j < greekLettersLc[i].length; j++) {
                greekLettersLc[i][j] = String.valueOf(gl++);
                if (gl == '\u03C2' /* skip ς */) gl++;
            }
    }

    private static final String GRIDPANE_TEXT_STYLE =
            "-fx-font-smoothing-type:lcd;-fx-font-size:6pt";

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
        final var shiftButton = new ToggleButton();
        shiftButton.setGraphic(shiftText);
        hBox.getChildren().add(shiftButton);
        shiftButton.setMaxHeight(Double.MAX_VALUE);
        shiftButton.setPrefWidth(30);
        shiftButton.setMinWidth(30);

        // Consts button grid
        // Lower case (Lc) means unshifted; upper case (Uc) means shifted.
        // Ths unshifted and shifted button grids are superimposed, but one is hidden.
        final var gridPane0Text = new Text("Consts");
        gridPane0Text.setStyle(GRIDPANE_TEXT_STYLE);
        final var gridPane0Lc = new GridPane();
        final var gridPane0Uc = new GridPane();
        final var gridStackPane0 = new StackPane(gridPane0Lc, gridPane0Uc);
        final var vBox0 = new VBox(gridPane0Text, gridStackPane0);
        hBox.getChildren().add(vBox0);
        var gridPane0 = gridPane0Lc;
        var constsTooltips = constsLcTooltips;
        for (var consts : new String[][]{constsLc, constsUc}) {
            for (int i = 0; i < consts.length; i++) {
                String con = consts[i];
                Button button = new Button(con);
                gridPane0.add(button, 0, i);
                button.setMaxWidth(Double.MAX_VALUE);
                button.setOnAction(actionEvent -> charButtonAction(con));
                button.setTooltip(new Tooltip(constsTooltips[i]));
            }
            gridPane0 = gridPane0Uc;
            constsTooltips = constsUcTooltips;
        }

        // Greek letters button grid
        // Ths unshifted and shifted button grids are superimposed, but one is hidden.
        final Text gridPane1TextLc = new Text("Greek Lower-Case Letters");
        gridPane1TextLc.setStyle(GRIDPANE_TEXT_STYLE);
        final Text gridPane1TextUc = new Text("Greek Upper-Case Letters");
        gridPane1TextUc.setStyle(GRIDPANE_TEXT_STYLE);
        final var textStackPane = new StackPane(gridPane1TextLc, gridPane1TextUc);
//        textStackPane.setAlignment(Pos.CENTER);
        final var gridPane1Lc = new GridPane();
        final var gridPane1Uc = new GridPane();
        final var gridStackPane1 = new StackPane(gridPane1Lc, gridPane1Uc);
        gridStackPane1.setAlignment(Pos.CENTER);
        final var vBox1 = new VBox(textStackPane, gridStackPane1);
        vBox1.setAlignment(Pos.CENTER);
        hBox.getChildren().add(vBox1);
        var gridPane1 = gridPane1Lc;
        for (var greekLetters : new String[][][]{greekLettersLc, greekLettersUc}) {
            for (int i = 0; i < greekLetters.length; i++)
                for (int j = 0; j < greekLetters[i].length; j++) {
                    String letter = greekLetters[i][j];
                    Button button = new Button(letter);
                    gridPane1.add(button, j, i);
                    button.setMaxWidth(Double.MAX_VALUE);
                    button.setOnAction(actionEvent -> charButtonAction(letter));
                }
            gridPane1 = gridPane1Uc;
        }

        // Implement the shift button
        gridPane0.setVisible(false);
        gridPane1Uc.setVisible(false);
        gridPane1TextUc.setVisible(false);
        shiftButton.setOnAction(actionEvent -> {
            gridPane0Uc.setVisible(shiftButton.isSelected());
            gridPane0Lc.setVisible(!shiftButton.isSelected());
            gridPane1Uc.setVisible(shiftButton.isSelected());
            gridPane1Lc.setVisible(!shiftButton.isSelected());
            gridPane1TextUc.setVisible(shiftButton.isSelected());
            gridPane1TextLc.setVisible(!shiftButton.isSelected());
        });

        // Close button
        final var closeText = new Text("Close");
        closeText.setRotate(90);
        final var closeButton = new Button();
        closeButton.setGraphic(closeText);
        hBox.getChildren().add(closeButton);
        closeButton.setMaxHeight(Double.MAX_VALUE);
        closeButton.setPrefWidth(30);
        closeButton.setMinWidth(30);
        closeButton.setOnAction(actionEvent -> popup.hide());
    }

    static void charButtonAction(String character) {
        // Insert character in target TextField at its caret:
        TextField textField = (TextField) target;
        if (textField.getSelectedText() == null)
            textField.insertText(textField.getCaretPosition(), character);
        else
            textField.replaceSelection(character);
        popup.hide();
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
            String b = map.get(a);
            if (b == null)
                builder.append(a);
            else {
                found = true;
                builder.append(b);
            }
        }
        return found ? builder.toString() : text;
    }
}
