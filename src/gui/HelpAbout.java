package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;

import common.Config;
import common.DesktopApi;
import common.Log;

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
 */
@SuppressWarnings("serial")
public class HelpAbout extends JDialog implements ActionListener {

	private JPanel contentPane;
	private final String AMSAT = "http://www.amsat.org";
	private final String FOX = "http://ww2.amsat.org/?page_id=1113";
	public final static String MANUAL = "foxtelem_manual.pdf";
	public final static String LEADERBOARD = "http://www.amsat.org/tlm/";
	public final static String SOFTWARE = "http://www.g0kla.com/foxtelem/";
	JButton btnClose;
	
	/**
	 * Create the frame.
	 */
	public HelpAbout(JFrame owner, boolean modal) {
		super(owner, modal);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(MainWindow.frame.getBounds().x+25, MainWindow.frame.getBounds().y+25, 800, 750);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		//ImagePanel panel = new ImagePanel("C:/Users/chris.e.thompson/Desktop/workspace/SALVAGE/data/stars1.png");
		JPanel panel = new JPanel();
		contentPane.add(panel, BorderLayout.CENTER);
		panel.setLayout(new BorderLayout(0, 0));

		JPanel northPanel = new JPanel();
		panel.add(northPanel, BorderLayout.NORTH);
		northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.X_AXIS));
		
		JPanel northApanel = new JPanel();
		JPanel northBpanel = new JPanel();
		northPanel.add(northApanel);
		northPanel.add(northBpanel);
		northApanel.setLayout(new BoxLayout(northApanel, BoxLayout.Y_AXIS));
		
		JPanel eastPanel = new JPanel();
		panel.add(eastPanel, BorderLayout.EAST);
		eastPanel.setLayout(new BoxLayout(eastPanel, BoxLayout.Y_AXIS));

//		JPanel centerPanelWrapper = new JPanel();
		
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
//		panel.add(centerPanel, BorderLayout.CENTER);

		JPanel southPanel = new JPanel();
		panel.add(southPanel, BorderLayout.SOUTH);

		JLabel lblAmsatFoxaTelemetry = new JLabel("<html><h2>AMSAT Telemetry Decoder and Analysis</h2></html>");
		lblAmsatFoxaTelemetry.setForeground(Color.BLUE);
		northApanel.add(lblAmsatFoxaTelemetry);

		addLine("Version " + Config.VERSION, northApanel);
		
		
