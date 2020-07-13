package fjwright.runreduce.functions;

import fjwright.runreduce.templates.Template;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

public class GammaEtc extends Template {
    @FXML
    private RadioButton numRadioButton;
    @FXML
    private ToggleGroup templateToggleGroup;
    @FXML
    private HBox hBox0, hBox1, hBox2, hBox3, hBox4, hBox5, hBox6, hBox7, hBox8, hBox9;
    @FXML
    private TextField gammaTextField, betaATextField, betaBTextField,
            digammaTextField, polygammaNTextField, polygammaZTextField,
            pATextField, pZTextField, iXTextField, iATextField, iBTextField,
            dilogTextField, pochhammerATextField, pochhammerNTextField,
            binCoeffMTextField, binCoeffNTextField, zetaTextField;
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
        complexCheckBox.setVisible(numRadioButton.isSelected());
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
