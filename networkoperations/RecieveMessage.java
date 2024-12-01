package networkoperations;

import java.io.ObjectInputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ListIterator;

import nodeoperations.HasCompleteFile;
import nodeoperations.MessageBody;
import nodeoperations.PeerConnection;
import nodeoperations.PeerProcess;
import nodeoperations.BitField;
import nodeoperations.Have;
import nodeoperations.Piece;
import fileparsers.LogWritter;

public class RecieveMessage extends Thread {

	private Socket socket;
	private int remotePeerID;
	private long pieceSize;
	
	public RecieveMessage(Socket socket, long pieceSize) {
		this.socket = socket;
		this.pieceSize = pieceSize;
		
		ListIterator<PeerConnection> it = PeerProcess.peers.listIterator();

		while(it.hasNext()) {
			PeerConnection p = (PeerConnection) it.next();
			
			if(p.getConnectionSocket().equals(socket)) {
				remotePeerID = p.getRemotePeerId();
			}
		}

	}
	
	@Override
	public void run() {
		
		while(true) {
			
			byte[] message = receiveMessage();
			int type = message[4];
			
			if(type == 0) {
				//choke
			}
			
			else if(type == 1) {
				//unchoke
			}
			
			else if(type == 2) {
				//interested
				
				System.out.println("Interested message received from " + remotePeerID);
				System.out.println();
				LogWritter.receiveInterested(remotePeerID);
			}
			
			else if(type == 3) {
				//not interested
				
				System.out.println("Not Interested message received from " + remotePeerID);
				System.out.println();
				LogWritter.receiveNotInterested(remotePeerID);
			}
			
			else if(type == 4) {
				//have
				
				byte[] temp = new byte[4];
				
				int x = 5;
				for (int i = 0; i < temp.length; i++) {
					temp[i] = message[x];
					x++;
				}
				
				int pieceNum = ByteBuffer.wrap(temp).getInt();
				
				ListIterator<PeerConnection> it = PeerProcess.peers.listIterator();

				while(it.hasNext()) {
					PeerConnection p = (PeerConnection) it.next();

					if(p.getConnectionSocket().equals(socket)) {
						byte[] field = p.getPeerBitfield();

						try {
							synchronized(field) {
								field = updateBitField(field, pieceNum);
								p.setPeerBitfield(field);
							}
						} catch (Exception e) {
							System.err.println(e);
						}
					}
				}

				System.out.println("Have message received from " + remotePeerID + " for piece " + pieceNum);
				System.out.println();
				LogWritter.receiveHave(remotePeerID, pieceNum);
			}
			
			else if(type == 6) {
				//request
				
				byte[] temp = new byte[4];
				
				int x = 5;
				for (int i = 0; i < temp.length; i++) {
					temp[i] = message[x];
					x++;
				}
				int pieceNum = ByteBuffer.wrap(temp).getInt();
				Integer i = Integer.valueOf(pieceNum);
				
				//send piece
				Piece piece = PeerProcess.chunkIndexPieceMap.get(i);
				
				//debugging start
				System.out.println("piece " + pieceNum + " requested from " + remotePeerID);
				System.out.println();
				//debugging end
				
				synchronized (PeerProcess.messageBody) {
					MessageBody m = new MessageBody();
					m.setSocket(socket);
					m.setMessage(piece.piece);
					PeerProcess.messageBody.add(m);
				}
			}
			
			else if(type == 7) {
				//piece
				
				byte index[] = new byte[4];
				
				int x = 5;
				for (int i = 0; i < index.length; i++) {
					index[i] = message[x];
					x++;
				}
				int pieceIndex = ByteBuffer.wrap(index).getInt();
				Integer num = Integer.valueOf(pieceIndex);
				byte[] piece = new byte[message.length - 9];
				for (int i = 0; i < piece.length; i++) {
					piece[i] = message[x];
					x++;
				}
			
				if(piece.length == pieceSize && !PeerProcess.chunkIndexPieceMap.containsKey(num)) {
					
					Piece p1 = new Piece(pieceIndex, piece);
					
					try {
						synchronized(PeerProcess.chunkIndexPieceMap) {
							PeerProcess.chunkIndexPieceMap.put(num, p1);
							Thread.sleep(30);
						}
					} catch (Exception e) {
						System.err.println(e);
					}
					
					
					System.out.println("piece " + pieceIndex + " received from " + remotePeerID);
					System.out.println();
					LogWritter.downloadPiece(remotePeerID, pieceIndex);

					
					try {
						synchronized(BitField.bitfield) {					
							BitField.modifyBitField(pieceIndex);
							Thread.sleep(20);
						}
					} 
					catch (InterruptedException e) {
						System.err.println(e);
					}
					
					//send have to all peers
					Have have = new Have(pieceIndex);
					
					ListIterator<PeerConnection> it = PeerProcess.peers.listIterator();
					
					while(it.hasNext()) {
						PeerConnection peer = (PeerConnection) it.next();
						//Socket s = peer.getSocket();
						
						synchronized (PeerProcess.messageBody) {
							MessageBody m = new MessageBody();
							m.setSocket(peer.getConnectionSocket());
							m.setMessage(have.have);
							PeerProcess.messageBody.add(m);
						}
					}
				}
				
			}
			
			else if(type == 8) {
				
				synchronized(PeerProcess.hasDownloadedCompleteFile) {
					
					ListIterator<HasCompleteFile> it = PeerProcess.hasDownloadedCompleteFile.listIterator();
					
					while(it.hasNext()) {
						HasCompleteFile peer = (HasCompleteFile)it.next();
						
						if(peer.getSocket().equals(socket)) {
							peer.setHasDownLoadedCompleteFile(true);
							break;
						}
					}
					
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						System.err.println(e);
					}
				}
			}
		}
	}
	
	private byte[] receiveMessage() {
		
		byte[] message = null;
		try {
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
			message = (byte[]) in.readObject();
		} 

		catch (Exception e) {
			System.exit(0);
		}
		return message;
	}
	
	public byte[] updateBitField(byte[] field, int pieceIndex) {
		int i = (pieceIndex - 1) / 8;
		int k = 7 - ((pieceIndex - 1) % 8);
		field[i + 5] = (byte) (field[i + 5] | (1<<k));
		return field;
	}
}
