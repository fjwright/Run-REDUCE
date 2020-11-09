package fjwright.runreduce;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class FontColorsDialog {

    @FXML
    private ColorPicker algebraicInputColourPicker, symbolicInputColourPicker,
            algebraicOutputColourPicker, symbolicOutputColourPicker, warningColourPicker, errorColourPicker;

    @FXML
    private void initialize() {
        algebraicInputColourPicker.setValue(Color.web(FontColors.algebraicInput));
        symbolicInputColourPicker.setValue(Color.web(FontColors.symbolicInput));
        algebraicOutputColourPicker.setValue(Color.web(FontColors.algebraicOutput));
        symbolicOutputColourPicker.setValue(Color.web(FontColors.symbolicOutput));
        warningColourPicker.setValue(Color.web(FontColors.warning));
        errorColourPicker.setValue(Color.web(FontColors.error));
    }

    public void algebraicInputColourAction(ActionEvent actionEvent) {

    }

    public void symbolicInputColourAction(ActionEvent actionEvent) {
    }

    public void algebraicOutputColourAction(ActionEvent actionEvent) {
    }

    public void symbolicOutputColourAction(ActionEvent actionEvent) {
    }

    public void warningColourAction(ActionEvent actionEvent) {
    }

    public void errorColourAction(ActionEvent actionEvent) {
    }

    private static String colorToWeb(Color color) {
        return String.format("rgba(%d,%d,%d,%.2f)",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255),
                color.getOpacity());
    }

    @FXML
    private void saveButtonAction(ActionEvent actionEvent) {
        FontColors.algebraicInput = colorToWeb(algebraicInputColourPicker.getValue());
        FontColors.symbolicInput = colorToWeb(symbolicInputColourPicker.getValue());
        FontColors.algebraicOutput = colorToWeb(algebraicOutputColourPicker.getValue());
        FontColors.symbolicOutput = colorToWeb(symbolicOutputColourPicker.getValue());
        FontColors.warning = colorToWeb(warningColourPicker.getValue());
        FontColors.error = colorToWeb(errorColourPicker.getValue());
        FontColors.save();
        // Close dialogue:
        cancelButtonAction(actionEvent);
    }

    @FXML
    private void cancelButtonAction(ActionEvent actionEvent) {
        // Close dialogue:
        Node source = (Node) actionEvent.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
