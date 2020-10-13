package fjwright.runreduce.templates;

import fjwright.runreduce.RunREDUCE;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

import static java.util.Arrays.stream;

public class Matrix extends Template {
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
    @Override
    protected void initialize() {
        super.initialize();
        cells = new TextField[][]{
                {cell00, cell01, cell02, cell03},
                {cell10, cell11, cell12, cell13},
                {cell20, cell21, cell22, cell23},
                {cell30, cell31, cell32, cell33}};
    }

    @Override
    protected String result() throws EmptyFieldException {
        // Construct an array of Strings from the array of TextFields:
        final String[][] stringArray =
                stream(cells).map(
                        row -> stream(row).map(TextField::getText).toArray(String[]::new)
                ).toArray(String[][]::new);
        // Determine the matrix dimensions (nRows * nCols) and convert empty strings to "0":
        int nColsI, nCols = 0, nRows = 0;
        for (int i = 0; i < stringArray.length; i++) {
            String[] row = stringArray[i];
            nColsI = 0;
            for (int j = 0; j < row.length; j++) {
                if (row[j].isEmpty()) row[j] = "0";
                else nColsI = j + 1;
            }
            if (nColsI > nCols) nCols = nColsI;
            if (nColsI > 0) nRows = i + 1;
        }
        if (nRows == 0) {
            RunREDUCE.alert(Alert.AlertType.ERROR, "Matrix Template Error",
                    "A least one field must be non-empty.");
            throw new EmptyFieldException();
        }
        // Construct and return the REDUCE input:
        StringBuilder text = new StringBuilder("mat(");
        for (int i = 0; i < nRows; i++) {
            String[] row = stringArray[i];
            if (i > 0) text.append(", ");
            text.append("(");
            for (int j = 0; j < nCols; j++) {
                if (j > 0) text.append(", ");
                text.append(row[j]);
            }
            text.append(")");
        }
        text.append(")");
        return text.toString();
    }
}
