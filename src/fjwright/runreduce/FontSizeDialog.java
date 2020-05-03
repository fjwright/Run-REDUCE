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
    private Label defaultFontSizeLabel; // Default font size is 15
    @FXML
    private Label oldSizeDemoLabel; // Sample text at old font size of 15
    @FXML
    private Spinner<Integer> newSizeValueSpinner;
    @FXML
    private Label newSizeDemoLabel; // Sample text at new font size of 15

    private static final String reduceFontName = "Consolas"; // Should be elsewhere!
    private final Font reduceFont = Font.font(reduceFontName); // Should be elsewhere!

    private Font newFont = /*RunREDUCE.*/reduceFont;
    private double newFontSize;

    private void setNewSizeDemoLabel() {
        newSizeDemoLabel.setText("Sample text at new font size of " + Math.round(newFontSize));
        newSizeDemoLabel.setFont(newFont);
    }

    // FixMe Throws a NullPointerException if you delete the font size and press Enter.
    // Presumably the spinnerValueFactory value converter needs to trap this error, maybe by wrapping in a try-catch.

    @FXML
    private void initialize() {
        defaultFontSizeLabel.setText("Default font size is " + Math.round(Font.font(reduceFontName).getSize()));
        newFontSize = newFont.getSize();
        oldSizeDemoLabel.setText("Sample text at old font size of " + Math.round(newFontSize));
        oldSizeDemoLabel.setFont(newFont);
        SpinnerValueFactory<Integer> spinnerValueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 30, (int) Math.round(newFontSize));
        newSizeValueSpinner.setValueFactory(spinnerValueFactory);
        setNewSizeDemoLabel();
        // Listen for changes on the spinner:
        spinnerValueFactory.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    newFontSize = newValue;
                    newFont = Font.font(reduceFontName, newFontSize);
                    setNewSizeDemoLabel();
                    // This should be unnecessary, but is kept as a precaution.
//                    Main.primaryStage.sizeToScene(); // This works!
                });
//        pack();                       // must be done dynamically
//        setLocationRelativeTo(frame); // ditto
//        setVisible(true);
    }

    @FXML
    private void okButtonAction(ActionEvent actionEvent) {
//        RunREDUCE.reduceFont = newFont;
//        RunREDUCE.reducePanel.outputTextPane.setFont(newFont);
//        RunREDUCE.reducePanel.inputTextArea.setFont(newFont);
//        RRPreferences.save(RRPreferences.FONTSIZE, newFontSize);
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
