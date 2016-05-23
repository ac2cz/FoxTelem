package FuncubeDecoder;

import java.io.File;
import java.io.FileNotFoundException;

import common.Config;
import common.Log;
import common.Spacecraft;
import telemetry.BitArray;
import telemetry.BitArrayLayout;
import telemetry.LayoutLoadException;
import telemetry.PayloadRtValues;

public class FUNcubeSpacecraft extends Spacecraft {
	public static final int[] TYPE_WHOLE_ORBIT = {1,2,3,4,5,6,7,8,9,10,11,12};
	public static final int[] TYPE_HIGH_RES = {13,17,21};
	public static final int[] TYPE_FITTER = {14,15,16,18,19,20,22,23,24};
	
	// layout filenames
	public String rtLayoutFileName;
	public String wholeOrbitLayoutFileName;
	public String highResLayoutFileName;
	public String fitterLayoutFileName;
	
	//Telemetry layouts
	public BitArrayLayout rtLayout;
	public BitArrayLayout wholeOrbitLayout;
	public BitArrayLayout highResLayout;
	public BitArrayLayout fitterLayout;
		
	public FUNcubeSpacecraft(String fileName) throws FileNotFoundException, LayoutLoadException {
		super(fileName);
		load();
		rtLayout = new BitArrayLayout(rtLayoutFileName);
		wholeOrbitLayout = new BitArrayLayout(wholeOrbitLayoutFileName);
		highResLayout = new BitArrayLayout(highResLayoutFileName);
		fitterLayout = new BitArrayLayout(fitterLayoutFileName);
		
	}
	
	public BitArray getPayloadByType(int type) {
		for (int i=0; i < TYPE_WHOLE_ORBIT.length; i++)
			if (type == TYPE_WHOLE_ORBIT[i])
				return new PayloadWholeOrbit(wholeOrbitLayout);
		for (int i=0; i < TYPE_HIGH_RES.length; i++)
			if (type == TYPE_HIGH_RES[i])
				return new PayloadHighRes(highResLayout);
		for (int i=0; i < TYPE_FITTER.length; i++)
			if (type == TYPE_FITTER[i])
				return new PayloadFitterMessages(fitterLayout);
		
		return null;
	}

	@Override
	protected void load() throws LayoutLoadException {
		super.load();
		try {
			rtLayoutFileName = getProperty("rtLayoutFileName");
			wholeOrbitLayoutFileName = getProperty("wholeOrbitLayoutFileName");
			highResLayoutFileName = getProperty("highResLayoutFileName");
			fitterLayoutFileName = getProperty("fitterLayoutFileName");
		} catch (NumberFormatException nf) {
			nf.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("Corrupt data found when loading Spacecraft file: " + Config.currentDir + File.separator + SPACECRAFT_DIR + File.separator +propertiesFileName );
		} catch (NullPointerException nf) {
			nf.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("Missing data value when loading Spacecraft file: " + Config.currentDir + File.separator + SPACECRAFT_DIR + File.separator +propertiesFileName );		
		}
	}

	@Override
	protected void save() {
		properties.setProperty("rtLayoutFileName", rtLayoutFileName);
		properties.setProperty("wholeOrbitLayoutFileName", wholeOrbitLayoutFileName);
		properties.setProperty("highResLayoutFileName", highResLayoutFileName);
		properties.setProperty("fitterLayoutFileName", fitterLayoutFileName);
		
	}

}
