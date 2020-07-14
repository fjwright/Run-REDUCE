package fjwright.runreduce.functions;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class ExpLogEtc extends Functions {
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

    @FXML
    @Override
    protected void symNumRadioButtonAction() {
        complexCheckBox.setVisible(numRadioButton.isSelected());
        expandlogsCheckBox.setVisible(!numRadioButton.isSelected());
        preciseCheckBox.setVisible(!numRadioButton.isSelected());
        preciseComplexCheckBox.setVisible(!numRadioButton.isSelected());
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
