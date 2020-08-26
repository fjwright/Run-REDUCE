package fjwright.runreduce;

import javafx.application.Platform;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private Label outputLabel;
    @FXML
    WebView outputWebView; // Accessed in RunREDUCEFrame.java
    @FXML
    private Label inputLabel;
    @FXML
    public TextArea inputTextArea; // Accessed in RunREDUCE & templates
    @FXML
    Button sendButton; // Accessed in RunREDUCEFrame.java
    @FXML
    private Button earlierButton, laterButton;

    private final WebEngine webEngine;
    private HTMLDocument doc;
    private HTMLElement html, head, inputPre;
    HTMLElement body;
    private JSObject katex;

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

    private boolean fmprintLoaded, hideNextOutput;
    private final List<String> stealthInputList = new ArrayList<>();

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

        outputWebView.setContextMenuEnabled(false);
        webEngine = outputWebView.getEngine();

        // See https://katex.org/docs/browser.html
        // KaTeX requires the use of the HTML5 doctype. Without it, KaTeX may not render properly.
        // See https://github.com/KaTeX/KaTeX/issues/1775 for a discussion about rules disappearing.
        webEngine.loadContent("<!DOCTYPE html><html><head>" +
                "<link rel='stylesheet' href='" + REDUCEPanel.class.getResource("katex/katex.min.css") + "'>" +
                "<script src='" + REDUCEPanel.class.getResource("katex/katex.min.js") + "'></script>" +
                "<style>pre{margin:0}</style>" +
//                "<style>.delimsizing.size4{font-size:200%}</style>" +
                "</head><body>" +
                "<p id='test'></p>" +
                "<script>katex.render('\\\\Biggl(\\\\frac{\\\\partial\\\\,f(x)}{\\\\partial\\\\,x}\\\\Biggr)^2', " +
                "document.getElementById('test'), " +
                "{throwOnError: false, displayMode: true, minRuleThickness: 0.1, output: 'html'});</script>" +
//                "<style>\n" +
//                "  .katex-version {display: none;}\n" +
//                "  .katex-version::after {content:\"0.10.2 or earlier\";}\n" +
//                "</style>\n" +
//                "<span class=\"katex\">\n" +
//                "  <span class=\"katex-mathml\">The KaTeX stylesheet is not loaded!</span>\n" +
//                "  <span class=\"katex-version rule\">KaTeX stylesheet version: </span>\n" +
//                "</span>" +
                "</body></html>");
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
                                "console.warn=function(message){bridge.log('JS WARN: '+message)}");
                        // End debugging support
                        outputWebViewAvailable();
                    }
                });
        webEngine.setOnError(System.err::println);

        // Note that a font name containing spaces needs quoting in CSS!
        inputTextArea.setStyle("-fx-font:" + RRPreferences.fontSize + " '" + RunREDUCE.reduceFontFamilyName + "'");
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

        // Create default style elements:

        HTMLElement fontFamilyStyle = (HTMLElement) doc.createElement("style");
        // Note that a font name containing spaces needs quoting in CSS!
        fontFamilyStyle.appendChild(doc.createTextNode(
                // Styling body doesn't work...
                String.format("pre{font-family:'%s','Courier New',Courier,monospace}", RunREDUCE.reduceFontFamilyName)));
        head.appendChild(fontFamilyStyle);

        // Default font size is 16px.
        fontSizeStyle = (HTMLElement) doc.createElement("style");
        fontSizeStyle.appendChild(doc.createTextNode(
                String.format("body{font-size:%dpx}", RRPreferences.fontSize)));
        head.appendChild(fontSizeStyle);

        promptWeightStyle = (HTMLElement) doc.createElement("style");
        promptWeightStyle.appendChild(doc.createTextNode(".prompt{font-weight:bold}"));
        if (RRPreferences.boldPromptsState) head.appendChild(promptWeightStyle);

        colorStyle = (HTMLElement) doc.createElement("style");
        colorStyle.appendChild(doc.createTextNode(
                ".algebraic-output{color:blue}.symbolic-output{color:#800080}" +
                        ".algebraic-input{color:red}.symbolic-input{color:#800000}"));
        if (RRPreferences.colouredIOState != RRPreferences.ColouredIO.NONE) head.appendChild(colorStyle);

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
        webEngine.executeScript("console.log('<p> font size: ' + " +
                "getComputedStyle(document.getElementsByTagName('p')[0], null).getPropertyValue('font-size'));" +
                "console.log('<p> font family: ' + " +
                "getComputedStyle(document.getElementsByTagName('p')[0], null).getPropertyValue('font-family'));" +
                "console.log('( font size: ' + " +
                "getComputedStyle(document.getElementsByClassName('size4')[0], null).getPropertyValue('font-size'));" +
                "console.log('( font family: ' + " +
                "getComputedStyle(document.getElementsByClassName('size4')[0], null).getPropertyValue('font-family'));");
    }

    void clearDisplay() {
        HTMLElement newBody = (HTMLElement) doc.createElement("body");
        html.replaceChild(newBody, body);
        body = newBody;
    }

    void updateFontSize(int newFontSize) {
        fontSizeStyle.getFirstChild().setNodeValue(String.format("body{font-size:%dpx}", newFontSize));
    }

    void setBoldPrompts(boolean enabled) {
        if (enabled) head.appendChild(promptWeightStyle);
        else head.removeChild(promptWeightStyle);
    }

    void setColouredIO(boolean enabled) {
        if (enabled) head.appendChild(colorStyle);
        else head.removeChild(colorStyle);
    }

    /**
     * Send input to REDUCE but hide any evidence from the I/O display.
     */
    private void stealthInput(String input) {
        // Should probably also reset crbuf!* and inputbuflis!*;
        // see the bottom of packages/redfront/redfront.red.
        hideNextOutput = true;
        sendStringToREDUCENoEcho(
                String.format("symbolic<<%s;statcounter:=statcounter-1;>>$\n", input));
    }

    /**
     * Control use of typeset I/O *provided* REDUCE is running.
     */
    void setTypesetMaths(boolean enabled) {
        if (runningREDUCE) {
            if (enabled) {
                if (fmprintLoaded) {
                    stealthInput("on fancy");
                } else {
                    stealthInput("load_package fmprint");
                    fmprintLoaded = true;
                }
            } else {
                stealthInput("off fancy");
            }
        } else {
            stealthInputList.add("load_package fmprint");
            fmprintLoaded = true;
        }
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
    private void scrollWebViewToBottom(boolean output) { // FixMe Redundant parameter!
        webEngine.executeScript("setTimeout(function(){document.body.scrollIntoView(false)}, 200);");
    }

    /*
     * REDUCE prompt + input elements should look like this:
     * <pre class=inputCSSClass><span class="prompt">Prompt</span>REDUCE input</pre>
     */
    void sendStringToREDUCEAndEcho(String text) {
        inputPre.appendChild(doc.createTextNode(text));
        // Make sure the new input text is visible:
        scrollWebViewToBottom(false);
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
        outputCSSClass = null; // for initial header
        // Special support for Redfront I/O colouring:
        RRPreferences.colouredIOState = RRPreferences.colouredIOIntent;
        beforeFirstPrompt = true;
        fmprintLoaded = false;

        if (RRPreferences.typesetMathsState) setTypesetMaths(true);

        String[] command = reduceCommand.buildCommand();
        if (command == null) return;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(RunREDUCE.reduceConfiguration.workingDir));
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
        if (RRPreferences.typesetMathsState) outputTypesetText(text, cssClass);
        else outputPlainText(text, cssClass);
    }

    private void outputPlainText(String text, String cssClass) {
        HTMLElement outputElement = (HTMLElement) doc.createElement("pre");
        outputElement.setTextContent(text);
        if (cssClass != null) outputElement.setClassName(cssClass);
        body.appendChild(outputElement);
    }

    private boolean inMathOutput = false;
    private final StringBuilder mathOutputSB = new StringBuilder();

    // FixMe Make the math output replacements table driven.
    private String reprocessedMathOutputString() {
        // By default, fmprint breaks lines at 80 characters,
        // and the resulting newlines break KaTeX rendering, so remove them:
        for (int i = mathOutputSB.length() - 1; i > 0; i--)
            if (mathOutputSB.charAt(i) == '\n') mathOutputSB.deleteCharAt(i);
        // fmprint outputs a non-standard macro, so \symb{182} -> \partial:
        int start = 0;
        String searchString = "\\symb{182}", replaceString = "\\partial";
        while ((start = mathOutputSB.indexOf(searchString, start)) != -1) {
            mathOutputSB.replace(start, start + searchString.length(), replaceString);
            start += replaceString.length();
        }
        return mathOutputSB.toString();
    }

    /**
     * This class represents the options passed to katex.render.
     * It *must* be entirely public and *must* be instantiated.
     */
    public static class KaTeXOptions {
        public static final boolean throwOnError = false;
        public static final boolean displayMode = true;
        public static final double minRuleThickness = 0.1; // Unnecessary for MathML output
        //        public static final String output = "html"; // No TeX or MathML annotation
//        public static final String output = "mathml";
        // MathML output gives better big delims but needs more space between terms
        // and more vertical space between math output regions.
    }

    private void outputTypesetText(String text, String cssClass) {
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

        if (beforeFirstPrompt) {
            if (!stealthInputList.isEmpty()) {
                for (var input : stealthInputList) stealthInput(input);
                stealthInputList.clear();
            }
            beforeFirstPrompt = false;
        }
    }

    private static final Pattern promptPattern = Pattern.compile("(?:\\d+([:*]) )|\\?");

    /**
     * This method is run in the JavaFX Application Thread to process
     * a batch of output from the REDUCEOutputThread for display.
     */
    private void processOutput(AtomicReference<String> textAtomicReferenceString) {
        // This test could probably be earlier in REDUCEOutputThread.call().
        if (hideNextOutput) {
            hideNextOutput = false;
            return;
        }
        String text = textAtomicReferenceString.get();
        int promptIndex; // possible start index of a prompt
        String promptString = null;
        Matcher promptMatcher = null;
        boolean promptFound = false;
        // If text ends with a prompt then promptMatcher.matches() is true.
        // Set promptString and set questionPrompt to true/false to indicate a question prompt.
        // Split off the final line, which should consist of the next input prompt:
        promptIndex = text.lastIndexOf("\n") + 1;
        if (promptIndex < text.length() &&
                (promptMatcher = promptPattern.matcher(promptString = text.substring(promptIndex))).matches()) {
            promptFound = true;
            questionPrompt = promptMatcher.group(1) == null;
        }
        if (beforeFirstPrompt) {
            // Do things relating to the first prompt here...
            if (promptFound) {
                // Text ends with the first prompt...
//                outputHeaderText(text.substring(0, promptIndex));
//                promptText(promptString, null);
//                beforeFirstPrompt = false;
            } else {
                // Before the first prompt...
                outputHeaderText(text);
                return;
            }
        }

        switch (RRPreferences.colouredIOState) {
            case NONE:
            default: // no IO display colouring, but maybe prompt processing
                inputCSSClass = null;
                if (promptFound) {
                    outputText(text.substring(0, promptIndex), null);
                    outputPromptText(promptString, null);
                } else
                    outputText(text, null);
                break;

            case MODAL: // mode coloured IO display processing
                if (promptFound) {
                    if (0 < promptIndex)
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
        } // end of switch (RunREDUCEPrefs.colouredIOState)

        scrollWebViewToBottom(true);
    }

    private static final Pattern PATTERN = Pattern.compile("\n1:"); // works better than "\n1: "

    private void processPromptMarkers(String text, int start) {
        // Delete the very first prompt. (This code may not be reliable!)
        Matcher matcher;
        if (beforeFirstPrompt && (matcher = PATTERN.matcher(text)).find(start)) {
            beforeFirstPrompt = false;
            text = matcher.replaceFirst("");
        }
        // Look for prompt markers:
        int promptStartMarker = text.indexOf("\u0001", start);
        int promptEndMarker = text.indexOf("\u0002", start);
        if (promptStartMarker >= 0 && promptEndMarker >= 0) {
            if (start < promptStartMarker)
                outputText(text.substring(start, promptStartMarker), outputCSSClass);
            String promptString = text.substring(promptStartMarker + 1, promptEndMarker);
            questionPrompt = promptString.equals("?");
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
