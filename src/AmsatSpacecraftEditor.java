import java.awt.Toolkit;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.UIManager;

import common.Config;
import common.Log;
import spacecraftEditor.InitalEditorSettings;
import spacecraftEditor.SpacecraftEditorWindow;

public class AmsatSpacecraftEditor {

	static String logFileDir = null;	
	
	public static void main(String[] args) {
		int arg = 0;
		while (arg < args.length) {
			if (args[arg].startsWith("-")) { // this is a switch			
				if ((args[arg].equalsIgnoreCase("-v")) || (args[arg].equalsIgnoreCase("-version"))) {
					System.out.println("AMSAT Spacecraft Editor. Version " + SpacecraftEditorWindow.VERSION);
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

		try { //to set the look and feel to be the same as the platform it is run on
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// Silently fail if we can't do this and go with default look and feel
			//e.printStackTrace();
		}
		
		AmsatSpacecraftEditor m = new AmsatSpacecraftEditor();
		if (Config.setup("AmsatEditor.properties")) {
			// Then this is the first time we have run the editor on this computer
			Config.setHome();
			if (logFileDir == null)
				m.initialRun();
		}
		
		Log.init("AmsatEditor");
		
		Config.minInit(logFileDir);
		Config.currentDir = Config.editorCurrentDir;
		Config.initSatelliteManager();
		
		
		SpacecraftEditorWindow editor = new SpacecraftEditorWindow();
		editor.setVisible(true);
	}
	
	public void initialRun() {
		
				try { //to set the look and feel to be the same as the platform it is run on
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					// Silently fail if we can't do this and go with default look and feel
					//e.printStackTrace();
				}

				try {
					JFrame window = new JFrame();
					window.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("images/fox.jpg")));
					//window.setVisible(true);

					// INITIAL RUN
					InitalEditorSettings f = new InitalEditorSettings(window, true);
					f.setVisible(true);

				} catch (Exception e) {
					Log.println("SERIOUS ERROR - Uncaught and thrown from Initial Setup");
					e.printStackTrace();
					e.printStackTrace(Log.getWriter());
					Log.errorDialog("SERIOUS ERROR - Uncaught and thrown from Initial Setup", e.getMessage());
					
				}
	}
}
