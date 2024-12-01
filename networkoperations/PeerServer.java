/*
package networkoperations;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.net.ServerSocket;
import java.io.IOException;

import nodeoperations.PeerConnection; 
import nodeoperations.Handshake; 
import nodeoperations.BitField; 
import nodeoperations.PeerProcess;


//Class will enable PeerConnection to function as a Server
public class PeerServer extends Thread{

    private static final String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ0000000000";
    private static final int HANDSHAKE_HEADER_LENGTH = 28;
    private static final int PEER_ID_LENGTH = 4;

    private int self_ID; 
    private int totalChunks; 
    private boolean allChunksPresent; 
    private int portno; 
    private int fileSize;
    private int chunkSize; 
    private int peerId;

    public PeerServer(int self_ID, int totalChunks, boolean allChunksPresent, int portno, long fileSize, long chunkSize){
        this.self_ID = self_ID;
        this.totalChunks = totalChunks;
        this.allChunksPresent = allChunksPresent;
        this.portno = portno;
        this.fileSize = (int) fileSize;
        this.chunkSize = (int) chunkSize;
    }

    //below method creates new thread upon .start() invocation from main process
    @Override
    public void run(){


        try (ServerSocket server = new ServerSocket(portno)) {
            //server socket created to listen for incoming connections on specified portno.

            //Server needs to keep accepting connections
            while(true){
                try{
                    Socket peerSocket = server.accept();
                    boolean canContinue = PerformHandshake(peerSocket);

                    if(canContinue)
                    {
        
                        PeerConnection p = new PeerConnection(self_ID, peerId, peerSocket); 

                        try{
                            exchangeBitfields(peerSocket,p);
                        } catch(IOException e){
                            System.err.println(e);
                        }

                        System.out.println("Finished exchange of Bit fields and Handshake");
                        PeerProcess.peers.add(p); 


                        startCommunicationThreads(peerId, peerSocket);
                    }
                    else{
                        System.out.println("Not a valid peer");
                    }
                }catch(IOException err){
                    System.err.println(err);
                }
            }
        }catch(IOException err){
            System.err.println(err);
        }
        
    }

    private boolean PerformHandshake(Socket peer){

        //Handshake Reception Logic
        ByteBuffer receivedHandshake  = null;

        try {
            ObjectInputStream in = new ObjectInputStream(peer.getInputStream());
            receivedHandshake  = ByteBuffer.wrap((byte[]) in.readObject());
        } catch (IOException e) {
            System.err.println(e);
        } catch (ClassNotFoundException err) {
            System.err.println(err);
        }

        //Handshake Sending
        try {
            ObjectOutputStream out = new ObjectOutputStream(peer.getOutputStream());
            out.writeObject(new Handshake(self_ID).handshake);
        } catch (IOException e) {
            System.err.println(e);
        }


        byte[] headerBytes = new byte[HANDSHAKE_HEADER_LENGTH];
        receivedHandshake.get(headerBytes, 0, HANDSHAKE_HEADER_LENGTH);
        String header = new String(headerBytes);

        byte[] peerIdBytes = new byte[PEER_ID_LENGTH];
        receivedHandshake.get(peerIdBytes, 0, PEER_ID_LENGTH);
        String peerIdStr = new String(peerIdBytes).trim();
        peerId = Integer.parseInt(peerIdStr);  

        boolean authorized = false;

        if(header.equals(HANDSHAKE_HEADER))
        {
            for (Integer currentId : PeerProcess.allPeerIDs) { 
                int curr_value = currentId.intValue();

                if(peerId == curr_value)
                {
                    authorized = true;
                    break;
                }
            }
        }

        return authorized;

    }
    private void startCommunicationThreads(int peer_ID, Socket socket) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.start();

        RequestPiece pieceReq = new RequestPiece(peer_ID, totalPieces, haveAllPieces, fileSize, pieceSize);
        pieceReq.start();

        RecieveMessage messageReceiver = new RecieveMessage(socket, pieceSize);
        messageReceiver.start();
    }

//    private void startCommunicationThreads(int peerId, Socket socket) {
//        //Add logic for messaging
//    }

    private void exchangeBitfields(Socket peerSocket, PeerConnection peer) throws IOException {
        ObjectOutputStream out = null;
        try{
            out = new ObjectOutputStream(peerSocket.getOutputStream());
            out.flush();
            // Server peer's bitfield
            out.writeObject(BitField.bitfield);
            // Receive client peer's bitfield
            ObjectInputStream in = null;
            in = new ObjectInputStream(peerSocket.getInputStream());
            peer.setPeerBitfield((byte[]) in.readObject());  
            peer.setPeerInterested(false);  
        }catch (Exception e) {
            throw new IOException("Invalid bitfield format", e);
        }     
    }  


}

*/

package networkoperations;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.net.ServerSocket;
import java.io.IOException;

