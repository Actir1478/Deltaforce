module pe.edu.upeu.coolbox {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires static lombok;
    requires jakarta.validation;

    opens pe.edu.upeu.coolbox to javafx.fxml;
    opens pe.edu.upeu.coolbox.controller to javafx.fxml;
    opens pe.edu.upeu.coolbox.model;
    exports pe.edu.upeu.coolbox;
}