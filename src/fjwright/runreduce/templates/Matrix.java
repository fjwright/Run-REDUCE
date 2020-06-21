package fjwright.runreduce.templates;

import fjwright.runreduce.RunREDUCE;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.lang.reflect.Array;

import static java.util.Arrays.stream;

public class Matrix {
    @FXML
    private TextField cell00, cell01, cell02, cell03;
    @FXML
    private TextField cell10, cell11, cell12, cell13;
    @FXML
    private TextField cell20, cell21, cell22, cell23;
    @FXML
    private TextField cell30, cell31, cell32, cell33;

    private TextField[][] cells;

    @FXML
    private void initialize() {
        cells = new TextField[][]{
                {cell00, cell01, cell02, cell03},
                {cell10, cell11, cell12, cell13},
                {cell20, cell21, cell22, cell23},
                {cell30, cell31, cell32, cell33}};
    }

    private String result() {
        final String[][] matrix =
                stream(cells).map(row -> stream(row).map(TextField::getText).filter(s -> !s.isEmpty()).toArray(String[]::new))
                        .filter(a -> a.length > 0).toArray(String[][]::new);
        if (stream(matrix).map(Array::getLength).distinct().count() != 1) {
            RunREDUCE.errorMessageDialog("Matrix Template",
                    "Rows do not all contain the same number of non-empty cells!");
            return null;
        }
        StringBuilder text = new StringBuilder("mat(");
        for (int i = 0; i < matrix.length; i++) {
            String[] row = matrix[i];
            if (i > 0) text.append(",");
            text.append("(");
            for (int j = 0; j < row.length; j++) {
                if (j > 0) text.append(",");
                text.append(row[j]);
            }
            text.append(")");
        }
        text.append(")");
        return text.toString();
    }

    @FXML
    private void editButtonAction(ActionEvent actionEvent) {
        // Insert in input editor if valid:
        final String r = result();
        if (r == null) return;
        final TextArea textArea = RunREDUCE.reducePanel.inputTextArea;
        textArea.insertText(textArea.getCaretPosition(), r);
        // Close dialogue:
        cancelButtonAction(actionEvent);
    }

    @FXML
    private void evaluateButtonAction(ActionEvent actionEvent) {
        // Send to REDUCE if valid:
        final String r = result();
        if (r == null) return;
        RunREDUCE.reducePanel.sendStringToREDUCEAndEcho(r + ";\n");
        // Close dialogue:
        cancelButtonAction(actionEvent);
    }

    @FXML
    private void cancelButtonAction(ActionEvent actionEvent) {
        // Close dialogue:
        Node source = (Node) actionEvent.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
