package fjwright.runreduce;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LoadPackagesDialog {
    @FXML
    private GridPane gridPane;
    @FXML
    private Button manualButton;

    private static final int COLUMNS = 10; // Adjust to taste!
    ToggleButton[] toggleButtons = new ToggleButton[RunREDUCEFrame.packageList.size()];

    @FXML
    private void initialize() {
        if (RunREDUCEFrame.packageList == null) return;
        int row = 0, col = 0, i = 0;
        ToggleButton tb;
        for (String pkg : RunREDUCEFrame.packageList) {
            gridPane.add(tb = new ToggleButton(pkg), col, row);
            tb.setMnemonicParsing(false); // to display _ as a normal label character!
            tb.setAlignment(Pos.CENTER_LEFT);
            tb.setMaxWidth(Double.MAX_VALUE);
            tb.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    ShowManualFor(((ToggleButton) e.getSource()).getText());
                } else if (e.getClickCount() == 2) {
                    RunREDUCE.reducePanel.menuSendStringToREDUCEAndEcho(
                            "load_package " + ((ToggleButton) e.getSource()).getText() + ";\n");
                    ((Stage) ((Node) e.getSource()).getScene().getWindow()).close();
                } else
                    manualButton.setDisable(Arrays.stream(toggleButtons).filter(ToggleButton::isSelected).count() != 1);
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

    private static final String[] redlogSubPackages = new String[]{
            "rlsupport", "rltools", "cl", "ofsf", "dvfsf", "acfsf", "dcfsf", "ibalp",
            "pasf", "qqe", "qqe_ofsf", "mri", "mri_ofsf", "mri_pasf", "talp", "smt"};

    static {
        Arrays.sort(redlogSubPackages);
    }

    @FXML
    private void manualButtonAction() {
        // Get the first (and should be the only) selected ToggleButton:
        Optional<ToggleButton> oTB =
                Arrays.stream(toggleButtons).filter(ToggleButton::isSelected).findFirst();
        if (oTB.isEmpty()) return;
        ShowManualFor(oTB.get().getText());
    }

    private void ShowManualFor(String searchText) { // E.g. odesolve
        // Use REDUCE Manual ToC as a jump table for searchText.
        if (Arrays.binarySearch(redlogSubPackages, searchText) >= 0) searchText = "redlog";
        searchText = searchText.toUpperCase();
        // Search for (e.g.) <a href="manualse139.html#x199-90800016.47" id="QQ2-199-920">
        // ODESOLVE: Ordinary differential equations solver</a>
        Pattern pattern = Pattern.compile(
                String.format("<a\\s+href=\"(manual.+?\\.html).*?\"\\s+id=\".+?\">%s: .*</a>", searchText));
        try {
            Matcher matcher = pattern.matcher(REDUCEManual.getToC());
            if (matcher.find())
                RunREDUCE.hostServices.showDocument(
                        REDUCEManual.getDirPath().resolve(matcher.group(1)).toString());
            else
                RunREDUCE.alert(Alert.AlertType.WARNING,
                        "REDUCE Manual Hyperlink Failure",
                        "Documentation not found for package " + searchText);
        } catch (IOException ignored) {
        }
    }
}
