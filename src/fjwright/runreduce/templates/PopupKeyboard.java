package fjwright.runreduce.templates;

import javafx.beans.binding.When;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Popup;

import java.util.HashMap;
import java.util.Map;

/**
 * This class provides a pop-up keyboard on Control-left-mouse-button and middle-mouse-button
 * providing special symbols, Greek letters, some elementary functions and predicates, and
 * all trigonometric (except atan2) and hyperbolic functions, using either radians or degrees.
 */
public class PopupKeyboard {
    private static Node target;
    private final static Popup popup = new Popup();

    /**
     * This method is registered as an event handler on the input editor.
     */
    public static void showPopupKbdOnInputEditor(MouseEvent mouseEvent) {
        if ((mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.isControlDown()) ||
                (mouseEvent.getButton() == MouseButton.MIDDLE)) {
            target = (Node) mouseEvent.getSource();
            popup.show(target.getScene().getWindow(),
                    mouseEvent.getScreenX(), mouseEvent.getScreenY());
            shiftButton.setSelected(mouseEvent.isShiftDown());
        }
    }

    /**
     * This method is registered as an event filter on the root node of a Template.
     */
    public static void showPopupKbdOnTemplate(MouseEvent mouseEvent) {
        if ((mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.isControlDown()) ||
                (mouseEvent.getButton() == MouseButton.MIDDLE)) {
            // A TextField contains content that contains a caret, so...
            target = (Node) mouseEvent.getTarget();
            while (target != null) {
                if (target instanceof TextField) {
                    popup.show(((Node) mouseEvent.getSource()).getScene().getWindow(),
                            mouseEvent.getScreenX(), mouseEvent.getScreenY());
                    shiftButton.setSelected(mouseEvent.isShiftDown());
                    break;
                }
                target = target.getParent();
            }
        }
    }

// Upper keyboard data: constants and Greek letters ===============================================

    // Array [0][...] is unshifted; [1][...] is shifted.
    // '\u200B' is a zero-width space used to distinguish characters to be decoded on input to REDUCE.
    private static final String[][] constants = {
            {"\u200B∞", "\u200Bπ"},
            {"\u200Bγ", "\u200Bφ"}};
    private static final String[][] constantTooltips = {
            {"Infinity: bigger than any real or natural number",
                    "Pi: Archimedes' circle constant = 3.14159..."},
            {"Euler_gamma: Euler's constant = 0.57722...",
                    "Golden_ratio: (1+√5)/2 = 1.61803..."}};
    private static final String[][] constantNames =
            {{"infinity", "pi"}, {"euler_gamma", "golden_ratio"}};

    private static final String[][][] greekLetters = new String[2][2][12];
    //  Α α, Β β, Γ γ, Δ δ, Ε ε, Ζ ζ, Η η, Θ θ, Ι ι, Κ κ, Λ λ, Μ μ,
    //  Ν ν, Ξ ξ, Ο ο, Π π, Ρ ρ, Σ ς/σ, Τ τ, Υ υ, Φ φ, Χ χ, Ψ ψ, Ω ω
    private static final String[][][] greekLetterNames = new String[2][2][12];

    static { // Initialise greekLetters and greekLetterNames:
        char gl = '\u03B1'; // α
        for (int s = 0; s < greekLetters.length; s++) {
            for (int i = 0; i < greekLetters[0].length; i++)
                for (int j = 0; j < greekLetters[0][i].length; j++) {
                    greekLetters[s][i][j] = String.valueOf(gl);
                    String name = Character.getName(gl++);
                    name = name.substring(name.lastIndexOf(' ') + 1);
                    if (s == 0) name = name.toLowerCase();
                    else name = Character.toString(name.charAt(0))
                            .concat(name.substring(1).toLowerCase());
                    greekLetterNames[s][i][j] = name;
                    // Skip ς and empty UC ς code point:
                    if (gl == '\u03C2' || gl == '\u03A2') gl++;
                }
            gl = '\u0391'; // Α
        }
    }

// Lower keyboard data: trigonometric and hyperbolic functions ====================================

