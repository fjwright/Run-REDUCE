package fjwright.runreduce.templates;

import fjwright.runreduce.RunREDUCE;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;

import java.util.regex.Pattern;

public class For extends Template {
    @FXML
    private TextField forVarTextField, fromTextField, stepTextField, untilTextField;
    @FXML
    private TextField foreachVarTextField, foreachListTextField, exprnTextField;
    @FXML
    private ChoiceBox<String> foreachChoiceBox, actionChoiceBox;
    @FXML
    private TabPane tabPane;

    @FXML
    @Override
    protected void initialize() {
        super.initialize();
        foreachChoiceBox.getSelectionModel().selectFirst();
        actionChoiceBox.getSelectionModel().selectFirst();
    }

    private static final Pattern LIST_OR_VAR_PATTERN = Pattern.compile("(?:[{!]|\\p{Alpha}).*");

    @FXML
    private void listOrVarCheckKeyTyped() {
        final String text = foreachListTextField.getText();
        if (!(text.isEmpty() || LIST_OR_VAR_PATTERN.matcher(text).matches())) {
            RunREDUCE.alert(Alert.AlertType.ERROR, "For Template Error",
                    "The 'list' entry must be a list or a variable that evaluates to a list.");
            foreachListTextField.setText("");
        }
    }

    @Override
    protected String result() throws EmptyFieldException {
        final var text = new StringBuilder();
        if (tabPane.getSelectionModel().getSelectedIndex() == 0) {
            // Iterate over a numerical range:
            text.append("for ").append(getTextCheckNonEmpty(forVarTextField))
                    .append(" := ").append(getTextCheckNonEmpty(fromTextField));
            final String step = stepTextField.getText().trim();
            if (step.isEmpty() || step.equals("1"))
                text.append(" : ");
            else
                text.append(" step ").append(step).append(" until ");
            text.append(getTextCheckNonEmpty(untilTextField));
        } else  // Iterate over a list:
            text.append("foreach ").append(getTextCheckNonEmpty(foreachVarTextField))
                    .append(" ").append(foreachChoiceBox.getSelectionModel().getSelectedItem())
                    .append(" ").append(getTextCheckNonEmpty(foreachListTextField));
        text.append(" ").append(actionChoiceBox.getSelectionModel().getSelectedItem())
                .append(" ").append(getTextCheckNonEmpty(exprnTextField));
        return text.toString();
    }
}
