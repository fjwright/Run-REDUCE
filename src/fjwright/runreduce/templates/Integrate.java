package fjwright.runreduce.templates;

import fjwright.runreduce.RunREDUCE;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class Integrate extends Template{
    @FXML
    private TextField integrandTextField, intVarTextField, lowLimTextField, upLimTextField;

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
        final var text = new StringBuilder("int(");
        text.append(integrand).append(",").append(intVar);
        if (!indefInt) text.append(",").append(lowLim).append(",").append(upLim);
        return text.append(")").toString();
    }
}
