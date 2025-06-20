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

public class Decompress {

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

    static void decodeFile() {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter file to be decoded:");
            String inputFilename = scanner.nextLine();
            System.out.println("Enter decoded file name:");
            String outputFilename = scanner.nextLine();

            DataInputStream in = new DataInputStream(new FileInputStream(inputFilename));
            FileOutputStream out = new FileOutputStream(outputFilename);

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

    public static void main(String[] args) {
        decodeFile();
    }
}
