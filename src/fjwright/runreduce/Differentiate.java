package fjwright.runreduce;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.regex.Pattern;

import static java.util.Arrays.stream;

public class Differentiate {
    @FXML
    private Label totalOrdLabel;
    @FXML
    private TextField depVarTextField, indVar0TextField, indVar1TextField, indVar2TextField;
    @FXML
    private TextField ord0TextField, ord1TextField, ord2TextField;

    private TextField[] indVarTextFields, ordTextFields;
    private final int[] orders = {1, 0, 0};
    private final static Pattern PATTERN = Pattern.compile("\\d+");

    @FXML
    private void initialize() {
        indVarTextFields = new TextField[]{indVar0TextField, indVar1TextField, indVar2TextField};
        ordTextFields = new TextField[]{ord0TextField, ord1TextField, ord2TextField};
    }

    private void indVarAction(final int n) {
        final String ordString;
        int order = 0;
        if (indVarTextFields[n].getText().isEmpty()) {
            if (n == 0) {
                // Error:
                System.err.println("The first independent variable is required!");
                indVarTextFields[n].setText("x"); // Error recovery
                return;
            }
            // order = 0;
        } else if ((ordString = ordTextFields[n].getText()).isEmpty())
            order = 1;
        else if (!(PATTERN.matcher(ordString).matches() && (order = Integer.parseInt(ordString)) > 0)) {
            // Error:
            System.err.println("The order must be a positive integer or empty!");
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

    private String result() {
        final var text = new StringBuilder("df(");
        text.append(depVarTextField.getText());
        for (var i = 0; i < 3; i++) {
            final String indVar = indVarTextFields[i].getText();
            if (!indVar.isEmpty()) {
                text.append(",").append(indVar);
                if (orders[i] > 1) text.append(",").append(orders[i]);
            }
        }
        return text.append(")").toString();
    }

    @FXML
    private void editButtonAction() {
        // Insert in input editor:
        System.out.println(result());
    }

    @FXML
    private void evaluateButtonAction() {
        // Send to REDUCE:
        System.out.println(result() + ";\n");
    }

    @FXML
    private void closeButtonAction(ActionEvent actionEvent) {
        // Close dialogue:
        Node source = (Node) actionEvent.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
