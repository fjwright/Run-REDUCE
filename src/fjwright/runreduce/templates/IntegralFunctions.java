package fjwright.runreduce.templates;

import fjwright.runreduce.RunREDUCE;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

public class IntegralFunctions extends Template {
    @FXML
    private RadioButton numRadioButton;
    @FXML
    private ToggleGroup templateToggleGroup;
    @FXML
    private HBox hBox0, hBox1, hBox2, hBox3, hBox4, hBox5, hBox6, hBox7, hBox8, hBox9, hBox10, hBox11;
    @FXML
    private TextField expIntTextField, logIntTextField, sinIntTextField, cosIntTextField;
    @FXML
    private TextField altSinIntTextField, altCosIntTextField, hypSinIntTextField, hypCosIntTextField;
    @FXML
    private TextField erfTextField, erfcTextField, fresnelSinIntTextField, fresnelCosIntTextField;
    @FXML /* Switches: default off */
    private CheckBox complexCheckBox;
    @FXML /* Switches: default on */
    private CheckBox savesfsCheckBox;

    private HBox[] hBoxes;

    @FXML
    @Override
    protected void initialize() {
        super.initialize();
        hBoxes = new HBox[]{hBox0, hBox1, hBox2, hBox3, hBox4, hBox5, hBox6, hBox7, hBox8, hBox9, hBox10, hBox11};
    }

    @FXML
    private void symNumRadioButtonAction() {
        complexCheckBox.setVisible(numRadioButton.isSelected());
        savesfsCheckBox.setVisible(numRadioButton.isSelected());
    }

    @FXML
    private void templateRadioButtonOnAction(ActionEvent actionEvent) {
        for (var hBox : hBoxes) hBox.setDisable(true);
        int i = (int) ((RadioButton) actionEvent.getSource()).getUserData();
        hBoxes[i].setDisable(false);
    }

    @FXML
    private void hyperlinkOnMouseClickedAction(MouseEvent mouseEvent) {
        RunREDUCE.hostServices.showDocument(
                (String) ((Node) mouseEvent.getTarget()).getParent().getUserData());
    }

    @Override
    String result() throws EmptyFieldException {
        if (numRadioButton.isSelected()) {
            switchNameOnOff("rounded");
            switchCheckBoxesOnOff(complexCheckBox);
            switchCheckBoxesOffOn(savesfsCheckBox);
        }
        StringBuilder text = new StringBuilder();
        switch ((int) templateToggleGroup.getSelectedToggle().getUserData()) {
            case 0:
                text.append("Ei(").append(getTextCheckNonEmpty(expIntTextField));
                break;
            case 1:
                text.append("li(").append(getTextCheckNonEmpty(logIntTextField));
                break;
            case 2:
                text.append("Si(").append(getTextCheckNonEmpty(sinIntTextField));
                break;
            case 3:
                text.append("Ci(").append(getTextCheckNonEmpty(cosIntTextField));
                break;
            case 4:
                text.append("s_i(").append(getTextCheckNonEmpty(altSinIntTextField));
                break;
            case 5:
                text.append("Cin(").append(getTextCheckNonEmpty(altCosIntTextField));
                break;
            case 6:
                text.append("Shi(").append(getTextCheckNonEmpty(hypSinIntTextField));
                break;
            case 7:
                text.append("Chi(").append(getTextCheckNonEmpty(hypCosIntTextField));
                break;
            case 8:
                text.append("erf(").append(getTextCheckNonEmpty(erfTextField));
                break;
            case 9:
                text.append("erfc(").append(getTextCheckNonEmpty(erfcTextField));
                break;
            case 10:
                text.append("Fresnel_S(").append(getTextCheckNonEmpty(fresnelSinIntTextField));
                break;
            case 11:
                text.append("Fresnel_C(").append(getTextCheckNonEmpty(fresnelCosIntTextField));
                break;
        }
        return text.append(")").toString();
    }
}
