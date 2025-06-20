import java.io.*;
import java.util.*;

class Node {
    char ch;
    double freq;
    Node left, right;

    Node(char ch, double freq) {
        this.ch = ch;
        this.freq = freq;
        this.left = null;
        this.right = null;
    }
}

class Compare implements Comparator<Node> {
    public int compare(Node a, Node b) {
        return Double.compare(a.freq, b.freq);
    }
}

public class Huffman {

    static boolean checkStopSequence(DataInputStream in) throws IOException {
        in.mark(3);
        int first = in.read();
        int second = in.read();
        int third = in.read();

        if (in.available() == 0) return false;
        if (first == '@' && second == '#' && third == '$') return true;

        in.reset();
        return false;
    }

    static boolean getNextBit(DataInputStream in, int[] bitCount, int[] buffer) throws IOException {
        if (bitCount[0] == 8) {
            if (checkStopSequence(in)) {
                throw new RuntimeException("Stop sequence detected. Stopping tree creation.");
            }
            int newByte = in.read();
            if (newByte == -1) {
                throw new EOFException("Unexpected end of file while reading bits.");
            }
            buffer[0] = newByte;
            bitCount[0] = 0;
        }
        boolean value = ((buffer[0] >> (7 - bitCount[0])) & 1) == 1;
        bitCount[0]++;
        return value;
    }

    static char extractCharacter(DataInputStream in, int[] bitCount, int[] buffer) throws IOException {
        int c = 0;
        for (int i = 0; i < 8; i++) {
            c = (c << 1) | (getNextBit(in, bitCount, buffer) ? 1 : 0);
        }
        return (char) c;
    }

    static Node createTree(DataInputStream in, int[] bitCount, int[] buffer) throws IOException {
        boolean value = getNextBit(in, bitCount, buffer);
        if (value) {
            return new Node(extractCharacter(in, bitCount, buffer), 0);
        } else {
            Node root = new Node('\0', 0);
            root.left = createTree(in, bitCount, buffer);
            root.right = createTree(in, bitCount, buffer);
            return root;
        }
    }

    static void getHuffmanCode(Node root, String s, Map<Character, String> huffmanCodes) {
        if (root == null) return;

        if (root.left == null && root.right == null) {
            if (s.equals("")) s = "0";
            huffmanCodes.put(root.ch, s);
        }

        getHuffmanCode(root.left, s + '0', huffmanCodes);
        getHuffmanCode(root.right, s + '1', huffmanCodes);
    }

    static void encodeTree(Node root, DataOutputStream out, int[] buffer, int[] bitCount) throws IOException {
        if (bitCount[0] == 8) {
            out.write(buffer[0]);
            buffer[0] = 0;
            bitCount[0] = 0;
        }
        if (root == null) return;

        if (root.left == null && root.right == null) {
            buffer[0] = (buffer[0] << 1) | 1;
            bitCount[0]++;
            if (bitCount[0] == 8) {
                out.write(buffer[0]);
                buffer[0] = 0;
                bitCount[0] = 0;
            }
            for (int i = 7; i >= 0; i--) {
                buffer[0] = (buffer[0] << 1) | ((root.ch >> i) & 1);
                bitCount[0]++;
                if (bitCount[0] == 8) {
                    out.write(buffer[0]);
                    buffer[0] = 0;
                    bitCount[0] = 0;
                }
            }
            return;
        } else {
            buffer[0] = buffer[0] << 1;
            bitCount[0]++;
        }

        encodeTree(root.left, out, buffer, bitCount);
        encodeTree(root.right, out, buffer, bitCount);
    }

    static void encodeFile(String inputFilename) throws IOException {
        FileInputStream inFile = new FileInputStream(inputFilename);
        Map<Character, Integer> freq = new HashMap<>();
        int total = 0;
        int ch;
        while ((ch = inFile.read()) != -1) {
            freq.put((char) ch, freq.getOrDefault((char) ch, 0) + 1);
            total++;
        }
        inFile.close();

        PriorityQueue<Node> pq = new PriorityQueue<>(new Compare());
        for (Map.Entry<Character, Integer> entry : freq.entrySet()) {
            pq.add(new Node(entry.getKey(), (double) entry.getValue() / total));
        }

        while (pq.size() > 1) {
            Node left = pq.poll();
            Node right = pq.poll();
            Node parent = new Node('\0', left.freq + right.freq);
            parent.left = left;
            parent.right = right;
            pq.add(parent);
        }

        Node root = pq.peek();
        Map<Character, String> huffmanCodes = new HashMap<>();
        getHuffmanCode(root, "", huffmanCodes);

        FileInputStream original = new FileInputStream(inputFilename);
        DataOutputStream encoded = new DataOutputStream(new FileOutputStream("test.bin"));
        int[] buffer = new int[1];
        int[] bitCount = new int[1];

        encodeTree(root, encoded, buffer, bitCount);

        if (bitCount[0] > 0) {
            buffer[0] = buffer[0] << (8 - bitCount[0]);
            encoded.write(buffer[0]);
        }

        encoded.write('@');
        encoded.write('#');
        encoded.write('$');

        bitCount[0] = 0;
        buffer[0] = 0;

        int byteRead;
        while ((byteRead = original.read()) != -1) {
            String code = huffmanCodes.get((char) byteRead);
            for (char bit : code.toCharArray()) {
                buffer[0] = (buffer[0] << 1) | (bit - '0');
                bitCount[0]++;
                if (bitCount[0] == 8) {
                    encoded.write(buffer[0]);
                    buffer[0] = 0;
                    bitCount[0] = 0;
                }
            }
        }
        if (bitCount[0] > 0) {
            buffer[0] = buffer[0] << (8 - bitCount[0]);
            encoded.write(buffer[0]);
        }

        original.close();
        encoded.close();
    }

    static void decodeFile() {
        try {
            DataInputStream in = new DataInputStream(new FileInputStream("test.bin"));
            FileOutputStream out = new FileOutputStream("decoded.txt");

            int[] bitCount = new int[1];
            int[] buffer = new int[1];
            buffer[0] = in.read();

            Node root = createTree(in, bitCount, buffer);

            in.read(); // '@'
            in.read(); // '#'
            in.read(); // '$'

            int readByte;
            Node temp = root;
            while ((readByte = in.read()) != -1) {
                for (int i = 7; i >= 0; i--) {
                    temp = ((readByte & (1 << i)) != 0) ? temp.right : temp.left;
                    if (temp.left == null && temp.right == null) {
                        out.write(temp.ch);
                        temp = root;
                    }
                }
            }
            in.close();
            out.close();
        } catch (IOException e) {
            System.out.println("Error during decoding: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        encodeFile("big.txt");
        decodeFile();
    }
} 