    // Array [0][][], unshifted, is elementary functions; [1][][], shifted, is predicates.
    private static final String[][][] elemPredFunctions = {
            {{"exp", "ln"}, {"√‾", "!"}},
            {{"numberp", "fixp"}, {"evenp", "primep"}}
    };
    private static final String[][][] elemPredTooltips = {
            {
                    {"exponential function",
                            "natural logarithm, i.e. to base e"},
                    {"square root",
                            "factorial"}},
            {
                    {"numberp n is true if n is a number",
                            "fixp n is true if n is an integer"},
                    {"evenp n is true if the number n is an even integer",
                            "primep n is true if n is prime"}}
    };

    // Array [0][][], unshifted, is direct functions; [1][][], shifted, is inverse functions.
    // Array [][0][] is trigonometric; [][1][] is hyperbolic.
    private static final String[][][] trigHypFunctions = new String[2][2][6];
    private static final String[][][] trigHypFunctionNames = new String[2][2][6];

    static { // Initialise trigHypFunctions:
        trigHypFunctions[0][0] = new String[]{"sin", "cos", "tan", "csc", "sec", "cot"};
        trigHypFunctionNames[0][0] = new String[]{"sine", "cosine", "tangent", "cosecant", "secant", "cotangent"};
        for (int j = 0; j < trigHypFunctions[0][0].length; j++) {
            // Hyperbolic analogues:
            trigHypFunctions[0][1][j] = trigHypFunctions[0][0][j] + "h";
            trigHypFunctionNames[0][1][j] = "hyperbolic " + trigHypFunctionNames[0][0][j];
            // Inverses:
            for (int i = 0; i < 2; i++) {
                trigHypFunctions[1][i][j] = "a" + trigHypFunctions[0][i][j];
                trigHypFunctionNames[1][i][j] = "inverse " + trigHypFunctionNames[0][i][j];
            }
        }
    }

// Build the pop-up dialogue ======================================================================

    private static final String HEADING_TEXT_STYLE =
            "-fx-font-smoothing-type:lcd;-fx-font-size:6pt";

    private static final ToggleButton shiftButton = new ToggleButton("Shift");
    private static final ToggleButton englishButton = new ToggleButton("English");
    private static final ToggleButton degreesButton = new ToggleButton();
    private static final GridPane[] topLeftGridPane = new GridPane[2];
    private static final GridPane[] topRightGridPane = new GridPane[2];
    private static final GridPane[] bottomLeftGridPane = new GridPane[2];
    private static final GridPane[] bottomRightGridPane = new GridPane[2];
    private static final Text topRightGridPaneText = new Text();
    private static final Text bottomLeftGridPaneText = new Text();
    private static final Text bottomRightGridPaneText = new Text();

    private static final double LETTER_BUTTON_WIDTH = 30;
    private static final double ELEM_FN_BUTTON_WIDTH = 62;
    private static final double TRIG_FN_BUTTON_WIDTH = 46;
    private static final double CORNER_BUTTON_WIDTH = 60;

