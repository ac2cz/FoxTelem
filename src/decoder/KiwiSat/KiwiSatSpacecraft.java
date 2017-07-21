package decoder.KiwiSat;

import java.io.File;
import java.io.FileNotFoundException;

import common.Spacecraft;
import decoder.Decoder;
import decoder.SourceAudio;
import telemetry.LayoutLoadException;

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
 * This is the KiwiSat Spacecraft definition
 * 
 */
public class KiwiSatSpacecraft extends Spacecraft{

	public KiwiSatSpacecraft(File fileName) throws FileNotFoundException, LayoutLoadException {
		super(fileName);
		load();
	}

	@Override
	public Decoder getDecoder(String n, SourceAudio as, int chan, int mode) {
		return new KiwiSatDecoder(as, chan);
	}

}
