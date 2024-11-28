package chessgameserver.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import chessgameserver.network.packets.GeneralPackets.ImageChunk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ImageChunkHandler extends Listener {
    private final Map<String, ChunkedFile> fileMap = new HashMap<>();

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof ImageChunk) {
            ImageChunk chunk = (ImageChunk) object;
            
            String folderPath = "uploaded-images";
            File folder = new File(folderPath);

            if (!folder.exists()) {
                folder.mkdirs(); 
            }

            fileMap.putIfAbsent(chunk.fileName, new ChunkedFile(chunk.totalChunks));
            ChunkedFile file = fileMap.get(chunk.fileName);
            file.addChunk(chunk.chunkIndex, chunk.imageData);

            if (file.isComplete()) {
                // Prepend the folder path to the file name
                File outputFile = new File(folder, chunk.fileName);
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(file.assemble());
                    System.out.println("File " + chunk.fileName + " reconstructed and saved to " + outputFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fileMap.remove(chunk.fileName);
            }
        }
    }


    private static class ChunkedFile {
        private final byte[][] chunks;
        private int receivedChunks = 0;

        public ChunkedFile(int totalChunks) {
            this.chunks = new byte[totalChunks][];
        }

        public void addChunk(int index, byte[] data) {
            if (chunks[index] == null) {
                chunks[index] = data;
                receivedChunks++;
            }
        }

        public boolean isComplete() {
            return receivedChunks == chunks.length;
        }

        public byte[] assemble() {
            int totalSize = 0;
            for (byte[] chunk : chunks) {
                totalSize += chunk.length;
            }

            byte[] fullData = new byte[totalSize];
            int offset = 0;
            for (byte[] chunk : chunks) {
                System.arraycopy(chunk, 0, fullData, offset, chunk.length);
                offset += chunk.length;
            }

            return fullData;
        }
    }
}
