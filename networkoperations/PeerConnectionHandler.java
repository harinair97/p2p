/*
package networkoperations;

import java.net.Socket;
import java.io.IOException;
import java.util.ArrayList;
import java.io.ObjectInputStream;
import java.net.UnknownHostException;
import java.io.ObjectOutputStream;
import java.util.ListIterator;

import nodeoperations.Handshake;
import nodeoperations.PeerConnection;
import nodeoperations.PeerProcess;
import nodeoperations.BitField;
import fileparsers.PeerInfoFileParse;

public class PeerConnectionHandler extends Thread {

    private static final String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ0000000000";

    private String hostAddress;
    private int connectionPort;
    private final int localPeerId;
    private final ArrayList<String[]> peerConnections = new ArrayList<>();
    private final int totalFileChunks;
    private final boolean isFileComplete;
    private final long totalFileSize;
    private final long chunkSize;

    public PeerConnectionHandler(int localPeerId, int totalFileChunks, boolean isFileComplete,long totalFileSize, long chunkSize) {
        this.localPeerId = localPeerId;
        this.totalFileChunks = totalFileChunks;
        this.isFileComplete = isFileComplete;
        this.totalFileSize = totalFileSize;
        this.chunkSize = chunkSize;
    }

    @Override
    public void run() {
        initializePeerConnections();
        establishConnections();
    }

    private void initializePeerConnections() {
        PeerInfoFileParse PeerInfoFileParse = new PeerInfoFileParse(localPeerId);
        peerConnections.addAll(PeerInfoFileParse.getPrecedingPeerDetails());
    }

    private void establishConnections() {
        ListIterator<String[]> connectionIterator = peerConnections.listIterator();

        while(connectionIterator.hasNext()) {
            String[] peerInfo = connectionIterator.next();
            hostAddress = peerInfo[1];
            connectionPort = Integer.parseInt(peerInfo[2]);

            try {
                Socket connectionSocket = new Socket(hostAddress, connectionPort);
                handlePeerConnection(connectionSocket, peerInfo);
            }
            catch (UnknownHostException e) {
                System.err.println("Host connection error: " + e);
            } catch (IOException e) {
                System.err.println("IO error during connection: " + e);
            }
        }
    }

    private void handlePeerConnection(Socket connectionSocket, String[] peerInfo) throws IOException {
        //Perform handshake
        Handshake handshakeMsg = new Handshake(localPeerId);
        sendHandshakeMessage(connectionSocket, handshakeMsg.handshake);

        byte[] receivedHandshake = receiveHandshakeMessage(connectionSocket);
        String receivedHeader = new String(receivedHandshake, 0, 28);
        String remotePeerId = new String(receivedHandshake, 28, 4);
        int remoteId = Integer.parseInt(remotePeerId);

        if(isValidHandshake(receivedHeader, remoteId)) {
            initializePeerCommunication(connectionSocket, peerInfo);
        } else {
            System.out.println("Unexpected peer connection");
            connectionSocket.close();
        }
    }

    private boolean isValidHandshake(String header, int remoteId) {
        if(!header.equals(HANDSHAKE_HEADER)) {
            return false;
        }

        for(Integer peerId : PeerProcess.allPeerIDs) {
            if(peerId != localPeerId && peerId == remoteId) { 
                return true;
            }
        }
        return false;
    }

    private void initializePeerCommunication(Socket connectionSocket, String[] peerInfo) {
        PeerConnection peerConnection = createPeerConnection(connectionSocket, peerInfo);

        //Exchange bitfields
        byte[] remoteBitfield = receiveBitfield(connectionSocket); 
        peerConnection.setPeerBitfield(remoteBitfield);
        sendBitfield(connectionSocket);
        System.out.println("The bit field exchange completed");

        registerPeerConnection(peerConnection);
        
        int remotePeerId = Integer.parseInt(peerInfo[0]);
        logConnection(remotePeerId);
        //startCommunicationThreads(remotePeerId);
    }

    private PeerConnection createPeerConnection(Socket socket, String[] peerInfo) {
        PeerConnection peerConnection = new PeerConnection(localPeerId, Integer.parseInt(peerInfo[0]), socket);
        return peerConnection;
    }

    private void registerPeerConnection(PeerConnection peerConnection) {
        synchronized (PeerProcess.peers) {
            PeerProcess.peers.add(peerConnection);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                System.err.println("Thread interruption: " + e);
            }
        }
    }



    private void logConnection(int remotePeerId) {
        System.out.println("Connection request sent to " + remotePeerId);
        System.out.println();
    }

//    private void startCommunicationThreads(int remotePeerId) {

//    }

    private void sendBitfield(Socket socket) {
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(BitField.bitfield);
        } catch (IOException e) {
            System.err.println("Error sending bitfield: " + e);
        }
    }

    private byte[] receiveBitfield(Socket socket) {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            return (byte[]) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error receiving bitfield: " + e);
            return null;
        }
    }

    private void sendHandshakeMessage(Socket socket, byte[] handshake) {
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(handshake);
        } catch (IOException e) {
            System.err.println("Error sending handshake: " + e);
        }
    }

    private byte[] receiveHandshakeMessage(Socket socket) {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            return (byte[]) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error receiving handshake: " + e);
            return null;
        }
    }
}*/


package networkoperations;

import java.net.Socket;
import java.io.IOException;
import java.util.ArrayList;
import java.io.ObjectInputStream;
import java.net.UnknownHostException;
import java.io.ObjectOutputStream;
import java.util.ListIterator;

