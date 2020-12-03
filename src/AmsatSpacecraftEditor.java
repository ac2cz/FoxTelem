import java.io.File;

import common.Config;
import common.Log;

public class AmsatSpacecraftEditor {

	static String logFileDir = null;
	
	public static void main(String[] args) {
		if (logFileDir == null)
			Config.homeDirectory = System.getProperty("user.home") + File.separator + ".FoxTelem";
		else
			Config.homeDirectory = logFileDir;

		if (Config.missing()) {
			// Then this is the first time we have run FoxTelem on this computer
			Config.setHome();
//			if (logFileDir == null)
//				m.initialRun();
		}
		
		Log.init("AmsatEditor");
		
		Config.currentDir = System.getProperty("user.dir"); //m.getCurrentDir(); 
		
		Config.minInit(logFileDir);
		
		SpacecraftEditorWindow editor = new SpacecraftEditorWindow();
		editor.setVisible(true);
	}
}
