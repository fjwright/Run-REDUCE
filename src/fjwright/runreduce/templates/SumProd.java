package fjwright.runreduce.templates;

import fjwright.runreduce.REDUCEConfiguration;
import fjwright.runreduce.RunREDUCE;
import javafx.beans.binding.When;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import java.io.File;

public class SumProd extends Template {
    @FXML
    private RadioButton sumRadioButton;
    @FXML
    private Hyperlink symHyperlink, numHyperlink;
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
        symHyperlink.visibleProperty().bind(numRadioButton.selectedProperty().not());
        numHyperlink.visibleProperty().bind(numRadioButton.selectedProperty());
    }

    @Override
    protected String result() throws EmptyFieldException {
        final var text = new StringBuilder();
        if (numRadioButton.isSelected()) {
            switchNameOnOff("rounded");
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

    @FXML
    private void redManHyperlinkOnAction(ActionEvent actionEvent) {
        RunREDUCE.hostServices.showDocument(
                (new File(RunREDUCE.reduceConfiguration.docRootDir,
                        (REDUCEConfiguration.windowsOS ? "lib/csl/reduce.doc/" : "/")
                                + ((Node) actionEvent.getTarget()).getUserData())).toString());
    }
}
