package telemServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.Socket;

import telemetry.Frame;

public class ServerProcess implements Runnable {

	private Socket socket = null;
	private int sequence = 0;
	public ServerProcess(Socket socket, int seq) {
		sequence = seq;
		this.socket = socket;
	}

	/**
	 * This is started when we have a TCP connection.  We read the data until the connection is closed
	 * This could be one or more STP files.
	 */
	public void run() {

		//Writer f = null;
		FileOutputStream f = null;
		//PrintWriter out = null;
		//BufferedReader in = null;
		InputStream in = null;
		try {
			//out = new PrintWriter(socket.getOutputStream(), true);
			//f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sequence + ".stp"), "utf-8"));
			f = new FileOutputStream(sequence + ".stp");
			//in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			in = socket.getInputStream();
			int c;
			while ((c = in.read()) != -1) {
				f.write(c);
				Character ch = (char) c;
				System.out.print(ch);
			}
			System.out.println();
			/*
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				System.out.println(inputLine);
				f.write(inputLine +"\r\n");
			}
			*/
//			out.close();
			in.close();
			socket.close();

			// At this point the file is on disk, so we can process it
			File stp = new File(sequence + ".stp");
			// Import it into the database
			Frame.importStpFile("", stp, false);
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try { f.close(); } catch (Exception ex) { /*ignore*/} 
		}
	}

}
