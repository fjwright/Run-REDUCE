package fjwright.runreduce;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.w3c.dom.html.HTMLDocument;
import org.w3c.dom.html.HTMLElement;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * This class provides the panel that displays REDUCE input and output.
 * The run method runs REDUCE as a sub-process.
 */
public class REDUCEPanel extends BorderPane {
    @FXML
    SplitPane splitPane; // Accessed in RunREDUCE.java
    @FXML
    private Label outputLabel;
    @FXML
    WebView outputWebView; // Accessed in RunREDUCEFrame.java
    @FXML
    private Label inputLabel;
    @FXML
    TextArea inputTextArea; // Accessed in RunREDUCE.java
    @FXML
    Button sendButton; // Accessed in RunREDUCEFrame.java
    @FXML
    private Button earlierButton;
    @FXML
    private Button laterButton;

    WebEngine webEngine;
    HTMLDocument document;
    HTMLElement style, body;

    private final List<String> inputList = new ArrayList<>();
    private int inputListIndex = 0;
    private int maxInputListIndex = 0;
    private static final Pattern quitPattern =
            Pattern.compile(".*\\b(?:bye|quit)\\s*[;$]?.*", Pattern.CASE_INSENSITIVE);
    private PrintWriter reduceInputPrintWriter;
    boolean runningREDUCE;
    String title; // REDUCE version if REDUCE is running
    private static final String outputLabelDefault = "Input/Output Display";
    private boolean firstPrompt, questionPrompt;
    final List<File> outputFileList = new ArrayList<>();

    private static final String ALGEBRAIC_OUTPUT_COLOR = "blue";
    private static final String SYMBOLIC_OUTPUT_COLOR = "#800080";
    private static final String ALGEBRAIC_INPUT_COLOR = "red";
    private static final String SYMBOLIC_INPUT_COLOR = "#800000";
    private static final String DEFAULT_COLOR = "black";
    private String promptColor = DEFAULT_COLOR;
    private String inputColor = DEFAULT_COLOR;
    private String outputColor = DEFAULT_COLOR;

    REDUCEPanel() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("REDUCEPanel.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        webEngine = outputWebView.getEngine();
        webEngine.loadContent("<html><head><style type='text/css'></style></head><body></body></html>");
        webEngine.getLoadWorker().stateProperty().addListener(
                (ov, oldState, newState) -> {
                    if (newState == State.SUCCEEDED) {
                        outputWebViewAvailable();
                    }
                });

        // Note that a font name containing spaces needs quoting in CSS!
        inputTextArea.setStyle("-fx-font:" + RRPreferences.fontSize + " '" + RunREDUCE.reduceFontFamilyName + "'");
    }

    /**
     * This method is run once the outputWebView is available.
     * Setting up the REDUCEPanel output display can now continue here.
     */
    private void outputWebViewAvailable() {
        // Use document factory methods to create new elements.
        document = (HTMLDocument) webEngine.getDocument();
        style = (HTMLElement) document.getElementsByTagName("style").item(0);
        style.appendChild(document.createTextNode(
                String.format("body{font-size:%d}", RRPreferences.fontSize))); // MUST be the first child!
        // Note that a font name containing spaces needs quoting in CSS!
        style.appendChild(document.createTextNode(
                String.format("pre{font-family:'%s','Courier New',Courier,monospace;}", RunREDUCE.reduceFontFamilyName)));
        body = document.getBody();

        // Auto-run REDUCE if appropriate:
        if (!RRPreferences.autoRunVersion.equals(RRPreferences.NONE)) {
            for (REDUCECommand cmd : RunREDUCE.reduceConfiguration.reduceCommandList)
                if (RRPreferences.autoRunVersion.equals(cmd.name)) {
                    // Run REDUCE.  (A direct call throws an error!)
                    run(cmd);
                    break;
                }
        } else
            // Reset enabled status of controls:
            reduceStopped();
    }

    void updateFontSize(int newFontSize) {
        style.getFirstChild().setNodeValue(String.format("body{font-size:%d}", newFontSize));
    }

    @FXML
    private void sendButtonClicked(MouseEvent mouseEvent) {
        sendAction(mouseEvent.isShiftDown());
    }

