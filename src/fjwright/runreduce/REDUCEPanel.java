package fjwright.runreduce;

import javafx.application.Platform;
import javafx.beans.value.ObservableBooleanValue;
import javafx.concurrent.Task;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.w3c.dom.html.HTMLDocument;
import org.w3c.dom.html.HTMLElement;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.getProperty;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/*
 * This class provides the panel that displays REDUCE input and output.
 * The run method runs REDUCE as a sub-process.
 */
public class REDUCEPanel extends BorderPane {
    @FXML
    BorderPane ioDisplayPane;
    @FXML
    SplitPane splitPane; // Accessed in RunREDUCE.java
    @FXML
    private Label outputLabel, inputLabel;
    @FXML
    ToggleButton hideEditorToggleButton;
    @FXML
    Label activeLabel;
    @FXML
    WebView outputWebView; // Accessed in RunREDUCEFrame.java
    @FXML
    public TextArea inputTextArea; // Accessed in RunREDUCE & templates
    @FXML
    Button sendButton; // Accessed in RunREDUCEFrame.java
    @FXML
    private Button earlierButton, laterButton;

    int fontSize;
    private boolean boldPromptsState, colouredIOState, typesetMathsState;
    private double[] dividerPositions;

    final WebEngine webEngine;
    HTMLDocument doc;
    HTMLElement html, body;
    private HTMLElement head, inputPre;
    private JSObject katex, katexMacros;
    JSObject window;

    /*
     * The <body> content should look like repeats of this structure:
     * <pre class=inputCSSClass><span class="prompt">Prompt</span>REDUCE input</pre>
     * <pre class=outputCSSClass>REDUCE output</pre> if non-typeset
     */

    private final List<String> inputList = new ArrayList<>();
    private int inputListIndex = 0;
    private int maxInputListIndex = 0;
    private static final Pattern quitPattern =
            Pattern.compile(".*\\b(?:bye|quit)\\s*[;$]?.*", Pattern.CASE_INSENSITIVE);
    private PrintWriter reduceInputPrintWriter;
    boolean runningREDUCE;
    String title; // REDUCE version if REDUCE is running
    private static final String outputLabelDefault = "Input/Output Display";
    private boolean beforeFirstPrompt, questionPrompt;
    final List<File> outputFileList = new ArrayList<>();

    private static final String ALGEBRAIC_INPUT_CSS_CLASS = "algebraic-input";
    private static final String SYMBOLIC_INPUT_CSS_CLASS = "symbolic-input";
    private static final String ALGEBRAIC_OUTPUT_CSS_CLASS = "algebraic-output";
    private static final String SYMBOLIC_OUTPUT_CSS_CLASS = "symbolic-output";
    private static final String WARNING_CSS_CLASS = "warning";
    private static final String ERROR_CSS_CLASS = "error";
    private static final String DEFAULT_OUTPUT_CSS_CLASS = null; // echoing of file input
    private String inputCSSClass;
    private String outputCSSClass;
    private HTMLElement fontSizeStyle;
    private HTMLElement promptWeightStyle;
    private HTMLElement colorStyle;

    private boolean rrprintLoaded, hideNextOutputAndPrompt, hideNextOutputShowPrompt;

    /*
     * JavaScript debugging support. See
     * https://stackoverflow.com/questions/28687640/javafx-8-webengine-how-to-get-console-log-from-javascript-to-system-out-in-ja
     */
    public static class JSBridge {
        public void log(String message) {
            System.err.println(message);
        }
    }

    // Maintain a strong reference to prevent garbage collection:
    // https://bugs.openjdk.java.net/browse/JDK-8154127
    private final JSBridge bridge = new JSBridge();

    REDUCEPanel() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("REDUCEPanel.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        fontSize = RRPreferences.fontSize;
        boldPromptsState = RRPreferences.boldPromptsState;
        colouredIOState = RRPreferences.colouredIOState;
        typesetMathsState = RRPreferences.typesetMathsState;

        outputWebView.setContextMenuEnabled(false); // Cannot be done in FXML!
        webEngine = outputWebView.getEngine();

        // See https://katex.org/docs/browser.html
        // KaTeX requires the use of the HTML5 doctype. Without it, KaTeX may not render properly.
        // See https://github.com/KaTeX/KaTeX/issues/1775 for a discussion about rules disappearing.
        webEngine.loadContent("<!DOCTYPE html><html><head>" +
                "<link rel='stylesheet' href='" + REDUCEPanel.class.getResource("katex/katex.min.css") + "'>" +
                "<script src='" + REDUCEPanel.class.getResource("katex/katex.min.js") + "'></script>" +
                "<style>pre{margin:0}body{margin-bottom:15px}</style>" +
                "</head><body>" +
                // Debug KaTeX:
//                "<style>.katex-version {display: none;}.katex-version::after {content:\"0.10.2 or earlier\";}</style>\n" +
//                "<span class=\"katex\">\n" +
//                "  <span class=\"katex-mathml\">The KaTeX stylesheet is not loaded!</span>\n" +
//                "  <span class=\"katex-version rule\">KaTeX stylesheet version: </span>\n" +
//                "</span>\n" +
                "</body></html>");
        webEngine.getLoadWorker().stateProperty().addListener(
                (ov, oldState, newState) -> {
                    if (newState == State.SUCCEEDED) {
                        // Begin debugging support
                        window = (JSObject) webEngine.executeScript("window");
                        window.setMember("bridge", bridge);
                        webEngine.executeScript(
                                "console.error=function(message){bridge.log('JS ERROR: '+message)};" +
                                        "console.info=function(message){bridge.log('JS INFO: '+message)};" +
                                        "console.log=function(message){bridge.log('JS LOG: '+message)};" +
                                        "console.warn=function(message){bridge.log('JS WARN: '+message)};");
                        // End debugging support
                        outputWebViewAvailable();
                    }
                });
        webEngine.setOnError(System.err::println);

        // Note that a font name containing spaces needs quoting in CSS!
        inputTextArea.setStyle("-fx-font:" + fontSize + " '" + RunREDUCE.reduceFontFamilyName + "'");
        // Register the pop-up keyboard handler on the input editor:
        inputTextArea.addEventHandler(MouseEvent.MOUSE_CLICKED, PopupKeyboard::showPopupKbdOnInputEditor);

        hideEditorToggleButton.selectedProperty().addListener(hide -> {
            if (((ObservableBooleanValue) hide).getValue()) {
                dividerPositions = splitPane.getDividerPositions();
                this.setCenter(ioDisplayPane);
                outputWebView.requestFocus();
            } else {
                splitPane.getItems().set(0, ioDisplayPane);
                splitPane.setDividerPositions(dividerPositions);
                this.setCenter(splitPane);
                inputTextArea.requestFocus();
            }
        });
    }

