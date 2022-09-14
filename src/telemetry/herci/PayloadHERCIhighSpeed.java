package telemetry.herci;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import common.Config;
import common.Spacecraft;
import decoder.FoxDecoder;
import telemetry.BitArrayLayout;
import telemetry.FramePart;

/**
 * 
 * @author chris.e.thompson
 *
 opt/project/flexi/devel/doc/flexi_telementry.txt

Updates to the telemetry formats
used with the FLEXI processor system
for the "HERCI" instrument.

Assumtions:
    Use the JUNO/WAVES software with
minimal changes.  Replace the JUNO/WAVES
IP/UDP/CIP headers with the FOX1/IHU headers.  
The low speed telemtry handler has the capability to 
impose a programmable delay (n*25mSec)
between successive transfer frames.
    The low speed communications channel, 
being asynchronous, is expected to shift 
out low bit first.  In other words, it 
emulates the function of a commercial UART.

    The high speed communications channel,
being synchronous, is expected to shift
out high bit first.  This interface is
intended for use in a legacy spacecraft 
environment.  HERCI does NOT make use of
a high speed telemetry channel.

    The transfer frame format is identical
for either channel type to allow ground 
software to conveniently deal with either
data type.

Changes:
    Change in header fields, eliminate
IP/UDP/CIP headers add in the FOX1/IHU
headers.


Data Formats
------------

This describes the instrument-specific byte
seend by IHU, that is the 868 byte instrument 
payload.  
Note that the header area looks awfully 
"Big-Endian".  This is historical holdover 
from the CASSINI/RPWS(8085) and JUNO/WAVES
(Y180/Z80) instruments. FLEXI (Y90/Z80)
continues this ugly tradition simply
because we see no reason to change it.

Transfer Frame:
         +0      +1      +2      +3
        +-------+-------+-------+-------+
     +0 |    Synchronization Pattern    |
        |   FA  |   F3  |   34  |   03  |
        +-------+-------+-------+-------+
     +4 |  CRC CCITT-16 |    Sequence   |
        |    Seed = 0   |   monotonic   |
        +-------+-------+-------+-------+
     +8 |         System Time           |
        | D31..D1 seconds, D0 qual Flag |
        +-------+-------+-------+-------+
    +12 |  Epoch Number | Record Length |
        |    1..32767   | from begining |
        +-------+-------+-------+-------+
    +16 |   Science minipackets
        .    see minipacket document
        .    for individual record
        .    formats
        |  
        +-------+-------+-------+-------+

  Synchronization Pattern:
      Standard 32bit synchronizatioin pattern.
      Always has FAF33403.
      This allows the HERCI/FLEXI telemetry to
      be treated as a simply byte-stream.  No
      additional byte-level framing is required
      to process the data.

  CRC CCITT-16:
                                      16    12    5
      The CCITT CRC 16 polynomial is X   + X   + X  + 1.
      Seed value is ZERO for the WAVES/FLEXI/HERCI implementation.
      (Sequence field is always non-zero so the CRC is
      effective for our purposes).

  Sequence: (D15..D8 are in column +2, D7..D0 are in column +3)
      Two fields in this 16 bit area.
      D15..D14 is a source field
          00        PANIC/FAILED (FSW didn't load)
          01        HRS	(HERCI soes not make use of HRS telemetry)
          10        LRS   (Science Telemetry)
          11        HSK   (Housekeeping)
      D13..D0 is a monotonically increasing frame count.

  System Time: (D31..D24 is at offset 0 and D7..D0 are at offset 3)
      Seconds from some arbritrary Epoch as a 32 bit integer.
      Bit 0 of time is relaced with a "time quality flag" that
      is set to a value of '1' to indicate that time may be
      suspect (in other words, there hasn't been a recent
      time update message from the host spacecraft).

  Epoch Number: (D15..D8 is at offset 0 and D7..D0 are at offset 1)
      This field identifies the epoch for the associated
      time field.  In the FOX1 environment, this is the
      number of times the host system has been reset.

  Record Length: (D15..D8 is at offset 2 and D7..D0 are at offset 3)
      This is the number of octets in the transfer frame.
      Length includes from the synch pattern through to 
      the last data in the buffer including any fill bytes.

  Science minipackets:
      Individual science packets are concatenated in this
      area.  Header fields are all identical to allow 
      consistent packet extraction but the format of each
      type is unique.  Lengths also vary with each type
      of data.



 */
public class PayloadHERCIhighSpeed extends FramePart {

