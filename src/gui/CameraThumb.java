package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

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
 * A camera thumbnail that we can display on the CameraTab.  It consists of a thumbnail image and a time stamp.
 * 
 */
@SuppressWarnings("serial")
public class CameraThumb extends JPanel {
	JLabel thumbImage;
	JLabel timestamp;
	JPanel top;
	JPanel bottom;
	
	public CameraThumb() {
		setLayout(new BorderLayout());
		top = new JPanel();
		add(top, BorderLayout.CENTER);
		thumbImage = new JLabel();
		top.add(thumbImage);
		bottom = new JPanel();
		add(bottom, BorderLayout.SOUTH);
		timestamp = new JLabel();
		timestamp.setFont(new Font("SansSerif", Font.BOLD, 10));
		bottom.setBackground(Color.WHITE);
		bottom.add(timestamp);
	}

	private void setTimestamp(int pc, int reset, long uptime) {
		timestamp.setText( reset + " / " + uptime + " / " + pc);
	}
	
	public void setThumb(BufferedImage thumb, int pc, int reset, long uptime) {
		thumbImage.setIcon(new ImageIcon(thumb));
		setTimestamp(pc, reset, uptime);
	}
	
}
