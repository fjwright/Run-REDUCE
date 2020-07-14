package fjwright.runreduce.functions;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class IntegralFunctions extends Functions {
    @FXML
    private TextField expIntTextField, logIntTextField, sinIntTextField, cosIntTextField;
    @FXML
    private TextField hypSinIntTextField, hypCosIntTextField;
    @FXML
    private TextField erfTextField, erfcTextField, fresnelSinIntTextField, fresnelCosIntTextField;

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
                text.append("Ei(").append(getTextCheckNonEmpty(expIntTextField));
                break;
            case 1:
                text.append("li(").append(getTextCheckNonEmpty(logIntTextField));
                preamble("load_package specfn;\n");
                break;
            case 2:
                text.append("Si(").append(getTextCheckNonEmpty(sinIntTextField));
                break;
            case 3:
                text.append("Ci(").append(getTextCheckNonEmpty(cosIntTextField));
                break;
            case 4:
                text.append("Shi(").append(getTextCheckNonEmpty(hypSinIntTextField));
                preamble("load_package specfn;\n");
                break;
            case 5:
                text.append("Chi(").append(getTextCheckNonEmpty(hypCosIntTextField));
                preamble("load_package specfn;\n");
                break;
            case 6:
                text.append("erf(").append(getTextCheckNonEmpty(erfTextField));
                break;
            case 7:
                text.append("erfc(").append(getTextCheckNonEmpty(erfcTextField));
                break;
            case 8:
                text.append("Fresnel_S(").append(getTextCheckNonEmpty(fresnelSinIntTextField));
                preamble("load_package specfn;\n");
                break;
            case 9:
                text.append("Fresnel_C(").append(getTextCheckNonEmpty(fresnelCosIntTextField));
                preamble("load_package specfn;\n");
                break;
        }
        return text.append(")").toString();
    }
}
