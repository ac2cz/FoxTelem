package telemetry.uw;

import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

import common.Config;
import common.Log;
import common.Spacecraft;
import telemetry.BitArrayLayout;
import telemetry.FoxPayloadStore;
import telemetry.FramePart;
@Deprecated
public class PayloadWODUwExperiment extends FramePart {
	public ArrayList<UwCanPacket> canPackets; 
	protected UwCanPacket canPacket; // the current CAN Packet we are adding bytes to
	public ArrayList<UwCanPacket> splitPackets; 
	protected UwCanPacket rawCanPacket;
//	private int startPacketSerial = 0;

//	public static final int WOD_RESETS_FIELD = 1;
//	public static final int WOD_UPTIME_FIELD = 2;
	public static final String WOD_RESETS = "WODTimestampReset";
	public static final String WOD_UPTIME = "WODTimestampUptime";
//	public static final int CRC_ERROR_FIELD = 3;
	public static final String CRC_ERROR = "crcError";
//	public static final int PAD2_FIELD = 4;
//	public static final String PAD2 = "pad2";
		
	public PayloadWODUwExperiment(BitArrayLayout lay, int id, long uptime, int resets) {
		super(TYPE_UW_WOD_EXPERIMENT,lay);
		canPackets = new ArrayList<UwCanPacket>();
		//if (Config.splitCanPackets)
		splitPackets = new ArrayList<UwCanPacket>();
		captureHeaderInfo(id, uptime, resets);
	}

	public PayloadWODUwExperiment(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, TYPE_UW_WOD_EXPERIMENT, date, st, lay);
		canPackets = new ArrayList<UwCanPacket>();
	}
	
	@Override
	protected void init() {
		// nothing extra to init here
	}
	
//	public void setStartSerial(int serial) {
//		startPacketSerial = serial;
//	}
	
	/**
	 * Add a byte to the next CAN Packet.  If the packet is full and we have more bytes, create another packet.
	 * We are finished once we have hit the ID 0x0000, which means end of CAN Packets or we run out of bytes.  
	 * That final packet is thrown away, unless it fit exactly and passes the isValid() check.
	 * away
	 * @param b
	 */
	int debugCount = 0;
	protected void addToCanPackets(byte b) {
			if (rawCanPacket == null) {
				rawCanPacket = new UwCanPacket(Config.satManager.getLayoutByName(id, Spacecraft.WOD_CAN_PKT_LAYOUT)); 
				rawCanPacket.setType(FramePart.TYPE_UW_WOD_CAN_PACKET);
				rawCanPacket.captureHeaderInfo(id, uptime, resets);
			}
			if (rawCanPacket.hasEndOfCanPacketsId()) return;
			rawCanPacket.addNext8Bits(b);
			if (rawCanPacket.isValid()) { // add the complete packet to the canPackets list
				canPackets.add(rawCanPacket);
				
				// If we are not on the server then split packets
				if (Config.splitCanPackets) {
					byte[] data = rawCanPacket.getBytes();
					BitArrayLayout canLayout = Config.satManager.getLayoutByCanId(id, rawCanPacket.getID());
					if (canLayout == null) Log.errorDialog("ERROR", "Missing CAN WOD Layout for CAN ID: "+rawCanPacket.getID());

					UwCanPacket newPacket = new UwCanPacket(id, resets, uptime, reportDate, data, canLayout);
					newPacket.setType(FramePart.TYPE_UW_CAN_PACKET_TELEM);
					splitPackets.add(newPacket);
				}
				rawCanPacket = new UwCanPacket(Config.satManager.getLayoutByName(id, Spacecraft.WOD_CAN_PKT_LAYOUT)); 
				rawCanPacket.setType(FramePart.TYPE_UW_WOD_CAN_PACKET);
				rawCanPacket.captureHeaderInfo(id, uptime, resets);
				
			}
//		}
	}
	
	@Override
	public void addNext8Bits(byte b) {
		if (numberBytesAdded <1)
			super.addNext8Bits(b);  // the flag byte
		else if (numberBytesAdded <72) {
			addToCanPackets(b);
			super.addNext8Bits(b);
		} else
			super.addNext8Bits(b); // deal with timestamp
	}
	
	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public boolean savePayloads(FoxPayloadStore payloadStore, int serial, boolean storeMode) {
		type = type * 100 + serial;
		copyBitsToFields(); // make sure reset / uptime correct
		if (!payloadStore.add(getFoxId(), getUptime(), getResets(), this))
			return false;
		int j = 0;
		for (UwCanPacket p : canPackets) {
			// Set the type here as it needs to span across payloads.  The uptime is NOT unique for multiple payloads in same Frame.
			int p_type = p.getType();
			p_type = p_type * 100 + serial + j++;
			p.setType(p_type);
			if (storeMode)
				p.newMode = newMode;
			if (!payloadStore.add(getFoxId(), getUptime(), getResets(), p))
				return false;
		}
		if (splitPackets != null && splitPackets.size() > 0) {
			j = 0;
			for (UwCanPacket p : splitPackets) {
				// Set the type here as it needs to span across payloads.  The uptime is NOT unique for multiple payloads in same Frame.
				int p_type = p.getType();
				p_type = p_type * 100 + serial + j++;
				p.setType(p_type);
				if (storeMode)
					p.newMode = newMode;
				if (!payloadStore.add(getFoxId(), getUptime(), getResets(), p))
					return false;
			}
		}
		return true;

	}
	
	
	@Override
	public void copyBitsToFields() {
		super.copyBitsToFields();
		resets = getRawValue(WOD_RESETS);
		uptime = getRawValue(WOD_UPTIME);
	}
	
	@Override
	public String toString() {
		copyBitsToFields();
		String s = "UW WOD EXPERIMENT PAYLOAD - " + canPackets.size() + " CAN PACKETS\n";
		s = s + "RESET: " + getRawValue(WOD_RESETS);
		s = s + "  UPTIME: " + getRawValue(WOD_UPTIME);
		s = s + "  TYPE: " +  type;
		s = s + "  OVERFLOW FLAG: " + getRawValue(CRC_ERROR) + "\n";
		//for (int p=0; p < canPackets.size(); p++) {
		//	s = s + canPackets.get(p).toString() + "    " ;
		//	if ((p+1)%3 == 0) s = s + "\n";
		//}
		//s=s+"\n";

		return s;
	}
	
	public byte[][] getCANPacketBytes(Date createDate) {
		return PayloadUwExperiment.getCANPacketBytes(canPackets, createDate);
	}

}
