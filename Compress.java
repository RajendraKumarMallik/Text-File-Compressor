import java.io.*;
import java.nio.file.*;
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

public class Compress {
    
    static void getHuffmanCode(Node root, String s, Map<Character, String> huffmanCodes) {
        if (root == null) return;

        if (root.left == null && root.right == null) {
            if (s.isEmpty()) s = "0";
            huffmanCodes.put(root.ch, s);
        }

        getHuffmanCode(root.left, s + '0', huffmanCodes);
        getHuffmanCode(root.right, s + '1', huffmanCodes);
    }

    static void encodeHuffmanTree(Node root, DataOutputStream out, BitSetWrapper bitset) throws IOException {
        if (root == null) return;

        if (root.left == null && root.right == null) {
            bitset.writeBit(1);
            for (int i = 7; i >= 0; i--) {
                bitset.writeBit((root.ch >> i) & 1);
            }
            return;
        } else {
            bitset.writeBit(0);
        }

        encodeHuffmanTree(root.left, out, bitset);
        encodeHuffmanTree(root.right, out, bitset);
    }

    static void encodeFile() {
        try {
            Path currentPath = Paths.get("").toAbsolutePath();
            System.out.println("Current Directory: " + currentPath);

            Files.list(currentPath)
                .filter(Files::isRegularFile)
                .forEach(p -> System.out.println("  " + p.getFileName()));

            Scanner scanner = new Scanner(System.in);
            System.out.print("\nEnter file name to compress: ");
            String filename = scanner.nextLine();

            File file = new File(filename);
            if (!file.exists()) {
                System.out.println("Error in loading file");
                return;
            }

            Map<Character, Integer> freq = new HashMap<>();
            int totalCount = 0;

            try (FileInputStream inFile = new FileInputStream(file)) {
                int ch;
                while ((ch = inFile.read()) != -1) {
                    freq.put((char) ch, freq.getOrDefault((char) ch, 0) + 1);
                    totalCount++;
                }
            }

            PriorityQueue<Node> pq = new PriorityQueue<>(new Compare());
            for (Map.Entry<Character, Integer> entry : freq.entrySet()) {
                pq.add(new Node(entry.getKey(), (double) entry.getValue() / totalCount));
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
            Map<Character, String> huffmanCode = new HashMap<>();
            getHuffmanCode(root, "", huffmanCode);

            System.out.print("Enter Encoded file name: ");
            String encodedFile = scanner.nextLine();

            try (FileInputStream inFile = new FileInputStream(file);
                 DataOutputStream outFile = new DataOutputStream(new FileOutputStream(encodedFile))) {

                BitSetWrapper bitset = new BitSetWrapper(outFile);
                encodeHuffmanTree(root, outFile, bitset);
                bitset.flushRemaining();

                outFile.writeByte('@');
                outFile.writeByte('#');
                outFile.writeByte('$');

                int ch;
                while ((ch = inFile.read()) != -1) {
                    String code = huffmanCode.get((char) ch);
                    for (char bc : code.toCharArray()) {
                        bitset.writeBit(bc - '0');
                    }
                }
                bitset.flushRemaining();
            }
            scanner.close();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        encodeFile();
    }
}

class BitSetWrapper {
    private int buffer = 0;
    private int bitCount = 0;
    private final DataOutputStream out;

    public BitSetWrapper(DataOutputStream out) {
        this.out = out;
    }

    public void writeBit(int bit) throws IOException {
        buffer = (buffer << 1) | bit;
        bitCount++;
        if (bitCount == 8) {
            out.writeByte(buffer);
            buffer = 0;
            bitCount = 0;
        }
    }

    public void flushRemaining() throws IOException {
        if (bitCount > 0) {
            buffer <<= (8 - bitCount);
            out.writeByte(buffer);
            buffer = 0;
            bitCount = 0;
        }
    }
}
