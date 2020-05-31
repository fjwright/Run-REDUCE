package fjwright.runreduce;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
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
    @FXML
    private GridPane commandGridPane;

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
        createCommandArgFCButtons();
    }

    /**
     * Reset all configuration data to the default.
     */
    @FXML
    private void resetAllDefaultsButtonAction() {
        setupDialog(RunREDUCE.reduceConfigurationDefault);
    }

    /**
     * Delete all configuration data for the selected REDUCE command.
     */
    @FXML
    private void deleteCommandButtonAction() {
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
    private void duplicateCommandButtonAction() {
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
    private void addCommandButtonAction() {
        REDUCECommand newCmd = new REDUCECommand("New Command");
        reduceCommandList.add(newCmd);
        setListViewItems();
        listView.getSelectionModel().selectLast();
        showREDUCECommand(newCmd);
    }

    @FXML
    private void saveButtonAction(ActionEvent actionEvent) {
        // Write form data back to REDUCEConfiguration
        // after validating generic root directory fields:
        String[] dirs = new String[]{
                reduceRootDirTextField.getText(),
                packagesRootDirTextField.getText(),
                docRootDirTextField.getText()};
        for (String dir : dirs)
            if (!new File(dir).canRead()) {
                RunREDUCE.errorMessageDialog("Invalid Directory",
                        "The directory\n" + dir + "\ndoes not exist or is not accessible.");
                return;
            }
        RunREDUCE.reduceConfiguration.reduceRootDir = dirs[0];
        RunREDUCE.reduceConfiguration.packagesRootDir = dirs[1];
        RunREDUCE.reduceConfiguration.docRootDir = dirs[2];
        saveREDUCECommand(listView.getSelectionModel().getSelectedItem());
        RunREDUCE.reduceConfiguration.reduceCommandList = reduceCommandList;
        RunREDUCE.reduceConfiguration.save();
        // Close dialogue:
        cancelButtonAction(actionEvent);
        // Rebuild the Run REDUCE submenus:
        RunREDUCE.runREDUCEFrame.runREDUCESubmenuBuild();
        RunREDUCE.runREDUCEFrame.autoRunREDUCESubmenuBuild();
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
    private void commandNameTextFieldAction() {
        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        reduceCommandList.get(selectedIndex).name = commandNameTextField.getText().trim();
        setListViewItems();
        listView.getSelectionModel().select(selectedIndex);
    }

    /**
     * Code run by the directory chooser (DC) buttons.
     */
    private void dcButtonAction(String title, String defaultDir, TextField textField) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        directoryChooser.setInitialDirectory(new File(defaultDir));
        File dir = directoryChooser.showDialog(RunREDUCE.primaryStage);
        if (dir != null) textField.setText(dir.toString());
    }

    @FXML
    private void reduceRootDirDCButtonAction() {
        dcButtonAction("REDUCE Root Directory",
                RunREDUCE.reduceConfigurationDefault.reduceRootDir,
                reduceRootDirTextField);
    }

    @FXML
    private void packagesRootDirDCButtonAction() {
        dcButtonAction("Packages Root Directory",
                RunREDUCE.reduceConfigurationDefault.packagesRootDir,
                packagesRootDirTextField);
    }

    @FXML
    private void docRootDirDCButtonAction() {
        dcButtonAction("Documentation Root Directory",
                RunREDUCE.reduceConfigurationDefault.docRootDir,
                docRootDirTextField);
    }

    @FXML
    private void commandRootDirDCButtonAction() {
        dcButtonAction("Command Root Directory",
                RunREDUCE.reduceConfigurationDefault.reduceRootDir,
                commandRootDirTextField);
    }

    /**
     * Code run by the file chooser (FC) buttons.
     */
    private void fcButtonAction(String title, TextField textField) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        String commandRootDir = commandRootDirTextField.getText();
        String defaultDir = !commandRootDir.isEmpty() ? commandRootDir :
                RunREDUCE.reduceConfigurationDefault.reduceRootDir;
        fileChooser.setInitialDirectory(new File(defaultDir));
        File file = fileChooser.showOpenDialog(RunREDUCE.primaryStage);
        if (file != null) textField.setText(file.toString());
    }

    @FXML
    private void commandPathNameFCButtonAction() {
        fcButtonAction("Command Path Name", commandPathNameTextField);
    }

    /**
     * Called in method initialize to create the command argument file chooser buttons.
     */
    private void createCommandArgFCButtons() {
        for (int i = 1; i < commandTextFieldArray.length; i++) {
            Button button = new Button("...");
            commandGridPane.add(button, 2, 4 + i);
            String title = "Command Argument " + i;
            TextField textField = commandTextFieldArray[i];
            button.setOnAction(event -> fcButtonAction(title, textField));
        }
    }
}