    static {
        popup.setAutoHide(true);
        final var mainGridPane = new GridPane();
        popup.getContent().add(mainGridPane);
        mainGridPane.setHgap(5);
        mainGridPane.setVgap(5);
        mainGridPane.setStyle("-fx-background-color:lightgray;-fx-border-color:darkgray;-fx-border-width:2px;" +
                "-fx-background-radius:3px;-fx-border-radius:3px;");

// Upper keyboard: constants and Greek letters ====================================================

        // Shift button
        shiftButton.setRotate(-90);
        // Embed in Group to accommodate the rotation:
        mainGridPane.add(new Group(shiftButton), 0, 0);
        shiftButton.setPrefWidth(CORNER_BUTTON_WIDTH);
        shiftButton.setTooltip(new Tooltip(
                "Click, or hold the Shift key, to show the secondary keyboard." +
                        "\nClick again, or release the Shift key, to show the primary keyboard."));

        final var topHBox = new HBox();
        mainGridPane.add(topHBox, 1, 0);
        topHBox.setSpacing(5);
        topHBox.setAlignment(Pos.CENTER);

        // Consts button grid
        // The unshifted and shifted button grids are superimposed, but one is hidden.
        final var topLeftGridPaneText = new Text("Consts");
        topLeftGridPaneText.setStyle(HEADING_TEXT_STYLE);
        final var topLeftStackPane = new StackPane();
        final var topLeftVBox = new VBox(topLeftGridPaneText, topLeftStackPane);
        topLeftVBox.setAlignment(Pos.CENTER);
        topHBox.getChildren().add(topLeftVBox);
        for (int s = 0; s < 2; s++) {
            topLeftStackPane.getChildren().add(topLeftGridPane[s] = new GridPane());
            for (int i = 0; i < constants[s].length; i++) {
                Button button = new Button(constants[s][i]);
                topLeftGridPane[s].add(button, 0, i);
                button.setPrefWidth(LETTER_BUTTON_WIDTH);
                button.setTooltip(new Tooltip(constantTooltips[s][i]));
                int[] indices = {s, i};
                button.setOnMouseClicked(mouseEvent -> topKbdButtonAction(indices));
            }
        }

        // Greek letters button grid
        // The unshifted and shifted button grids are superimposed, but one is hidden.
        topRightGridPaneText.setStyle(HEADING_TEXT_STYLE);
        topRightGridPaneText.textProperty().bind(
                new When(shiftButton.selectedProperty())
                        .then(new When(englishButton.selectedProperty())
                                .then("Greek Upper-Case Letter Names in English")
                                .otherwise("Greek Upper-Case Letters"))
                        .otherwise(new When(englishButton.selectedProperty())
                                .then("Greek Lower-Case Letter Names in English")
                                .otherwise("Greek Lower-Case Letters")));
        final var topRightGridStackPane = new StackPane();
        final var topRightVBox = new VBox(topRightGridPaneText, topRightGridStackPane);
        topRightVBox.setAlignment(Pos.CENTER);
        topHBox.getChildren().add(topRightVBox);
        for (int s = 0; s < 2; s++) {
            topRightGridStackPane.getChildren().add(topRightGridPane[s] = new GridPane());
            for (int i = 0; i < greekLetters[s].length; i++)
                for (int j = 0; j < greekLetters[s][i].length; j++) {
                    Button button = new Button(greekLetters[s][i][j]);
                    topRightGridPane[s].add(button, j, i);
                    button.setPrefWidth(LETTER_BUTTON_WIDTH);
                    button.setTooltip(new Tooltip(greekLetterNames[s][i][j]));
                    int[] indices = {s, i, j};
                    button.setOnMouseClicked(mouseEvent -> topKbdButtonAction(indices));
                }
        }

        // English button
        englishButton.setRotate(90);
        englishButton.setPadding(new Insets(4, 0, 4, 0));
        mainGridPane.add(new Group(englishButton), 2, 0);
        englishButton.setPrefWidth(CORNER_BUTTON_WIDTH);
        englishButton.setTooltip(new Tooltip(
                "Click, or hold the Alt key, to output Greek letters spelt out in English." +
                        "\nClick again, or release the Alt key, to output Greek letters normally."));

// Lower keyboard: trigonometric and hyperbolic functions =========================================

        // Close button
        final var closeButton = new Button("Close");
        closeButton.setRotate(-90);
        mainGridPane.add(new Group(closeButton), 0, 1);
        closeButton.setPrefWidth(CORNER_BUTTON_WIDTH);
        closeButton.setOnAction(actionEvent -> popup.hide());
        closeButton.setTooltip(new Tooltip(
                "Close the pop-up.\nPressing the Escape key also closes the pop-up."));

        final var bottomHBox = new HBox();
        mainGridPane.add(bottomHBox, 1, 1);
        bottomHBox.setSpacing(5);
        bottomHBox.setAlignment(Pos.CENTER);

        // ElemPredFunctions button grid
        // The unshifted and shifted button grids are superimposed, but one is hidden.
        bottomLeftGridPaneText.setStyle(HEADING_TEXT_STYLE);
        bottomLeftGridPaneText.textProperty().bind(
                new When(shiftButton.selectedProperty())
                        .then("Predicates")
                        .otherwise("Elementary Functions"));
        final var bottomLeftStackPane = new StackPane();
        final var bottomLeftVBox = new VBox(bottomLeftGridPaneText, bottomLeftStackPane);
        bottomLeftVBox.setAlignment(Pos.CENTER);
        bottomHBox.getChildren().add(bottomLeftVBox);
        for (int s = 0; s < 2; s++) {
            bottomLeftStackPane.getChildren().add(bottomLeftGridPane[s] = new GridPane());
            for (int i = 0; i < elemPredFunctions[s].length; i++)
                for (int j = 0; j < elemPredFunctions[s][i].length; j++) {
                    Button button = new Button(elemPredFunctions[s][i][j]);
                    bottomLeftGridPane[s].add(button, j, i);
                    button.setPrefWidth(ELEM_FN_BUTTON_WIDTH);
                    button.setPadding(new Insets(4, 0, 4, 0));
                    button.setTooltip(new Tooltip(elemPredTooltips[s][i][j]));
                    button.setOnMouseClicked(mouseEvent ->
                            btmKbdButtonAction(mouseEvent, false));
                }
        }

        // TrigHyp functions button grid
        // The unshifted and shifted button grids are superimposed, but one is hidden.
        bottomRightGridPaneText.setStyle(HEADING_TEXT_STYLE);
        bottomRightGridPaneText.textProperty().bind(
                new When(shiftButton.selectedProperty())
                        .then(new When(degreesButton.selectedProperty())
                                .then("Inverse Trigonometric (Degrees) and Hyperbolic Functions")
                                .otherwise("Inverse Trigonometric (Radians) and Hyperbolic Functions"))
                        .otherwise(new When(degreesButton.selectedProperty())
                                .then("Trigonometric (Degrees) and Hyperbolic Functions")
                                .otherwise("Trigonometric (Radians) and Hyperbolic Functions")));
        final var bottomRightGridStackPane = new StackPane();
        final var bottomRightVBox = new VBox(bottomRightGridPaneText, bottomRightGridStackPane);
        bottomRightVBox.setAlignment(Pos.CENTER);
        bottomHBox.getChildren().add(bottomRightVBox);
        for (int s = 0; s < 2; s++) {
            bottomRightGridStackPane.getChildren().add(bottomRightGridPane[s] = new GridPane());
            for (int i = 0; i < trigHypFunctions[s].length; i++)
                for (int j = 0; j < trigHypFunctions[s][i].length; j++) {
                    Button button = new Button(trigHypFunctions[s][i][j]);
                    bottomRightGridPane[s].add(button, j, i);
                    button.setPrefWidth(TRIG_FN_BUTTON_WIDTH);
                    button.setPadding(new Insets(4, 0, 4, 0));
                    button.setTooltip(new Tooltip(trigHypFunctionNames[s][i][j]));
                    button.setOnMouseClicked(mouseEvent ->
                            btmKbdButtonAction(mouseEvent, true));
                }
        }

        // Degrees button
        degreesButton.setRotate(90);
        degreesButton.setPadding(new Insets(4, 0, 4, 0));
        mainGridPane.add(new Group(degreesButton), 2, 1);
        degreesButton.setPrefWidth(CORNER_BUTTON_WIDTH);
        degreesButton.setTooltip(new Tooltip(
                "Click, or hold the Alt key, to use degrees for trigonometric functions." +
                        "\nClick again, or release the Alt key, to use radians for trigonometric functions."));
        degreesButton.textProperty().bind(
                new When(degreesButton.selectedProperty()).then("Degrees").otherwise("Radians"));

        // Handle the shift state for the button grids
        ObservableBooleanValue prop = shiftButton.selectedProperty().not();
        for (int s = 0; s < 2; s++) {
            topLeftGridPane[s].visibleProperty().bind(prop);
            topRightGridPane[s].visibleProperty().bind(prop);
            bottomLeftGridPane[s].visibleProperty().bind(prop);
            bottomRightGridPane[s].visibleProperty().bind(prop);
            prop = shiftButton.selectedProperty();
        }
    }

