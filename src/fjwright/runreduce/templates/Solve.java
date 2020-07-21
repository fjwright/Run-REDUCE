package fjwright.runreduce.templates;

import fjwright.runreduce.RunREDUCE;
import javafx.beans.binding.When;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import static java.util.Arrays.stream;

public class Solve extends Template {
    @FXML
    private HBox symText, numText;
    @FXML
    private TextField eqn1TextField, eqn2TextField, eqn3TextField;
    @FXML
    private TextField var1TextField, var2TextField, var3TextField;
    @FXML
    private Label startFromLabel;
    @FXML
    private TextField start1TextField, start2TextField, start3TextField;
    @FXML
    private GridPane mainGridPane, symGridPane, numGridPane;
    @FXML /* Default off */
    private CheckBox multiplicitiesCheckBox, fullrootsCheckBox;
    @FXML /* Default on */
    private CheckBox trigformCheckBox, solvesingularCheckBox, allbranchCheckBox, arbvarsCheckBox;
    @FXML /* Default off */
    private CheckBox complexCheckBox;
    @FXML
    private TextField accuracyTextBox, iterationsTextBox;

    private TextField[] eqnTextFields, varTextFields;

    private final ColumnConstraints[] columnConstraints = new ColumnConstraints[]{
            new ColumnConstraints(), new ColumnConstraints(), new ColumnConstraints()
    };
    private final Insets symInsets = new Insets(0, -10, 0, 0);
    private final Insets numInsets = new Insets(0);

    @FXML
    @Override
    protected void initialize() {
        super.initialize();
        addSymNumRadioButtons();
        eqnTextFields = new TextField[]{eqn1TextField, eqn2TextField, eqn3TextField};
        varTextFields = new TextField[]{var1TextField, var2TextField, var3TextField};
        numText.visibleProperty().bind(numRadioButton.selectedProperty());
        symText.visibleProperty().bind(numRadioButton.selectedProperty().not());
        startFromLabel.visibleProperty().bind(numRadioButton.selectedProperty());
        for (var startTextField : new TextField[]{start1TextField, start2TextField, start3TextField})
            startTextField.visibleProperty().bind(numRadioButton.selectedProperty());
        mainGridPane.paddingProperty().bind(
                new When(numRadioButton.selectedProperty()).then(numInsets).otherwise(symInsets));
        mainGridPane.getColumnConstraints().setAll(columnConstraints);
        columnConstraints[0].percentWidthProperty().bind(
                new When(numRadioButton.selectedProperty()).then(60).otherwise(75));
        columnConstraints[1].percentWidthProperty().bind(
                new When(numRadioButton.selectedProperty()).then(20).otherwise(25));
        columnConstraints[2].percentWidthProperty().bind(
                new When(numRadioButton.selectedProperty()).then(20).otherwise(0));
        numGridPane.visibleProperty().bind(numRadioButton.selectedProperty());
        symGridPane.visibleProperty().bind(numRadioButton.selectedProperty().not());
    }

    @Override
    protected String result() throws EmptyFieldException {
        if (numRadioButton.isSelected()) {
            preamble("load_package numeric;\n");
            switchNameOnOff("rounded");
            switchCheckBoxesOnOff(complexCheckBox);
        } else {
            switchCheckBoxesOnOff(multiplicitiesCheckBox, fullrootsCheckBox);
            switchCheckBoxesOffOn(trigformCheckBox, solvesingularCheckBox, allbranchCheckBox, arbvarsCheckBox);
        }
        // Process equations:
        String[] eqns = stream(eqnTextFields).map(t -> t.getText().trim()).filter(s -> !s.isEmpty()).toArray(String[]::new);
        if (eqns.length == 0) {
            RunREDUCE.errorMessageDialog("Solve Template Error",
                    "At least one equation/expression field is required.");
            throw new EmptyFieldException();
        }
        // Construct and return the REDUCE input:
        StringBuilder text = new StringBuilder();
        if (numRadioButton.isSelected()) text.append("num_");
        text.append("solve(");
        if (eqns.length == 1) text.append(eqns[0]);
        else text.append("{").append(String.join(", ", eqns)).append("}");
        // Process variables and, if numeric, start values:
        String[] vars = stream(varTextFields).map(t -> t.getText().trim()).filter(s -> !s.isEmpty()).toArray(String[]::new);
        if (numRadioButton.isSelected()) {
            // Numeric:
            if (vars.length == 0) {
                RunREDUCE.errorMessageDialog("Solve Numeric Template Error",
                        "All variable(s) must be specified.");
                throw new EmptyFieldException();
            }
            text.append(", ");
            if (vars.length == 1) {
                text.append(vars[0]);
                String start = start1TextField.getText().trim();
                if (!start.isEmpty()) text.append(" = ").append(start);
            } else {
                text.append("{");
                for (int i = 0; i < vars.length; i++) {
                    if (i > 0) text.append(", ");
                    text.append(vars[i]);
                    String start = start1TextField.getText().trim();
                    if (!start.isEmpty()) text.append(" = ").append(start);
                }
                text.append("}");
            }
            var s = accuracyTextBox.getText().trim();
            if (!s.isEmpty()) text.append(", accuracy = ").append(s);
            s = iterationsTextBox.getText().trim();
            if (!s.isEmpty()) text.append(", iterations = ").append(s);
        } else {
            // Symbolic:
            if (vars.length > 0) {
                text.append(", ");
                if (vars.length == 1) text.append(vars[0]);
                else text.append("{").append(String.join(", ", vars)).append("}");
            }
        }
        return text.append(")").toString();
    }
}