    @FXML
    private void sendButtonKeyTyped(KeyEvent keyEvent) {
        if (keyEvent.getCharacter().equals(" "))
            sendAction(keyEvent.isShiftDown());
    }

    private void sendAction(boolean isShiftDown) {
        String text = inputTextArea.getText();
        if (text.length() > 0) {
            inputList.add(text);
            sendInteractiveInputToREDUCE(text, !questionPrompt && !isShiftDown);
            inputTextArea.clear();
            inputListIndex = inputList.size();
            maxInputListIndex = inputListIndex - 1;
            earlierButton.setDisable(false);
            laterButton.setDisable(true);
            if (quitPattern.matcher(text).matches()) {
                runningREDUCE = false;
                outputFileList.clear();
                // Reset enabled status of controls:
                reduceStopped();
            }
            // Return the focus to the input text area:
            inputTextArea.requestFocus();
        }
    }

    @FXML
    private void earlierButtonAction() {
        if (inputListIndex > 0) {
            inputTextArea.setText(inputList.get(--inputListIndex));
            if (inputListIndex <= maxInputListIndex)
                laterButton.setDisable(false);
        }
        if (inputListIndex == 0)
            earlierButton.setDisable(true);
        // Return the focus to the input text area:
        inputTextArea.requestFocus();
    }

    @FXML
    private void laterButtonAction() {
        if (inputListIndex < maxInputListIndex) {
            inputTextArea.setText(inputList.get(++inputListIndex));
        } else {
            inputTextArea.clear();
            inputListIndex = maxInputListIndex + 1;
        }
        if (inputListIndex > 0) {
            earlierButton.setDisable(false);
        }
        if (inputListIndex > maxInputListIndex) {
            laterButton.setDisable(true);
        }
        // Return the focus to the input text area:
        inputTextArea.requestFocus();
    }

