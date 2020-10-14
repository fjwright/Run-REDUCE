package fjwright.runreduce;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static java.lang.System.getProperty;

/**
 * Take a local copy of the data in the REDUCE configuration object, let the user edit it,
 * and then save it back to the REDUCE configuration object only on clicking the Save button.
 */
public class REDUCEConfigDialog {
    @FXML
    private TextField reduceRootDirTextField, packagesDirTextField,
            manualDirTextField, primersDirTextField, workingDirTextField;
    @FXML
    private ListView<String> listView;
    @FXML
    private TextField commandNameTextField, commandRootDirTextField, commandPathNameTextField;
    @FXML
    private TextField arg1TextField, arg2TextField, arg3TextField, arg4TextField, arg5TextField;
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
        packagesDirTextField.setText(reduceConfiguration.packagesDir);
        manualDirTextField.setText(reduceConfiguration.manualDir);
        primersDirTextField.setText(reduceConfiguration.primersDir);
        workingDirTextField.setText(reduceConfiguration.workingDir);
        reduceCommandList = reduceConfiguration.reduceCommandList.copy();
        setListViewItems();
        listView.getSelectionModel().selectFirst();
        showREDUCECommand(reduceCommandList.get(0));
    }

    @FXML
    private void initialize() {
        commandTextFieldArray = new TextField[]{commandPathNameTextField,
                arg1TextField, arg2TextField, arg3TextField, arg4TextField, arg5TextField};
        setupDialog(RunREDUCE.reduceConfiguration);
        listView.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue<? extends String> ov, String old_val, String new_val) -> {
                    if (old_val != null) {
                        try {
                            saveREDUCECommand(old_val);
                        } catch (FileNotFoundException e) {
                            return;
                        }
                    }
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

    private String directoryReadableCheck(String dir) throws FileNotFoundException {
        if (new File(dir).canRead()) return dir;
        else {
            RunREDUCE.alert(Alert.AlertType.ERROR, "Invalid Directory",
                    "The directory\n" + dir + "\ndoes not exist or is not accessible.");
            throw new FileNotFoundException();
        }
    }

    @FXML
    private void saveButtonAction(ActionEvent actionEvent) {
        // Write form data back to REDUCEConfiguration
        // after validating generic root directory fields:
        try {
            RunREDUCE.reduceConfiguration.reduceRootDir =
                    directoryReadableCheck(reduceRootDirTextField.getText());
            RunREDUCE.reduceConfiguration.packagesDir =
                    directoryReadableCheck(packagesDirTextField.getText());
            RunREDUCE.reduceConfiguration.manualDir =
                    directoryReadableCheck(manualDirTextField.getText());
            RunREDUCE.reduceConfiguration.primersDir =
                    directoryReadableCheck(primersDirTextField.getText());
            RunREDUCE.reduceConfiguration.workingDir =
                    directoryReadableCheck(workingDirTextField.getText());
            saveREDUCECommand(listView.getSelectionModel().getSelectedItem());
            RunREDUCE.reduceConfiguration.reduceCommandList = reduceCommandList;
            RunREDUCE.reduceConfiguration.save();
            // Close dialogue:
            cancelButtonAction(actionEvent);
            // Rebuild the Run REDUCE submenus:
            RunREDUCE.runREDUCEFrame.runREDUCESubmenuBuild();
            RunREDUCE.runREDUCEFrame.autoRunREDUCESubmenuBuild();
        } catch (FileNotFoundException ignored) {
        }
    }

    /**
     * Check that fileOrDir is a readable file or directory if it begins with $REDUCE or alwaysCheck == true.
     */
    private String fileOrDirReadableCheck(String fileOrDir, boolean alwaysCheck) throws FileNotFoundException {
        if (fileOrDir.startsWith("$REDUCE")) {// replace $REDUCE/ or $REDUCE\
            fileOrDir = Paths.get(RunREDUCE.reduceConfiguration.reduceRootDir).
                    resolve(fileOrDir.substring(8)).toString();
            alwaysCheck = true;
        }
        if (alwaysCheck) {
            if (new File(fileOrDir).canRead()) return fileOrDir;
            else {
                RunREDUCE.alert(Alert.AlertType.ERROR, "Invalid Directory or File",
                        fileOrDir + "\ndoes not exist or is not accessible.");
                throw new FileNotFoundException();
            }
        } else return fileOrDir;
    }

    /**
     * Save the command-specific text fields in the dialogue to the specified REDUCE command.
     */
    private void saveREDUCECommand(String commandName) throws FileNotFoundException {
        REDUCECommand cmd = null;
        for (REDUCECommand c : reduceCommandList)
            if (c.name.equals(commandName)) {
                cmd = c;
                break;
            }
        if (cmd == null) return; // Report an error?
        cmd.name = commandNameTextField.getText().trim();
        cmd.rootDir = directoryReadableCheck(commandRootDirTextField.getText().trim());
        // Do not save blank arguments:
        for (int i = 0, j = 0; i < commandTextFieldArray.length; i++) {
            String element = commandTextFieldArray[i].getText().trim();
            if (!element.isEmpty()) cmd.command[j++] = fileOrDirReadableCheck(element, i == 0);
        }
    }

    @FXML
    private void cancelButtonAction(ActionEvent actionEvent) {
        // Close dialogue:
        Node source = (Node) actionEvent.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
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
                RunREDUCE.reduceConfigurationDefault.packagesDir,
                packagesDirTextField);
    }

    @FXML
    private void manualDirDCButtonAction() {
        dcButtonAction("REDUCE Manual Directory",
                RunREDUCE.reduceConfigurationDefault.manualDir,
                manualDirTextField);
    }

    @FXML
    private void primersDirDCButtonAction() {
        dcButtonAction("REDUCE Primers Directory",
                RunREDUCE.reduceConfigurationDefault.primersDir,
                primersDirTextField);
    }

    // Doesn't work correctly if static!
    private final ContextMenu workingDirContextMenu = new ContextMenu();

    {
        MenuItem menuItem = new MenuItem("Your Home Directory");
        workingDirContextMenu.getItems().add(menuItem);
        menuItem.setOnAction(e ->
                workingDirTextField.setText(getProperty("user.home")));
        menuItem = new MenuItem("Run-REDUCE-FX Directory");
        workingDirContextMenu.getItems().add(menuItem);
        menuItem.setOnAction(e ->
                workingDirTextField.setText(getProperty("user.dir")));
        menuItem = new MenuItem("Choose Any Directory");
        workingDirContextMenu.getItems().add(menuItem);
        menuItem.setOnAction(e ->
                dcButtonAction("REDUCE Working Directory",
                        RunREDUCE.reduceConfigurationDefault.workingDir,
                        workingDirTextField));
    }

    @FXML
    private void workingDirDCButtonAction() {
        workingDirContextMenu.show(workingDirTextField, Side.BOTTOM, 0, 0);
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
