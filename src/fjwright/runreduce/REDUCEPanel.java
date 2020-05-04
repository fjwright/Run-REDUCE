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

import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
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
                    // Run REDUCE.  (A direct call hangs the GUI!)
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
                    REDUCEOutputThread(p.getInputStream(), outputNodeObservableList, this);
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
//        RRPreferences.colouredIOState = RRPreferences.colouredIOIntent;
//        if (RRPreferences.colouredIOState == RRPreferences.ColouredIO.REDFRONT) {
//            // Tidy up the initial prompt. Waiting for it is NECESSARY:
//            StyledDocument styledDoc = outputTextPane.getStyledDocument();
//            try {
//                // This typically sleeps a couple of times.
//                while (!(styledDoc.getLength() >= 3 &&
//                        styledDoc.getText(styledDoc.getLength() - 3, 3).equals("1: "))) {
////                System.err.println("Waiting...");
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//                styledDoc.remove(styledDoc.getLength() - 4, 4);
//            } catch (BadLocationException e) {
//                e.printStackTrace();
//            }
//            sendStringToREDUCENoEcho("load_package redfront;\n");
//        }

        // Return the focus to the input text area:
        inputTextArea.requestFocus();
    }
}

/**
 * This thread reads from the REDUCE output pipe and appends it to the GUI output pane.
 */
class REDUCEOutputThread extends Thread {
    private final InputStream input; // REDUCE pipe output (buffered)
    private final ObservableList<Node> outputNodeObservableList; // GUI output pane
    private final REDUCEPanel reducePanel;
    static final SimpleAttributeSet algebraicPromptAttributeSet = new SimpleAttributeSet();
    static final SimpleAttributeSet symbolicPromptAttributeSet = new SimpleAttributeSet();
    static final SimpleAttributeSet algebraicOutputAttributeSet = new SimpleAttributeSet();
    static final SimpleAttributeSet symbolicOutputAttributeSet = new SimpleAttributeSet();
    static final SimpleAttributeSet algebraicInputAttributeSet = new SimpleAttributeSet();
    static final SimpleAttributeSet symbolicInputAttributeSet = new SimpleAttributeSet();
    static SimpleAttributeSet promptAttributeSet = new SimpleAttributeSet();
    static SimpleAttributeSet inputAttributeSet;
    static SimpleAttributeSet outputAttributeSet;
    private static final Pattern promptPattern = Pattern.compile("\\d+([:*]) ");
    private final StringBuilder text = new StringBuilder(); // Must not be static!

//    private static final Color ALGEBRAICOUTPUTCOLOR = Color.blue;
//    private static final Color SYMBOLICOUTPUTCOLOR = new Color(0x80_00_80);
//    private static final Color ALGEBRAICINPUTCOLOR = Color.red;
//    private static final Color SYMBOLICINPUTCOLOR = new Color(0x80_00_00);

    REDUCEOutputThread(InputStream input, ObservableList<Node> outputNodeObservableList, REDUCEPanel reducePanel) {
        this.input = input;
        this.outputNodeObservableList = outputNodeObservableList;
        this.reducePanel = reducePanel;
//        StyleConstants.setForeground(algebraicOutputAttributeSet, ALGEBRAICOUTPUTCOLOR);
//        StyleConstants.setForeground(symbolicOutputAttributeSet, SYMBOLICOUTPUTCOLOR);
//        StyleConstants.setForeground(algebraicInputAttributeSet, ALGEBRAICINPUTCOLOR);
//        StyleConstants.setForeground(symbolicInputAttributeSet, SYMBOLICINPUTCOLOR);
//        StyleConstants.setForeground(algebraicPromptAttributeSet, ALGEBRAICINPUTCOLOR);
//        StyleConstants.setForeground(symbolicPromptAttributeSet, SYMBOLICINPUTCOLOR);
    }

