package fjwright.runreduce;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class Integrate {
    @FXML
    private TextField integrandTextField, intVarTextField, lowLimTextField, upLimTextField;

    private String result() {
        final String integrand = integrandTextField.getText(), intVar = intVarTextField.getText();
        if (integrand.isEmpty() || intVar.isEmpty()) {
            RunREDUCE.errorMessageDialog("Integrate Template",
                    "The integrand and integration variable are both required!");
            return null;
        }
        final String lowLim = lowLimTextField.getText(), upLim = upLimTextField.getText();
        final boolean indefInt;
        if ((indefInt = lowLim.isEmpty()) ^ upLim.isEmpty()) {
            RunREDUCE.errorMessageDialog("Integrate Template",
                    "The limits must be both empty or both specified!");
            return null;
        }
        final var text = new StringBuilder("int(");
        text.append(integrand).append(",").append(intVar);
        if (!indefInt) text.append(",").append(lowLim).append(",").append(upLim);
        return text.append(")").toString();
    }

    @FXML
    private void editButtonAction(ActionEvent actionEvent) {
        // Insert in input editor if valid:
        final String r = result();
        if (r == null) return;
        final TextArea textArea = RunREDUCE.reducePanel.inputTextArea;
        textArea.insertText(textArea.getCaretPosition(), r);
        // Close dialogue:
        cancelButtonAction(actionEvent);
    }

    @FXML
    private void evaluateButtonAction(ActionEvent actionEvent) {
        // Send to REDUCE if valid:
        final String r = result();
        if (r == null) return;
        RunREDUCE.reducePanel.sendStringToREDUCEAndEcho(r + ";\n");
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
