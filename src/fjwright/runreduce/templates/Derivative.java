package fjwright.runreduce.templates;

import fjwright.runreduce.RunREDUCE;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.regex.Pattern;

import static java.util.Arrays.stream;

public class Derivative extends Template {
    @FXML
    private Label totalOrdLabel;
    @FXML
    private TextField depVarTextField, indVar0TextField, indVar1TextField, indVar2TextField;
    @FXML
    private TextField ord0TextField, ord1TextField, ord2TextField;

    private TextField[] indVarTextFields, ordTextFields;
    private final int[] orders = {1, 0, 0};

    private final static Pattern NUMBER_PATTERN = Pattern.compile("[1-9]\\d*");

    @FXML @Override
    protected void initialize() {
        super.initialize();
        indVarTextFields = new TextField[]{indVar0TextField, indVar1TextField, indVar2TextField};
        ordTextFields = new TextField[]{ord0TextField, ord1TextField, ord2TextField};
    }

    private void indVarAction(final int n) {
        final String indVar = indVarTextFields[n].getText(), ordString;
        int order;
        if (indVar.isEmpty())
            order = 0;
        else if (!VAR_PATTERN.matcher(indVar).matches()) {
            RunREDUCE.errorMessageDialog("Derivative Template Error",
                    "An independent variable entry must be an identifier or empty.");
            indVarTextFields[n].setText(""); // Error recovery
            return;
        } else if ((ordString = ordTextFields[n].getText()).isEmpty())
            order = 1;
        else if (!(NUMBER_PATTERN.matcher(ordString).matches() && (order = Integer.parseInt(ordString)) > 0)) {
            RunREDUCE.errorMessageDialog("Derivative Template Error",
                    "An order entry must be a positive integer or empty.");
            ordTextFields[n].setText(""); // Error recovery
            order = 1;
        }
        orders[n] = order;
        order = stream(orders).sum();
        if (order > 1)
            totalOrdLabel.setText(Integer.toString(order));
        else
            totalOrdLabel.setText("");
    }

    @FXML
    private void indVar1KeyTyped() {
        indVarAction(0);
    }

    @FXML
    private void indVar2KeyTyped() {
        indVarAction(1);
    }

    @FXML
    private void indVar3KeyTyped() {
        indVarAction(2);
    }

    @Override
    protected String result() throws EmptyFieldException {
        final String depVar = depVarTextField.getText();
        if (depVar.isEmpty()) {
            RunREDUCE.errorMessageDialog("Derivative Template Error",
                    "A dependent variable or expression is required.");
            throw new EmptyFieldException();
        }
        final var text = new StringBuilder("df(");
        text.append(depVar);
        for (var i = 0; i < 3; i++) {
            final String indVar = indVarTextFields[i].getText();
            if (!indVar.isEmpty()) {
                text.append(", ").append(indVar);
                if (orders[i] > 1) text.append(", ").append(orders[i]);
            } else if (i == 0) {
                RunREDUCE.errorMessageDialog("Derivative Template Error",
                        "The first independent variable is required.");
                throw new EmptyFieldException();
            }
        }
        return text.append(")").toString();
    }
}
