package telemetry.test;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import common.Config;
import common.Log;
import telemetry.LayoutLoadException;
import telemetry.frames.FrameLayout;

public class FrameLayoutTest {

	@Test
	public void testFrameLayout() {
		String logFileDir = "C:\\Users\\chris\\Desktop\\Test\\JUNIT_TEST";
		Config.homeDirectory = logFileDir;
		Log.init("FrameLayoutTest");
		Config.currentDir = System.getProperty("user.dir"); //m.getCurrentDir(); 	
		Config.minInit(logFileDir); 
		Config.initSatelliteManager();
		Log.println("LogFileDir is:" + Config.logFileDirectory);

		try {
			FrameLayout frame = new FrameLayout(6, "HUSKY_SAT_Type0_ALL_WOD.frame");
			
			assertEquals(6,frame.getNumberOfPayloads());
			assertEquals("wodcantelemetry",frame.getPayloadName(0));
			assertEquals(78,frame.getPayloadLength(0));
			assertEquals("wodtelemetry",frame.getPayloadName(1));
			assertEquals(78,frame.getPayloadLength(1));
			assertEquals("wodcantelemetry",frame.getPayloadName(2));
			assertEquals(78,frame.getPayloadLength(2));
			assertEquals("wodtelemetry",frame.getPayloadName(3));
			assertEquals(78,frame.getPayloadLength(3));
			assertEquals("wodcantelemetry",frame.getPayloadName(4));
			assertEquals(78,frame.getPayloadLength(4));
			assertEquals("wodtelemetry",frame.getPayloadName(5));
			assertEquals(78,frame.getPayloadLength(5));
			
		} catch (LayoutLoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
