package fjwright.runreduce.functions;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class OrthoPoly extends Functions {
    @FXML
    private TextField jacobiAlphaTextField, jacobiBetaTextField, jacobiNTextField, jacobiXTextField;
    @FXML
    private TextField gegenbauerLambdaTextField, gegenbauerNTextField, gegenbauerXTextField;
    @FXML
    private TextField chebyshev1NTextField, chebyshev1XTextField, chebyshev2NTextField, chebyshev2XTextField;
    @FXML
    private TextField legendreNTextField, legendreXTextField, assocLegendreMTextField, assocLegendreNTextField, assocLegendreXTextField;
    @FXML
    private TextField laguerreNTextField, laguerreXTextField, genLaguerreAlphaTextField, genLaguerreNTextField, genLaguerreXTextField;
    @FXML
    private TextField hermiteNTextField, hermiteXTextField;

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
                text.append("JacobiP(").append(getTextCheckNonEmpty(jacobiNTextField))
                        .append(", ").append(getTextCheckNonEmpty(jacobiAlphaTextField))
                        .append(", ").append(getTextCheckNonEmpty(jacobiBetaTextField))
                        .append(", ").append(getTextCheckNonEmpty(jacobiXTextField));
                break;
            case 1:
                text.append("GegenbauerP(").append(getTextCheckNonEmpty(gegenbauerNTextField))
                        .append(", ").append(getTextCheckNonEmpty(gegenbauerLambdaTextField))
                        .append(", ").append(getTextCheckNonEmpty(gegenbauerXTextField));
                break;
            case 2:
                text.append("ChebyshevT(").append(getTextCheckNonEmpty(chebyshev1NTextField))
                        .append(", ").append(getTextCheckNonEmpty(chebyshev1XTextField));
                break;
            case 3:
                text.append("ChebyshevU(").append(getTextCheckNonEmpty(chebyshev2NTextField))
                        .append(", ").append(getTextCheckNonEmpty(chebyshev2XTextField));
                break;
            case 4:
                text.append("LegendreP(").append(getTextCheckNonEmpty(legendreNTextField))
                        .append(", ").append(getTextCheckNonEmpty(legendreXTextField));
                break;
            case 5:
                text.append("LegendreP(").append(getTextCheckNonEmpty(assocLegendreNTextField))
                        .append(", ").append(getTextCheckNonEmpty(assocLegendreMTextField))
                        .append(", ").append(getTextCheckNonEmpty(assocLegendreXTextField));
                break;
            case 6:
                text.append("LaguerreP(").append(getTextCheckNonEmpty(laguerreNTextField))
                        .append(", ").append(getTextCheckNonEmpty(laguerreXTextField));
                break;
            case 7:
                text.append("LaguerreP(").append(getTextCheckNonEmpty(genLaguerreNTextField))
                        .append(", ").append(getTextCheckNonEmpty(genLaguerreAlphaTextField))
                        .append(", ").append(getTextCheckNonEmpty(genLaguerreXTextField));
                break;
            case 8:
                text.append("HermiteP(").append(getTextCheckNonEmpty(hermiteNTextField))
                        .append(", ").append(getTextCheckNonEmpty(hermiteXTextField));
                break;
        }
        return text.append(")").toString();
    }
}
