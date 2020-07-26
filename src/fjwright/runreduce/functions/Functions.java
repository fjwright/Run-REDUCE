package fjwright.runreduce.functions;

import fjwright.runreduce.templates.Template;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public abstract class Functions extends Template {
    @FXML
    protected ToggleGroup templateToggleGroup;
    @FXML
    private HBox hBox0, hBox1, hBox2, hBox3, hBox4, hBox5, hBox6, hBox7, hBox8, hBox9, hBox10, hBox11;

    @FXML /* Switches: default off */ protected CheckBox complexCheckBox;
    @FXML /* Switches: default on */ protected CheckBox savesfsCheckBox;

    private HBox[] hBoxes;

    @FXML
    @Override
    protected void initialize() {
        super.initialize();
        addSymNumRadioButtons();
        if (complexCheckBox != null) complexCheckBox.visibleProperty().bind(numRadioButton.selectedProperty());
        if (savesfsCheckBox != null) savesfsCheckBox.visibleProperty().bind(numRadioButton.selectedProperty());
        hBoxes = new HBox[]{hBox0, hBox1, hBox2, hBox3, hBox4, hBox5, hBox6, hBox7, hBox8, hBox9, hBox10, hBox11};

        // Add REDUCE Manual hyperlinks:
        var hBox = new HBox(new Label("REDUCE Manual: "));
        hBox.setAlignment(Pos.CENTER);
        var tooltip = new Tooltip("Click to visit the appropriate section of the REDUCE Manual in your default browser.");
        var hyperlink1 = new Hyperlink("Mathematical Functions");
        hyperlink1.setUserData("Mathematical Functions");
        hyperlink1.setOnAction(this::redManHyperlinkOnAction);
        hyperlink1.setTooltip(tooltip);
        var hyperlink2 = new Hyperlink("SPECFN Package");
        hyperlink2.setUserData("SPECFN: Package for special functions");
        hyperlink2.setOnAction(this::redManHyperlinkOnAction);
        hyperlink2.setTooltip(tooltip);
        hBox.getChildren().addAll(hyperlink1, hyperlink2);
        templateRoot.getChildren().add(2, hBox);
    }

    @FXML
    private void templateRadioButtonOnAction(ActionEvent actionEvent) {
        for (var hBox : hBoxes) {
            if (hBox == null) break;
            hBox.setDisable(true);
        }
        int i = (int) ((RadioButton) actionEvent.getSource()).getUserData();
        hBoxes[i].setDisable(false);
    }
}
