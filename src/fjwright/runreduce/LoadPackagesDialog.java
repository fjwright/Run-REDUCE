package fjwright.runreduce;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class LoadPackagesDialog {
    @FXML
    private GridPane gridPane;

    private static final int COLUMNS = 12; // Tweak later
    ToggleButton[] toggleButtons = new ToggleButton[RunREDUCEFrame.packageList.size()];

    @FXML
    private void initialize() {
        if (RunREDUCEFrame.packageList == null) return;
        int row = 0, col = 0, i = 0;
        ToggleButton tb;
        for (String pkg : RunREDUCEFrame.packageList) {
            gridPane.add(tb = new ToggleButton(pkg), col, row);
            toggleButtons[i++] = tb;
            if (++col == COLUMNS) {
                col = 0;
                row++;
            }
        }
    }

    @FXML
    private void loadButtonAction(ActionEvent actionEvent) {
        List<String> selectedPackages = new ArrayList<>();
        for (ToggleButton tb : toggleButtons) {
            if (tb.isSelected()) {
                selectedPackages.add(tb.getText());
            }
        }
        if (!selectedPackages.isEmpty()) {
            StringBuilder text = new StringBuilder("load_package ");
            text.append(selectedPackages.get(0));
            for (int i = 1; i < selectedPackages.size(); i++) {
                text.append(", ");
                text.append(selectedPackages.get(i));
            }
            text.append(";\n");
            RunREDUCE.reducePanel.sendStringToREDUCEAndEcho(text.toString());
        }
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