    @FXML
    private void inputTextAreaOnKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.isControlDown()) {
            switch (keyEvent.getCode()) {
                case ENTER:
                    sendAction(keyEvent.isShiftDown());
                    break;
                case UP:
                    earlierButtonAction();
                    break;
                case DOWN:
                    laterButtonAction();
            }
        }
    }

    private void sendInteractiveInputToREDUCE(String text, boolean autoTerminate) {
        // Strip trailing white space and if autoTerminate then ensure the input ends with a terminator:
        int i = text.length() - 1;
        char c = 0;
        while (i >= 0 && Character.isWhitespace(c = text.charAt(i))) i--;
        text = text.substring(0, i + 1);
        if (c == ';' || c == '$' || !autoTerminate) text += "\n";
        else text += ";\n";
        sendStringToREDUCEAndEcho(text);
    }

    void sendStringToREDUCEAndEcho(String text) {
        HTMLElement element = (HTMLElement) document.createElement("pre");
        element.setAttribute("style", "color:" + inputColor);
        element.appendChild(document.createTextNode(text));
        body.appendChild(element);
        // Make sure the new input text is visible:
//        outputScrollPane.setVvalue(1.0);
        sendStringToREDUCENoEcho(text);
        // Return the focus to the input text area:
        inputTextArea.requestFocus();
    }

    void sendStringToREDUCENoEcho(String text) {
        // Send the input to the REDUCE input pipe:
        if (reduceInputPrintWriter != null) {
            reduceInputPrintWriter.print(text);
            reduceInputPrintWriter.flush();
        }
    }

    void setSelected(boolean selected) {
        if (selected) {
//            outputTextFlow.setStyle("-fx-control-inner-background: white;");
//            inputTextArea.setStyle("-fx-control-inner-background: white;");
            inputTextArea.setDisable(!runningREDUCE);
            outputLabel.setDisable(false);
            inputLabel.setDisable(false);
        } else {
//            outputTextFlow.setStyle("-fx-control-inner-background: #F8F8F8;");
//            inputTextArea.setStyle("-fx-control-inner-background: #F8F8F8;");
            inputTextArea.setDisable(true);
            outputLabel.setDisable(true);
            inputLabel.setDisable(true);
        }
    }

    /**
     * Run the specified REDUCE command in this REDUCE panel.
     */
    void run(REDUCECommand reduceCommand) {
        outputColor = DEFAULT_COLOR; // for initial header
        // Special support for Redfront I/O colouring:
        RRPreferences.colouredIOState = RRPreferences.colouredIOIntent;
        firstPrompt = true;

        String[] command = reduceCommand.buildCommand();
        if (command == null) return;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            // pb.redirectInput(ProcessBuilder.Redirect.INHERIT); // Works!
            Process p = pb.start();

            // Assign the REDUCE input stream to an instance field:
            OutputStreamWriter osr = new OutputStreamWriter(p.getOutputStream());
            reduceInputPrintWriter = new PrintWriter(osr);

            // Start a thread to handle the REDUCE output stream
            // (assigned to a global variable):
            REDUCEOutputThread outputGobbler = new REDUCEOutputThread(p.getInputStream());
            Thread th = new Thread(outputGobbler);
            th.setDaemon(true); // terminate after all the stages are closed
            th.start();

            // Reset enabled state of controls:
            reduceStarted();
        } catch (Exception exc) {
            RunREDUCE.errorMessageDialog("REDUCE Process", "Error running REDUCE:\n" + exc);
        }

        title = reduceCommand.name;
        outputLabel.setText(outputLabelDefault + "  |  " + title);
        if (RRPreferences.displayPane == RRPreferences.DisplayPane.TABBED)
            RunREDUCE.tabPane.getSelectionModel().getSelectedItem().setText(title);

        runningREDUCE = true;

        // Special support for Redfront I/O colouring:
        if (RRPreferences.colouredIOState == RRPreferences.ColouredIO.REDFRONT)
            sendStringToREDUCENoEcho("load_package redfront;\n");

        // Return the focus to the input text area:
        inputTextArea.requestFocus();
    }

    // REDUCE output processing *******************************************************************

    /**
     * This thread reads from the REDUCE output pipe and appends it to the GUI output pane.
     */
    private class REDUCEOutputThread extends Task<Void> {
        private final InputStream input; // REDUCE pipe output (buffered)

        private REDUCEOutputThread(InputStream input) {
            this.input = input;
        }

        @Override
        public Void call() {
            // Must output characters rather than lines so that prompt appears!
            final StringBuilder text = new StringBuilder();
            try (InputStreamReader isr = new InputStreamReader(input);
                 BufferedReader br = new BufferedReader(isr)) {
                int c;
                for (; ; ) {
                    if (isCancelled()) break;
                    if (!br.ready()) {
                        int textLength = text.length();
                        if (textLength > 0) {
//                        Platform.runLater(() -> {
//                            // The TextFlow can only be modified on the JavaFX Application Thread!
                            processOutput(text.toString(), textLength);
//                        });
                            text.setLength(0); // reset string builder to gather new output
                        } else
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                if (isCancelled()) break;
                            }
                    } else if ((c = br.read()) != -1) {
                        if (RunREDUCE.debugOutput) {
                            if (Character.isISOControl((char) c)) {
                                if ((char) c != '\r') {
                                    if ((char) c == '\n')
                                        text.append((char) c);
                                    else
                                        text.append('|').append((char) c).append('^').append((char) (c + 64)).append('|');
                                }
                            } else
                                text.append((char) c);
                        } else {
                            if ((char) c != '\r') // ignore CRs
                                text.append((char) c);
                        }
                    } else break;
                }
            } catch (Exception exc) {
                exc.printStackTrace();
            }
            return null;
        }
    }