	public static final int MAX_PAYLOAD_SIZE = 868;
	public static final int MAX_HEADER_SIZE = 16; // 16 bytes in the header
	
	int epoch; // captured from the telem header
	long time; // captured from the telem header
	
	public PayloadHERCIhighSpeed(BitArrayLayout lay) {
		super(TYPE_HERCI_HIGH_SPEED_DATA, lay);
	}

	/**
	 * Load this payload from disk
	 * @param id
	 * @param resets
	 * @param uptime
	 * @param date
	 * @param st
	 */
	public PayloadHERCIhighSpeed(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, TYPE_HERCI_HIGH_SPEED_DATA, date, st, lay);
		MAX_BYTES = MAX_PAYLOAD_SIZE;
	}
	
	public PayloadHERCIhighSpeed(ResultSet r, BitArrayLayout lay) throws SQLException {
		super(r, TYPE_HERCI_HIGH_SPEED_DATA, lay);
	}

	@Override
	protected void init() {
		MAX_BYTES = MAX_PAYLOAD_SIZE;
//		fieldValue = new int[MAX_PAYLOAD_SIZE];  // we declare this as the max payload size rather than the size of the layout so that we include all of the minipackets
	}

	/**
	 * Calculate the telemetry and return it
	 * @return
	 */
	@Deprecated
	public HerciHighspeedHeader calculateTelemetryPalyoad() {
		HerciHighspeedHeader radTelem = new HerciHighspeedHeader(resets, uptime, Config.satManager.getLayoutByName(id, Spacecraft.HERCI_HS_HEADER_LAYOUT));
		for (int k=0; k<HerciHighspeedHeader.MAX_RAD_TELEM_BYTES; k++) { 
			radTelem.addNext8Bits(fieldValue[k]);
		}
		radTelem.copyBitsToFields();
		epoch = radTelem.fieldValue[HerciHighspeedHeader.HERCI_EPOCH_FIELD];
		time = radTelem.fieldValue[HerciHighspeedHeader.HERCI_TIME_FIELD];
		return radTelem;
	}
	@Deprecated
	public ArrayList<HerciHighSpeedPacket> calculateTelemetryPackets() {
		ArrayList<HerciHighSpeedPacket> packets = new ArrayList<HerciHighSpeedPacket>();
		
		boolean morePackets = true;
		int packetDataStart = PayloadHERCIhighSpeed.MAX_HEADER_SIZE;
		while (morePackets) {
			HerciHighSpeedPacket radTelem = new HerciHighSpeedPacket(id, resets, uptime, epoch, time, Config.satManager.getLayoutByName(id, Spacecraft.HERCI_HS_PKT_LAYOUT));
			for (int k=0; k<HerciHighSpeedPacket.MAX_PACKET_HEADER_BYTES; k++) { 
				radTelem.addNext8Bits(fieldValue[packetDataStart+k]);  
			}
			packetDataStart = packetDataStart+HerciHighSpeedPacket.MAX_PACKET_HEADER_BYTES;
			radTelem.initPacket();
			if (radTelem.getLength() > 0) {
				if (packetDataStart + radTelem.getLength()+1 <= MAX_PAYLOAD_SIZE-MAX_HEADER_SIZE) {
					for (int k=0; k<radTelem.getLength()+1; k++) {
						radTelem.addNext8Bits(fieldValue[k+packetDataStart]);
					}
					packetDataStart = radTelem.getLength()+1 + packetDataStart; 
					packets.add(radTelem);
				} else {
					morePackets = false;
				}
			} else {
				morePackets = false;
			}
		}
		return packets;
	}

	
	@Override
	public String toString() {
		copyBitsToFields();
		String s = new String();
		s = s + "HERCI EXPERIMENT HIGH SPEED DATA: " + MAX_BYTES + " bytes\n";
		for (int i =0; i< MAX_BYTES; i++) {
			s = s + FoxDecoder.hex(fieldValue[i]) + " ";
			// Print 32 bytes in a row
			if ((i+1)%32 == 0) s = s + "\n";
		}
		return s;
	}
	
	public String toFile() {
		copyBitsToFields();
		String s = new String();
		s = s + reportDate + "," + id + "," + resets + "," + uptime + "," + type + ",";
		for (int i=0; i < fieldValue.length-1; i++) {
			//s = s + Decoder.dec(fieldValue[i]) + ",";
			s = s + fieldValue[i] + ",";
		}
		// add the final field with no comma delimiter
		//s = s + Decoder.dec(fieldValue[fieldValue.length-1]);
		s = s + fieldValue[fieldValue.length-1];
		return s;
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

}
