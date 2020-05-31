package fjwright.runreduce;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.regex.Pattern;

public class FontSizeDialog {
    @FXML
    private Label defaultFontSizeLabel; // Default font size is ...
    @FXML
    private Label oldSizeDemoLabel; // Sample text at old font size of ...
    @FXML
    private Spinner<Integer> newSizeValueSpinner;
    @FXML
    private Label newSizeDemoLabel; // Sample text at new font size of ...

    private static final int MIN_FONT_SIZE = 5;
    private static final int MAX_FONT_SIZE = 30;
    private int newFontSize;
    private static final Pattern PATTERN = Pattern.compile("\\d+");

    private void setNewSizeDemoLabel() {
        newSizeDemoLabel.setText("Sample text at new font size of " + newFontSize);
        newSizeDemoLabel.setStyle("-fx-font:" + newFontSize + " " + RunREDUCE.reduceFontFamilyName);
    }

    @FXML
    private void initialize() {
        newFontSize = RRPreferences.fontSize;
        defaultFontSizeLabel.setText("Default font size is " +
                Math.round(Font.font(RunREDUCE.reduceFontFamilyName).getSize()));
        oldSizeDemoLabel.setText("Sample text at old font size of " + newFontSize);
        oldSizeDemoLabel.setStyle("-fx-font:" + newFontSize + " " + RunREDUCE.reduceFontFamilyName);
        SpinnerValueFactory<Integer> spinnerValueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(MIN_FONT_SIZE, MAX_FONT_SIZE, newFontSize);
        newSizeValueSpinner.setValueFactory(spinnerValueFactory);
        setNewSizeDemoLabel();
        // Listen for changes on the spinner:
        spinnerValueFactory.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    newFontSize = newValue;
                    setNewSizeDemoLabel();
                    // This should be unnecessary, but is kept as a precaution.
//                    stage.sizeToScene(); // This works!
                });
        // Validate direct input via the editor:
        newSizeValueSpinner.getEditor().setOnAction(event -> {
            TextField textField = (TextField) event.getTarget();
            if (PATTERN.matcher(textField.getText()).matches()) {
                newFontSize = Integer.parseInt(textField.getText());
                if (newFontSize < MIN_FONT_SIZE) newFontSize = MIN_FONT_SIZE;
                else if (newFontSize > MAX_FONT_SIZE) newFontSize = MAX_FONT_SIZE;
                setNewSizeDemoLabel();
            }
            textField.setText(Integer.toString(newFontSize));
        });
    }

    @FXML
    private void okButtonAction(ActionEvent actionEvent) {
        RRPreferences.save(RRPreferences.FONTSIZE, newFontSize);
        RunREDUCE.fontFamilyAndSizeStyle = "-fx-font-family:" + RunREDUCE.reduceFontFamilyName
                + ";-fx-font-size:" + newFontSize;
        RunREDUCE.reducePanel.inputTextArea.setStyle(
                "-fx-font:" + newFontSize + " " + RunREDUCE.reduceFontFamilyName);
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