//    fontFamilyAndSizeStyle ="font-family:"+RunREDUCE.reduceFontFamilyName
//                    +";font-size:"+RRPreferences.fontSize;

    private String outputText(String text) {
//        Text t = new Text(text);
//        t.setStyle(RunREDUCE.fontFamilyAndSizeStyle);
        return text;
    }

    private String outputText(String text, String color) {
//        Text t = new Text(text);
//        t.setStyle(RunREDUCE.fontFamilyAndSizeStyle + ";-fx-fill:" + color);
        return text;
    }

    private String promptText(String text, String color) {
//        Text t = new Text(text);
//        if (RRPreferences.boldPromptsState)
//            t.setStyle(RunREDUCE.fontFamilyAndSizeStyle + ";-fx-font-weight:bold" + ";-fx-fill:" + color);
//        else
//            t.setStyle(RunREDUCE.fontFamilyAndSizeStyle + ";-fx-fill:" + color);
        return text;
    }

    private static final Pattern promptPattern = Pattern.compile("(?:\\d+([:*]) )|\\?");

    /**
     * Process a batch of output from the REDUCEOutputThread and
     * pass is back to the JavaFX Application Thread for display.
     */
    private void processOutput(String text, int textLength) {
        int promptIndex;
        String promptString;
        Matcher promptMatcher;
        List<String> textList = new ArrayList<>();

        switch (RRPreferences.colouredIOState) {
            case NONE:
            default: // no IO display colouring, but maybe prompt processing
                inputColor = DEFAULT_COLOR;
                promptIndex = text.lastIndexOf("\n") + 1;
                if (promptIndex < textLength &&
                        (promptMatcher = promptPattern.matcher(promptString = text.substring(promptIndex))).matches()) {
                    questionPrompt = promptMatcher.group(1) == null;
                    textList.add(outputText(text.substring(0, promptIndex)));
                    textList.add(promptText(promptString, DEFAULT_COLOR));
                } else
                    textList.add(outputText(text));
                break;

            case MODAL: // mode coloured IO display processing
                // Split off the final line, which should consist of the next input prompt:
                promptIndex = text.lastIndexOf("\n") + 1;
                if (promptIndex < textLength &&
                        (promptMatcher = promptPattern.matcher(promptString = text.substring(promptIndex))).matches()) {
                    questionPrompt = promptMatcher.group(1) == null;
                    if (0 < promptIndex)
                        textList.add(outputText(text.substring(0, promptIndex), outputColor));
                    // Only colour output *after* initial REDUCE header.
                    if (!questionPrompt) {
                        switch (promptMatcher.group(1)) {
                            case "*":
                                promptColor = SYMBOLIC_INPUT_COLOR;
                                inputColor = SYMBOLIC_INPUT_COLOR;
                                outputColor = SYMBOLIC_OUTPUT_COLOR;
                                break;
                            case ":":
                            default:
                                promptColor = ALGEBRAIC_INPUT_COLOR;
                                inputColor = ALGEBRAIC_INPUT_COLOR;
                                outputColor = ALGEBRAIC_OUTPUT_COLOR;
                                break;
                        }
                    }
                    textList.add(promptText(promptString, promptColor));
                } else
                    textList.add(outputText(text, outputColor));
                break; // end of case RunREDUCEPrefs.MODAL

            case REDFRONT: // redfront coloured IO display processing
                /*
                 * The markup output by the redfront package uses ASCII control characters:
                 * ^A prompt ^B input
                 * ^C algebraic-mode output ^D
                 * where ^A = \u0001, etc. ^A/^B and ^C/^D should always be paired.
                 * Prompts and input are always red, algebraic-mode output is blue,
                 * but any other output (echoed input or symbolic-mode output) is not coloured.
                 */
                // Must process arbitrary chunks of output, which may not contain matched pairs of start and end markers:
                inputColor = ALGEBRAIC_INPUT_COLOR;
                int start = 0;
                for (; ; ) {
                    int algOutputStartMarker = text.indexOf("\u0003", start);
                    int algOutputEndMarker = text.indexOf("\u0004", start);
                    if (algOutputStartMarker >= 0 && algOutputEndMarker >= 0) {
                        if (algOutputStartMarker < algOutputEndMarker) {
                            // TEXT < algOutputStartMarker < TEXT < algOutputEndMarker
                            if (start < algOutputStartMarker)
                                textList.add(outputText(text.substring(start, algOutputStartMarker)));
                            textList.add(outputText(text.substring(algOutputStartMarker + 1, algOutputEndMarker), ALGEBRAIC_OUTPUT_COLOR));
                            outputColor = DEFAULT_COLOR;
                            start = algOutputEndMarker + 1;
                        } else {
                            // TEXT < algOutputEndMarker < TEXT < algOutputStartMarker
                            textList.add(outputText(text.substring(start, algOutputEndMarker), ALGEBRAIC_OUTPUT_COLOR));
                            if (algOutputEndMarker + 1 < algOutputStartMarker)
                                textList.add(outputText(text.substring(algOutputEndMarker + 1, algOutputStartMarker)));
                            outputColor = ALGEBRAIC_OUTPUT_COLOR;
                            start = algOutputStartMarker + 1;
                        }
                    } else if (algOutputStartMarker >= 0) {
                        // TEXT < algOutputStartMarker < TEXT
                        if (start < algOutputStartMarker)
                            textList.add(outputText(text.substring(start, algOutputStartMarker)));
                        textList.add(outputText(text.substring(algOutputStartMarker + 1), ALGEBRAIC_OUTPUT_COLOR));
                        outputColor = ALGEBRAIC_OUTPUT_COLOR;
                        break;
                    } else if (algOutputEndMarker >= 0) {
                        // TEXT < algOutputEndMarker < TEXT
                        textList.add(outputText(text.substring(start, algOutputEndMarker), ALGEBRAIC_OUTPUT_COLOR));
                        outputColor = DEFAULT_COLOR;
                        processPromptMarkers(text, algOutputEndMarker + 1, textList);
                        break;
                    } else {
                        // No algebraic output markers
                        processPromptMarkers(text, start, textList);
                        break;
                    }
                }
                break; // end of case RunREDUCEPrefs.REDFRONT
        } // end of switch (RunREDUCEPrefs.colouredIOState)

        // This list can only be modified on the JavaFX Application Thread! True???
        Platform.runLater(() -> {
            HTMLElement element = (HTMLElement) document.createElement("pre");
//            element.setAttribute("style", RunREDUCE.fontFamilyAndSizeStyle + ";color:" + inputColor);
            StringBuilder stringBuilder = new StringBuilder();
            for (String s : textList) stringBuilder.append(s);
            element.appendChild(document.createTextNode(stringBuilder.toString()));
            body.appendChild(element);
//            outputScrollPane.setVvalue(1.0);
        });
    }

    private static final Pattern PATTERN = Pattern.compile("\n1:"); // works better than "\n1: "

    private void processPromptMarkers(String text, int start, List<String> textList) {
        // Delete the very first prompt. (This code may not be reliable!)
        Matcher matcher;
        if (firstPrompt && (matcher = PATTERN.matcher(text)).find(start)) {
            firstPrompt = false;
            text = matcher.replaceFirst("");
        }
        // Look for prompt markers:
        int promptStartMarker = text.indexOf("\u0001", start);
        int promptEndMarker = text.indexOf("\u0002", start);
        if (promptStartMarker >= 0 && promptEndMarker >= 0) {
            if (start < promptStartMarker)
                textList.add(outputText(text.substring(start, promptStartMarker), outputColor));
            String promptString = text.substring(promptStartMarker + 1, promptEndMarker);
            questionPrompt = promptString.equals("?");
            textList.add(promptText(promptString, ALGEBRAIC_INPUT_COLOR));
        } else
            textList.add(outputText(text.substring(start), outputColor));
    }

    // Menu processing etc. ***********************************************************************

    // Menu item disabled statuses:
    private boolean inputFileMenuItemDisabled;
    private boolean inputPackageFileMenuItemDisabled;
    private boolean outputNewFileMenuItemDisabled;
    private boolean outputOpenFileMenuItemDisabled;
    private boolean outputHereMenuItemDisabled;
    private boolean shutFileMenuItemDisabled;
    private boolean shutLastMenuItemDisabled;
    private boolean loadPackagesMenuItemDisabled;
    private boolean stopREDUCEMenuItemDisabled;
    private boolean runREDUCESubmenuDisabled;

    private static final RunREDUCEFrame FRAME = RunREDUCE.runREDUCEFrame;

    /**
     * Reset disabled status of controls as appropriate when REDUCE is not running.
     */
    void reduceStopped() {
        startingOrStoppingREDUCE(false);
    }

    /**
     * Reset disabled status of controls as appropriate when REDUCE has just started.
     */
    void reduceStarted() {
        startingOrStoppingREDUCE(true);
    }

    private void startingOrStoppingREDUCE(boolean starting) {
        // Items to enable/disable when REDUCE starts/stops running:
        inputTextArea.setDisable(!starting);
        sendButton.setDisable(!starting);
        FRAME.inputFileMenuItem.setDisable(inputFileMenuItemDisabled = !starting);
        FRAME.inputPackageFileMenuItem.setDisable(inputPackageFileMenuItemDisabled = !starting);
        FRAME.outputNewFileMenuItem.setDisable(outputNewFileMenuItemDisabled = !starting);
        FRAME.loadPackagesMenuItem.setDisable(loadPackagesMenuItemDisabled = !starting);
        FRAME.stopREDUCEMenuItem.setDisable(stopREDUCEMenuItemDisabled = !starting);
        // Items to disable/enable when REDUCE starts/stops running:
        FRAME.runREDUCESubmenu.setDisable(runREDUCESubmenuDisabled = starting);
        // Items to disable always when REDUCE starts or stops running:
        FRAME.outputOpenFileMenuItem.setDisable(outputOpenFileMenuItemDisabled = true);
        FRAME.outputHereMenuItem.setDisable(outputHereMenuItemDisabled = true);
        FRAME.shutFileMenuItem.setDisable(shutFileMenuItemDisabled = true);
        FRAME.shutLastMenuItem.setDisable(shutLastMenuItemDisabled = true);
    }

    /**
     * Update the disabled status of the menus.
     */
    void updateMenus() {
        FRAME.inputFileMenuItem.setDisable(inputFileMenuItemDisabled);
        FRAME.inputPackageFileMenuItem.setDisable(inputPackageFileMenuItemDisabled);
        FRAME.outputNewFileMenuItem.setDisable(outputNewFileMenuItemDisabled);
        FRAME.outputOpenFileMenuItem.setDisable(outputOpenFileMenuItemDisabled);
        FRAME.outputHereMenuItem.setDisable(outputHereMenuItemDisabled);
        FRAME.shutFileMenuItem.setDisable(shutFileMenuItemDisabled);
        FRAME.shutLastMenuItem.setDisable(shutLastMenuItemDisabled);
        FRAME.loadPackagesMenuItem.setDisable(loadPackagesMenuItemDisabled);
        FRAME.stopREDUCEMenuItem.setDisable(stopREDUCEMenuItemDisabled);
        FRAME.runREDUCESubmenu.setDisable(runREDUCESubmenuDisabled);
    }

    /**
     * Called as outputFileDisableMenuItems(false) when outputNewFileMenuItemAction
     * or outputOpenFileMenuItemAction run.
     */
    void outputFileDisableMenuItems(boolean disable) {
        FRAME.outputHereMenuItem.setDisable(outputHereMenuItemDisabled = disable);
        FRAME.shutFileMenuItem.setDisable(shutFileMenuItemDisabled = disable);
        FRAME.shutLastMenuItem.setDisable(shutLastMenuItemDisabled = disable);
        FRAME.outputOpenFileMenuItem.setDisable(outputOpenFileMenuItemDisabled =
                disable || outputFileList.size() <= 1);
    }

    /**
     * Called when shutFileMenuItemAction or shutLastMenuItemAction run.
     */
    void outputFileDisableMenuItemsMaybe() {
        if (outputFileList.isEmpty()) outputFileDisableMenuItems(true);
        if (outputFileList.size() == 1)
            FRAME.outputOpenFileMenuItem.setDisable(outputOpenFileMenuItemDisabled = true);
    }

    /**
     * Called when outputHereMenuItemAction run.
     * Can output to an open output file if there is one.
     */
    void outputHereDisableMenuItemsMaybe() {
        FRAME.outputHereMenuItem.setDisable(outputHereMenuItemDisabled = true);
        if (!outputFileList.isEmpty())
            FRAME.outputOpenFileMenuItem.setDisable(outputOpenFileMenuItemDisabled = false);
    }
}
