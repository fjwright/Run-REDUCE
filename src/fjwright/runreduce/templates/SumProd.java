package fjwright.runreduce.templates;

import javafx.beans.binding.When;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

public class SumProd extends Template {
    @FXML
    private RadioButton sumRadioButton;
    @FXML
    private Label sumProdLabel;
    @FXML
    private TextField varTextField, lowLimTextField, upLimTextField, operandTextField;

    private final RadioButton numRadioButton = new RadioButton("Numeric");

    @FXML
    @Override
    protected void initialize() {
        super.initialize();
        var symRadioButton = new RadioButton("Symbolic");
        symRadioButton.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        symRadioButton.setSelected(true);
        new ToggleGroup().getToggles().addAll(symRadioButton, numRadioButton);
        var hBox = new HBox(symRadioButton, numRadioButton);
        hBox.setSpacing(10);
        hBox.setAlignment(Pos.CENTER);
        templateRoot.getChildren().add(0, hBox);
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
