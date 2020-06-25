package fjwright.runreduce.templates;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

import static fjwright.runreduce.templates.Templates.*;

public class SumProd {
    @FXML
    private ToggleButton sumToggleButton;
    @FXML
    private Label sumProdLabel;
    @FXML
    private TextField varTextField, lowLimTextField, upLimTextField, operandTextField;

    @FXML
    private void sumToggleButtonAction() {
        sumProdLabel.setText("∑");
    }

    @FXML
    private void prodToggleButtonAction() {
        sumProdLabel.setText("∏");
    }

// Check field entries dynamically ================================================================

    @FXML
    private void varCheckKeyTyped() {
        Templates.varCheckKeyTyped(varTextField);
    }

    @FXML
    private void upLimCheckKeyTyped() {
        intOrVarCheckKeyTyped(upLimTextField);
    }

    @FXML
    private void lowLimCheckKeyTyped() {
        intOrVarCheckKeyTyped(lowLimTextField);
    }

// Process the result =============================================================================

    private String result() throws EmptyFieldException {
        var text = new StringBuilder();
        text.append("for ").append(getTextCheckNonEmpty(varTextField))
                .append(" := ").append(getTextCheckNonEmpty(lowLimTextField))
                .append(" : ").append(getTextCheckNonEmpty(upLimTextField));
        if (sumToggleButton.isSelected())
            text.append(" sum ");
        else
            text.append(" product ");
        text.append(getTextCheckNonEmpty(operandTextField));
        return text.toString();
    }

    @FXML
    private void editButtonAction(ActionEvent actionEvent) {
        // Insert in input editor if valid:
        try {
            Templates.editButtonAction(actionEvent, result());
        } catch (EmptyFieldException ignored) {
        }
    }

    @FXML
    private void evaluateButtonAction(ActionEvent actionEvent) {
        // Send to REDUCE if valid:
        try {
            Templates.evaluateButtonAction(actionEvent, result());
        } catch (EmptyFieldException ignored) {
        }
    }

    @FXML
    private void cancelButtonAction(ActionEvent actionEvent) {
        // Close dialogue:
        Templates.cancelButtonAction(actionEvent);
    }
}
