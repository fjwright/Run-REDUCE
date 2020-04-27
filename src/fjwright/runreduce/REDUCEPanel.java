package fjwright.runreduce;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

import java.awt.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/*
 * This class provides the panel that displays REDUCE input and output.
 * The run method runs REDUCE as a sub-process.
 */
public class REDUCEPanel extends BorderPane {
    // Fields defined in RunREDUCEFrame.fxml:
    public Label outputLabel;
    public TextArea outputTextArea;
    public TextArea inputTextArea;
    public Button earlierButton;
    public Button sendButton;
    public Button laterButton;

    private final List<String> inputList = new ArrayList<>();
    private int inputListIndex = 0;
    private int maxInputListIndex = 0;
    private static final Pattern pattern =
            Pattern.compile(".*\\b(?:bye|quit)\\s*[;$]?.*", Pattern.CASE_INSENSITIVE);
    private PrintWriter reduceInputPrintWriter;
    //    MenuItemStatus menuItemStatus = new MenuItemStatus();
    boolean runningREDUCE;
    String title; // REDUCE version if REDUCE is running
    static final String outputLabelDefault = "Input/Output Display";
    static Color deselectedBackground = new Color(0xF8_F8_F8);

    public REDUCEPanel() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("REDUCEPanel.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        // Give the input text area the initial focus:
//        inputTextArea.requestFocusInWindow();

    }

    public void sendButtonAction(ActionEvent actionEvent) {
        String text = inputTextArea.getText();
        if (text.length() > 0) {
            inputList.add(text);
            sendInteractiveInputToREDUCE(text, true);
            inputTextArea.setText(null);
            inputListIndex = inputList.size();
            maxInputListIndex = inputListIndex - 1;
            earlierButton.setDisable(false);
            laterButton.setDisable(true);
            if (pattern.matcher(text).matches()) {
                sendButton.setDisable(!(runningREDUCE = false));
                // Reset enabled status of menu items:
//                RunREDUCE.reducePanel.menuItemStatus.reduceStopped();
            }
            // Return the focus to the input text area:
//            inputTextArea.requestFocusInWindow();
        }
    }

    public void earlierButtonAction(ActionEvent actionEvent) {
        System.err.println("Earlier button pressed.");
    }

    public void laterButtonAction(ActionEvent actionEvent) {
        System.err.println("Later button pressed.");
    }

    void sendInteractiveInputToREDUCE(String text, boolean autoTerminate) {
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
        outputTextArea.appendText(text);
        // Make sure the new input text is visible, even if there was
        // a selection in the output text area:
//        outputTextPane.setCaretPosition(styledDoc.getLength());
//        sendStringToREDUCENoEcho(text);
    }

    void sendStringToREDUCENoEcho(String text) {
        // Send the input to the REDUCE input pipe:
        if (reduceInputPrintWriter != null) {
            reduceInputPrintWriter.print(text);
            reduceInputPrintWriter.flush();
        }
    }

}
