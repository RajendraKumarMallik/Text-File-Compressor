import java.io.*;
import java.util.*;

class Node {
    char ch;
    double freq;
    Node left, right;

    Node(char c, double f) {
        this.ch = c;
        this.freq = f;
        this.left = null;
        this.right = null;
    }
}

class Compare implements Comparator<Node> {
    public int compare(Node a, Node b) {
        return Double.compare(a.freq, b.freq);
    }
}

public class Compress_file {

    public static void getHuffmanCode(Node root, String s, Map<Character, String> huffmanCodes) {
        if (root == null) return;

        if (root.left == null && root.right == null) {
            if (s.equals("")) s = "0";
            huffmanCodes.put(root.ch, s);
        }

        getHuffmanCode(root.left, s + '0', huffmanCodes);
        getHuffmanCode(root.right, s + '1', huffmanCodes);
    }

    public static void encodeHuffmanTree(Node root, DataOutputStream out, int[] buffer, int[] bitCount) throws IOException {
        if (root.left == null && root.right == null) {
            writeBit(out, buffer, bitCount, 1);
            writeByte(out, buffer, bitCount, (byte) root.ch);
            return;
        } else {
            writeBit(out, buffer, bitCount, 0);
        }
        encodeHuffmanTree(root.left, out, buffer, bitCount);
        encodeHuffmanTree(root.right, out, buffer, bitCount);
    }

    public static void writeBit(DataOutputStream out, int[] buffer, int[] bitCount, int bit) throws IOException {
        buffer[0] = (buffer[0] << 1) | bit;
        bitCount[0]++;
        if (bitCount[0] == 8) {
            out.write(buffer[0]);
            buffer[0] = 0;
            bitCount[0] = 0;
        }
    }

    public static void writeByte(DataOutputStream out, int[] buffer, int[] bitCount, byte b) throws IOException {
        for (int i = 7; i >= 0; i--) {
            writeBit(out, buffer, bitCount, (b >> i) & 1);
        }
    }

    public static void flushBits(DataOutputStream out, int[] buffer, int[] bitCount) throws IOException {
        if (bitCount[0] > 0) {
            buffer[0] <<= (8 - bitCount[0]);
            out.write(buffer[0]);
            buffer[0] = 0;
            bitCount[0] = 0;
        }
    }

    public static void encodeFile() throws IOException {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter file name to compress: ");
        String filename = sc.nextLine();

        File inputFile = new File(filename);
        if (!inputFile.exists()) {
            System.out.println("Error loading file.");
            return;
        }

        FileInputStream fis = new FileInputStream(inputFile);
        Map<Character, Integer> freq = new HashMap<>();
        int total = 0;
        int b;
        while ((b = fis.read()) != -1) {
            freq.put((char) b, freq.getOrDefault((char) b, 0) + 1);
            total++;
        }
        fis.close();

        PriorityQueue<Node> pq = new PriorityQueue<>(new Compare());
        for (Map.Entry<Character, Integer> entry : freq.entrySet()) {
            double prob = (double) entry.getValue() / total;
            pq.add(new Node(entry.getKey(), prob));
        }

        while (pq.size() > 1) {
            Node left = pq.poll();
            Node right = pq.poll();
            Node root = new Node('\0', left.freq + right.freq);
            root.left = left;
            root.right = right;
            pq.add(root);
        }

        Node root = pq.peek();
        Map<Character, String> huffmanCodes = new HashMap<>();
        getHuffmanCode(root, "", huffmanCodes);

        System.out.print("Enter encoded output filename: ");
        String encodedFile = sc.nextLine();
        DataOutputStream out = new DataOutputStream(new FileOutputStream(encodedFile));

        int[] buffer = new int[1];
        int[] bitCount = new int[1];

        encodeHuffmanTree(root, out, buffer, bitCount);
        flushBits(out, buffer, bitCount);

        out.write('@');
        out.write('#');
        out.write('$');

        fis = new FileInputStream(inputFile);
        while ((b = fis.read()) != -1) {
            String code = huffmanCodes.get((char) b);
            for (char c : code.toCharArray()) {
                writeBit(out, buffer, bitCount, c - '0');
            }
        }
        flushBits(out, buffer, bitCount);
        fis.close();
        out.close();
        sc.close();
    }

    public static boolean checkStopSequence(DataInputStream in) throws IOException {
        in.mark(3);
        int first = in.read();
        int second = in.read();
        int third = in.read();
        if (first == '@' && second == '#' && third == '$') return true;
        in.reset();
        return false;
    }

    public static boolean getNextBit(DataInputStream in, int[] bitCount, int[] buffer) throws IOException {
        if (bitCount[0] == 8) {
            if (checkStopSequence(in)) throw new RuntimeException("Stop sequence detected.");
            int byteRead = in.read();
            if (byteRead == -1) throw new EOFException("Unexpected end of file.");
            buffer[0] = byteRead;
            bitCount[0] = 0;
        }
        boolean val = ((buffer[0] >> (7 - bitCount[0])) & 1) == 1;
        bitCount[0]++;
        return val;
    }

    public static char extractCharacter(DataInputStream in, int[] bitCount, int[] buffer) throws IOException {
        int c = 0;
        for (int i = 0; i < 8; i++) {
            c = (c << 1) | (getNextBit(in, bitCount, buffer) ? 1 : 0);
        }
        return (char) c;
    }

    public static Node createTree(DataInputStream in, int[] bitCount, int[] buffer) throws IOException {
        boolean bit = getNextBit(in, bitCount, buffer);
        if (bit) {
            return new Node(extractCharacter(in, bitCount, buffer), 0);
        } else {
            Node node = new Node('\0', 0);
            node.left = createTree(in, bitCount, buffer);
            node.right = createTree(in, bitCount, buffer);
            return node;
        }
    }

    public static void decodeFile() throws IOException {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter file to decode: ");
        String inputFilename = sc.nextLine();
        System.out.print("Enter output file name: ");
        String outputFilename = sc.nextLine();

        DataInputStream in = new DataInputStream(new FileInputStream(inputFilename));
        FileOutputStream out = new FileOutputStream(outputFilename);

        int[] bitCount = new int[1];
        int[] buffer = new int[1];
        buffer[0] = in.read();

        Node root = createTree(in, bitCount, buffer);

        in.read(); // '@'
        in.read(); // '#'
        in.read(); // '$'

        Node temp = root;
        int readByte;
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
        sc.close();
    }

    public static void main(String[] args) {
        try {
            encodeFile();
            decodeFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
