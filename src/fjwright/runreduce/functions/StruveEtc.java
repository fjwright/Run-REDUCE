package fjwright.runreduce.functions;

import fjwright.runreduce.templates.Template;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

public class StruveEtc extends Template {
    @FXML
    private RadioButton numRadioButton;
    @FXML
    private ToggleGroup templateToggleGroup;
    @FXML
    private HBox hBox0, hBox1, hBox2, hBox3, hBox4, hBox5, hBox6, hBox7, hBox8, hBox9;
    @FXML
    private TextField struveHNuTextField, struveHZTextField, struveLNuTextField, struveLZTextField;
    @FXML
    private TextField lommel1MuTextField, lommel1NuTextField, lommel1ZTextField, lommel2MuTextField;
    @FXML
    private TextField lommel2NuTextField, lommel2ZTextField, kummerMATextField, kummerMBTextField;
    @FXML
    private TextField kummerMZTextField, kummerUATextField, kummerUBTextField, kummerUZTextField;
    @FXML
    private TextField whittakerMKappaTextField, whittakerMMuTextField, whittakerMZTextField;
    @FXML
    private TextField whittakerWKappaTextField, whittakerWMuTextField, whittakerWZTextField;
    @FXML
    private TextField sphericalHarmonicNTextField, sphericalHarmonicMTextField,
            sphericalHarmonicThetaTextField, sphericalHarmonicPhiTextField;
    @FXML
    private TextField solidHarmonicNTextField, solidHarmonicMTextField, solidHarmonicXTextField,
            solidHarmonicYTextField, solidHarmonicZTextField, solidHarmonicR2TextField;
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
                text.append("StruveH(").append(getTextCheckNonEmpty(struveHNuTextField))
                        .append(", ").append(getTextCheckNonEmpty(struveHZTextField));
                break;
            case 1:
                text.append("StruveL(").append(getTextCheckNonEmpty(struveLNuTextField))
                        .append(", ").append(getTextCheckNonEmpty(struveLZTextField));
                break;
            case 2:
                text.append("Lommel1(").append(getTextCheckNonEmpty(lommel1MuTextField))
                        .append(", ").append(getTextCheckNonEmpty(lommel1NuTextField))
                        .append(", ").append(getTextCheckNonEmpty(lommel1ZTextField));
                break;
            case 3:
                text.append("Lommel2(").append(getTextCheckNonEmpty(lommel2MuTextField))
                        .append(", ").append(getTextCheckNonEmpty(lommel2NuTextField))
                        .append(", ").append(getTextCheckNonEmpty(lommel2ZTextField));
                break;
            case 4:
                text.append("KummerM(").append(getTextCheckNonEmpty(kummerMATextField))
                        .append(", ").append(getTextCheckNonEmpty(kummerMBTextField))
                        .append(", ").append(getTextCheckNonEmpty(kummerMZTextField));
                break;
            case 5:
                text.append("KummerU(").append(getTextCheckNonEmpty(kummerUATextField))
                        .append(", ").append(getTextCheckNonEmpty(kummerUBTextField))
                        .append(", ").append(getTextCheckNonEmpty(kummerUZTextField));
                break;
            case 6:
                text.append("WhittakerM(").append(getTextCheckNonEmpty(whittakerMKappaTextField))
                        .append(", ").append(getTextCheckNonEmpty(whittakerMMuTextField))
                        .append(", ").append(getTextCheckNonEmpty(whittakerMZTextField));
                break;
            case 7:
                text.append("WhittakerW(").append(getTextCheckNonEmpty(whittakerWKappaTextField))
                        .append(", ").append(getTextCheckNonEmpty(whittakerWMuTextField))
                        .append(", ").append(getTextCheckNonEmpty(whittakerWZTextField));
                break;
            case 8:
                text.append("SphericalHarmonicY(").append(getTextCheckNonEmpty(sphericalHarmonicNTextField))
                        .append(", ").append(getTextCheckNonEmpty(sphericalHarmonicMTextField))
                        .append(", ").append(getTextCheckNonEmpty(sphericalHarmonicThetaTextField))
                        .append(", ").append(getTextCheckNonEmpty(sphericalHarmonicPhiTextField));
                break;
            case 9:
                text.append("SolidHarmonicY(").append(getTextCheckNonEmpty(solidHarmonicNTextField))
                        .append(", ").append(getTextCheckNonEmpty(solidHarmonicMTextField))
                        .append(", ").append(getTextCheckNonEmpty(solidHarmonicXTextField))
                        .append(", ").append(getTextCheckNonEmpty(solidHarmonicYTextField))
                        .append(", ").append(getTextCheckNonEmpty(solidHarmonicZTextField))
                        .append(", ").append(getTextCheckNonEmpty(solidHarmonicR2TextField));
                break;
        }
        return text.append(")").toString();
    }
}
