package se.uu.it.uno.openrqcli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.ArrayDataEncoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.SymbolType;
import net.fec.openrq.decoder.SourceBlockDecoder;
import net.fec.openrq.encoder.SourceBlockEncoder;
import net.fec.openrq.parameters.FECParameters;

public class Main {
	public static class PartialParameters {
		final String op;
		final int packetSize;
		final int nSourceBlocks;
		final long thirdArg;
		
		public PartialParameters(String[] args) throws IllegalArgumentException {
			if (args.length < 3 || args.length > 4) {
				throw new IllegalArgumentException("Incorrect number of arguments.");
			}
			
			op = args[0];
			packetSize = Integer.parseInt(args[1]);
			nSourceBlocks = Integer.parseInt(args[2]);
			if (args.length == 4) {
				thirdArg = Long.parseLong(args[3]);
			} else {
				thirdArg = -1;
			}
		}
	}
	
	private static byte[] readStdin() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[32*1024];
		int n;
		
		while ((n = System.in.read(buf)) > 0) {
			baos.write(buf, 0, n);
		}
		
		return baos.toByteArray();
	}
	
	public static void encode(PartialParameters pParams) throws IOException {
		byte[] data = readStdin();
		FECParameters params = FECParameters.newParameters(data.length, pParams.packetSize-8, pParams.nSourceBlocks);
		System.err.println("Encoder " + params);
		
		ArrayDataEncoder encoder = OpenRQ.newEncoder(data, params);
		ArrayList<Iterator<EncodingPacket>> pktIters = new ArrayList<Iterator<EncodingPacket>>(params.numberOfSourceBlocks());
		for (SourceBlockEncoder sbe : encoder.sourceBlockIterable()) {
			pktIters.add(sbe.newIterableBuilder().build().iterator());
		}

		int nOutput = 0;
		while (true) {
			for (Iterator<EncodingPacket> it : pktIters) {
				EncodingPacket pkt = it.next();
				if (pkt.symbolType() == SymbolType.SOURCE) {
					/* Skip all source packets. */
					continue;
				}
				byte[] pktData = pkt.asArray();
//				System.err.println(pkt.symbolType() + " " + pkt.sourceBlockNumber() + " " + pkt.encodingSymbolID() + " " + pktData.length);
				System.out.write(pktData);
				if (pktData.length != pParams.packetSize) {
					System.out.write(new byte[pParams.packetSize-pktData.length]);
				}
				
				nOutput++;
				
				if (pParams.thirdArg != -1 && nOutput >= pParams.thirdArg) {
					return;
				}
			}
		}
	}
	
	public static void decode(PartialParameters pParams) throws IOException {
		FECParameters params = FECParameters.newParameters(pParams.thirdArg, pParams.packetSize-8, pParams.nSourceBlocks);
		ArrayDataDecoder decoder = OpenRQ.newDecoderWithTwoOverhead(params);
		
		System.err.println("Decoder " + params);
		
		byte[] buf = new byte[pParams.packetSize];
		while (System.in.read(buf) == pParams.packetSize) {
			EncodingPacket pkt = decoder.parsePacket(buf, true).value();
			int sourceBlockNumber = pkt.sourceBlockNumber();
			SourceBlockDecoder sbd = decoder.sourceBlock(sourceBlockNumber);
			sbd.putEncodingPacket(pkt);
			if (decoder.isDataDecoded()) {
				System.err.println("Decoding succeeded.");
				break;
			}
		}
		
		if (decoder.isDataDecoded()) {
			System.out.write(decoder.dataArray());
			System.out.flush();
		} else {
			System.err.println("Decoding failed.");
		}
	}	

	public static void main(String[] argv) throws IOException {
		PartialParameters pParams = new PartialParameters(argv);
		if ("encode".equals(pParams.op)) {
			encode(pParams);
		} else if ("decode".equals(pParams.op)) {
			decode(pParams);
		} else {
			throw new IllegalArgumentException("Unknown operation: " + pParams.op);
		}
	}
}