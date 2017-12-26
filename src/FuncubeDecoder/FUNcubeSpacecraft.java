package FuncubeDecoder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import common.Config;
import common.Log;
import common.Spacecraft;
import telemetry.BitArray;
import telemetry.BitArrayLayout;
import telemetry.FramePart;
import telemetry.LayoutLoadException;
import telemetry.PayloadRtValues;

public class FUNcubeSpacecraft extends Spacecraft {
	public static final int[] TYPE_WHOLE_ORBIT = {1,2,3,4,5,6,7,8,9,10,11,12};
	public static final int[] TYPE_HIGH_RES = {13,17,21};
	public static final int[] TYPE_FITTER = {14,15,16,18,19,20,22,23,24};
	
	public static final String WHOLE_ORBIT_LAYOUT = "WHOLE_ORBIT";
	public static final String HIGH_RES_LAYOUT = "HIGH_RES";
	public static final String FITTER_LAYOUT = "FITTER";
	
	public static final int FUNCUBE_ID = 100;
	
	// layout filenames
	//public String rtLayoutFileName;
	//public String wholeOrbitLayoutFileName;
	//public String highResLayoutFileName;
	//public String fitterLayoutFileName;
	
	//Telemetry layouts
	//public BitArrayLayout rtLayout;
	//public BitArrayLayout wholeOrbitLayout;
	//public BitArrayLayout highResLayout;
	//public BitArrayLayout fitterLayout;
		
	public FUNcubeSpacecraft(File fileName) throws LayoutLoadException, IOException {
		super(fileName);
		load();
		
		
	}
	
	public FramePart getPayloadByType(int type) {
		for (int i=0; i < TYPE_WHOLE_ORBIT.length; i++)
			if (type == TYPE_WHOLE_ORBIT[i])
				return new PayloadWholeOrbit(getLayoutByName(WHOLE_ORBIT_LAYOUT));
		for (int i=0; i < TYPE_HIGH_RES.length; i++)
			if (type == TYPE_HIGH_RES[i])
				return new PayloadHighRes(getLayoutByName(HIGH_RES_LAYOUT));
		for (int i=0; i < TYPE_FITTER.length; i++)
			if (type == TYPE_FITTER[i])
				return new PayloadFitterMessages(getLayoutByName(FITTER_LAYOUT));
		
		return null;
	}

	@Override
	protected void load() throws LayoutLoadException {
		super.load();
		
	}

	@Override
	protected void save() {
		super.save();
	}

}