import nodeoperations.Handshake;
import nodeoperations.PeerConnection;
import nodeoperations.PeerProcess;
import nodeoperations.BitField;
import fileparsers.PeerInfoFileParse;
import fileparsers.LogWritter;
import networkoperations.SendMessage;
import networkoperations.RecieveMessage;
import networkoperations.RequestPiece;

public class PeerConnectionHandler extends Thread {

    private static final String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ0000000000";

    private String hostAddress;
    private int connectionPort;
    private final int localPeerId;
    private final ArrayList<String[]> peerConnections = new ArrayList<>();
    private final int totalFileChunks;
    private final boolean isFileComplete;
    private final long totalFileSize;
    private final long chunkSize;

    public PeerConnectionHandler(int localPeerId, int totalFileChunks, boolean isFileComplete, long totalFileSize, long chunkSize) {
        this.localPeerId = localPeerId;
        this.totalFileChunks = totalFileChunks;
        this.isFileComplete = isFileComplete;
        this.totalFileSize = totalFileSize;
        this.chunkSize = chunkSize;
    }

    @Override
    public void run() {
        initializePeerConnections();
        establishConnections();
    }

    private void initializePeerConnections() {
        PeerInfoFileParse PeerInfoFileParse = new PeerInfoFileParse(localPeerId);
        peerConnections.addAll(PeerInfoFileParse.getPrecedingPeerDetails());
    }

    private void establishConnections() {
        ListIterator<String[]> connectionIterator = peerConnections.listIterator();

        while(connectionIterator.hasNext()) {
            String[] peerInfo = connectionIterator.next();
            hostAddress = peerInfo[1];
            connectionPort = Integer.parseInt(peerInfo[2]);

            try {
                Socket connectionSocket = new Socket(hostAddress, connectionPort);
                handlePeerConnection(connectionSocket, peerInfo);
            }
            catch (UnknownHostException e) {
                System.err.println("Host connection error: " + e);
            } catch (IOException e) {
                System.err.println("IO error during connection: " + e);
            }
        }
    }

    private void handlePeerConnection(Socket connectionSocket, String[] peerInfo) throws IOException {
        //Perform handshake
        Handshake handshakeMsg = new Handshake(localPeerId);
        sendHandshakeMessage(connectionSocket, handshakeMsg.handshake);

        byte[] receivedHandshake = receiveHandshakeMessage(connectionSocket);

        // Extract header and remote peer ID similar to the original implementation
        byte[] temp = new byte[28];
        System.arraycopy(receivedHandshake, 0, temp, 0, 28);
        String receivedHeader = new String(temp);

        byte[] idTemp = new byte[4];
        System.arraycopy(receivedHandshake, 28, idTemp, 0, 4);
        String remotePeerIdStr = new String(idTemp);
        int remoteId = Integer.parseInt(remotePeerIdStr);

        if(isValidHandshake(receivedHeader, remoteId)) {
            initializePeerCommunication(connectionSocket, peerInfo);
        } else {
            System.out.println("Unexpected peer connection");
            connectionSocket.close();
        }
    }

    private boolean isValidHandshake(String header, int remoteId) {
        if(!header.equals(HANDSHAKE_HEADER)) {
            return false;
        }

        for(Integer peerId : PeerProcess.allPeerIDs) {
            if(peerId != localPeerId && peerId == remoteId) {
                return true;
            }
        }
        return false;
    }

    private void initializePeerCommunication(Socket connectionSocket, String[] peerInfo) {
        PeerConnection peerConnection = createPeerConnection(connectionSocket, peerInfo);
        int remotePeerId = Integer.parseInt(peerInfo[0]);

        //Exchange bitfields
        byte[] remoteBitfield = receiveBitfield(connectionSocket);
        peerConnection.setPeerBitfield(remoteBitfield);
        sendBitfield(connectionSocket);

        registerPeerConnection(peerConnection);

        logConnection(remotePeerId);
        startCommunicationThreads(connectionSocket, remotePeerId);
    }

    private void startCommunicationThreads(Socket connectionSocket, int remotePeerId) {
        // Start sending messages thread
        SendMessage sendMessage = new SendMessage();
        sendMessage.start();

        // Start requesting pieces thread
        RequestPiece pieceRequest = new RequestPiece(remotePeerId, totalFileChunks, isFileComplete, totalFileSize, chunkSize);
        pieceRequest.start();

        // Start receiving messages thread
        RecieveMessage messageReceiver = new RecieveMessage(connectionSocket, chunkSize);
        messageReceiver.start();
    }

    private PeerConnection createPeerConnection(Socket socket, String[] peerInfo) {
        PeerConnection peerConnection = new PeerConnection(localPeerId, Integer.parseInt(peerInfo[0]), socket);
        return peerConnection;
    }

    private void registerPeerConnection(PeerConnection peerConnection) {
        synchronized (PeerProcess.peers) {
            PeerProcess.peers.add(peerConnection);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                System.err.println("Thread interruption: " + e);
            }
        }
    }

    private void logConnection(int remotePeerId) {
        System.out.println("Connection request sent to " + remotePeerId);
        System.out.println();
        LogWritter.makeTCPConnection(remotePeerId);
    }

    private void sendBitfield(Socket socket) {
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(BitField.bitfield);
        } catch (IOException e) {
            System.err.println("Error sending bitfield: " + e);
        }
    }

    private byte[] receiveBitfield(Socket socket) {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            return (byte[]) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error receiving bitfield: " + e);
            return null;
        }
    }

    private void sendHandshakeMessage(Socket socket, byte[] handshake) {
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(handshake);
        } catch (IOException e) {
            System.err.println("Error sending handshake: " + e);
        }
    }

    private byte[] receiveHandshakeMessage(Socket socket) {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            return (byte[]) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error receiving handshake: " + e);
            return null;
        }
    }
}