    // WebView control ****************************************************************************

    /*
     * WebView uses WebKit, which is also used in Safari and Chrome.
     *
     * Access to Document Model: The WebEngine objects create and manage a Document Object Model (DOM)
     *     for their Web pages. The model can be accessed and modified using Java DOM Core classes.
     *     The getDocument() method provides access to the root of the model.
     *     Additionally DOM Event specification is supported to define event handlers in Java code.
     * Threading: WebEngine objects must be created and accessed solely from the JavaFX Application thread.
     *     This rule also applies to any DOM and JavaScript objects obtained from the WebEngine object.
     */

    // Evaluate this expression in the debugger to see the current document as HTML:
    // webEngine.executeScript("document.documentElement.outerHTML");
    // Ref: https://stackoverflow.com/questions/14273450/get-the-contents-from-the-webview-using-javafx

    /**
     * This method is run once the outputWebView is available.
     * Setting up the REDUCEPanel output display can now continue here.
     */
    private void outputWebViewAvailable() {
        // Use document factory methods to create new elements.
        doc = (HTMLDocument) webEngine.getDocument();
        html = (HTMLElement) doc.getDocumentElement();
        head = (HTMLElement) html.getElementsByTagName("head").item(0);
        body = doc.getBody();
        katex = (JSObject) webEngine.executeScript("katex");
//        katexMacros = (JSObject) webEngine.executeScript(
//                "var katexMacros={'\\\\Int':'\\\\int','\\\\>':''};katexMacros;");

        // Create default style elements:

        HTMLElement fontFamilyStyle = (HTMLElement) doc.createElement("style");
        // Note that a font name containing spaces needs quoting in CSS!
        fontFamilyStyle.appendChild(doc.createTextNode(
                // Styling body doesn't work...
                String.format("pre{font-family:'%s','Courier New',Courier,monospace}", RunREDUCE.reduceFontFamilyName)));
        head.appendChild(fontFamilyStyle);

        fontSizeStyle = (HTMLElement) doc.createElement("style");
        fontSizeStyle.appendChild(doc.createTextNode(null));
        updateFontSize(0);
        head.appendChild(fontSizeStyle);

        promptWeightStyle = (HTMLElement) doc.createElement("style");
        promptWeightStyle.appendChild(doc.createTextNode(".prompt{font-weight:bold}"));
        if (boldPromptsState) head.appendChild(promptWeightStyle);

        colorStyle = (HTMLElement) doc.createElement("style");
        colorStyle.appendChild(doc.createTextNode(null));
        updateFontColours();
        if (colouredIOState) head.appendChild(colorStyle);

        // Auto-run REDUCE if appropriate:
        if (!RRPreferences.autoRunVersion.equals(RRPreferences.NONE)) {
            for (REDUCECommand cmd : RunREDUCE.reduceConfiguration.reduceCommandList)
                if (RRPreferences.autoRunVersion.equals(cmd.name)) {
                    run(cmd);
                    break;
                }
        } else
            // Reset enabled status of controls:
            reduceStopped();

        // DEBUGGING...
//        webEngine.executeScript("console.log('Body font size: ' + " +
//                "getComputedStyle(document.body, null).getPropertyValue('font-size'))");
    }

    void updateFontSize(int newFontSize) {
        if (newFontSize > 0) fontSize = newFontSize;
        fontSizeStyle.getFirstChild().setNodeValue(String.format("body{font-size:%dpx}", fontSize));
    }

    void updateFontColours() {
        colorStyle.getFirstChild().setNodeValue(String.format(
                ".algebraic-input{color:%s}.symbolic-input{color:%s}" +
                        ".algebraic-output{color:%s}.symbolic-output{color:%s}" +
                        ".warning{color:black;background-color:%s}" +
                        ".error{color:black;background-color:%s}",
                FontColors.algebraicInput, FontColors.symbolicInput,
                FontColors.algebraicOutput, FontColors.symbolicOutput,
                FontColors.warning, FontColors.error));
    }

