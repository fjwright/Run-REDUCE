module Run.REDUCE.FX {
    requires java.desktop;
    requires java.prefs;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires jdk.xml.dom;
    requires jdk.jsobject;
    opens fjwright.runreduce to javafx.fxml;
    opens fjwright.runreduce.templates to javafx.fxml;
    opens fjwright.runreduce.functions to javafx.fxml;
    exports fjwright.runreduce;
}
