package fjwright.runreduce.templates;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.stream;

public class Solve extends Template {
    @FXML
    private TextField eqn1TextField, eqn2TextField, eqn3TextField;
    @FXML
    private TextField var1TextField, var2TextField, var3TextField;
    @FXML /* Default off */
    private CheckBox multiplicitiesCheckBox, fullrootsCheckBox;
    @FXML /* Default on */
    private CheckBox trigformCheckBox, solvesingularCheckBox, allbranchCheckBox, arbvarsCheckBox;

    private TextField[] eqnTextFields, varTextFields;

    @FXML
    @Override
    protected void initialize() {
        super.initialize();
        eqnTextFields = new TextField[]{eqn1TextField, eqn2TextField, eqn3TextField};
        varTextFields = new TextField[]{var1TextField, var2TextField, var3TextField};
    }

    @Override
    String result() throws EmptyFieldException {
        switchesOnOff(multiplicitiesCheckBox, fullrootsCheckBox);
        switchesOffOn(trigformCheckBox, solvesingularCheckBox, allbranchCheckBox, arbvarsCheckBox);
        String[] eqns = stream(eqnTextFields).map(t -> t.getText().trim()).filter(s -> !s.isEmpty()).toArray(String[]::new);
        String[] vars = stream(varTextFields).map(t -> t.getText().trim()).filter(s -> !s.isEmpty()).toArray(String[]::new);
        if (eqns.length == 0) throw new EmptyFieldException();
        // Construct and return the REDUCE input:
        StringBuilder text = new StringBuilder("solve(");
        if (eqns.length == 1) text.append(eqns[0]);
        else {
            text.append("{");
            text.append(String.join(", ", eqns));
            text.append("}");
        }
        if (vars.length > 0) {
            text.append(", ");
            if (vars.length == 1) text.append(vars[0]);
            else {
                text.append("{");
                text.append(String.join(", ", vars));
                text.append("}");
            }
        }
        text.append(")");
        return text.toString();
    }
}