//		addLine("<html>Written by <b>Chris Thompson AC2CZ</b><br><br></html>", northApanel);
		addUrl("Written by ", "www.g0kla.com", "<b>Chris Thompson</b>", " AC2CZ", northApanel);
		addUrl("You can browse ", MANUAL, "the manual", " for help", northApanel);
		
		addUrl("Visit the ", FOX, "Amsat Fox", " pages to learn more about Fox-1 and the Radio Amateur Satellite Corporation.", northApanel);
		addUrl("Please consider ", AMSAT, "donating", " to this and future AMSAT missions", northApanel);
		addLine(" ", northApanel);
		addUrl("\nThis program is distributed in the hope that it will be useful, "
				+ "but WITHOUT ANY WARRANTY; without even the implied warranty of "
				+ "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the ",
				"http://www.gnu.org/licenses/gpl-3.0.en.html", "<b>GNU General Public License</b>", " for more details. ", northApanel);
		addLine(" ", northApanel);
		addLine("This software also includes:", northApanel);
		addUrl("- Phil Karn (KA9Q) RS Decoder, ported to Java by ", "https://github.com/phlash/java-sdr", "<b>Phil Ashby M6IPX</b>", ", (part of the FUNcube team) released under the CC Non-Commercial Share Alike license", northApanel);
		addUrl("- Portions of SdrTrunk DSP code for AirSpy and RTL USB by ", "https://github.com/DSheirer/sdrtrunk", "<b>DSheirer</b>", ", released underGPL", northApanel);
		addUrl("- Purejavahid library developed by ", "https://github.com/nyholku/purejavahidapi", "<b>Kustaa Nyholm / SpareTimeLabs</b>", ", released under the BSD license", northApanel);
		addUrl("- JTransforms FFT library developed by ", "https://sites.google.com/site/piotrwendykier/software/jtransforms", "<b>Piotr Wendykier</b>", ", released under the BSD license", northApanel);
		addUrl("- Java DDE developed by ", "http://jdde.pretty-tools.com/", "<b>Pretty Tools</b>", ", released as free and opensource", northApanel);
		addUrl("- Java Predict Port by ", "https://github.com/badgersoftdotcom/predict4java", "<b>G4DPZ</b>", ", released under GPL", northApanel);
		addUrl("- Predict is by", "http://www.qsl.net/kd2bd/predict.html", "<b>KD2BD</b>", ", released under GPL", northApanel);
		addUrl("- Equidistant Map by ", "https://commons.wikimedia.org/wiki/File:World_V2.0.svg", "<b>Myvolcano</b>", ", released under CC0 1.0", northApanel);
		addUrl("- Color Equidistant Map by ", "https://commons.wikimedia.org/wiki/File:Equirectangular_projection_SW.jpg", "<b>Strebe</b>", ", released under CC BY-SA 3.0", northApanel);
		
		JScrollPane scrollPane = new JScrollPane (centerPanel, 
				   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		//centerPanelWrapper.add(scrollPane);
		panel.add(scrollPane, BorderLayout.CENTER);
		
		addLine("<html><br><b><u>The original Fox1 Engineering Team</b></u></html>", centerPanel);
		addLine("<html><table style='mso-cellspacing: 0in' cellspacing='0' cellpadding='2' >"
				+"<tr><tbody><td><b>Jerry Buxton N0JY</b></td><td>Program Manager</td>"
				+ "		<td><b>Tony Monteiro AA2TX (SK)</b></td><td>Original driving force</td></tr>"
				
				+"<tr> 	<td><b>Burns Fisher W2BFJ</b></td><td>Software team co-lead</td> "
				+" 		<td><b>Cory Abate K8SPN</b></td><td>Solar panel design</td> </tr> "
				
				+"<tr> 	<td><b>Andrew Abken KN6ZA</b></td><td>Structure machining</td> "
				+"		<td><b>Bryan Baker N4DTV</b></td><td>Command and Control RF Uplink</td> </tr> "
				
				+"<tr> <td><b>Jonathan Brandenburg KF5IDY</b></td><td>Experiment Control</td> "
				+ "		<td><b>Arlen Cordoba</b></td><td>Flight unit structure machining</td></tr> "
				
				+ "<tr> <td><b>Don Corrington AK2S</b></td><td>Power supply</td> "
				+ "		<td><b>Bob Davis KF4KSS</b></td><td>Mechanical, Environmental</td></tr> "

				+ "<tr> <td><b>Paul Finch WD5IDM</b></td><td>RF Testing</td> "
				+ "		<td><b>Joe Fitzgerald KM1P</b></td><td>IT Support and Management</td></tr> "

				+ "<tr> <td><b>Bob Fitzpatrick KB5SQG</b></td><td>Transponder Testing</td> "
				+ "		<td><b>Bdale Garbee KB0G</b></td><td>IHU and Command & Control Hardware</td></tr> "

				+ "<tr> <td><b>Dan Habecker W9EQ</b></td><td>RF Design and Test</td> "
				+ "		<td><b>Bryan Hoyer K7UDR</b></td><td>Battery and EXP4</td></tr> "

				+ "<tr> <td><b>Dick Jansson KD1K</b></td><td>Thermal Design</td> "
				+ "		<td><b>Mark Kanawati N4TPY</b></td><td>Solar Cells</td></tr> "
				
				+ "<tr> <td><b>Phil Karn KA9Q</b></td><td>Downlink FEC</td> "
				+ "		<td><b>John Klingelhoeffer WB4LNM</b></td><td>RF, Command and Control</td></tr> "

				+ "<tr> <td><b>Taylor Klotz K4OTZ</b></td><td>Command and Control RF</td> "
				+ "		<td><b>Dino Lorenzini KC4YMG</b></td><td>Solar Cells</td></tr> "
				
				+ "<tr> <td><b>Steve Lubbers KE8FP</b></td><td>IHU Software</td> "
				+ "		<td><b>Mike McCann KB2GHZ</b></td><td>IHU Software</td></tr> "

				+ "<tr> <td><b>Brian McCarthy N1ZZD</b></td><td>Spin Detection</td> "
				+ "		<td><b>Lou McFadin W5DID</b></td><td>Conformal coating</td></tr> "

				+ "<tr> <td><b>Larry Phelps K4OZS</b></td><td>Satellite construction</td> "
				+ "		<td><b>Fred Piering WD9HNU</b></td><td>RF System Design</td></tr> "

				+ "<tr> <td><b>David Ping WB6DP</b></td><td>Antennae</td> "
				+ "		<td><b>Douglas Quagliana KA2UPW</b></td><td>Telemetry and Ground station architecture</td></tr> "

				+ "<tr> <td><b>Bill Reed NX5R</b></td><td>Software team co-lead, DSP</td> "
				+ "		<td><b>Brenton Salmi KB1LQD</b></td><td>Maximum Power Point Tracker</td></tr> "

				+ "<tr> <td><b>Bryce Salmi KB1LQC</b></td><td>Maximum Power Point Tracker</td> "
				+ "		<td><b>Kelly Shaddrick W0RK</b></td><td>Battery Testing, Inventory control software</td></tr> "

				+ "<tr> <td><b>Ron Tassi N3AEA</b></td><td>Power Supply, RF</td> "
				+ "		<td><b>Damon Wascom KC5CQW</b></td><td>PCB Construction</td></tr> "

				+ "<tr> <td><b>Melanie Wascom KF5TNK</b></td><td>PCB Construction</td> "
				+ "		<td><b>Everett Yost KB9XI</b></td><td>Batteries</td></tr> "

				+ "<tr> <td><b>Bruce Herrick WW1M</b></td><td>L Band Uplink</td> "
				+ "		<td><b>Dan Hubert VE9DAN</b></td><td>L Band Uplink</td></tr> "

				+ "<tr> <td><b>Elizabeth Schenk KC1AXX</b></td><td>L Band Uplink</td> "
				+ "		<td><b>Dave Smith W6TE</b></td><td>L Band Uplink</td></tr> "

				+ "<tr> <td><b>Alfred Watts AF5VH</b></td><td>L Band Uplink</td> "
				+ "		<td><b>Bronson Crothers N1ZAQ (SK)</b></td><td>Power Systems</td></tr> "

				+ "</tbody></table></html>", centerPanel);
		
		
		
		
		
		BufferedImage wPic = null;
		try {
			wPic = ImageIO.read(this.getClass().getResource("/images/fox.jpg"));
		} catch (IOException e) {
			e.printStackTrace(Log.getWriter());
		}
		if (wPic != null) {
			JLabel wIcon = new JLabel(new ImageIcon(wPic));
			northBpanel.add(wIcon);
		}
		btnClose = new JButton("Close");
		btnClose.addActionListener(this);
		southPanel.add(btnClose);

	}
	 
	private void addUrl(String pre, final String url, String text, String post, JPanel panel) {
		JLabel website = new JLabel();
		//website.setFont(new Font("SansSerif", Font.PLAIN, Config.displayModuleFontSize));
		website.setForeground(Color.BLACK);
		panel.add(website);

	        website.setText("<html>"+pre+"<a href=\"\">"+text+"</a>"+post+"</html>");
	        website.setCursor(new Cursor(Cursor.HAND_CURSOR));
	        website.addMouseListener(new MouseAdapter() {
	            @Override
	            public void mouseClicked(MouseEvent e) {
	                    try {
	                            DesktopApi.browse(new URI(url));
	                    } catch (URISyntaxException ex) {
	                            //It looks like there's a problem
	                    	ex.printStackTrace();
	                    }
	            }
	        });
	    }
	private void addLine(String text, JPanel panel) {
		JLabel lblVersion = new JLabel(text);
		//lblVersion.setFont(new Font("SansSerif", Font.PLAIN, Config.displayModuleFontSize));
		lblVersion.setForeground(Color.BLACK);
		panel.add(lblVersion);

	}
	
	@Override
	public void actionPerformed(ActionEvent e) {		
		if (e.getSource() == btnClose) {
			this.dispose();
		}

		
	}

}
