package fjwright.runreduce;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

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
    Label outputLabel;
    @FXML
    ScrollPane outputScrollPane;
    @FXML
    TextFlow outputTextFlow;
    @FXML
    TextArea inputTextArea;
    @FXML
    Button sendButton;
    @FXML
    Button earlierButton;
    @FXML
    Button laterButton;

    // Menu item status:
    boolean inputFileMenuItemDisabled;
    boolean outputFileMenuItemDisabled;
    boolean loadPackagesMenuItemDisabled;
    boolean stopREDUCEMenuItemDisabled;
    boolean runREDUCESubmenuDisabled;
    boolean outputHereMenuItemDisabled;
    boolean shutFileMenuItemDisabled;
    boolean shutLastMenuItemDisabled;

    private final ObservableList<Node> outputNodeObservableList;
    private final List<String> inputList = new ArrayList<>();
    private int inputListIndex = 0;
    private int maxInputListIndex = 0;
    private static final Pattern pattern =
            Pattern.compile(".*\\b(?:bye|quit)\\s*[;$]?.*", Pattern.CASE_INSENSITIVE);
    private PrintWriter reduceInputPrintWriter;
    boolean runningREDUCE;
    String title; // REDUCE version if REDUCE is running
    static final String outputLabelDefault = "Input/Output Display";
