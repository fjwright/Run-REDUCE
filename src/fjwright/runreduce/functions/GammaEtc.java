package fjwright.runreduce.functions;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class GammaEtc extends Functions {
    @FXML
    private TextField gammaTextField, betaATextField, betaBTextField,
            digammaTextField, polygammaNTextField, polygammaZTextField,
            pATextField, pZTextField, iXTextField, iATextField, iBTextField,
            dilogTextField, pochhammerATextField, pochhammerNTextField,
            binCoeffMTextField, binCoeffNTextField, zetaTextField;

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
                text.append("Gamma(").append(getTextCheckNonEmpty(gammaTextField));
                break;
            case 1:
                text.append("Beta(").append(getTextCheckNonEmpty(betaATextField))
                        .append(", ").append(getTextCheckNonEmpty(betaBTextField));
                break;
            case 2:
                text.append("psi(").append(getTextCheckNonEmpty(digammaTextField));
                break;
            case 3:
                text.append("polygamma(").append(getTextCheckNonEmpty(polygammaNTextField))
                        .append(", ").append(getTextCheckNonEmpty(polygammaZTextField));
                break;
            case 4:
                text.append("iGamma(").append(getTextCheckNonEmpty(pATextField))
                        .append(", ").append(getTextCheckNonEmpty(pZTextField));
                break;
            case 5:
                text.append("iBeta(").append(getTextCheckNonEmpty(iATextField))
                        .append(", ").append(getTextCheckNonEmpty(iBTextField))
                        .append(", ").append(getTextCheckNonEmpty(iXTextField));
                break;
            case 6:
                text.append("dilog(").append(getTextCheckNonEmpty(dilogTextField));
                break;
            case 7:
                text.append("Pochhammer(").append(getTextCheckNonEmpty(pochhammerATextField))
                        .append(", ").append(getTextCheckNonEmpty(pochhammerNTextField));
                break;
            case 8:
                text.append("binomial(").append(getTextCheckNonEmpty(binCoeffMTextField))
                        .append(", ").append(getTextCheckNonEmpty(binCoeffNTextField));
                break;
            case 9:
                text.append("zeta(").append(getTextCheckNonEmpty(zetaTextField));
                break;
        }
        return text.append(")").toString();
    }
}
