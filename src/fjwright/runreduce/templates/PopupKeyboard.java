package fjwright.runreduce.templates;

import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Popup;

/**
 * This class provides a pop-up keyboard on the middle mouse button for special symbols and Greek letters.
 */
class PopupKeyboard {
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

    private final static Popup popup = new Popup();
    private static Node target;

    static {
        final var hBox = new HBox();
        popup.getContent().add(hBox);
        hBox.setAlignment(Pos.CENTER);
        hBox.setStyle("-fx-background-color: gray; -fx-border-color: darkgray; -fx-border-insets: 2px; -fx-border-radius: 2px;");

        final var gridPane0 = new GridPane();
        hBox.getChildren().add(gridPane0);
        for (int i = 0; i < symbols.length; i++) {
            String symbol = symbols[i];
            Button button = new Button(symbol);
            gridPane0.add(button, 0, i);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(actionEvent -> {
                ((TextField) target).setText(symbol);
                popup.hide();
            });
        }

        hBox.getChildren().add(new Separator(Orientation.VERTICAL));

        final var gridPane = new GridPane();
        hBox.getChildren().add(gridPane);
        for (int i = 0; i < greekLetters.length; i++)
            for (int j = 0; j < greekLetters[i].length; j++) {
                String letter = greekLetters[i][j];
                Button button = new Button(letter);
                gridPane.add(button, j, i);
                button.setMaxWidth(Double.MAX_VALUE);
                button.setOnAction(actionEvent -> {
                    ((TextField) target).setText(letter);
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
    }

    static void middleMouseButtonClicked(MouseEvent mouseEvent) {
        if (!(mouseEvent.getButton() == MouseButton.MIDDLE &&
                ((target = ((Node) mouseEvent.getTarget()).getParent())
                        instanceof TextField)))
            return;
        popup.show(((Node) mouseEvent.getSource()).getScene().getWindow());
    }
}