    void clearDisplay() {
        HTMLElement newBody = (HTMLElement) doc.createElement("body");
        html.replaceChild(newBody, body);
        body = newBody;
    }

    /**
     * Called in RRPreferences.save (only) to control bold prompts.
     */
    void setBoldPrompts(boolean enabled) {
        boldPromptsState = enabled;
        if (enabled) head.appendChild(promptWeightStyle);
        else head.removeChild(promptWeightStyle);
    }

    /**
     * Called in RRPreferences.save (only) to control coloured I/O.
     */
    void setColouredIO(boolean enabled) {
        colouredIOState = enabled;
        if (enabled) head.appendChild(colorStyle);
        else head.removeChild(colorStyle);
        if (runningREDUCE) {
            if (enabled) {
                if (rrprintLoaded) {
                    stealthInput("rrprint 'coloured!-output");
                } else {
//                stealthInput("load_package rrprint");
                    hideNextOutputAndPrompt = true;
                    sendStringToREDUCENoEcho("symbolic begin scalar !*msg,!*redefmsg,!*comp:=t;" +
                            inputRRprint() + "rrprint 'coloured!-output;" +
                            "crbuf!*:=cdr crbuf!*;inputbuflis!*:=cdr inputbuflis!*;" +
                            "statcounter:=statcounter-1;end$\n");
                    rrprintLoaded = true;
                }
            } else if (!typesetMathsState) stealthInput("rrprint nil");
        }
    }

    /**
     * Called in RRPreferences.save (only) to control typeset I/O.
     */
    void setTypesetMaths(boolean enabled) {
        typesetMathsState = enabled;
        if (runningREDUCE) {
            if (enabled) {
                if (rrprintLoaded) {
                    stealthInput("rrprint 'fancy!-output");
                } else {
//                    stealthInput("load_package rrprint");
                    hideNextOutputAndPrompt = true;
                    sendStringToREDUCENoEcho("symbolic begin scalar !*msg,!*redefmsg,!*comp:=t;" +
                            inputRRprint() + "rrprint 'fancy!-output;" +
                            "crbuf!*:=cdr crbuf!*;inputbuflis!*:=cdr inputbuflis!*;" +
                            "statcounter:=statcounter-1;end$\n");
                    rrprintLoaded = true;
                }
            } else if (colouredIOState) stealthInput("rrprint 'coloured!-output");
            else stealthInput("rrprint nil");
        }
    }

    private static String inputRRprint;

