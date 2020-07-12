package fjwright.runreduce.functions;

import fjwright.runreduce.templates.Template;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

public class AiryBessel extends Template {
    @FXML
    private RadioButton numRadioButton;
    @FXML
    private ToggleGroup templateToggleGroup;
    @FXML
    private HBox hBox0, hBox1, hBox2, hBox3, hBox4, hBox5, hBox6, hBox7, hBox8, hBox9;
    @FXML
    private TextField aiTextField, biTextField, aiPrimeTextField, biPrimeTextField;
    @FXML
    private TextField jNuTextField, jZTextField, yNuTextField, yZTextField;
    @FXML
    private TextField iNuTextField, iZTextField, kNuTextField, kZTextField;
    @FXML
    private TextField h1NuTextField, h1ZTextField, h2NuTextField, h2ZTextField;
    @FXML /* Switches: default off */
    private CheckBox complexCheckBox;
    @FXML /* Switches: default on */
    private CheckBox savesfsCheckBox;

    private HBox[] hBoxes;

    @FXML
    @Override
    protected void initialize() {
        super.initialize();
        hBoxes = new HBox[]{hBox0, hBox1, hBox2, hBox3, hBox4, hBox5, hBox6, hBox7, hBox8, hBox9};
    }

    @FXML
    private void symNumRadioButtonAction() {
//        complexCheckBox.setVisible(numRadioButton.isSelected());
        savesfsCheckBox.setVisible(numRadioButton.isSelected());
    }

    @FXML
    private void templateRadioButtonOnAction(ActionEvent actionEvent) {
        for (var hBox : hBoxes) hBox.setDisable(true);
        int i = (int) ((RadioButton) actionEvent.getSource()).getUserData();
        hBoxes[i].setDisable(false);
    }

    @Override
    protected String result() throws EmptyFieldException {
        if (numRadioButton.isSelected()) {
            switchNameOnOff("rounded");
            switchCheckBoxesOnOff(complexCheckBox);
            switchCheckBoxesOffOn(savesfsCheckBox);
        }
        StringBuilder text = new StringBuilder();
        switch ((int) templateToggleGroup.getSelectedToggle().getUserData()) {
            case 0:
                text.append("Airy_Ai(").append(getTextCheckNonEmpty(aiTextField));
                break;
            case 1:
                text.append("Airy_Bi(").append(getTextCheckNonEmpty(biTextField));
                break;
            case 2:
                text.append("Airy_AiPrime(").append(getTextCheckNonEmpty(aiPrimeTextField));
                break;
            case 3:
                text.append("Airy_BiPrime(").append(getTextCheckNonEmpty(biPrimeTextField));
                break;
            case 4:
                text.append("BesselJ(").append(getTextCheckNonEmpty(jNuTextField))
                        .append(", ").append(getTextCheckNonEmpty(jZTextField));
                break;
            case 5:
                text.append("BesselY(").append(getTextCheckNonEmpty(yNuTextField))
                        .append(", ").append(getTextCheckNonEmpty(yZTextField));
                break;
            case 6:
                text.append("BesselI(").append(getTextCheckNonEmpty(iNuTextField))
                        .append(", ").append(getTextCheckNonEmpty(iZTextField));
                break;
            case 7:
                text.append("BesselK(").append(getTextCheckNonEmpty(kNuTextField))
                        .append(", ").append(getTextCheckNonEmpty(kZTextField));
                break;
            case 8:
                text.append("Hankel1(").append(getTextCheckNonEmpty(h1NuTextField))
                        .append(", ").append(getTextCheckNonEmpty(h1ZTextField));
                break;
            case 9:
                text.append("Hankel2(").append(getTextCheckNonEmpty(h2NuTextField))
                        .append(", ").append(getTextCheckNonEmpty(h2ZTextField));
                break;
        }
        return text.append(")").toString();
    }
}
