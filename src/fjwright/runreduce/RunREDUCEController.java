package fjwright.runreduce;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

/*
 * This class controls the RunREDUCE menu bar.
 * Note that the current directory for REDUCE is the directory from
 * which this GUI was run, so filenames must be relative to that
 * directory or absolute; I currently use the latter.
 */
public class RunREDUCEController implements Initializable {

    public Menu helpMenu;

    /**
     * Called to initialize a controller after its root element has been
     * completely processed.
     *
     * @param location  The location used to resolve relative paths for the root object, or
     *                  <tt>null</tt> if the location is not known.
     * @param resources The resources used to localize the root object, or <tt>null</tt> if
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Dynamically finish building menus.

        /* ********* *
         * Help menu *
         * ********* */

        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            int helpMenuItemIndex = 0;

            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                MenuItem userGuideMenuItem = new MenuItem("Run-REDUCE User Guide (HTML)");
                helpMenu.getItems().add(helpMenuItemIndex++, userGuideMenuItem);
//                userGuideMenuItem.setToolTipText("Open the Run-REDUCE User Guide in the default web browser.");
                userGuideMenuItem.setOnAction(e ->
                {
                    try {
                        desktop.browse(new URI("https://fjwright.github.io/Run-REDUCE/UserGuide.html"));
                    } catch (IOException | URISyntaxException ex) {
                        ex.printStackTrace();
                    }
                });
            }
            helpMenu.getItems().add(helpMenuItemIndex++, new SeparatorMenuItem());

            String[][] manuals = {
                    {"REDUCE Manual (HTML)", "lib/csl/reduce.doc/manual.html", "manual.html"},
                    {"REDUCE Manual (PDF)", "lib/csl/reduce.doc/manual.pdf", "manual.pdf.gz"},
                    {"Inside Reduce (PDF)", "doc/insidereduce.pdf", "insidereduce.pdf.gz"},
                    {"REDUCE Symbolic Mode Primer (PDF)", "doc/primer.pdf", "primer.pdf.gz"},
                    {"Standard Lisp Report (PDF)", "doc/sl.pdf", "sl.pdf.gz"}
            };

            if (desktop.isSupported(Desktop.Action.OPEN)) {
                for (String[] manual : manuals) {
                    MenuItem menuItem = new MenuItem(manual[0]);
                    helpMenu.getItems().add(helpMenuItemIndex++, menuItem);
//                    menuItem.setToolTipText("Open this manual in the default application.");
                    menuItem.setOnAction(e ->
                    {
                        try { // FixMe
//                            desktop.open(RRPreferences.windowsOS ?
//                                    // ToDo Make the directory used below configurable?
//                                    new File(RunREDUCE.reduceConfiguration.packagesRootDir, manual[1]) :
//                                    new File("/usr/share/doc/reduce", manual[2]));
                            desktop.open(new File("D:/Program Files/Reduce", manual[1]));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    });
                }
            }
            helpMenu.getItems().add(helpMenuItemIndex, new SeparatorMenuItem());
        }

    }

    /* ********* *
     * File menu *
     * ********* */

    public void inputFileMenuItemAction(ActionEvent actionEvent) {
    }

    public void outputFileMenuItemAction(ActionEvent actionEvent) {
    }

    public void outputHereMenuItemAction(ActionEvent actionEvent) {
    }

    public void shutFileMenuItemAction(ActionEvent actionEvent) {
    }

    public void shutLastMenuItemAction(ActionEvent actionEvent) {
    }

    public void loadPackagesMenuItemAction(ActionEvent actionEvent) {
    }

    public void saveLogMenuItemAction(ActionEvent actionEvent) {
    }

    public void exitMenuItemAction(ActionEvent actionEvent) {
    }

    /* *********** *
     * REDUCE menu *
     * *********** */

    public void stopREDUCEMenuItemAction(ActionEvent actionEvent) {
    }

    public void clearDisplayMenuItemAction(ActionEvent actionEvent) {
    }

    public void configureREDUCEMenuItemAction(ActionEvent actionEvent) {
    }

    /* ********* *
     * View menu *
     * ********* */

    public void javaLFRadioButtonMenuItemAction(ActionEvent actionEvent) {
    }

    public void nativeLFRadioButtonMenuItemAction(ActionEvent actionEvent) {
    }

    public void motifLFRadioButtonMenuItemAction(ActionEvent actionEvent) {
    }

    public void fontSizeMenuItemAction(ActionEvent actionEvent) {
    }

    public void boldPromptsCheckBoxAction(ActionEvent actionEvent) {
    }

    public void noColouredIORadioButtonAction(ActionEvent actionEvent) {
    }

    public void modeColouredIORadioButtonAction(ActionEvent actionEvent) {
    }

    public void redfrontColouredIORadioButtonAction(ActionEvent actionEvent) {
    }

    public void singlePaneRadioButtonAction(ActionEvent actionEvent) {
    }

    public void splitPaneRadioButtonAction(ActionEvent actionEvent) {
    }

    public void tabbedPaneRadioButtonAction(ActionEvent actionEvent) {
    }

    public void addTabMenuItemAction(ActionEvent actionEvent) {
    }

    public void removeTabMenuItemAction(ActionEvent actionEvent) {
    }

    /* ********* *
     * Help menu *
     * ********* */

    public void aboutMenuItemAction(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Prototype version 0.1\n" +
                        "\u00A9 Francis Wright, April 2020");
        alert.setTitle("About Run-REDUCE");
        alert.setHeaderText("Run REDUCE in a JavaFX GUI");
        alert.showAndWait();
    }

}
