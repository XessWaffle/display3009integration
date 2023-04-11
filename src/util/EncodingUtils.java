package util;

import javafx.util.Pair;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class EncodingUtils {

    public static class Node {
        public int value;
        public int frequency;
        public Node left, right;

        public Node(int value, int frequency){
            this.value = value;
            this.frequency = frequency;
        }
        public int getFrequency(){
            return frequency;
        }
    }

    public static void WriteToFile(String name, HashMap<Integer, ByteBuffer> encoded) throws IOException{

        Path path = Paths.get(name);
        Files.deleteIfExists(path);

        Set<StandardOpenOption> options = new HashSet<>();
        options.add(StandardOpenOption.CREATE);
        options.add(StandardOpenOption.APPEND);
        FileChannel fileChannel = FileChannel.open(path, options);

        for(int i : encoded.keySet()) {
            fileChannel.write(encoded.get(i));
        }

        fileChannel.close();
    }


    public static void HuffmanEncode(HashMap<Integer, ArrayList<Integer>> samples, HashMap<Integer, ByteBuffer> encoded, int sectors){

        for(int i = 0; i < sectors; i++){

            PriorityQueue<Node> huffmanQueue = new PriorityQueue<>(sectors/2, Comparator.comparingInt(Node::getFrequency));
            HashMap<Integer, Integer> frequency = new HashMap<>();
            LinkedHashMap<Integer, Integer> runLength = new LinkedHashMap<>();

            ArrayList<Integer> curr = samples.get(i);

            int index = 0;
            int prevColor = 0;

            // Frequency Analysis/Run Length Encoding
            for(int color : curr){
                frequency.putIfAbsent(color, 0);
                frequency.put(color, frequency.get(color) + 1);

                if(index == 0 || prevColor != color){
                    runLength.put(color, index);
                }

                index++;
                prevColor = color;
            }

            // Huffman PQueue
            for(int color : frequency.keySet())
                huffmanQueue.add(new Node(color, frequency.get(color)));

            while (huffmanQueue.size() != 1) {
                Node left = huffmanQueue.poll();
                Node right = huffmanQueue.poll();

                Node next = new Node(0, left.frequency + right.frequency);
                next.left = left;
                next.right = right;

                huffmanQueue.add(next);
            }

            Node root = huffmanQueue.poll();

            // Huffman Encoding Extraction
            HashMap<Integer, Pair<Integer, Integer>> codes = HuffmanCodes(root);

            // Encoded message
            ByteBuffer codedBuffer = ByteBuffer.allocate(samples.get(i).size() * 12);

            byte toPush = 0;
            int position = 0;

            HuffmanTreeEncode(root, codedBuffer);

            for(int color : runLength.keySet()){
                Pair<Integer, Integer> code = codes.get(color);
                int trueCode = code.getKey();

                for(int j = 0; j < code.getValue(); j++){
                    toPush |= trueCode & 1;
                    toPush <<= 1;
                    trueCode >>= 1;
                    position++;

                    if(position == 8) {
                        codedBuffer.put(toPush);
                        position = 0;
                        toPush = 0;
                    }
                }
            }

            if(position != 0){
                codedBuffer.put(toPush);
            }

            for(int color : runLength.keySet()){
                codedBuffer.put((byte)(runLength.get(color) & 0xFF));
            }

            ByteBuffer trueBuffer = ByteBuffer.allocate(codedBuffer.position());
            trueBuffer.put(0, codedBuffer, 0, codedBuffer.position());

            encoded.put(i, trueBuffer);
        }


    }

    private static void HuffmanTreeEncode(Node root, ByteBuffer codedBuffer) {
        Node positionTracker = new Node(0, 0);
        HuffmanEncodingHelper(root, positionTracker, codedBuffer);

        if(positionTracker.frequency != 0){
            while(positionTracker.frequency < 32){
                positionTracker.value <<= 1;
                positionTracker.frequency++;
            }
            codedBuffer.putInt(positionTracker.value);
        }

    }

    private static void HuffmanEncodingHelper(Node root, Node pt, ByteBuffer codedBuffer){
        if(root.left == null && root.right == null){
            pt.value <<= 1;
            pt.frequency++;
            pt.value += 1;
            if(pt.frequency >= 32){
                codedBuffer.putInt(pt.value);
                pt.value = 0;
                pt.frequency = 0;
            }

            for(int i = 23; i >= 0; i--){
                pt.value <<= 1;
                pt.frequency++;
                pt.value += root.value >> i & 0x1;
                if(pt.frequency >= 32){
                    codedBuffer.putInt(pt.value);
                    pt.value = 0;
                    pt.frequency = 0;
                }
            }
        }

        if(root.left != null){
            pt.value <<= 1;
            pt.frequency += 1;
            if(pt.frequency >= 32){
                codedBuffer.putInt(pt.value);
                pt.value = 0;
                pt.frequency = 0;
            }
            HuffmanEncodingHelper(root.left, pt, codedBuffer);
        }

        if(root.right != null){
            pt.value <<= 1;
            pt.frequency += 1;
            if(pt.frequency >= 32){
                codedBuffer.putInt(pt.value);
                pt.value = 0;
                pt.frequency = 0;
            }
            HuffmanEncodingHelper(root.right, pt, codedBuffer);
        }

    }

    private static HashMap<Integer, Pair<Integer, Integer>> HuffmanCodes(Node root){
        HashMap<Integer, Pair<Integer, Integer>> codes = new HashMap<>();
        HuffmanCodesHelper(root, 0, 0, codes);
        return codes;
    }

    private static void HuffmanCodesHelper(Node root, int code, int length, HashMap<Integer, Pair<Integer, Integer>> codes){
        if(root.left != null){
            code <<= 1;
            length += 1;
            HuffmanCodesHelper(root.left, code, length, codes);
        }

        if(root.right != null){
            code <<= 1;
            code += 1;
            length += 1;
            HuffmanCodesHelper(root.right, code, length, codes);
        }

        if(root.left == null && root.right == null){
            codes.put(root.value, new Pair<>(code, length));
            System.out.println(String.format("%x %s %d", root.value, Integer.toBinaryString(code), length));
        }
    }
}
