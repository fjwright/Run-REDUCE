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
import java.nio.file.InvalidPathException;
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
    private CheckBox useShellCheckBox, checkCommandCheckBox;
    @FXML
    private Label commandPathnameLabel;
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
        checkCommandCheckBox.visibleProperty().bind(useShellCheckBox.selectedProperty());
        commandPathnameLabel.visibleProperty().bind(useShellCheckBox.selectedProperty().not()
                .or(checkCommandCheckBox.selectedProperty()));
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
        listView.getSelectionModel().selectedIndexProperty().removeListener(listViewListener);
        listViewObservableList.setAll(
                reduceCommandList.stream().map(cmd -> cmd.name).collect(Collectors.toList()));
        listView.getSelectionModel().selectFirst();
        listView.getSelectionModel().selectedIndexProperty().addListener(listViewListener);
        showREDUCECommand(reduceCommandList.get(0));
    }

    /**
     * Save the old REDUCE command to reduceCommandList, otherwise any changes
     * will be lost, and display the new REDUCE command from reduceCommandList.
     * Abort the switch in case of errors in the old command.
     */
    private final ChangeListener<Number> listViewListener = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
            if (old_val != null) {
                try {
                    saveREDUCECommand((int) old_val);
                } catch (FileNotFoundException | DuplicateCommandLabelException e) {
                    ov.removeListener(this);
                    listView.getSelectionModel().select((int) old_val); // Don't want this to do any checking or saving.
                    ov.addListener(this);
                    return;
                }
            }
            showREDUCECommand(reduceCommandList.get((int) new_val));
        }
    };

    /**
     * Select a ListView item by index after first removing listViewListener
     * to avoid any checking or saving, then add listViewListener.
     */
    private void listViewSelectIndexRemoveAddListener(int index) {
        listView.getSelectionModel().selectedIndexProperty().removeListener(listViewListener);
        listView.getSelectionModel().select(index);
        listView.getSelectionModel().selectedIndexProperty().addListener(listViewListener);
    }

    /**
     * Reset all configuration data to the default.
     */
    @FXML
    private void resetAllDefaultsButtonAction() {
        setupDialog(RunREDUCE.reduceConfigurationDefault);
    }

    /**
     * Reset the default configuration data for the selected REDUCE command.
     * Ignore if there is no default command with the selected command name.
     */
    @FXML
    private void resetCommandButtonAction() {
        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        REDUCECommand oldCmd = reduceCommandList.get(selectedIndex);
        String cmdName = listView.getSelectionModel().getSelectedItem();
        // The default order may have been changed, so match by command name:
        for (REDUCECommand cmd : RunREDUCE.reduceConfigurationDefault.reduceCommandList)
            if (cmd.name.equals(cmdName)) {
                oldCmd.useShell = cmd.useShell;
                oldCmd.checkCommand = cmd.checkCommand;
                oldCmd.rootDir = cmd.rootDir;
                oldCmd.command = cmd.command;
                showREDUCECommand(oldCmd);
                break;
            }
    }

    /**
     * Delete all configuration data for the selected REDUCE command.
     */
    @FXML
    private void deleteCommandButtonAction() {
        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        reduceCommandList.remove(selectedIndex);
        listView.getSelectionModel().selectedIndexProperty().removeListener(listViewListener);
        listViewObservableList.remove(selectedIndex);
        listView.getSelectionModel().selectedIndexProperty().addListener(listViewListener);
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
                oldCmd.name + " New", oldCmd.useShell, oldCmd.checkCommand, oldCmd.rootDir, oldCmd.command);
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
        useShellCheckBox.setSelected(cmd.useShell);
        checkCommandCheckBox.setSelected(cmd.checkCommand);
        commandRootDirTextField.setText(cmd.rootDir);
        int i;
        for (i = 0; i < cmd.command.length; i++)
            commandTextFieldArray[i].setText(cmd.command[i]);
        for (; i < commandTextFieldArray.length; i++)
            commandTextFieldArray[i].setText("");
    }

    /**
     * Write form data back to REDUCEConfiguration
     * after validating directory and file fields.
     */
    @FXML
    private void saveButtonAction(ActionEvent actionEvent) {
        String s;
        try {
            // Optional:
            RunREDUCE.reduceConfiguration.reduceRootDir =
                    directoryTextFieldCheckReadable(reduceRootDirTextField);
            // All required:
            s = directoryTextFieldCheckReadable(packagesDirTextField);
            RunREDUCE.reduceConfiguration.packagesDir =
                    s.isEmpty() ? RunREDUCE.reduceConfigurationDefault.packagesDir : s;
            s = directoryTextFieldCheckReadable(manualDirTextField);
            RunREDUCE.reduceConfiguration.manualDir =
                    s.isEmpty() ? RunREDUCE.reduceConfigurationDefault.manualDir : s;
            s = directoryTextFieldCheckReadable(primersDirTextField);
            RunREDUCE.reduceConfiguration.primersDir =
                    s.isEmpty() ? RunREDUCE.reduceConfigurationDefault.primersDir : s;
            s = directoryTextFieldCheckReadable(workingDirTextField);
            RunREDUCE.reduceConfiguration.workingDir =
                    s.isEmpty() ? RunREDUCE.reduceConfigurationDefault.workingDir : s;
            saveREDUCECommand(listView.getSelectionModel().getSelectedIndex());
            RunREDUCE.reduceConfiguration.reduceCommandList = reduceCommandList;
            RunREDUCE.reduceConfiguration.save();
            // Close dialogue:
            cancelButtonAction(actionEvent);
            // Rebuild the Run REDUCE submenus:
            RunREDUCE.runREDUCEFrame.runREDUCESubmenuBuild();
            RunREDUCE.runREDUCEFrame.autoRunREDUCESubmenuBuild();
        } catch (FileNotFoundException | DuplicateCommandLabelException ignored) {
        }
    }

    /**
     * Check that dir is empty or a readable directory and if not throw an exception.
     */
    private String directoryTextFieldCheckReadable(TextField dirTextField) throws FileNotFoundException {
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
                    dir +
                            "\ndoes not exist or is not accessible." +
                            "\nRun-REDUCE may not operate correctly! Continue anyway?");
        return newpath.toString();
    }

    /**
     * Check that fileOrDir is a readable file or directory if it begins with $REDUCE or ~ or alwaysCheck == true.
     * If not throw an exception.
     */
    private String commandTextFieldCheckReadable(TextField commandTextField, String commandRootDir,
                                                 boolean alwaysCheck, boolean check) throws FileNotFoundException {
        String commandOrArg = commandTextField.getText().trim();
        Path path;
        // commandOrArg may or may not be a filepath, so...
        try {
            path = Path.of(commandOrArg);
        } catch (InvalidPathException e) {
            return commandOrArg;
        }
        Path newpath = path;
        // Replace leading $REDUCE in *local* path copy of commandOrArg:
        if (check && path.startsWith("$REDUCE")) {
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
                commandOrArg = newpath.toString();
            }
        if (check && alwaysCheck && !newpath.toFile().canRead())
            confirm("Invalid Directory or File",
                    commandOrArg +
                            "\ndoes not exist or is not accessible." +
                            "\nREDUCE may not run! Continue anyway?");
        return commandOrArg;
    }

    /**
     * Display a standard modal JavaFX pop-up WARNING alert and wait for a response.
     * OK just returns; Cancel throws a FileNotFoundException.
     */
    public static void confirm(String headerText, String contentText) throws FileNotFoundException {
        Alert alert = new Alert(Alert.AlertType.WARNING, contentText, ButtonType.OK, ButtonType.CANCEL);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setHeaderText(headerText);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.CANCEL)
            throw new FileNotFoundException();
    }

    private static class DuplicateCommandLabelException extends Exception {
    }

    /**
     * Update the command name from the value of the command label field and check it is unique.
     */
    private void updateCommandNameCheckUnique(int selectedIndex) throws DuplicateCommandLabelException {
        String newName = commandNameTextField.getText().trim();
        reduceCommandList.get(selectedIndex).name = newName;
        listView.getSelectionModel().selectedIndexProperty().removeListener(listViewListener);
        listViewObservableList.set(selectedIndex, newName);
        listView.getSelectionModel().selectedIndexProperty().addListener(listViewListener);
        for (int i = 0; i < reduceCommandList.size(); i++)
            if (i != selectedIndex &&
                    reduceCommandList.get(i).name.equals(newName)) {
                RunREDUCE.alert(Alert.AlertType.ERROR,
                        "Duplicate Command Label",
                        "\"" + newName + "\" is already used.\n" +
                                "Please choose a unique command label.");
                throw new DuplicateCommandLabelException();
            }
    }

    /**
     * Save the command-specific text fields in the dialogue
     * to the specified REDUCE command in reduceCommandList.
     * This is a local save; nothing is saved back to Run-REDUCE.
     */
    private void saveREDUCECommand(int commandIndex) throws FileNotFoundException, DuplicateCommandLabelException {
        REDUCECommand cmd = reduceCommandList.get(commandIndex);
        // Update the elements of cmd:
        updateCommandNameCheckUnique(commandIndex); // in case edited but not confirmed!
        cmd.useShell = useShellCheckBox.isSelected();
        cmd.checkCommand = checkCommandCheckBox.isSelected();
        String commandRootDir = cmd.rootDir = directoryTextFieldCheckReadable(commandRootDirTextField);
        if (commandRootDir.isEmpty()) commandRootDir = reduceRootDirTextField.getText().trim();
        // Must replace the whole command array because its length may have changed:
        List<String> commandList = new ArrayList<>();
        for (int i = 0; i < commandTextFieldArray.length; i++) {
            String element = commandTextFieldCheckReadable(
                    commandTextFieldArray[i], commandRootDir, i == 0,
                    i != 0 || checkCommandCheckBox.isSelected());
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
     * Update reduceCommandList and the ListView when the Command Label TextField is edited.
     */
    @FXML
    private void commandNameTextFieldAction() {
        try {
            updateCommandNameCheckUnique(listView.getSelectionModel().getSelectedIndex());
        } catch (DuplicateCommandLabelException ignored) {
        }
    }

    /**
     * Move the current command up the list, or to the bottom if it was at the top.
     */
    @FXML
    private void moveCommandUpButtonAction() {
        listView.getSelectionModel().selectedIndexProperty().removeListener(listViewListener);
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
        listView.getSelectionModel().selectedIndexProperty().addListener(listViewListener);
    }

    /**
     * Move the current command down the list, or to the top if it was at the bottom.
     */
    @FXML
    private void moveCommandDownButtonAction() {
        listView.getSelectionModel().selectedIndexProperty().removeListener(listViewListener);
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
        listView.getSelectionModel().selectedIndexProperty().addListener(listViewListener);
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
        try {
            String defaultDir = directoryTextFieldCheckReadable(commandRootDirTextField);
            if (defaultDir.isEmpty())
                defaultDir = directoryTextFieldCheckReadable(reduceRootDirTextField);
            if (defaultDir.isEmpty())
                defaultDir = RunREDUCE.reduceConfigurationDefault.reduceRootDir;
            dcButtonAction("Command Root Directory", defaultDir, commandRootDirTextField);
        } catch (FileNotFoundException ignored) {
        }
    }

    /**
     * Code run by the file chooser (FC) buttons.
     */
    private void fcButtonAction(String title, TextField textField) {
        try {
            String defaultDir = directoryTextFieldCheckReadable(commandRootDirTextField);
            if (defaultDir.isEmpty())
                defaultDir = directoryTextFieldCheckReadable(reduceRootDirTextField);
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(title);
            fileChooser.setInitialDirectory(new File(
                    defaultDir.isEmpty() ? RunREDUCE.reduceConfigurationDefault.reduceRootDir : defaultDir));
            File file = fileChooser.showOpenDialog(null);
            if (file != null) // Use the *visible* default directory:
                if (defaultDir.isEmpty())
                    textField.setText(file.toString());
                else { // Replace defaultDir root segment by $REDUCE if possible:
                    Path $REDUCE = Path.of(defaultDir), path = file.toPath();
                    path = Path.of("$REDUCE").resolve($REDUCE.relativize(path));
                    textField.setText(path.toString());
                }
        } catch (FileNotFoundException ignored) {
        }
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
            commandGridPane.add(button, 2, 5 + i);
            String title = "Command Argument " + i;
            TextField textField = commandTextFieldArray[i];
            button.setOnAction(event -> fcButtonAction(title, textField));
        }
    }
}
