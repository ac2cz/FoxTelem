package macos;

import gui.HelpAbout;
import gui.MainWindow;

import com.apple.eawt.AboutHandler;
import com.apple.eawt.AppEvent.AboutEvent;

/**
 * 
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2015 amsat.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Support the Mac specific way to show the Help About menu
 * 
 *
 */
public class MacAboutHandler implements AboutHandler  {

	@Override
	public void handleAbout(AboutEvent e) {
		HelpAbout help = new HelpAbout(MainWindow.frame, true);
		help.setVisible(true);
	}

}
