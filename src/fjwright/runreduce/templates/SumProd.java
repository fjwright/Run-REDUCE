package fjwright.runreduce.templates;

import javafx.beans.binding.When;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

public class SumProd extends Template {
    @FXML
    private RadioButton sumRadioButton;
    @FXML
    private Label sumProdLabel;
    @FXML
    private TextField varTextField, lowLimTextField, upLimTextField, operandTextField;

    @FXML
    @Override
    protected void initialize() {
        super.initialize();
        addSymNumRadioButtons();
        sumProdLabel.textProperty().bind(
                new When(sumRadioButton.selectedProperty()).then("∑").otherwise("∏"));
    }

    @Override
    protected String result() throws EmptyFieldException {
        final var text = new StringBuilder();
        if (numRadioButton.isSelected()) {
            text.append("for ").append(getTextCheckNonEmpty(varTextField))
                    .append(" := ").append(getTextCheckNonEmpty(lowLimTextField))
                    .append(" : ").append(getTextCheckNonEmpty(upLimTextField));
            if (sumRadioButton.isSelected())
                text.append(" sum ");
            else
                text.append(" product ");
            text.append(getTextCheckNonEmpty(operandTextField));
        } else {
            if (sumRadioButton.isSelected())
                text.append("sum(");
            else
                text.append("prod(");
            text.append(getTextCheckNonEmpty(operandTextField))
                    .append(", ").append(getTextCheckNonEmpty(varTextField));
            var t = lowLimTextField.getText().trim();
            if (!t.isEmpty()) {
                text.append(", ").append(t);
                t = upLimTextField.getText().trim();
                if (!t.isEmpty()) text.append(", ").append(t);
            }
            text.append(")");
        }
        return text.toString();
    }
}
