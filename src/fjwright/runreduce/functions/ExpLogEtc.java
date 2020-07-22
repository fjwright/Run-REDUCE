package fjwright.runreduce.functions;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class ExpLogEtc extends Functions {
    @FXML
    private TextField expTextField, powATextField, powBTextField;
    @FXML
    private TextField lnTextField, logATextField, logBTextField, log10TextField;
    @FXML
    private TextField sqrtTextField, rootATextField, rootBTextField, facTextField;
    @FXML
    private TextField atan2YTextField, atan2XTextField, atan2DYTextField, atan2DXTextField;
    @FXML
    private TextField binCoeffNTextField, binCoeffMTextField;
    @FXML
    private TextField hypotXTextField, hypotYTextField;
    @FXML /* Switches: default off */
    private CheckBox expandlogsCheckBox, preciseComplexCheckBox, complexCheckBox;
    @FXML /* Switches: default on */
    private CheckBox preciseCheckBox;

    @FXML
    @Override
    protected void initialize() {
        super.initialize();
        expandlogsCheckBox.visibleProperty().bind(numRadioButton.selectedProperty().not());
        preciseCheckBox.visibleProperty().bind(numRadioButton.selectedProperty().not());
        preciseComplexCheckBox.visibleProperty().bind(numRadioButton.selectedProperty().not());
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
                text.append("log(").append(getTextCheckNonEmpty(lnTextField));
                break;
            case 2:
                text.append("logb(").append(getTextCheckNonEmpty(logBTextField))
                        .append(", ").append(getTextCheckNonEmpty(logATextField));
                break;
            case 3:
                text.append("(").append(getTextCheckNonEmpty(powATextField))
                        .append(")^(").append(getTextCheckNonEmpty(powBTextField));
                break;
            case 4:
                text.append("sqrt(").append(getTextCheckNonEmpty(sqrtTextField));
                break;
            case 5:
                text.append("hypot(").append(getTextCheckNonEmpty(hypotXTextField))
                        .append(", ").append(getTextCheckNonEmpty(hypotYTextField));
                break;
            case 6:
                text.append("log10(").append(getTextCheckNonEmpty(log10TextField));
                break;
            case 7:
                text.append("(").append(getTextCheckNonEmpty(rootBTextField)).append(")^(1/(")
                        .append(getTextCheckNonEmpty(rootATextField)).append(")");
                break;
            case 8:
                text.append("atan2(").append(getTextCheckNonEmpty(atan2YTextField))
                        .append(", ").append(getTextCheckNonEmpty(atan2XTextField));
                break;
            case 9:
                text.append("factorial(").append(getTextCheckNonEmpty(facTextField));
                break;
            case 10:
                text.append("binomial(").append(getTextCheckNonEmpty(binCoeffNTextField))
                        .append(", ").append(getTextCheckNonEmpty(binCoeffMTextField));
                break;
            case 11:
                preamble("load_package trigd;\n");
                text.append("atan2d(").append(getTextCheckNonEmpty(atan2DYTextField))
                        .append(", ").append(getTextCheckNonEmpty(atan2DXTextField));
                break;
        }
        return text.append(")").toString();
    }
}
