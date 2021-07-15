import java.io.File;

import common.Config;
import common.Log;
import spacecraftEditor.SpacecraftEditorWindow;

public class AmsatSpacecraftEditor {

	static String logFileDir = null;
	public static final String VERSION = "0.1";
	
	public static void main(String[] args) {
		int arg = 0;
		while (arg < args.length) {
			if (args[arg].startsWith("-")) { // this is a switch			
				if ((args[arg].equalsIgnoreCase("-v")) || (args[arg].equalsIgnoreCase("-version"))) {
					System.out.println("AMSAT Spacecraft Editor. Version " + VERSION);
					System.exit(0);
				}
			} else {
				// we have no more switches, so start reading command line paramaters
				Log.println("Command Line Param LogFileDir: " + args[arg]);
				logFileDir = args[arg];
			}
			arg++;
		}
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
		Config.initSatelliteManager();
		
		SpacecraftEditorWindow editor = new SpacecraftEditorWindow();
		editor.setVisible(true);
	}
}
