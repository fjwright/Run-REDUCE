package fjwright.runreduce;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private static ObservableList<String> listViewObservableList;
    private static final String USER_HOME_DIR = getProperty("user.home");

    @FXML
    private void initialize() {
        commandTextFieldArray = new TextField[]{commandPathNameTextField,
                arg1TextField, arg2TextField, arg3TextField, arg4TextField, arg5TextField};
        // From ListView documentation:
        // The elements of the ListView are contained within the items ObservableList.
        // This ObservableList is automatically observed by the ListView, such that any changes
        // that occur inside the ObservableList will be automatically shown in the ListView itself.
        // *** Therefore, call listView.setItems() only once to initialize the ListView. ***
        listView.setItems(listViewObservableList = FXCollections.observableArrayList());
        setupDialog(RunREDUCE.reduceConfiguration);
        createCommandArgFCButtons();
    }

    /**
     * Assign all generic fields and specific fields for the first REDUCE command.
     * Called by initialize() and resetAllDefaultsButtonAction() only.
     */
    private void setupDialog(REDUCEConfigurationType reduceConfiguration) {
        reduceRootDirTextField.setText(reduceConfiguration.reduceRootDir);
        packagesDirTextField.setText(reduceConfiguration.packagesDir);
        manualDirTextField.setText(reduceConfiguration.manualDir);
        primersDirTextField.setText(reduceConfiguration.primersDir);
        workingDirTextField.setText(reduceConfiguration.workingDir);
        reduceCommandList = reduceConfiguration.reduceCommandList.copy();
        listView.getSelectionModel().selectedItemProperty().removeListener(listViewListener);
        listViewObservableList.setAll(
                reduceCommandList.stream().map(cmd -> cmd.name).collect(Collectors.toList()));
        listView.getSelectionModel().selectFirst();
        listView.getSelectionModel().selectedItemProperty().addListener(listViewListener);
        showREDUCECommand(reduceCommandList.get(0));
    }

    /**
     * Save the old REDUCE command to reduceCommandList, otherwise any changes
     * will be lost, and display the new REDUCE command from reduceCommandList.
     * Abort the switch in case of errors in the old command.
     */
    private final ChangeListener<String> listViewListener = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
            if (old_val != null) {
                try {
                    saveREDUCECommand(old_val);
                } catch (FileNotFoundException e) {
                    ov.removeListener(this);
                    listView.getSelectionModel().select(old_val); // Don't want this to do any checking or saving.
                    ov.addListener(this);
                    return;
                }
            }
            for (REDUCECommand cmd : reduceCommandList)
                if (cmd.name.equals(new_val)) {
                    showREDUCECommand(cmd);
                    break;
                }
        }
    };

    /**
     * Select a ListView item by index after first removing listViewListener
     * to avoid any checking or saving, then add listViewListener.
     */
    private void listViewSelectIndexRemoveAddListener(int index) {
        listView.getSelectionModel().selectedItemProperty().removeListener(listViewListener);
        listView.getSelectionModel().select(index);
        listView.getSelectionModel().selectedItemProperty().addListener(listViewListener);
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
        listViewObservableList.remove(selectedIndex);
        int size = reduceCommandList.size(); // new size!
        if (size > 0) {
            if (selectedIndex == size) selectedIndex--;
            listViewSelectIndexRemoveAddListener(selectedIndex);
            showREDUCECommand(reduceCommandList.get(selectedIndex));
        } else
            addCommandButtonAction();
    }

    /**
     * Duplicate all configuration data for the selected REDUCE command
     * and add it as a new command at the bottom of the list.
     */
    @FXML
    private void duplicateCommandButtonAction() {
        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        REDUCECommand oldCmd = reduceCommandList.get(selectedIndex);
        REDUCECommand newCmd = new REDUCECommand(
                oldCmd.name + " New", oldCmd.rootDir, oldCmd.command);
        reduceCommandList.add(newCmd);
        listViewObservableList.add(newCmd.name);
        listViewSelectIndexRemoveAddListener(reduceCommandList.size() - 1);
        showREDUCECommand(newCmd);
    }

    /**
     * Add blank configuration data for a new REDUCE command at the bottom of the list.
     */
    @FXML
    private void addCommandButtonAction() {
        REDUCECommand newCmd = new REDUCECommand("New Command");
        reduceCommandList.add(newCmd);
        listViewObservableList.add(newCmd.name);
        listViewSelectIndexRemoveAddListener(listViewObservableList.size() - 1);
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

    @FXML
    private void saveButtonAction(ActionEvent actionEvent) {
        // Write form data back to REDUCEConfiguration
        // after validating directory and file fields:
        String s;
        try {
            // Optional:
            RunREDUCE.reduceConfiguration.reduceRootDir =
                    directoryTextFieldReadableCheck(reduceRootDirTextField);
            // All required:
            s = directoryTextFieldReadableCheck(packagesDirTextField);
            RunREDUCE.reduceConfiguration.packagesDir =
                    s.isEmpty() ? RunREDUCE.reduceConfigurationDefault.packagesDir : s;
            s = directoryTextFieldReadableCheck(manualDirTextField);
            RunREDUCE.reduceConfiguration.manualDir =
                    s.isEmpty() ? RunREDUCE.reduceConfigurationDefault.manualDir : s;
            s = directoryTextFieldReadableCheck(primersDirTextField);
            RunREDUCE.reduceConfiguration.primersDir =
                    s.isEmpty() ? RunREDUCE.reduceConfigurationDefault.primersDir : s;
            s = directoryTextFieldReadableCheck(workingDirTextField);
            RunREDUCE.reduceConfiguration.workingDir =
                    s.isEmpty() ? RunREDUCE.reduceConfigurationDefault.workingDir : s;
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
     * Check that dir is empty or a readable directory and if not throw an exception.
     */
    private String directoryTextFieldReadableCheck(TextField dirTextField) throws FileNotFoundException {
        String dir = dirTextField.getText().trim();
        if (dir.isEmpty()) return dir;
        // Convert leading ~ to user's home directory:
        Path path = Path.of(dir), newpath = path;
        if (path.startsWith("~")) {
            newpath = Path.of(USER_HOME_DIR);
            if (path.getNameCount() > 1)
                newpath = newpath.resolve(path.subpath(1, path.getNameCount()));
        }
        if (!newpath.toFile().canRead())
            confirm("Invalid Directory",
                    "The directory\n" +
                            dir +
                            "\ndoes not exist or is not accessible." +
                            "\nRun-REDUCE may not operate correctly! Continue?");
        return newpath.toString();
    }

    /**
     * Check that fileOrDir is a readable file or directory if it begins with $REDUCE or ~ or alwaysCheck == true.
     * If not throw an exception.
     */
    private String fileOrDirTextFieldReadableCheck(TextField fileOrDirTextField, String commandRootDir,
                                                   boolean alwaysCheck) throws FileNotFoundException {
        String fileOrDir = fileOrDirTextField.getText().trim();
        Path path = Path.of(fileOrDir), newpath = path;
        // Replace leading $REDUCE in *local* path copy of fileOrDir:
        if (path.startsWith("$REDUCE")) {
            newpath = Paths.get(commandRootDir);
            if (path.getNameCount() > 1)
                newpath = newpath.resolve(path.subpath(1, path.getNameCount()));
            alwaysCheck = true;
        } else
            // Convert leading ~ to user's home directory:
            if (path.startsWith("~")) {
                newpath = Path.of(USER_HOME_DIR);
                if (path.getNameCount() > 1)
                    newpath = newpath.resolve(path.subpath(1, path.getNameCount()));
                alwaysCheck = true;
                fileOrDir = newpath.toString();
            }
        if (alwaysCheck && !newpath.toFile().canRead())
            confirm("Invalid Directory or File",
                    fileOrDir +
                            "\ndoes not exist or is not accessible." +
                            "\nREDUCE may not run! Continue?");
        return fileOrDir;
    }

    /**
     * Display a standard modal JavaFX pop-up CONFIRMATION dialogue and wait for a response.
     * OK just returns; Cancel throws a FileNotFoundException exception.
     */
    public static void confirm(String headerText, String contentText) throws FileNotFoundException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.CANCEL)
            throw new FileNotFoundException();
    }

    /**
     * Save the command-specific text fields in the dialogue
     * to the specified REDUCE command in reduceCommandList.
     * This is a local save; nothing is saved back to Run-REDUCE.
     */
    private void saveREDUCECommand(String commandName) throws FileNotFoundException {
        // Find the command cmd in REDUCECommandList with the current name commandName:
        REDUCECommand cmd = null;
        for (REDUCECommand c : reduceCommandList)
            if (c.name.equals(commandName)) {
                cmd = c;
                break;
            }
        if (cmd == null) return; // Report an error?
        // Update the elements of cmd:
        cmd.name = commandNameTextField.getText().trim(); // in case edited but not confirmed!
        String commandRootDir = cmd.rootDir = directoryTextFieldReadableCheck(commandRootDirTextField);
        if (commandRootDir.isEmpty()) commandRootDir = reduceRootDirTextField.getText().trim();
        // Must replace the whole command array because its length may have changed:
        List<String> commandList = new ArrayList<>();
        for (int i = 0; i < commandTextFieldArray.length; i++) {
            String element = fileOrDirTextFieldReadableCheck(
                    commandTextFieldArray[i], commandRootDir, i == 0);
            if (!element.isEmpty()) commandList.add(element);
        }
        cmd.command = commandList.toArray(new String[0]);
    }

    @FXML
    private void cancelButtonAction(ActionEvent actionEvent) {
        // Close dialogue:
        Node source = (Node) actionEvent.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    /**
     * Update reduceCommandList and the ListView when the command name TextField is edited.
     */
    @FXML
    private void commandNameTextFieldAction() {
        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        String text = commandNameTextField.getText().trim();
        reduceCommandList.get(selectedIndex).name = text;
        listView.getSelectionModel().selectedItemProperty().removeListener(listViewListener);
        listViewObservableList.set(selectedIndex, text);
        listView.getSelectionModel().selectedItemProperty().addListener(listViewListener);
    }

    /**
     * Move the current command up the list, or to the bottom if it was at the top.
     */
    @FXML
    private void moveCommandUpButtonAction() {
        listView.getSelectionModel().selectedItemProperty().removeListener(listViewListener);
        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        REDUCECommand cmd = reduceCommandList.remove(selectedIndex);
        listViewObservableList.remove(selectedIndex);
        if (selectedIndex == 0) {
            reduceCommandList.add(cmd);
            listViewObservableList.add(cmd.name);
            listView.getSelectionModel().select(reduceCommandList.size() - 1);
        } else {
            reduceCommandList.add(--selectedIndex, cmd);
            listViewObservableList.add(selectedIndex, cmd.name);
            listView.getSelectionModel().select(selectedIndex);
        }
        listView.getSelectionModel().selectedItemProperty().addListener(listViewListener);
    }

    /**
     * Move the current command down the list, or to the top if it was at the bottom.
     */
    @FXML
    private void moveCommandDownButtonAction() {
        listView.getSelectionModel().selectedItemProperty().removeListener(listViewListener);
        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        REDUCECommand cmd = reduceCommandList.remove(selectedIndex);
        listViewObservableList.remove(selectedIndex);
        if (selectedIndex == reduceCommandList.size()) {
            reduceCommandList.add(0, cmd);
            listViewObservableList.add(0, cmd.name);
            listView.getSelectionModel().select(0);
        } else {
            reduceCommandList.add(++selectedIndex, cmd);
            listViewObservableList.add(selectedIndex, cmd.name);
            listView.getSelectionModel().select(selectedIndex);
        }
        listView.getSelectionModel().selectedItemProperty().addListener(listViewListener);
    }

// Code for the [...] buttons: directory and file choosers =======================================

    /**
     * Code run by the directory chooser (DC) buttons.
     */
    private void dcButtonAction(String title, String defaultDir, TextField textField) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        directoryChooser.setInitialDirectory(new File(defaultDir));
        File dir = directoryChooser.showDialog(null);
        if (dir != null) textField.setText(dir.toString());
    }

    @FXML
    private void reduceRootDirDCButtonAction() {
        dcButtonAction("REDUCE Root Directory",
                RunREDUCE.reduceConfigurationDefault.reduceRootDir,
                reduceRootDirTextField);
    }

    @FXML
    private void packagesDirDCButtonAction() {
        dcButtonAction("REDUCE Packages Directory",
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
                workingDirTextField.setText(USER_HOME_DIR));
        menuItem = new MenuItem("Run-REDUCE Directory");
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
        String defaultDir = commandRootDirTextField.getText().trim();
        if (defaultDir.isEmpty())
            defaultDir = RunREDUCE.reduceConfigurationDefault.reduceRootDir;
        fileChooser.setInitialDirectory(new File(defaultDir));
        File file = fileChooser.showOpenDialog(null);
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
