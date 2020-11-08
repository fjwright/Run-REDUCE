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
        algebraicInputColourPicker.setValue(Color.web(REDUCEPanel.algebraicInputCssColour));
        symbolicInputColourPicker.setValue(Color.web(REDUCEPanel.symbolicInputCssColour));
        algebraicOutputColourPicker.setValue(Color.web(REDUCEPanel.algebraicOutputCssColour));
        symbolicOutputColourPicker.setValue(Color.web(REDUCEPanel.symbolicOutputCssColour));
        warningColourPicker.setValue(Color.web(REDUCEPanel.warningCssColour));
        errorColourPicker.setValue(Color.web(REDUCEPanel.errorCssColour));
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
        return String.format("rgba(%d,%d,%d,%2f)",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255),
                color.getOpacity());
    }

    @FXML
    private void saveButtonAction(ActionEvent actionEvent) {
        REDUCEPanel.algebraicInputCssColour = colorToWeb(algebraicInputColourPicker.getValue());
        REDUCEPanel.symbolicInputCssColour = colorToWeb(symbolicInputColourPicker.getValue());
        REDUCEPanel.algebraicOutputCssColour = colorToWeb(algebraicOutputColourPicker.getValue());
        REDUCEPanel.symbolicOutputCssColour = colorToWeb(symbolicOutputColourPicker.getValue());
        REDUCEPanel.warningCssColour = colorToWeb(warningColourPicker.getValue());
        REDUCEPanel.errorCssColour = colorToWeb(errorColourPicker.getValue());
        RunREDUCE.reducePanel.updateFontColours();
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