    /**
     * This method is called when a top keyboard button is clicked.
     */
    private static void topKbdButtonAction(int[] indices) {
        String text;
        if (englishButton.isSelected()) {
            if (indices.length == 2) text = constantNames[indices[0]][indices[1]];
            else text = greekLetterNames[indices[0]][indices[1]][indices[2]];
        } else {
            if (indices.length == 2) text = constants[indices[0]][indices[1]];
            else text = greekLetters[indices[0]][indices[1]][indices[2]];
        }
        // Overwrite selected text in target TextInputControl or
        // insert text at caret if no text is selected:
        final TextInputControl textInput = (TextInputControl) target;
        if (textInput.getSelectedText().isEmpty())
            textInput.insertText(textInput.getCaretPosition(), text);
        else
            textInput.replaceSelection(text);
        popup.hide();
    }

    /**
     * This method is called when a bottom keyboard button is clicked.
     */
    private static void btmKbdButtonAction(MouseEvent mouseEvent, boolean trigHyp) {
        String text = ((Button) mouseEvent.getSource()).getText();
        if (trigHyp) {
            if (degreesButton.isSelected()) text += "d";
            // FixMe Need to call preamble("load_package trigd;\n") -- just once! --
            // FixMe but can't do that from this static context!
        } else
            switch (text) {
                case "√‾":
                    text = "\u200B√ ";
                    break;
                case "!":
                    text = "factorial";
                    break;
            }
        // Wrap selected text in target TextInputControl or
        // insert text at caret if no text is selected:
        final TextInputControl textInput = (TextInputControl) target;
        if (textInput.getSelectedText().isEmpty())
            textInput.insertText(textInput.getCaretPosition(), text);
        else {
            final IndexRange selection = textInput.getSelection();
            textInput.insertText(selection.getStart(), text + "(");
            textInput.insertText(selection.getEnd() + text.length() + 1, ")");
        }
        popup.hide();
    }

// Handle physical Shift and Alt keys =============================================================

    static {
        popup.addEventHandler(KeyEvent.ANY, keyEvent -> {
            switch (keyEvent.getCode()) {
                case SHIFT:
                    shiftButton.setSelected(keyEvent.isShiftDown());
                    break;
                case ALT:
                    englishButton.setSelected(keyEvent.isAltDown());
                    degreesButton.setSelected(keyEvent.isAltDown());
                    break;
            }
        });
    }

// Decoding special symbols to the names used in REDUCE ===========================================

    static Map<Character, String> map = new HashMap<>();

    static {
        map.put('∞', "infinity");
        map.put('π', "pi");
        map.put('γ', "euler_gamma");
        map.put('φ', "golden_ratio");
        map.put('√', "sqrt");
    }

    public static String decode(final String text) {
        final StringBuilder builder = new StringBuilder();
        boolean found = false;
        for (int i = 0; i < text.length(); i++) {
            char a = text.charAt(i);
            if (a == '\u200B') {
                found = true;
                a = text.charAt(++i);
                String b = map.get(a);
                // In case the symbolic constant is deleted but the marker is left!
                builder.append(b == null ? a : b);
            } else
                builder.append(a);
        }
        return found ? builder.toString() : text;
    }
}
