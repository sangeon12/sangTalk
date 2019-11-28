package util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class Util {
	public static void showAlert(String title, String msg, AlertType type) {
		Alert a = new Alert(type);
		a.setTitle(title);
		a.setHeaderText(null);
		a.setContentText(msg);
		
		a.show();
	}
}
