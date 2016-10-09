package network;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import telemetry.FoxFramePart;
import common.Config;
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
public class FtpLogs implements Runnable {
	boolean running = false;
	String server = "n0jy.org";
    int port = 21;
    String user = "g0kla";
    String pass = "argyl3sw3@t3R";
    
    FTPClient ftpClient = new FTPClient();
    
	public void stopProcessing() {
		running = false;
	}
	
	@Override
	public void run() {
		running = true;
		
		while(running) {
			try {
				Thread.sleep(1000 * 60 * Config.ftpPeriod); // refresh data periodically
			} catch (InterruptedException e) {
				Log.println("ERROR: ftpLogs thread interrupted");
				e.printStackTrace(Log.getWriter());
			} 			
			
			
			try {
				if (Config.ftpFiles)
					sendFiles();
			} catch (SocketException e) {
				Log.println("ERROR: ftpLogs socket exception");
				e.printStackTrace(Log.getWriter());
			} catch (IOException e) {
				Log.println("ERROR: ftpLogs IO Exception");
				e.printStackTrace(Log.getWriter());
			}
		}
		
	}

	private void sendFiles() throws SocketException, IOException {
		ftpClient.connect(server, port);
		Log.println("Connected to " + server + ".");
	    Log.print(ftpClient.getReplyString());
	    int reply = ftpClient.getReplyCode();
	    if(!FTPReply.isPositiveCompletion(reply)) {
	    	  ftpClient.disconnect();
	        Log.println("FTP server refused connection.");
	    } else {
	    	ftpClient.login(user, pass);
	    	Log.println("Logging in..");
	    	Log.print(ftpClient.getReplyString());

	    	ftpClient.enterLocalPassiveMode();	
	    	ftpClient.setControlKeepAliveTimeout(300); // set timeout to 5 minutes.  Send NOOPs to keep control channel alive
	    	
	    	ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
	    	
	    	/**
	    	 * This is currently disabled because the payload store changed.  We probablly only want to send for one
	    	 * satellite as this is only used for debugging
	    	 * 
	    	sendFile(PayloadStore.RT_LOG);
	    	sendFile(PayloadStore.MAX_LOG);
	    	sendFile(PayloadStore.MIN_LOG);
	    	sendFile(PayloadStore.RAD_LOG);
*/
	    	ftpClient.disconnect();
	    }
	}
	
	@SuppressWarnings("unused")
	private void sendFile(String fileName) throws FileNotFoundException {
        File firstLocalFile = new File(fileName);
        
        String firstRemoteFile = FoxFramePart.fileDateStamp() + "_" + Config.callsign.toLowerCase() + "_" + fileName ;
        InputStream inputStream = new FileInputStream(firstLocalFile);

        
        Log.println("Uploading log file: " + fileName);
        boolean done;
		try {
			done = ftpClient.storeFile(firstRemoteFile, inputStream);
			Log.print(ftpClient.getReplyString());
			inputStream.close();
	        if (done) {
	        	Log.println(" ... success.");
	        } else
	        	Log.println(" ... fail.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.println(" ... fail.");
		}
        
        
	}
}
