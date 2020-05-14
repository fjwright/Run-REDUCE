package fjwright.runreduce;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Take a local copy of the data in the REDUCE configuration object, let the user edit it,
 * and then save it back to the REDUCE configuration object only on clicking the Save button.
 */
public class REDUCEConfigDialog {
    @FXML
    private TextField reduceRootDirTextField;
    @FXML
    private TextField packagesRootDirTextField;
    @FXML
    private TextField docRootDirTextField;
    @FXML
    private ListView<String> listView;
    @FXML
    private TextField commandNameTextField;
    @FXML
    private TextField commandRootDirTextField;
    @FXML
    private TextField commandPathNameTextField;
    @FXML
    private TextField arg1TextField;
    @FXML
    private TextField arg2TextField;
    @FXML
    private TextField arg3TextField;
    @FXML
    private TextField arg4TextField;
    @FXML
    private TextField arg5TextField;

    private static TextField[] commandTextFieldArray;
    private static REDUCECommandList reduceCommandList; // local copy

    private void setListViewItems() {
        listView.setItems(FXCollections.observableArrayList(
                reduceCommandList.stream().map(cmd -> cmd.name).collect(Collectors.toList())));
    }

    private void setupDialog(REDUCEConfigurationType reduceConfiguration) {
        reduceRootDirTextField.setText(reduceConfiguration.reduceRootDir);
        packagesRootDirTextField.setText(reduceConfiguration.packagesRootDir);
        docRootDirTextField.setText(reduceConfiguration.docRootDir);
        reduceCommandList = reduceConfiguration.reduceCommandList.copy();
        showREDUCECommand(reduceCommandList.get(0));
        setListViewItems();
        listView.getSelectionModel().selectFirst();
    }

    @FXML
    private void initialize() {
        commandTextFieldArray = new TextField[]{commandPathNameTextField,
                arg1TextField, arg2TextField, arg3TextField, arg4TextField, arg5TextField};
        setupDialog(RunREDUCE.reduceConfiguration);
        listView.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue<? extends String> ov, String old_val, String new_val) -> {
                    if (old_val != null) saveREDUCECommand(old_val);
                    for (REDUCECommand cmd : reduceCommandList)
                        if (cmd.name.equals(new_val)) {
                            showREDUCECommand(cmd);
                            break;
                        }
                });
    }

    /**
     * Reset all configuration data to the default.
     */
    @FXML
    private void resetAllDefaultsButtonAction(/*ActionEvent actionEvent*/) {
        setupDialog(RunREDUCE.reduceConfigurationDefault);
    }

    /**
     * Delete all configuration data for the selected REDUCE command.
     */
    @FXML
    private void deleteCommandButtonAction(/*ActionEvent actionEvent*/) {
        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        reduceCommandList.remove(selectedIndex);
        setListViewItems();
        int size = reduceCommandList.size(); // new size!
        if (size > 0) {
            if (selectedIndex == size) selectedIndex--;
            listView.getSelectionModel().select(selectedIndex);
            showREDUCECommand(reduceCommandList.get(selectedIndex));
        } else
            addCommandButtonAction();
    }

    /**
     * Duplicate all configuration data for the selected REDUCE command.
     */
    @FXML
    private void duplicateCommandButtonAction(/*ActionEvent actionEvent*/) {
        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        REDUCECommand oldCmd = reduceCommandList.get(selectedIndex++);
        // selectedIndex is now incremented to the index of the duplicate entry.
        REDUCECommand newCmd = new REDUCECommand(
                oldCmd.name + " New", oldCmd.rootDir, oldCmd.command);
        reduceCommandList.add(selectedIndex, newCmd);
        setListViewItems();
        listView.getSelectionModel().select(selectedIndex);
        showREDUCECommand(newCmd);
    }

    /**
     * Add blank configuration data for a new REDUCE command at the bottom of the list.
     */
    @FXML
    private void addCommandButtonAction(/*ActionEvent actionEvent*/) {
        REDUCECommand newCmd = new REDUCECommand("New Command");
        reduceCommandList.add(newCmd);
        setListViewItems();
        listView.getSelectionModel().selectLast();
        showREDUCECommand(newCmd);
    }

    @FXML
    private void saveButtonAction(ActionEvent actionEvent) {
        // Write form data back to REDUCEConfiguration:
        saveREDUCECommand(listView.getSelectionModel().getSelectedItem());
        RunREDUCE.reduceConfiguration.reduceRootDir = reduceRootDirTextField.getText();
        RunREDUCE.reduceConfiguration.packagesRootDir = packagesRootDirTextField.getText();
        RunREDUCE.reduceConfiguration.docRootDir = docRootDirTextField.getText();
        RunREDUCE.reduceConfiguration.reduceCommandList = reduceCommandList;
        RunREDUCE.reduceConfiguration.save();
        // Close dialogue:
        cancelButtonAction(actionEvent);
    }

    @FXML
    private void cancelButtonAction(ActionEvent actionEvent) {
        // Close dialogue:
        Node source = (Node) actionEvent.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    /**
     * Set the command-specific text fields in the dialogue from the specified REDUCE command.
     */
    private void showREDUCECommand(REDUCECommand cmd) {
        commandNameTextField.setText(cmd.name);
        commandRootDirTextField.setText(cmd.rootDir);
        int i;
        for (i = 0; i < cmd.command.length; i++)
            commandTextFieldArray[i].setText(cmd.command[i]);
        for (; i < commandTextFieldArray.length; i++)
            commandTextFieldArray[i].setText("");
    }

    /**
     * Save the command-specific text fields in the dialogue to the specified REDUCE command.
     */
    private void saveREDUCECommand(String commandName) {
        REDUCECommand cmd = null;
        for (REDUCECommand c : reduceCommandList)
            if (c.name.equals(commandName)) {
                cmd = c;
                break;
            }
        if (cmd == null) return; // Report an error?
        cmd.name = commandNameTextField.getText().trim();
        cmd.rootDir = commandRootDirTextField.getText().trim();
        // Do not save blank arguments:
        cmd.command = Arrays.stream(commandTextFieldArray).map(e -> e.getText().trim())
                .filter(e -> !e.isEmpty()).toArray(String[]::new);
    }

    /**
     * Update the ListView when the command name TextField is edited.
     */
    @FXML
    private void commandNameTextFieldAction(/*ActionEvent actionEvent*/) {
        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        reduceCommandList.get(selectedIndex).name = commandNameTextField.getText().trim();
        setListViewItems();
        listView.getSelectionModel().select(selectedIndex);
    }
}
