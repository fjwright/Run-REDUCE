package fjwright.runreduce.functions;

import fjwright.runreduce.templates.Template;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

public class ExpLogEtc extends Template {
    @FXML
    private RadioButton numRadioButton;
    @FXML
    private ToggleGroup templateToggleGroup;
    @FXML
    private HBox hBox0, hBox1, hBox2, hBox3, hBox4, hBox5, hBox6, hBox7, hBox8;
    @FXML
    private TextField expTextField, powATextField, powBTextField, pow10TextField;
    @FXML
    private TextField lnTextField, logATextField, logBTextField, log10TextField;
    @FXML
    private TextField sqrtTextField, rootATextField, rootBTextField, facTextField;
    @FXML /* Switches: default off */
    private CheckBox expandlogsCheckBox, preciseComplexCheckBox, complexCheckBox;
    @FXML /* Switches: default on */
    private CheckBox preciseCheckBox;

    private HBox[] hBoxes;

    @FXML
    @Override
    protected void initialize() {
        super.initialize();
        hBoxes = new HBox[]{hBox0, hBox1, hBox2, hBox3, hBox4, hBox5, hBox6, hBox7, hBox8};
    }

    @FXML
    private void symNumRadioButtonAction() {
        complexCheckBox.setVisible(numRadioButton.isSelected());
        expandlogsCheckBox.setVisible(!numRadioButton.isSelected());
        preciseCheckBox.setVisible(!numRadioButton.isSelected());
        preciseComplexCheckBox.setVisible(!numRadioButton.isSelected());
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
        } else {
            switchCheckBoxesOnOff(preciseComplexCheckBox, expandlogsCheckBox);
            switchCheckBoxesOffOn(preciseCheckBox);
        }
        StringBuilder text = new StringBuilder();
        switch ((int) templateToggleGroup.getSelectedToggle().getUserData()) {
            case 0:
                text.append("exp(").append(getTextCheckNonEmpty(expTextField));
                break;
            case 1:
                text.append("(").append(getTextCheckNonEmpty(powATextField)).append(")^(")
                        .append(getTextCheckNonEmpty(powBTextField));
                break;
            case 2:
                text.append("10^(").append(getTextCheckNonEmpty(pow10TextField));
                break;
            case 3:
                text.append("log(").append(getTextCheckNonEmpty(lnTextField));
                break;
            case 4:
                text.append("logb(").append(getTextCheckNonEmpty(logBTextField)).append(", ")
                        .append(getTextCheckNonEmpty(logATextField));
                break;
            case 5:
                text.append("log10(").append(getTextCheckNonEmpty(log10TextField));
                break;
            case 6:
                text.append("sqrt(").append(getTextCheckNonEmpty(sqrtTextField));
                break;
            case 7:
                text.append("(").append(getTextCheckNonEmpty(rootBTextField)).append(")^(1/(")
                        .append(getTextCheckNonEmpty(rootATextField)).append(")");
                break;
            case 8:
                text.append("factorial(").append(getTextCheckNonEmpty(facTextField));
                break;
        }
        return text.append(")").toString();
    }
}
