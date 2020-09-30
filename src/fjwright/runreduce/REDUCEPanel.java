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
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.w3c.dom.html.HTMLDocument;
import org.w3c.dom.html.HTMLElement;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private ToggleButton hideEditorToggleButton;
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
    private boolean boldPromptsState, typesetMathsState;
    private RRPreferences.ColouredIO colouredIOState;
    private double[] dividerPositions;

    final WebEngine webEngine;
    private HTMLDocument doc;
    private HTMLElement html, head, inputPre;
    HTMLElement body;
    private JSObject katex, katexMacros;

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

    private static final String ALGEBRAIC_OUTPUT_CSS_CLASS = "algebraic-output";
    private static final String SYMBOLIC_OUTPUT_CSS_CLASS = "symbolic-output";
    private static final String ALGEBRAIC_INPUT_CSS_CLASS = "algebraic-input";
    private static final String SYMBOLIC_INPUT_CSS_CLASS = "symbolic-input";
    private String inputCSSClass;
    private String outputCSSClass;
    private HTMLElement fontSizeStyle;
    private HTMLElement promptWeightStyle;
    private HTMLElement colorStyle;

    private boolean fmprintLoaded, hideNextOutputAndPrompt, hideNextOutputShowPrompt;

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

        outputWebView.setContextMenuEnabled(false);
        webEngine = outputWebView.getEngine();

        // See https://katex.org/docs/browser.html
        // KaTeX requires the use of the HTML5 doctype. Without it, KaTeX may not render properly.
        // See https://github.com/KaTeX/KaTeX/issues/1775 for a discussion about rules disappearing.
        webEngine.loadContent("<!DOCTYPE html><html><head>" +
                "<link rel='stylesheet' href='" + REDUCEPanel.class.getResource("katex/katex.min.css") + "'>" +
                "<script src='" + REDUCEPanel.class.getResource("katex/katex.min.js") + "'></script>" +
                "<style>pre{margin:0}</style>" +
                "</head><body></body></html>");
        webEngine.getLoadWorker().stateProperty().addListener(
                (ov, oldState, newState) -> {
                    if (newState == State.SUCCEEDED) {
                        // Begin debugging support
                        JSObject window = (JSObject) webEngine.executeScript("window");
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
            } else {
                splitPane.getItems().set(0, ioDisplayPane);
                splitPane.setDividerPositions(dividerPositions);
                this.setCenter(splitPane);
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
        fontSizeStyle.appendChild(doc.createTextNode(
                String.format("body{font-size:%dpx}", fontSize)));
        head.appendChild(fontSizeStyle);

        promptWeightStyle = (HTMLElement) doc.createElement("style");
        promptWeightStyle.appendChild(doc.createTextNode(".prompt{font-weight:bold}"));
        if (boldPromptsState) head.appendChild(promptWeightStyle);

        colorStyle = (HTMLElement) doc.createElement("style");
        colorStyle.appendChild(doc.createTextNode(
                ".algebraic-output{color:blue}.symbolic-output{color:#800080}" +
                        ".algebraic-input{color:red}.symbolic-input{color:#800000}"));
        if (colouredIOState != RRPreferences.ColouredIO.NONE) head.appendChild(colorStyle);

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

    void clearDisplay() {
        HTMLElement newBody = (HTMLElement) doc.createElement("body");
        html.replaceChild(newBody, body);
        body = newBody;
    }

    void updateFontSize(int newFontSize) {
        fontSize = newFontSize;
        fontSizeStyle.getFirstChild().setNodeValue(String.format("body{font-size:%dpx}", newFontSize));
    }

    /**
     * Called in RRPreferences.save (only) to control bold prompts.
     */
    void setBoldPrompts(boolean enabled) {
        boldPromptsState = enabled;
        if (enabled) head.appendChild(promptWeightStyle);
        else head.removeChild(promptWeightStyle);
    }

    private boolean redfrontLoaded;

    /**
     * Called in RRPreferences.save (only) to control coloured I/O.
     */
    void setColouredIO() {
        colouredIOState = RRPreferences.colouredIOState;
        if (colouredIOState != RRPreferences.ColouredIO.NONE) head.appendChild(colorStyle);
        else head.removeChild(colorStyle);
        if (colouredIOState == RRPreferences.ColouredIO.REDFRONT && runningREDUCE && !redfrontLoaded) {
            // Hide the interaction with REDUCE:
            hideNextOutputAndPrompt = true;
            sendStringToREDUCENoEcho(
                    // See the bottom of packages/redfront/redfront.red.
                    // Done this way because CSL does not allow statcounter to be rebound!
                    "symbolic begin scalar c:=crbuf!*,i:=inputbuflis!*,s:=statcounter;" +
                            "load_package redfront;crbuf!*:=cdr c;inputbuflis!*:=cdr i;statcounter:=s-1;end$\n");
            redfrontLoaded = true;
        }
    }

    /**
     * Called in RRPreferences.save (only) to control typeset I/O.
     */
    void setTypesetMaths(boolean enabled) {
        typesetMathsState = enabled;
        if (runningREDUCE) {
            if (enabled) {
                if (fmprintLoaded) {
                    stealthInput("on fancy");
                } else {
//                    stealthInput("load_package fmprint");
                    hideNextOutputAndPrompt = true;
                    sendStringToREDUCENoEcho("symbolic begin scalar !*msg,!*redefmsg,!*comp:=t;" +
                            inputRRprint() +
                            "crbuf!*:=cdr crbuf!*;inputbuflis!*:=cdr inputbuflis!*;" +
                            "statcounter:=statcounter-1;end$\n");
                    fmprintLoaded = true;
                }
            } else {
                stealthInput("off fancy");
            }
        }
    }

    private static String inputRRprint;

    /**
     * Return essentially the string
     *   out "/dev/null"; in "../rrprint.red"; shut "/dev/null";
     * in a way that works on Windows and non-Windows platforms.
     * If running from a jar file then copy the resource to a real temporary file first.
     */
    private String inputRRprint() {
        if (inputRRprint != null) return inputRRprint;
        String rrprintFilename = "rrprint.red";
        URL url = RunREDUCEFrame.class.getResource(rrprintFilename);
        if (url == null) {
            RunREDUCE.errorMessageDialog("Typeset Maths",
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

    private void sendAction(boolean isShiftDown) {
        String text = inputTextArea.getText();
        if (text.length() > 0) {
            inputList.add(text);
            text = PopupKeyboard.decode(text);
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
        webEngine.executeScript("setTimeout(function(){document.body.scrollIntoView(false)},200);");
    }

    /*
     * REDUCE prompt + input elements should look like this:
     * <pre class=inputCSSClass><span class="prompt">Prompt</span>REDUCE input</pre>
     */
    void sendStringToREDUCEAndEcho(String text) {
        inputPre.appendChild(doc.createTextNode(text));
        // Make sure the new input text is visible:
        scrollWebViewToBottom();
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

    /**
     * Run the specified REDUCE command in this REDUCE panel.
     */
    void run(REDUCECommand reduceCommand) {
        outputCSSClass = null; // for initial header
        beforeFirstPrompt = true;
        fmprintLoaded = false;
        redfrontLoaded = false;

        String[] command = reduceCommand.buildCommand();
        if (command == null) return;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(RunREDUCE.reduceConfiguration.workingDir));
            // pb.redirectErrorStream(true);
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
        if (typesetMathsState) {
            if (RunREDUCE.runREDUCEFrame.showTeXMarkupCheckMenuItem.isSelected()) {
                outputPlainText(text, cssClass);
            }
            outputTypesetMaths(text, cssClass);
        } else outputPlainText(text, cssClass);
    }

    private void outputPlainText(String text, String cssClass) {
        HTMLElement outputElement = (HTMLElement) doc.createElement("pre");
        outputElement.setTextContent(text);
        if (cssClass != null) outputElement.setClassName(cssClass);
        body.appendChild(outputElement);
    }

    private boolean inMathOutput = false;
    private StringBuilder mathOutputSB = new StringBuilder();
    private static final Pattern TIMES_PATTERN = Pattern.compile("\\\\\\*");
    private static final Pattern LR_PATTERN = Pattern.compile("\\\\(left|right)");
    // fmprint outputs a non-standard macro, so \symb{182} -> \partial, etc:
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
        // By default, fmprint breaks lines at 80 characters,
        // and the resulting newlines break KaTeX rendering, so remove them:
        for (int i = mathOutputSB.length() - 1; i > 0; i--)
            if (mathOutputSB.charAt(i) == '\n') mathOutputSB.deleteCharAt(i);

        // Trailing discretionary times (\*) -> \times, otherwise delete:
        int len = mathOutputSB.length();
        if (len >= 2 && mathOutputSB.substring(len - 2).equals("\\*"))
            mathOutputSB.replace(len - 2, len, "\\times");
        Matcher matcher = TIMES_PATTERN.matcher(mathOutputSB);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) matcher.appendReplacement(sb, "");
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
        // fmprint delimits LaTeX output with ^P (DLE, 0x10) before and ^Q (DC1, 0x11) after
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
                    // Skip ^Q and following white space:
                    for (start = finish + 1; start < textLength; start++)
                        if (!Character.isWhitespace(text.charAt(start))) break;
                } else { // ^Q not found so all maths
                    mathOutputSB.append(text, start, textLength);
                    return;
                }
            } else { // Not in maths so create a non-maths element:
                // Look for start of maths, ^P:
                if ((finish = text.indexOf('\u0010', start)) != -1) { // ^P found
                    // Output current non-maths element:
                    if (start < finish) outputPlainText(text.substring(start, finish), cssClass);
                    start = finish + 1; // skip ^P
                    // Start new maths output:
                    inMathOutput = true;
                } else { // ^P not found so all non-maths:
                    outputPlainText(text.substring(start), cssClass);
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

    // Allow ^A prompt ^B in case redfront mode turned on then off.
    private static final Pattern promptPattern = Pattern.compile("\u0001?((?:\\d+([:*]) )|.*\\?.*)\u0002?");

    /**
     * This method is run in the JavaFX Application Thread to process
     * a batch of output from the REDUCEOutputThread for display.
     */
    private void processOutput(AtomicReference<String> textAtomicReferenceString) {
        String text = textAtomicReferenceString.get();
        int promptIndex; // possible start index of newline followed by a prompt line
        String promptString = null;
        Matcher promptMatcher = null;
        // If text ends with a prompt then promptMatcher.matches() is true.
        // Beginning of string then newline is crucial to detecting the prompt following subsequent stealth input.
        // Leading control (newline or ) then newline is crucial to detecting the prompt following initial stealth input.
        // But a question prompt may not be preceded by a newline.
        promptIndex = text.lastIndexOf("\n");
        if ((promptIndex <= 0 || Character.isISOControl(text.charAt(promptIndex - 1))) &&
//                promptIndex < text.length() && // Dont' see how this could be false!
                (promptMatcher = promptPattern.matcher(text.substring(++promptIndex))).matches()) {
            // Now promptIndex = actual start index of the prompt line.
            questionPrompt = promptMatcher.group(2) == null;
            if (promptIndex > 0 || questionPrompt) promptString = promptMatcher.group(1); // exclude ^A/^B
        }
        // Handle stealth input:
        if (hideNextOutputAndPrompt) {
            // Hide output up to and including the next prompt:
            if (promptString != null) hideNextOutputAndPrompt = false;
            return;
        }
        if (hideNextOutputShowPrompt) {
            // Hide output up to but excluding the newline before the next prompt:
            if (promptString != null) {
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
                if (colouredIOState != RRPreferences.ColouredIO.REDFRONT && !typesetMathsState) {
                    if (colouredIOState == RRPreferences.ColouredIO.NONE) {
                        outputHeaderText(text.substring(0, promptIndex));
                        outputPromptText(promptString, null);
                        return;
                    } // else MODAL so fall through to the general case code
                } else {
                    outputHeaderText(text.substring(0, promptIndex - 1)); // without trailing newline
                    if (colouredIOState == RRPreferences.ColouredIO.REDFRONT) {
                        sendStringToREDUCENoEcho("load_package redfront$\n");
                        redfrontLoaded = true;
                    }
                    if (typesetMathsState) {
                        hideNextOutputShowPrompt = true;
                        sendStringToREDUCENoEcho("symbolic begin scalar !*msg,!*redefmsg,!*comp:=t;" +
                                inputRRprint() +
                                "crbuf!*:=nil;inputbuflis!*:=nil;statcounter:=0;end$\n");
                        fmprintLoaded = true;
                    }
                    return;
                }
            } else {
                // Before the first prompt...
                outputHeaderText(text);
                return;
            }
        }

        switch (colouredIOState) {
            case NONE:
            default: // no IO display colouring, but maybe prompt processing
                inputCSSClass = null;
                if (promptString != null) {
                    outputText(text.substring(0, promptIndex), null);
                    outputPromptText(promptString, null);
                } else
                    outputText(text, null);
                break;

            case MODAL: // mode coloured IO display processing
                if (promptString != null) {
                    outputText(text.substring(0, promptIndex), outputCSSClass);
                    // Only colour output *after* initial REDUCE header.
                    if (!questionPrompt) switch (promptMatcher.group(2)) {
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
                inputCSSClass = ALGEBRAIC_INPUT_CSS_CLASS;
                int start = 0;
                for (; ; ) {
                    int algOutputStartMarker = text.indexOf("\u0003", start);
                    int algOutputEndMarker = text.indexOf("\u0004", start);
                    if (algOutputStartMarker >= 0 && algOutputEndMarker >= 0) {
                        if (algOutputStartMarker < algOutputEndMarker) {
                            // TEXT < algOutputStartMarker < TEXT < algOutputEndMarker
                            if (start < algOutputStartMarker)
                                outputText(text.substring(start, algOutputStartMarker), null);
                            outputText(text.substring(algOutputStartMarker + 1, algOutputEndMarker), ALGEBRAIC_OUTPUT_CSS_CLASS);
                            outputCSSClass = null;
                            start = algOutputEndMarker + 1;
                        } else {
                            // TEXT < algOutputEndMarker < TEXT < algOutputStartMarker
                            outputText(text.substring(start, algOutputEndMarker), ALGEBRAIC_OUTPUT_CSS_CLASS);
                            if (algOutputEndMarker + 1 < algOutputStartMarker)
                                outputText(text.substring(algOutputEndMarker + 1, algOutputStartMarker), null);
                            outputCSSClass = ALGEBRAIC_OUTPUT_CSS_CLASS;
                            start = algOutputStartMarker + 1;
                        }
                    } else if (algOutputStartMarker >= 0) {
                        // TEXT < algOutputStartMarker < TEXT
                        if (start < algOutputStartMarker)
                            outputText(text.substring(start, algOutputStartMarker), null);
                        outputText(text.substring(algOutputStartMarker + 1), ALGEBRAIC_OUTPUT_CSS_CLASS);
                        outputCSSClass = ALGEBRAIC_OUTPUT_CSS_CLASS;
                        break;
                    } else if (algOutputEndMarker >= 0) {
                        // TEXT < algOutputEndMarker < TEXT
                        outputText(text.substring(start, algOutputEndMarker), ALGEBRAIC_OUTPUT_CSS_CLASS);
                        outputCSSClass = null;
                        processPromptMarkers(text, algOutputEndMarker + 1);
                        break;
                    } else {
                        // No algebraic output markers
                        processPromptMarkers(text, start);
                        break;
                    }
                }
                break; // end of case RunREDUCEPrefs.REDFRONT
        } // end of switch (colouredIOState)

        scrollWebViewToBottom();
    }

    private void processPromptMarkers(String text, int start) {
        // FixMe Merge this more with the prompt handling code at the start of processOutput().
        // Look for prompt markers:
        int promptStartMarker = text.indexOf("\u0001", start);
        int promptEndMarker = text.indexOf("\u0002", start);
        if (promptStartMarker >= 0 && promptEndMarker >= 0) {
            if (start < promptStartMarker)
                outputText(text.substring(start, promptStartMarker), outputCSSClass);
            String promptString = text.substring(promptStartMarker + 1, promptEndMarker);
            outputPromptText(promptString, ALGEBRAIC_INPUT_CSS_CLASS);
        } else
            outputText(text.substring(start), outputCSSClass);
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
    private boolean templatesMenuDisabled;
    private boolean functionsMenuDisabled;

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
        FRAME.stopREDUCEMenuItem.setDisable(stopREDUCEMenuItemDisabled);
        FRAME.loadPackagesMenuItem.setDisable(loadPackagesMenuItemDisabled);
        // View menu items:
        FRAME.boldPromptsCheckBox.setSelected(boldPromptsState);
        FRAME.setSelectedColouredIORadioButton(colouredIOState);
        FRAME.typesetMathsCheckBox.setSelected(typesetMathsState);
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