    public void run() {
        outputAttributeSet = null; // for initial header
        switch (RRPreferences.colouredIOState) {
            case NONE:
                inputAttributeSet = null;
                break;
            case REDFRONT:
                inputAttributeSet = algebraicInputAttributeSet;
                break;
        }
        // Must output characters rather than lines so that prompt appears!
        try (InputStreamReader isr = new InputStreamReader(input);
             BufferedReader br = new BufferedReader(isr)) {
            int c;
            for (; ; ) {
                if (!br.ready()) {
                    int textLength = text.length();
                    if (textLength > 0)
                        processOutput(textLength, reducePanel);
                    else
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

    /**
     * Process a batch of output in this REDUCEOutputThread and
     * pass is back to the JavaFX Application Thread for display.
     */
    private void processOutput(int textLength, REDUCEPanel reducePanel) {
        int promptIndex;
        String promptString;
        switch (RRPreferences.colouredIOState) {
            case NONE:
            default: // no IO display colouring, but maybe prompt processing
                if ((RRPreferences.boldPromptsState) &&
                        (promptIndex = text.lastIndexOf("\n") + 1) < textLength &&
                        promptPattern.matcher(promptString = text.substring(promptIndex)).matches()) {
                    Text t1 = new Text(text.substring(0, promptIndex));
                    t1.setFont(RunREDUCE.reduceFont);
                    Text t2 = new Text(promptString);
                    t2.setFont(RunREDUCE.reduceFontBold);
                    t2.setFill(Color.RED); // FixMe TEMPORARY FOR TESTING!
                    // This list can only be modified on the JavaFX Application Thread!
                    Platform.runLater(() -> {
                        outputNodeObservableList.addAll(t1, t2);
                        reducePanel.outputScrollPane.setVvalue(1.0); // FixMe Doesn't completely work!
                    });
                } else {
                    Text t = new Text(text.toString());
                    t.setFont(RunREDUCE.reduceFont);
                    // This list can only be modified on the JavaFX Application Thread!
                    Platform.runLater(() -> {
                        outputNodeObservableList.add(t);
                    });
                }
                break;

            case MODAL: // mode coloured IO display processing
                // Split off the final line, which should consist of the next input prompt:
                promptIndex = text.lastIndexOf("\n") + 1;
                Matcher promptMatcher;
                if (promptIndex < textLength &&
                        (promptMatcher = promptPattern.matcher(promptString = text.substring(promptIndex))).matches()) {
//                                    styledDoc.insertString(styledDoc.getLength(), text.substring(0, promptIndex), outputAttributeSet);
                    // Only colour output *after* initial REDUCE header.
                    switch (promptMatcher.group(1)) {
                        case "*":
                            promptAttributeSet = symbolicPromptAttributeSet;
                            inputAttributeSet = symbolicInputAttributeSet;
                            outputAttributeSet = symbolicOutputAttributeSet;
                            break;
                        case ":":
                        default:
                            promptAttributeSet = algebraicPromptAttributeSet;
                            inputAttributeSet = algebraicInputAttributeSet;
                            outputAttributeSet = algebraicOutputAttributeSet;
                            break;
                    }
//                                    styledDoc.insertString(styledDoc.getLength(), promptString, promptAttributeSet);
                } else
//                                    styledDoc.insertString(styledDoc.getLength(), text.toString(), outputAttributeSet);
                    break; // end of case RunREDUCEPrefs.MODE

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
                for (; ; ) {
                    int algOutputStartMarker = text.indexOf("\u0003");
                    int algOutputEndMarker = text.indexOf("\u0004");
                    if (algOutputStartMarker >= 0 && algOutputEndMarker >= 0) {
                        if (algOutputStartMarker < algOutputEndMarker) {
                            // TEXT < algOutputStartMarker < TEXT < algOutputEndMarker
//                                            styledDoc.insertString(styledDoc.getLength(), text.substring(0, algOutputStartMarker), null);
//                                            styledDoc.insertString(styledDoc.getLength(), text.substring(algOutputStartMarker + 1, algOutputEndMarker), algebraicOutputAttributeSet);
                            outputAttributeSet = null;
                            text.delete(0, algOutputEndMarker + 1);
                        } else {
                            // TEXT < algOutputEndMarker < TEXT < algOutputStartMarker
//                                            styledDoc.insertString(styledDoc.getLength(), text.substring(0, algOutputEndMarker), algebraicOutputAttributeSet);
//                                            styledDoc.insertString(styledDoc.getLength(), text.substring(algOutputEndMarker + 1, algOutputStartMarker), null);
                            outputAttributeSet = algebraicOutputAttributeSet;
                            text.delete(0, algOutputStartMarker + 1);
                        }
                    } else if (algOutputStartMarker >= 0) {
                        // TEXT < algOutputStartMarker < TEXT
//                                        styledDoc.insertString(styledDoc.getLength(), text.substring(0, algOutputStartMarker), null);
//                                        styledDoc.insertString(styledDoc.getLength(), text.substring(algOutputStartMarker + 1), algebraicOutputAttributeSet);
                        outputAttributeSet = algebraicOutputAttributeSet;
                        break;
                    } else if (algOutputEndMarker >= 0) {
                        // TEXT < algOutputEndMarker < TEXT
//                                        styledDoc.insertString(styledDoc.getLength(), text.substring(0, algOutputEndMarker), algebraicOutputAttributeSet);
                        outputAttributeSet = null;
//                        processPromptMarkers(algOutputEndMarker + 1);
                        break;
                    } else {
                        // No algebraic output markers.
//                        processPromptMarkers(0);
                        break;
                    }
                }
                break; // end of case RunREDUCEPrefs.REDFRONT
        } // end of switch (RunREDUCEPrefs.colouredIOState)

        text.setLength(0); // delete any remaining text
    }

    private void processPromptMarkers(int start) throws BadLocationException {
        // Look for prompt markers:
        int promptStartMarker = text.indexOf("\u0001", start);
        int promptEndMarker = text.indexOf("\u0002", start);
        if (promptStartMarker >= 0 && promptEndMarker >= 0) {
//            styledDoc.insertString(styledDoc.getLength(), text.substring(start, promptStartMarker), outputAttributeSet);
//            styledDoc.insertString(styledDoc.getLength(), text.substring(promptStartMarker + 1, promptEndMarker), algebraicPromptAttributeSet);
//            styledDoc.insertString(styledDoc.getLength(), text.substring(promptEndMarker + 1), null);
        } else {
//            styledDoc.insertString(styledDoc.getLength(), text.substring(start), outputAttributeSet);
        }
    }
}
