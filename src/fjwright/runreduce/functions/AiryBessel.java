package fjwright.runreduce.functions;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class AiryBessel extends Functions {
    @FXML
    private TextField aiTextField, biTextField, aiPrimeTextField, biPrimeTextField;
    @FXML
    private TextField jNuTextField, jZTextField, yNuTextField, yZTextField;
    @FXML
    private TextField iNuTextField, iZTextField, kNuTextField, kZTextField;
    @FXML
    private TextField h1NuTextField, h1ZTextField, h2NuTextField, h2ZTextField;

    @FXML
    @Override
    protected void symNumRadioButtonAction() {
//        complexCheckBox.setVisible(numRadioButton.isSelected());
        savesfsCheckBox.setVisible(numRadioButton.isSelected());
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
