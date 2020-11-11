package fjwright.runreduce;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class FontColorsDialog {
    @FXML
    private Label algebraicInputLabel, symbolicInputLabel,
            algebraicOutputLabel, symbolicOutputLabel, warningLabel, errorLabel;
    @FXML
    private ColorPicker algebraicInputColorPicker, symbolicInputColorPicker,
            algebraicOutputColorPicker, symbolicOutputColorPicker, warningColorPicker, errorColorPicker;

    @FXML
    private void initialize() {
        algebraicInputLabel.textFillProperty().bind(algebraicInputColorPicker.valueProperty());
        symbolicInputLabel.textFillProperty().bind(symbolicInputColorPicker.valueProperty());
        algebraicOutputLabel.textFillProperty().bind(algebraicOutputColorPicker.valueProperty());
        symbolicOutputLabel.textFillProperty().bind(symbolicOutputColorPicker.valueProperty());
        warningLabel.backgroundProperty().bind(new ObjectBinding<>() {
            final ObjectProperty<Color> colorObjectProperty = warningColorPicker.valueProperty();

            {
                super.bind(colorObjectProperty);
            }

            @Override
            protected Background computeValue() {
                return new Background(
                        new BackgroundFill(colorObjectProperty.get(), null, null));
            }
        });
        errorLabel.backgroundProperty().bind(new ObjectBinding<>() {
            final ObjectProperty<Color> colorObjectProperty = errorColorPicker.valueProperty();

            {
                super.bind(colorObjectProperty);
            }

            @Override
            protected Background computeValue() {
                return new Background(
                        new BackgroundFill(colorObjectProperty.get(), null, null));
            }
        });

        algebraicInputColorPicker.setValue(Color.web(FontColors.algebraicInput));
        symbolicInputColorPicker.setValue(Color.web(FontColors.symbolicInput));
        algebraicOutputColorPicker.setValue(Color.web(FontColors.algebraicOutput));
        symbolicOutputColorPicker.setValue(Color.web(FontColors.symbolicOutput));
        warningColorPicker.setValue(Color.web(FontColors.warning));
        errorColorPicker.setValue(Color.web(FontColors.error));
    }

    /**
     * Convert a Color to an RGBA web string.
     */
    private static String colorToWeb(Color color) {
        return String.format("rgba(%d,%d,%d,%.2f)",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255),
                color.getOpacity());
    }

    @FXML
    private void resetDefaultsButtonAction() {
        algebraicInputColorPicker.setValue(Color.web(FontColors.algebraicInputDefault));
        symbolicInputColorPicker.setValue(Color.web(FontColors.symbolicInputDefault));
        algebraicOutputColorPicker.setValue(Color.web(FontColors.algebraicOutputDefault));
        symbolicOutputColorPicker.setValue(Color.web(FontColors.symbolicOutputDefault));
        warningColorPicker.setValue(Color.web(FontColors.warningDefault));
        errorColorPicker.setValue(Color.web(FontColors.errorDefault));
    }

    @FXML
    private void saveButtonAction(ActionEvent actionEvent) {
        FontColors.algebraicInput = colorToWeb(algebraicInputColorPicker.getValue());
        FontColors.symbolicInput = colorToWeb(symbolicInputColorPicker.getValue());
        FontColors.algebraicOutput = colorToWeb(algebraicOutputColorPicker.getValue());
        FontColors.symbolicOutput = colorToWeb(symbolicOutputColorPicker.getValue());
        FontColors.warning = colorToWeb(warningColorPicker.getValue());
        FontColors.error = colorToWeb(errorColorPicker.getValue());
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
