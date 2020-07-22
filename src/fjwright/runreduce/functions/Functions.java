package fjwright.runreduce.functions;

import fjwright.runreduce.templates.Template;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
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
