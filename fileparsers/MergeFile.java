package fileparsers;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import nodeoperations.PeerProcess;
import nodeoperations.Piece;

public class MergeFile {

    /**
     * Reassemble file pieces into a complete file
     *
     * Changes:
     * - Renamed parameters to be more descriptive (totalPieces -> pieceCount, PeerID -> peerId)
     * - Simplified path construction logic
     * - Improved error handling and logging
     *
     * @param pieceCount Total number of file pieces
     * @param peerId Peer identifier
     * @param totalFileSize Total file size
     * @param standardPieceSize Size of each piece
     */
    public void reassemble(int pieceCount, int peerId, long totalFileSize, long standardPieceSize) {
        // CHANGE: Use Path for more robust path handling
        Path outputDirectory = Paths.get(System.getProperty("user.dir"))
                .getParent()
                .resolve("peer_" + peerId);

        try {
            // CHANGE: Use Files.createDirectories for safer directory creation
            Files.createDirectories(outputDirectory);

            // CHANGE: Simplified file path resolution
            Path outputFilePath = outputDirectory.resolve(PeerProcess.targetFileName);

            try (FileOutputStream outputStream = new FileOutputStream(outputFilePath.toFile())) {
                // Process all pieces except the last one
                for (int currentPieceIndex = 1; currentPieceIndex < pieceCount; currentPieceIndex++) {
                    writePieceToFile(outputStream, currentPieceIndex);
                }

                // Process the last piece separately
                writeLastPiece(outputStream, pieceCount, totalFileSize, standardPieceSize);
            }
        } catch (IOException e) {
            // CHANGE: More informative error logging
            System.err.println("File reassembly failed: " + e.getMessage());
        }
    }

    /**
     * Write a standard piece to the output file
     *
     * Changes:
     * - Renamed variables for clarity
     * - Extracted piece size calculation into a more readable method
     *
     * @param outputStream Output file stream
     * @param pieceIndex Index of the piece to write
     * @throws IOException if writing fails
     */
    private void writePieceToFile(FileOutputStream outputStream, int pieceIndex) throws IOException {
        // CHANGE: More explicit variable name for the piece
        Piece currentPiece = PeerProcess.chunkIndexPieceMap.get(pieceIndex);

        // CHANGE: Separated piece size extraction into a clear method
        int actualPieceDataSize = extractPieceDataSize(currentPiece.piece);

        // CHANGE: Extracted data extraction to a utility method
        byte[] pieceData = extractPieceData(currentPiece.piece, actualPieceDataSize);

        outputStream.write(pieceData);
    }

    /**
     * Write the last piece to the output file
     *
     * Changes:
     * - Improved variable names
     * - Simplified logic
     *
     * @param outputStream Output file stream
     * @param totalPieceCount Total number of pieces
     * @param totalFileSize Total file size
     * @param standardPieceSize Original piece size
     * @throws IOException if writing fails
     */
    private void writeLastPiece(FileOutputStream outputStream, int totalPieceCount,
                                long totalFileSize, long standardPieceSize) throws IOException {
        // CHANGE: More explicit variable name
        Piece finalPiece = PeerProcess.chunkIndexPieceMap.get(totalPieceCount);

        // CHANGE: Simplified last piece size calculation
        int lastPieceSize = (int) (totalFileSize % standardPieceSize);

        // CHANGE: Extracted data extraction to a utility method
        byte[] lastPieceData = extractPieceData(finalPiece.piece, lastPieceSize);

        outputStream.write(lastPieceData);
    }

    /**
     * Extract piece data size from the first 4 bytes
     *
     * @param pieceBytes Piece byte array
     * @return Actual piece data size
     */
    private int extractPieceDataSize(byte[] pieceBytes) {
        // CHANGE: Extracted 4-byte size extraction into a separate method
        byte[] sizeBytes = new byte[4];
        System.arraycopy(pieceBytes, 0, sizeBytes, 0, 4);
        return ByteBuffer.wrap(sizeBytes).getInt() - 4;
    }

    /**
     * Extract piece data from byte array
     *
     * @param pieceBytes Source piece byte array
     * @param dataSize Size of data to extract
     * @return Extracted piece data
     */
    private byte[] extractPieceData(byte[] pieceBytes, int dataSize) {
        // CHANGE: Simplified data extraction
        byte[] extractedData = new byte[dataSize];
        System.arraycopy(pieceBytes, 9, extractedData, 0, dataSize);
        return extractedData;
    }
}