//    static Color deselectedBackground = new Color(0xF8_F8_F8);

    private static final Color ALGEBRAIC_OUTPUT_COLOR = Color.BLUE;
    private static final Color SYMBOLIC_OUTPUT_COLOR = Color.rgb(0x80, 0x00, 0x80);
    private static final Color ALGEBRAIC_INPUT_COLOR = Color.RED;
    private static final Color SYMBOLIC_INPUT_COLOR = Color.rgb(0x80, 0x00, 0x00);

    static final Color DEFAULT_COLOR = Color.BLACK;
    static Color inputColor = DEFAULT_COLOR;
    static Color outputColor = DEFAULT_COLOR;

    public REDUCEPanel() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("REDUCEPanel.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        outputNodeObservableList = outputTextFlow.getChildren();

        // Auto-run REDUCE if appropriate:
        if (!RRPreferences.autoRunVersion.equals(RRPreferences.NONE))
            for (REDUCECommand cmd : RunREDUCE.reduceConfiguration.reduceCommandList)
                if (RRPreferences.autoRunVersion.equals(cmd.version)) {
                    // Run REDUCE.  (A direct call throws an error!)
                    Platform.runLater(() -> run(cmd));
                    break;
                }

        // Give the input text area the initial focus:
        inputTextArea.requestFocus(); // FixMe This doesn't seem to be working!
        // Reset menu item status as appropriate when REDUCE is not running:
        // This causes problems if called here but OK where REDUCEPanel instantiated, currently in RunREDUCE!
//        RunREDUCE.runREDUCEFrame.reduceStopped();
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
            boolean questionPrompt = false;
//            for (int i = outputTextFlow.getLength() - 1; i > 0; i--) {
//                String c = outputTextArea.getText(i, i + 1);
//                if (c.equals("\n")) {
//                    // found start of last line
//                    break;
//                } else if (c.equals("?")) {
//                    // found question mark
//                    questionPrompt = true;
//                    break;
//                }
//            }
            // if isShiftDown then do not auto terminate, hence if !isShiftDown then auto terminate:
            sendInteractiveInputToREDUCE(text, !questionPrompt && !isShiftDown);
            inputTextArea.clear();
            inputListIndex = inputList.size();
            maxInputListIndex = inputListIndex - 1;
            earlierButton.setDisable(false);
            laterButton.setDisable(true);
            if (pattern.matcher(text).matches()) {
                runningREDUCE = false;
                sendButton.setDisable(true);
                // Reset enabled status of menu items:
                RunREDUCE.runREDUCEFrame.reduceStopped();
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
        Text t = new Text(text);
        t.setFont(RunREDUCE.reduceFont);
        t.setFill(inputColor);
        outputNodeObservableList.add(t);
        // Make sure the new input text is visible, even if there was
        // a selection in the output text area:
//        outputTextArea.end();
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

    /**
     * Run the specified REDUCE command in this REDUCE panel.
     */
    void run(REDUCECommand reduceCommand) {
        outputColor = DEFAULT_COLOR; // for initial header
        RRPreferences.colouredIOState = RRPreferences.colouredIOIntent;
        switch (RRPreferences.colouredIOState) {
            case NONE:
                inputColor = DEFAULT_COLOR;
                break;
            case REDFRONT:
                inputColor = ALGEBRAIC_INPUT_COLOR;
                break;
        }

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
            REDUCEOutputThread outputGobbler = new
                    REDUCEOutputThread(p.getInputStream(), this);
            outputGobbler.start();

            // Reset menu item status as appropriate when REDUCE has just started.
            RunREDUCE.runREDUCEFrame.reduceStarted();
        } catch (Exception exc) {
            RunREDUCE.errorMessageDialog(
                    "Error running REDUCE -- " + exc,
                    "REDUCE Process Error");
        }

        title = reduceCommand.version;
        outputLabel.setText(outputLabelDefault + "  |  " + title);
//        if (RRPreferences.displayPane == RRPreferences.DisplayPane.TABBED) {
//            int tabIndex = RunREDUCE.tabbedPane.indexOfComponent(this);
//            RunREDUCE.tabbedPane.setTitleAt(tabIndex, title);
//            RunREDUCE.tabbedPane.getTabComponentAt(tabIndex).invalidate();
//        }

        runningREDUCE = true;
        sendButton.setDisable(false);

        // Special support for Redfront I/O colouring:
        if (RRPreferences.colouredIOState == RRPreferences.ColouredIO.REDFRONT) {
            // Tidy up the initial prompt. Waiting for it is NECESSARY:
            // This typically sleeps a couple of times.
//            Text t;
//            while (!(outputNodeObservableList.size() > 0 &&
//                    (t = (Text) outputNodeObservableList.get(outputNodeObservableList.size() - 1)).
//                            getText().endsWith("1: "))) {
////                System.err.println("Waiting...");
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            t.setText(t.getText().substring(0, t.getText().length() - 4));
            sendStringToREDUCENoEcho("load_package redfront;\n");
        }

        // Return the focus to the input text area:
        inputTextArea.requestFocus();
    }

    private Text outputText(String text) {
        Text t = new Text(text);
        t.setFont(RunREDUCE.reduceFont);
        return t;
    }

    private Text outputText(String text, Color color) {
        Text t = new Text(text);
        t.setFont(RunREDUCE.reduceFont);
        t.setFill(color);
        return t;
    }

    private Text promptText(String text, Color color) {
        Text t = new Text(text);
        if (RRPreferences.boldPromptsState)
            t.setFont(RunREDUCE.reduceFontBold);
        else
            t.setFont(RunREDUCE.reduceFont);
        t.setFill(color);
        return t;
    }

    private static final Pattern promptPattern = Pattern.compile("\\d+([:*]) ");

    /**
     * Process a batch of output from the REDUCEOutputThread and
     * pass is back to the JavaFX Application Thread for display.
     */
    void processOutput(String text, int textLength) {
        int promptIndex;
        String promptString;
        List<Text> textList = new ArrayList<>();
        Color promptColor;

        switch (RRPreferences.colouredIOState) {
            case NONE:
            default: // no IO display colouring, but maybe prompt processing
                if (RRPreferences.boldPromptsState &&
                        (promptIndex = text.lastIndexOf("\n") + 1) < textLength &&
                        promptPattern.matcher(promptString = text.substring(promptIndex)).matches()) {
                    textList.add(outputText(text.substring(0, promptIndex)));
                    textList.add(promptText(promptString, DEFAULT_COLOR));
                } else
                    textList.add(outputText(text));
                break;

            case MODAL: // mode coloured IO display processing
                // Split off the final line, which should consist of the next input prompt:
                promptIndex = text.lastIndexOf("\n") + 1;
                Matcher promptMatcher;
                if (promptIndex < textLength &&
                        (promptMatcher = promptPattern.matcher(promptString = text.substring(promptIndex))).matches()) {
                    textList.add(outputText(text.substring(0, promptIndex), outputColor));
                    // Only colour output *after* initial REDUCE header.
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

        // This list can only be modified on the JavaFX Application Thread!
        Platform.runLater(() -> {
            outputNodeObservableList.addAll(textList);
            outputScrollPane.setVvalue(1.0);
        });
    }

    private void processPromptMarkers(String text, int start, List<Text> textList) {
        // Look for prompt markers:
        int promptStartMarker = text.indexOf("\u0001", start);
        int promptEndMarker = text.indexOf("\u0002", start);
        if (promptStartMarker >= 0 && promptEndMarker >= 0) {
            textList.add(outputText(text.substring(start, promptStartMarker), outputColor));
            textList.add(promptText(text.substring(promptStartMarker + 1, promptEndMarker), ALGEBRAIC_INPUT_COLOR));
        } else
            textList.add(outputText(text.substring(start), outputColor));
    }
}

/**
 * This thread reads from the REDUCE output pipe and appends it to the GUI output pane.
 */
class REDUCEOutputThread extends Thread {
    private final InputStream input; // REDUCE pipe output (buffered)
    private final REDUCEPanel reducePanel;

    REDUCEOutputThread(InputStream input, REDUCEPanel reducePanel) {
        this.input = input;
        this.reducePanel = reducePanel;
    }

    @Override
    public void run() {
        // Must output characters rather than lines so that prompt appears!
        final StringBuilder text = new StringBuilder();
        try (InputStreamReader isr = new InputStreamReader(input);
             BufferedReader br = new BufferedReader(isr)) {
            int c;
            for (; ; ) {
                if (!br.ready()) {
                    int textLength = text.length();
                    if (textLength > 0) {
//                        Platform.runLater(() -> {
//                            // The TextFlow can only be modified on the JavaFX Application Thread!
                        reducePanel.processOutput(text.toString(), textLength);
//                        });
                        text.setLength(0); // reset string builder to gather new output
                    } else
                        Thread.sleep(10);
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
    }
}
