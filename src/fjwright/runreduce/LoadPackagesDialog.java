package fjwright.runreduce;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LoadPackagesDialog {
    @FXML
    private GridPane gridPane;

    private static final int COLUMNS = 10; // Adjust to taste!
    ToggleButton[] toggleButtons = new ToggleButton[RunREDUCEFrame.packageList.size()];

    @FXML
    private void initialize() {
        if (RunREDUCEFrame.packageList == null) return;
        int row = 0, col = 0, i = 0;
        ToggleButton tb;
        for (String pkg : RunREDUCEFrame.packageList) {
            gridPane.add(tb = new ToggleButton(pkg), col, row);
            tb.setAlignment(Pos.CENTER_LEFT);
            tb.setMaxWidth(Double.MAX_VALUE);
            tb.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    RunREDUCE.reducePanel.menuSendStringToREDUCEAndEcho(
                            "load_package " + ((ToggleButton) e.getSource()).getText() + ";\n");
                    ((Stage) ((Node) e.getSource()).getScene().getWindow()).close();
                }
            });
            toggleButtons[i++] = tb;
            if (++col == COLUMNS) {
                col = 0;
                row++;
            }
        }
    }

    @FXML
    private void loadButtonAction(ActionEvent actionEvent) {
        List<String> selectedPackages = Arrays.stream(toggleButtons).filter(ToggleButton::isSelected)
                .map(Labeled::getText).collect(Collectors.toList());
        if (!selectedPackages.isEmpty()) {
            StringBuilder text = new StringBuilder("load_package ");
            text.append(selectedPackages.get(0));
            for (int i = 1; i < selectedPackages.size(); i++) {
                text.append(", ");
                text.append(selectedPackages.get(i));
            }
            text.append(";\n");
            RunREDUCE.reducePanel.menuSendStringToREDUCEAndEcho(text.toString());
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
