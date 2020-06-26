package fjwright.runreduce.templates;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

public class SumProd extends Template {
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

    @Override
    String result() throws EmptyFieldException {
        final var text = new StringBuilder();
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
}
