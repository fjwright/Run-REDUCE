package fjwright.runreduce.templates;

import fjwright.runreduce.RunREDUCE;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class Integrate extends Template {
    @FXML
    private HBox symText, numText;
    @FXML
    private RadioButton numRadioButton;
    @FXML
    private TextField integrandTextField, intVarTextField, lowLimTextField, upLimTextField;
    @FXML
    private GridPane numGridPane;
    @FXML
    private TextField accuracyTextBox, iterationsTextBox;

    @FXML
    private void symRadioButtonAction() {
        numRadioButtonAction();
    }

    @FXML
    private void numRadioButtonAction() {
        numText.setVisible(numRadioButton.isSelected());
        symText.setVisible(!numRadioButton.isSelected());
//        startFromLabel.setVisible(numRadioButton.isSelected());
//        for (var startTextField : startTextFields) {
//            startTextField.setVisible(numRadioButton.isSelected());
//        }
//        if (numRadioButton.isSelected()) {
//            mainGridPane.getColumnConstraints().setAll(numColumnConstraints);
//            mainGridPane.setPadding(numInsets);
//        } else {
//            mainGridPane.getColumnConstraints().setAll(symColumnConstraints);
//            mainGridPane.setPadding(symInsets);
//        }
        numGridPane.setVisible(numRadioButton.isSelected());
//        symGridPane.setVisible(!numRadioButton.isSelected());
    }

    @Override
    String resultPreamble() {
        if (numRadioButton.isSelected()) {
            return "load_package numeric;\n";
        } else
            return "";
    }

    @Override
    String result() throws Template.EmptyFieldException {
        final String integrand = integrandTextField.getText(), intVar = intVarTextField.getText();
        if (integrand.isEmpty() || intVar.isEmpty()) {
            RunREDUCE.errorMessageDialog("Integrate Template Error",
                    "The integrand and integration variable are both required.");
            throw new EmptyFieldException();
        }
        final String lowLim = lowLimTextField.getText(), upLim = upLimTextField.getText();
        final boolean indefInt;
        if ((indefInt = lowLim.isEmpty()) ^ upLim.isEmpty()) {
            RunREDUCE.errorMessageDialog("Integrate Template Error",
                    "The limits must be both empty or both specified.");
            throw new EmptyFieldException();
        }
        if (indefInt && numRadioButton.isSelected()) {
            RunREDUCE.errorMessageDialog("Integrate Template Error",
                    "The limits must be specified when the 'Numeric' option is selected.");
            throw new EmptyFieldException();
        }
        final var text = new StringBuilder();
        if (numRadioButton.isSelected()) text.append("num_");
        text.append("int(").append(integrand).append(", ").append(intVar);
        if (!indefInt) text.append(", ").append(lowLim).append(", ").append(upLim);
        if (numRadioButton.isSelected()) {
            switchNameOnOff("rounded");
            var s = accuracyTextBox.getText().trim();
            if (!s.isEmpty()) text.append(", accuracy = ").append(s);
            s = iterationsTextBox.getText().trim();
            if (!s.isEmpty()) text.append(", iterations = ").append(s);
        }
        return text.append(")").toString();
    }
}
