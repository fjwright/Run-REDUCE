package fjwright.runreduce;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class FontSizeDialog {
    @FXML
    private Label defaultFontSizeLabel; // Default font size is ...
    @FXML
    private Label oldSizeDemoLabel; // Sample text at old font size of ...
    @FXML
    private Spinner<Integer> newSizeValueSpinner;
    @FXML
    private Label newSizeDemoLabel; // Sample text at new font size of ...

    private Font newFont = RunREDUCE.reduceFont;
    private int newFontSize;

    private void setNewSizeDemoLabel() {
        newSizeDemoLabel.setText("Sample text at new font size of " + newFontSize);
        newSizeDemoLabel.setFont(newFont);
    }

    // FixMe Throws a NullPointerException if you delete the font size and press Enter.
    // Presumably the spinnerValueFactory value converter needs to trap this error, maybe by wrapping in a try-catch.

    @FXML
    private void initialize() {
        defaultFontSizeLabel.setText("Default font size is " +
                Math.round(Font.font(RunREDUCE.reduceFontFamilyName).getSize()));
        newFontSize = (int) Math.round(newFont.getSize());
        oldSizeDemoLabel.setText("Sample text at old font size of " + newFontSize);
        oldSizeDemoLabel.setFont(newFont);
        SpinnerValueFactory<Integer> spinnerValueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 30, newFontSize);
        newSizeValueSpinner.setValueFactory(spinnerValueFactory);
        setNewSizeDemoLabel();
        // Listen for changes on the spinner:
        spinnerValueFactory.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    newFontSize = newValue;
                    newFont = Font.font(RunREDUCE.reduceFontFamilyName, newFontSize);
                    setNewSizeDemoLabel();
                    // This should be unnecessary, but is kept as a precaution.
//                    stage.sizeToScene(); // This works!
                });
    }

    @FXML
    private void okButtonAction(ActionEvent actionEvent) {
        RunREDUCE.reduceFont = newFont;
        RRPreferences.save(RRPreferences.FONTSIZE, newFontSize);
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