import fileparsers.LogWritter;
import nodeoperations.PeerConnection;
import nodeoperations.Handshake;
import nodeoperations.BitField;
import nodeoperations.PeerProcess;

public class PeerServer extends Thread {

    private static final String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ0000000000";
    private static final int HANDSHAKE_HEADER_LENGTH = 28;
    private static final int PEER_ID_LENGTH = 4;

    private int self_ID;
    private int totalChunks;
    private boolean allChunksPresent;
    private int portno;
    private int fileSize;
    private int chunkSize;
    private int peerId;

    public PeerServer(int self_ID, int totalChunks, boolean allChunksPresent, int portno, long fileSize, long chunkSize) {
        this.self_ID = self_ID;
        this.totalChunks = totalChunks;
        this.allChunksPresent = allChunksPresent;
        this.portno = portno;
        this.fileSize = (int) fileSize;
        this.chunkSize = (int) chunkSize;
    }

    @Override
    public void run() {
        System.out.println("Starting PeerServer on port " + portno);

        try (ServerSocket server = new ServerSocket(portno)) {
            while (true) {
                try {
                    Socket peerSocket = server.accept();
                    System.out.println("Received connection request from " + peerSocket.getInetAddress());

                    boolean canContinue = performHandshake(peerSocket);

                    if (canContinue) {
                        PeerConnection p = new PeerConnection(self_ID, peerId, peerSocket);

                        try {
                            exchangeBitfields(peerSocket, p);
                            System.out.println("Bitfield exchange successful with peer " + peerId);
                        } catch (IOException e) {
                            System.err.println("Bitfield exchange failed: " + e.getMessage());
                            continue;
                        }

                        // Log TCP connection made

                        PeerProcess.peers.add(p);

                        startCommunicationThreads(peerId, peerSocket);
                    } else {
                        System.out.println("Unauthorized peer connection attempt from " + peerSocket.getInetAddress());
                        peerSocket.close();
                    }
                } catch (IOException err) {
                    System.err.println("Error processing peer connection: " + err.getMessage());
                }
            }
        } catch (IOException err) {
            System.err.println("Server socket creation failed: " + err.getMessage());
        }
    }

    private boolean performHandshake(Socket peer) {
        System.out.println("Performing handshake with peer");

        ByteBuffer receivedHandshake = null;

        try {
            ObjectInputStream in = new ObjectInputStream(peer.getInputStream());
            receivedHandshake = ByteBuffer.wrap((byte[]) in.readObject());
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Handshake reception error: " + e.getMessage());
            return false;
        }

        try {
            ObjectOutputStream out = new ObjectOutputStream(peer.getOutputStream());
            out.writeObject(new Handshake(self_ID).handshake);
        } catch (IOException e) {
            System.err.println("Handshake sending error: " + e.getMessage());
            return false;
        }

        byte[] headerBytes = new byte[HANDSHAKE_HEADER_LENGTH];
        receivedHandshake.get(headerBytes, 0, HANDSHAKE_HEADER_LENGTH);
        String header = new String(headerBytes);

        byte[] peerIdBytes = new byte[PEER_ID_LENGTH];
        receivedHandshake.get(peerIdBytes, 0, PEER_ID_LENGTH);
        String peerIdStr = new String(peerIdBytes).trim();
        peerId = Integer.parseInt(peerIdStr);

        boolean authorized = false;

        if (header.equals(HANDSHAKE_HEADER)) {
            for (Integer currentId : PeerProcess.allPeerIDs) {
                int curr_value = currentId.intValue();

                if (peerId == curr_value) {
                    authorized = true;
                    break;
                }
            }
        }

        return authorized;
    }

    private void startCommunicationThreads(int peer_ID, Socket socket) {
        System.out.println("Starting communication threads for peer " + peer_ID);

        // Sending thread
        SendMessage sendMessage = new SendMessage();
        sendMessage.start();

        // Piece request thread
        RequestPiece pieceReq = new RequestPiece(peer_ID, totalChunks, allChunksPresent, fileSize, chunkSize);
        pieceReq.start();

        // Message receive thread
        RecieveMessage messageReceiver = new RecieveMessage(socket, chunkSize);
        messageReceiver.start();
    }

    private void exchangeBitfields(Socket peerSocket, PeerConnection peer) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(peerSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(peerSocket.getInputStream())) {

            out.flush();
            // Server peer's bitfield
            out.writeObject(BitField.bitfield);

            // Receive client peer's bitfield
            peer.setPeerBitfield((byte[]) in.readObject());
            peer.setPeerInterested(false);

            System.out.println("Bitfield exchange completed successfully for peer " + peer.getRemotePeerId());
        } catch (ClassNotFoundException e) {
            System.err.println("Invalid bitfield format: " + e.getMessage());
            throw new IOException("Invalid bitfield format", e);
        }
    }
}