    /**
     * Return essentially the string
     * out "/dev/null"; in "../rrprint.red"; shut "/dev/null";
     * in a way that works on Windows and non-Windows platforms.
     * If running from a jar file then copy the resource to a real temporary file first.
     */
    private String inputRRprint() {
        if (inputRRprint != null) return inputRRprint;
        String rrprintFilename = "rrprint.red";
        URL url = RunREDUCEFrame.class.getResource(rrprintFilename);
        if (url == null) {
            RunREDUCE.alert(Alert.AlertType.ERROR, "Typeset Maths",
                    "Resource file \"" + rrprintFilename + "\" could not be located.");
        } else if (url.getProtocol().equals("file")) // Useful during development only!
            rrprintFilename = new File(url.getFile()).toString(); // to avoid leading / on Windows
        else { // Normal case: when running a jar file the protocol is jar.
            File rrprintTmpFile = new File(getProperty("java.io.tmpdir"), rrprintFilename);
            try (InputStream in = url.openStream()) {
                Files.copy(in, rrprintTmpFile.toPath(), REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            rrprintFilename = rrprintTmpFile.getPath();
        }
        return inputRRprint =
                String.format("out\"%s\";in\"%s\";shut\"%1$s\";",
                        REDUCEConfigurationType.windowsOS ? "nul" : "/dev/null",
                        rrprintFilename);
    }

    /**
     * Send input to REDUCE but hide any evidence from the I/O display.
     */
    private void stealthInput(String input) {
        // Hide the interaction with REDUCE:
        // See the bottom of packages/redfront/redfront.red.
        hideNextOutputAndPrompt = true;
        sendStringToREDUCENoEcho(
                String.format("symbolic <<%s;crbuf!*:=cdr crbuf!*;" +
                        "inputbuflis!*:=cdr inputbuflis!*;statcounter:=statcounter-1;>>$\n", input));
    }

    // User input processing **********************************************************************

    @FXML
    private void sendButtonClicked(MouseEvent mouseEvent) {
        sendAction(mouseEvent.isShiftDown());
    }

    @FXML
    private void sendButtonKeyTyped(KeyEvent keyEvent) {
        if (keyEvent.getCharacter().equals(" "))
            sendAction(keyEvent.isShiftDown());
    }

    private void addToInputList(String text) {
        inputList.add(text);
        inputListIndex = inputList.size();
        maxInputListIndex = inputListIndex - 1;
        earlierButton.setDisable(false);
        laterButton.setDisable(true);
    }

    private void sendAction(boolean isShiftDown) {
        String text = inputTextArea.getText();
        if (text.length() > 0) {
            addToInputList(text);
            text = PopupKeyboard.decode(text);
            sendInteractiveInputToREDUCE(text, !questionPrompt && !isShiftDown);
            if (quitPattern.matcher(text).matches()) {
                // Reset enabled status of controls:
                reduceStoppedMaybe();
            }
            inputTextArea.clear();
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

    public void sendInteractiveInputToREDUCE(String text, boolean autoTerminate) {
        // Strip trailing white space and if autoTerminate then ensure the input ends with a terminator:
        int i = text.length() - 1;
        char c = 0;
        while (i >= 0 && Character.isWhitespace(c = text.charAt(i))) i--;
        text = text.substring(0, i + 1);
        if (c == ';' || c == '$' || !autoTerminate) text += "\n";
        else text += ";\n";
        sendStringToREDUCEAndEcho(text);
    }

    /**
     * Scroll the REDUCE output to the bottom.
     * Must be run in the JavaFX Application Thread.
     * A sufficient delay seems necessary for the input to be rendered!
     * This may not be the best solution but it seems to work provided the delay is long enough.
     */
    private void scrollWebViewToBottom() {
        webEngine.executeScript("setTimeout(function(){document.documentElement.scrollIntoView(false)},300);" +
                // Scroll again after KaTeX has had time to complete:
                "setTimeout(function(){document.documentElement.scrollIntoView(false)},1000);");
    }

    /**
     * Send a string generated by a menu to REDUCE, echo it
     * and add it to the input history flagged as menu input.
     */
    void menuSendStringToREDUCEAndEcho(String text) {
        sendStringToREDUCEAndEcho(text);
        addToInputList(text);
    }

    /**
     * Send a string as a command to REDUCE and echo it.
     */
    void sendStringToREDUCEAndEcho(String text) {
        if (inputPre != null) {
            inputPre.appendChild(doc.createTextNode(text));
            // Make sure the new input text is visible:
            scrollWebViewToBottom();
        }
        sendStringToREDUCENoEcho(text);
        // Return the focus to the input text area:
        inputTextArea.requestFocus();
    }

    /*
     * REDUCE prompt + input elements should look like this:
     * <pre class=inputCSSClass><span class="prompt">Prompt</span>REDUCE input</pre>
     */

    /**
     * Send a string to the REDUCE input pipe provided REDUCE is still alive.
     */
    void sendStringToREDUCENoEcho(String text) {
        if (reduceProcess.isAlive() && reduceInputPrintWriter != null) {
            reduceInputPrintWriter.print(text);
            reduceInputPrintWriter.flush();
        } else {
            RunREDUCE.alert(Alert.AlertType.ERROR, "REDUCE Process",
                    "REDUCE is no longer running!");
            reduceStopped();
        }
    }

    void setSelected(boolean selected) {
        if (selected) {
//            outputTextFlow.setStyle("-fx-control-inner-background: white;");
//            outputWebView.setDisable(false); // Not useful
            activeLabel.setVisible(true);
            inputTextArea.setDisable(!runningREDUCE);
            outputLabel.setDisable(false);
            inputLabel.setDisable(false);
        } else {
//            outputTextFlow.setStyle("-fx-control-inner-background: #F8F8F8;");
//            outputWebView.setDisable(true); // Not useful
            activeLabel.setVisible(false);
            inputTextArea.setDisable(true);
            outputLabel.setDisable(true);
            inputLabel.setDisable(true);
        }
    }

    REDUCECommand previousREDUCECommand;
    Process reduceProcess;
    private REDUCEOutputThread outputThread;

    /**
     * Run the specified REDUCE command in this REDUCE panel.
     */
    void run(REDUCECommand reduceCommand) {
        outputCSSClass = null; // for initial header
        beforeFirstPrompt = true;
        rrprintLoaded = false;

        String[] command = reduceCommand.buildCommand();
        if (command == null) return;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(RunREDUCE.reduceConfiguration.workingDir));
            pb.redirectErrorStream(true); // necessary if start-up fails
            // pb.redirectInput(ProcessBuilder.Redirect.INHERIT); // Works!
            reduceProcess = pb.start();

            // Assign the REDUCE input stream to an instance field:
            OutputStreamWriter osr = new OutputStreamWriter(reduceProcess.getOutputStream());
            reduceInputPrintWriter = new PrintWriter(osr);

            // Start a thread to handle the REDUCE output stream
            // (assigned to a global variable):
            outputThread = new REDUCEOutputThread(reduceProcess.getInputStream());
            Thread th = new Thread(outputThread);
            th.setDaemon(true); // terminate after all the stages are closed
            th.start();
        } catch (Exception exc) {
            RunREDUCE.alert(Alert.AlertType.ERROR, "REDUCE Process",
                    "Error starting REDUCE:\n" + exc +
                            "\nCheck the REDUCE configuration.");
        }
        title = reduceCommand.name;
        // Return the focus to the input text area:
        inputTextArea.requestFocus();
        previousREDUCECommand = reduceCommand;
    }

    /**
     * Implement "Stop REDUCE" in the REDUCE menu.
     */
    void stop() {
        sendStringToREDUCEAndEcho("bye;\n");
        reduceStoppedMaybe();
    }

    /**
     * Implement "Restart REDUCE" in the REDUCE menu.
     */
    void restart() {
        outputThread.cancel(true); // to avoid PSL REDUCE leaving "quitting" in the display pane.
        sendStringToREDUCENoEcho("bye;\n");
        // Reset enabled status of controls:
        reduceStopped();
        try {
            reduceProcess.waitFor(1, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
        clearDisplay();
        if (previousREDUCECommand != null) run(previousREDUCECommand);
    }

    /**
     * Implement "Kill REDUCE" in the REDUCE menu.
     */
    void kill() {
//        reduceProcess.destroyForcibly();
        reduceProcess.destroy();
        // Reset enabled status of controls:
        try {
            reduceProcess.waitFor();
            reduceStopped();
            RunREDUCE.alert(Alert.AlertType.INFORMATION,
                    "REDUCE process status",
                    "REDUCE has been killed!",
                    "REDUCE Process");
        } catch (InterruptedException e) {
            RunREDUCE.alert(Alert.AlertType.ERROR, "REDUCE Process",
                    "REDUCE may not have been killed! You are advised to restart Run-REDUCE.");
        }
    }

    /**
     * Called in RunREDUCE.stop() only.
     */
    void terminateREDUCEifRunning() {
        if (runningREDUCE) {
            outputThread.cancel(true);
            reduceProcess.destroyForcibly();
        }
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
                while (!isCancelled()) {
                    if (!br.ready()) {
                        // This type declaration (or similar) is critical for inter-thread communication:
                        AtomicReference<String> textAtomicReferenceString = new AtomicReference<>(text.toString());
                        if (text.length() > 0) {
                            Platform.runLater(() -> {
                                // The DOM can only be modified on the JavaFX Application Thread!
                                processOutput(textAtomicReferenceString);
                            });
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
//            System.err.println("REDUCE has stopped running for some reason!");
            return null;
        }
    }

    /**
     * Output header text that is always preformatted and plain.
     */
    private void outputHeaderText(String text) {
        HTMLElement outputElement = (HTMLElement) doc.createElement("pre");
        outputElement.setTextContent(text);
        body.appendChild(outputElement);
    }

    /**
     * Append output text to the WebView control with the specified CSS class.
     */
    private void outputText(String text, String cssClass) {
        if (SYMBOLIC_OUTPUT_CSS_CLASS.equals(cssClass))
            outputPreformattedText(text, cssClass);
        else if (typesetMathsState) {
            if (RunREDUCE.runREDUCEFrame.showTeXMarkupCheckMenuItem.isSelected())
                outputPreformattedText(text, cssClass);
            outputTypesetMaths(text, cssClass);
        } else
            outputAlgebraicModeText(text, cssClass);
    }

    private static final Pattern WARNING_ERROR_PATTERN =
            Pattern.compile("^\\*{3}(\\*{2}).*", Pattern.MULTILINE);

    /**
     * Output preformatted text that may contain a warning or error message
     * in its own <pre> element.
     */
    private void outputPreformattedText(String text, String cssClass) {
        Matcher matcher;
        HTMLElement outputElement = (HTMLElement) doc.createElement("pre");
        if (colouredIOState &&
                // Check for a warning or error:
                (matcher = WARNING_ERROR_PATTERN.matcher(text)).find()) {
            outputElement.setTextContent(text.substring(0, matcher.start()));
            HTMLElement span = (HTMLElement) doc.createElement("span");
            span.setTextContent(text.substring(matcher.start(), matcher.end()));
            span.setClassName(matcher.start(1) >= 0 ? ERROR_CSS_CLASS : WARNING_CSS_CLASS);
            outputElement.appendChild(span);
            outputElement.appendChild(doc.createTextNode(text.substring(matcher.end())));
        } else
            outputElement.setTextContent(text);
        if (cssClass != null) outputElement.setClassName(cssClass);
        body.appendChild(outputElement);
    }

    private boolean inAlgOutput = false;

    /**
     * Handle a chunk of algebraic mode output and display
     * as much as possible by calling outputPreformattedText().
     */
    private void outputAlgebraicModeText(String text, String cssClass) {
        /*
         * rrprint delimits non-typeset algebraic output with ASCII control characters
         * (always at the start of a line):
         * ^C<algebraic-mode output>
         * ^D
         * where ^C = \u0003, ^D = \u0004. ^C/^D should always be paired (eventually).
         * But must process arbitrary chunks of output, which may
         * not contain matched pairs of start and end markers.
         * Algebraic-mode output is coloured blue (by default),but echoed input is not coloured.
         */
        int textLength = text.length(), start = 0, finish;
        while (start < textLength)
            if (inAlgOutput) { // in algebraic output; look for end of algebraic output, ^D:
                if ((finish = text.indexOf('\u0004', start)) != -1) { // ^D found
                    // Finish current algebraic output:
                    outputPreformattedText(text.substring(start, finish), cssClass);
                    inAlgOutput = false;
                    start = finish + 1; // Skip ^D:
                } else { // ^D not found so all algebraic output:
                    outputPreformattedText(text.substring(start), cssClass);
                    return;
                }
            } else { // Not in algebraic output so create a non-algebraic output element:
                // Look for start of algebraic output, ^C:
                if ((finish = text.indexOf('\u0003', start)) != -1) { // ^C found
                    // Output current non-algebraic output element:
                    if (start < finish) outputPreformattedText(text.substring(start, finish), DEFAULT_OUTPUT_CSS_CLASS);
                    start = finish + 1; // skip ^C
                    // Start new algebraic output:
                    inAlgOutput = true;
                } else { // ^C not found so all non-algebraic output:
                    outputPreformattedText(text.substring(start), DEFAULT_OUTPUT_CSS_CLASS);
                    return;
                }
            }
    }

    private boolean inMathOutput = false;
    private StringBuilder mathOutputSB = new StringBuilder();
    private static final Pattern TIMES_PATTERN = Pattern.compile("\\\\\\*");
    private static final Pattern LR_PATTERN = Pattern.compile("\\\\(left|right)");
    // rrprint could output a non-standard macro, so \symb{182} -> \partial, etc:
    // This mapping corresponds to the Microsoft Windows Symbol font.
    private static final Pattern SYMB_PATTERN = Pattern.compile("\\\\symb\\{(\\d+)}");
    private static final Map<String, String> SYMB_MAP = new HashMap<>();

    static {
//        SYMB_MAP.put("32", "\\\\ ");
        SYMB_MAP.put("34", "\\\\forall "); // redlog/rl/rlprint.red
        SYMB_MAP.put("36", "\\\\exists "); // redlog/rl/rlprint.red
        SYMB_MAP.put("38", "\\\\&"); // redlog/ibalp/ibalp.red
//        SYMB_MAP.put("124", "|");
//        SYMB_MAP.put("182", "\\\\partial ");
        SYMB_MAP.put("198", "\\\\emptyset "); // redlog
//        SYMB_MAP.put("216", "\\\\neg ");
        SYMB_MAP.put("217", "\\\\wedge "); // excalc/wedge.red
        SYMB_MAP.put("219", "\\\\,\\\\longleftrightarrow\\\\,"); // redlog/rl/rlprint.red
        SYMB_MAP.put("220", "\\\\,\\\\longleftarrow\\\\,"); // redlog/rl/rlprint.red
        SYMB_MAP.put("222", "\\\\,\\\\longrightarrow\\\\,"); // redlog/rl/rlprint.red
    }

    private String reprocessedMathOutputString() {
        // By default, rrprint breaks lines at 80 characters,
        // and the resulting newlines break KaTeX rendering, so remove them:
        for (int i = mathOutputSB.length() - 1; i > 0; i--)
            if (mathOutputSB.charAt(i) == '\n') mathOutputSB.deleteCharAt(i);

        // Trailing discretionary times \* -> \times, otherwise \* -> \,:
        int len = mathOutputSB.length();
        if (len >= 2 && mathOutputSB.substring(len - 2).equals("\\*"))
            mathOutputSB.replace(len - 2, len, "\\times");
        Matcher matcher = TIMES_PATTERN.matcher(mathOutputSB);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) matcher.appendReplacement(sb, "\\\\,");
        matcher.appendTail(sb);
        mathOutputSB = sb;

        // Ensure \left and \right control words match:
        int start = 0, leftMinusRightCount = 0;
        matcher = LR_PATTERN.matcher(mathOutputSB);
        while (matcher.find(start)) {
            switch (matcher.group(1)) {
                case "left":
                    leftMinusRightCount++;
                    break;
                case "right":
                    leftMinusRightCount--;
                    break;
            }
            start = matcher.end();
        }
        if (leftMinusRightCount > 0)
            mathOutputSB.append("\\right.".repeat(leftMinusRightCount));
        else if (leftMinusRightCount < 0)
            mathOutputSB.insert(0, "\\left.".repeat(-leftMinusRightCount));

        return SYMB_PATTERN.matcher(mathOutputSB).replaceAll(matchResult -> {
            String result = SYMB_MAP.get(matchResult.group(1));
            return result != null ? result : "\\\\symb\\\\{" + matchResult.group(1) + "\\\\}";
        });
    }

    /**
     * This class represents the options passed to katex.render.
     * It *must* be entirely public and *must* be instantiated.
     */
    public class KaTeXOptions {
        public final boolean throwOnError = false;
        public final boolean displayMode = true;
        public final double minRuleThickness = 0.1;
        //        public static final String output = "html";
        // Default is output = htmlAndMathml: Outputs HTML for visual rendering and includes MathML for accessibility.
        // The MathML includes the TeX input as annotation, which is what I currently output in the session log.
//        public JSObject macros;
//
//        KaTeXOptions() {
//            macros = katexMacros; // a JS object constructed in outputWebViewAvailable()
//        }
    }

    private void outputTypesetMaths(String text, String cssClass) {
        // rrprint delimits LaTeX output with ^P (DLE, 0x10) before and ^Q (DC1, 0x11) after
        // (always at the start/finish of a the end).
        int textLength = text.length(), start = 0, finish;
        while (start < textLength)
            if (inMathOutput) { // in maths; look for end of maths, ^Q:
                if ((finish = text.indexOf('\u0011', start)) != -1) { // ^Q found
                    // Finish current maths output:
                    mathOutputSB.append(text, start, finish);
                    HTMLElement mathOutputElement = (HTMLElement) doc.createElement("div");
                    if (cssClass != null) mathOutputElement.setClassName(cssClass);
                    body.appendChild(mathOutputElement);
                    // Call katex.render with a TeX expression and a DOM element to render into, e.g.
                    // katex.render("c = \\pm\\sqrt{a^2 + b^2}", element, {throwOnError: false});
//                    webEngine.executeScript(
//                            "katex.render(String.raw`" + reprocessedMathOutputString() + "`, " +
//                                    "document.getElementsByTagName('div')[" + divIndex++ + "], " +
//                                    "{throwOnError: false, displayMode: true, minRuleThickness: 0.1, output: 'html'});");
                    katex.call("render", reprocessedMathOutputString(), mathOutputElement, new KaTeXOptions());
                    mathOutputSB.setLength(0);
                    inMathOutput = false;
                    // Skip ^Q and one following newline:
                    start = finish + 2;
                } else { // ^Q not found so all maths
                    mathOutputSB.append(text, start, textLength);
                    return;
                }
            } else { // Not in maths so create a non-maths element:
                // Look for start of maths, ^P:
                if ((finish = text.indexOf('\u0010', start)) != -1) { // ^P found
                    // Output current non-maths element:
                    if (start < finish) outputPreformattedText(text.substring(start, finish), DEFAULT_OUTPUT_CSS_CLASS);
                    start = finish + 1; // skip ^P
                    // Start new maths output:
                    inMathOutput = true;
                } else { // ^P not found so all non-maths:
                    outputPreformattedText(text.substring(start), DEFAULT_OUTPUT_CSS_CLASS);
                    return;
                }
            }
    }

    /**
     * Append prompt text to the WebView control with the specified CSS class.
     */
    private void outputPromptText(String text, String cssClass) {
        // REDUCE prompt + input elements should look like this:
        // <pre class=inputCSSClass><span class="prompt">Prompt</span>REDUCE input</pre>
        inputPre = (HTMLElement) doc.createElement("pre");
        if (cssClass != null) inputPre.setClassName(cssClass);
        body.appendChild(inputPre);
        HTMLElement span = (HTMLElement) doc.createElement("span");
        span.setClassName("prompt");
        span.setTextContent(text);
        inputPre.appendChild(span);
    }

    private static final Pattern
            questionAnswer = Pattern.compile("[yYnN]\n"),
            promptPattern = Pattern.compile("(?:\\d+([:*]) )|.*\\?.*");

    /**
     * This method is run in the JavaFX Application Thread to process
     * a batch of output from the REDUCEOutputThread for display.
     */
    private void processOutput(AtomicReference<String> textAtomicReferenceString) {
        String text = textAtomicReferenceString.get();
        // Strip any echoed "y/Y/n/N" after responding to a user query:
        if (questionPrompt && questionAnswer.matcher(text).lookingAt()) text = text.substring(2);
        int promptIndex = text.lastIndexOf("\n") + 1; // start index of a possible prompt line
        Matcher promptMatcher;
        String promptString = null;
        questionPrompt = false;
        if ((promptMatcher = promptPattern.matcher(text.substring(promptIndex))).matches()) {
            promptString = promptMatcher.group();
            questionPrompt = promptMatcher.group(1) == null;
        }
        // Handle stealth input after REDUCE has started due to menu actions:
        if (hideNextOutputAndPrompt) {
            // Set above in setColouredIO() and setTypesetMaths().
            // Hide output up to and including the prompt after the
            // next batch of output (which is preceded by a newline):
            if (promptString != null && promptIndex > 0)
                hideNextOutputAndPrompt = false;
            return;
        }
        // Handle stealth input when REDUCE starts due to stored preferences:
        if (hideNextOutputShowPrompt) {
            // Set below if beforeFirstPrompt.
            // Hide output up to but excluding the newline before the prompt
            // after the next batch of output (which is preceded by a newline):
            if (promptString != null && promptIndex > 0) {
                text = text.substring(promptIndex - 1);
                promptIndex = 1;
                hideNextOutputShowPrompt = false;
            } else return;
        }

        if (beforeFirstPrompt) {
            // Do things relating to the first prompt here...
            if (promptString != null) {
                // Text ends with the first prompt...
                beforeFirstPrompt = false;
                // Reset enabled state of controls:
                reduceStarted();
                outputLabel.setText(outputLabelDefault + "  |  " + title);
                if (RRPreferences.displayPane == RRPreferences.DisplayPane.TABBED)
                    RunREDUCE.tabPane.getSelectionModel().getSelectedItem().setText(title);
                if (!colouredIOState && !typesetMathsState) {
                    outputHeaderText(text.substring(0, promptIndex));
                    outputPromptText(promptString, null);
                } else { // colouredIOState || typesetMathsState
                    outputHeaderText(text.substring(0, promptIndex - 1)); // without trailing newline
                    hideNextOutputShowPrompt = true;
                    sendStringToREDUCENoEcho("symbolic begin scalar !*msg,!*redefmsg,!*comp:=t;" +
                            inputRRprint() + "rrprint '" +
                            (typesetMathsState ? "fancy" : "coloured") + "!-output;" +
                            "crbuf!*:=nil;inputbuflis!*:=nil;statcounter:=0;end$\n");
                    rrprintLoaded = true;
                }
            } else {
                // Before the first prompt...
                outputHeaderText(text);
            }
            return;
        }

        String queryString = null;
        if (questionPrompt && RunREDUCE.runREDUCEFrame.popupQueriesCheckMenuItem.isSelected()) {
            // Split off REDUCE user query from preceding text:
            // Determining what the query should be is somewhat heuristic!
            if (promptString.endsWith("? (Y or N) ")) { // PSL
                queryString = promptString.substring(0, promptString.length() - 10); // Strip trailing " (Y or N) ".
            } else { // CSL: query string is followed by "\n\n?".
                text = text.substring(0, promptIndex).stripTrailing();
                promptIndex = text.lastIndexOf('\n') + 1;
                queryString = text.substring(promptIndex, text.length() - 9); // Strip trailing " (Y or N)".
            }
            if (promptIndex > 0) promptIndex--;
            text = text.substring(0, promptIndex);
            promptString = null;
        }

        if (colouredIOState) {
            // Coloured IO display:
            if (promptString != null) {
                outputText(text.substring(0, promptIndex), outputCSSClass);
                // Only colour output *after* initial REDUCE header.
                if (!questionPrompt) switch (promptMatcher.group(1)) {
                    case "*":
                        inputCSSClass = SYMBOLIC_INPUT_CSS_CLASS;
                        outputCSSClass = SYMBOLIC_OUTPUT_CSS_CLASS;
                        break;
                    case ":":
                    default:
                        inputCSSClass = ALGEBRAIC_INPUT_CSS_CLASS;
                        outputCSSClass = ALGEBRAIC_OUTPUT_CSS_CLASS;
                        break;
                }
                outputPromptText(promptString, inputCSSClass);
            } else
                outputText(text, outputCSSClass);
        } else {
            // No IO display colouring, but maybe bold prompt:
            inputCSSClass = null;
            if (promptString != null) {
                outputText(text.substring(0, promptIndex), null);
                outputPromptText(promptString, null);
            } else
                outputText(text, null);
        }
        scrollWebViewToBottom();

        if (queryString != null) {
            // Handle REDUCE user query as pop-up:
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    queryString, ButtonType.YES, ButtonType.NO);
            alert.setHeaderText("REDUCE User Query");
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            Optional<ButtonType> result = alert.showAndWait();
            sendStringToREDUCENoEcho(result.isPresent() &&
                    result.get() == ButtonType.YES ? "y\n" : "n\n");
        }
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
    private boolean runREDUCESubmenuDisabled;
    private boolean loadPackagesMenuItemDisabled;
    private boolean stopREDUCEMenuItemDisabled;
    private boolean restartREDUCEMenuItemDisabled;
    private boolean killREDUCEMenuItemDisabled;
    private boolean templatesMenuDisabled;
    private boolean functionsMenuDisabled;

    private static final RunREDUCEFrame FRAME = RunREDUCE.runREDUCEFrame;

    /**
     * Reset disabled status of controls as appropriate when REDUCE is not running.
     */
    void reduceStopped() {
        runningREDUCE = false;
        outputFileList.clear();
        startingOrStoppingREDUCE(false);
    }

    /**
     * Reset disabled status of controls as appropriate provided REDUCE has stopped running.
     */
    void reduceStoppedMaybe() {
        try {
            reduceProcess.waitFor(1, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
        if (!reduceProcess.isAlive()) reduceStopped();
    }

    /**
     * Reset disabled status of controls as appropriate when REDUCE has just started.
     */
    void reduceStarted() {
        runningREDUCE = true;
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
        FRAME.restartREDUCEMenuItem.setDisable(restartREDUCEMenuItemDisabled = !starting);
        FRAME.killREDUCEMenuItem.setDisable(killREDUCEMenuItemDisabled = !starting);
        FRAME.templatesMenu.setDisable(templatesMenuDisabled = !starting);
        FRAME.functionsMenu.setDisable(functionsMenuDisabled = !starting);
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
        // File menu items:
        FRAME.inputFileMenuItem.setDisable(inputFileMenuItemDisabled);
        FRAME.inputPackageFileMenuItem.setDisable(inputPackageFileMenuItemDisabled);
        FRAME.outputNewFileMenuItem.setDisable(outputNewFileMenuItemDisabled);
        FRAME.outputOpenFileMenuItem.setDisable(outputOpenFileMenuItemDisabled);
        FRAME.outputHereMenuItem.setDisable(outputHereMenuItemDisabled);
        FRAME.shutFileMenuItem.setDisable(shutFileMenuItemDisabled);
        FRAME.shutLastMenuItem.setDisable(shutLastMenuItemDisabled);
        // REDUCE menu items:
        FRAME.runREDUCESubmenu.setDisable(runREDUCESubmenuDisabled);
        FRAME.loadPackagesMenuItem.setDisable(loadPackagesMenuItemDisabled);
        FRAME.stopREDUCEMenuItem.setDisable(stopREDUCEMenuItemDisabled);
        FRAME.restartREDUCEMenuItem.setDisable(restartREDUCEMenuItemDisabled);
        FRAME.killREDUCEMenuItem.setDisable(killREDUCEMenuItemDisabled);
        // View menu items:
        FRAME.boldPromptsCheckMenuItem.setSelected(boldPromptsState);
        FRAME.colouredIOCheckMenuItem.setSelected(colouredIOState);
        FRAME.typesetMathsCheckMenuItem.setSelected(typesetMathsState);
        // Templates and Functions menus:
        FRAME.templatesMenu.setDisable(templatesMenuDisabled);
        FRAME.functionsMenu.setDisable(functionsMenuDisabled);
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
