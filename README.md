# Lossless Text Compression & Decompression

## 🔹 Overview
This project implements a **lossless text compression and decompression** algorithm using **Huffman coding**. It encodes files into a compressed binary format with an embedded Huffman tree, allowing accurate reconstruction of the original data.

## 🚀 Features
- **Lossless Compression & Decompression**  
  - Uses Huffman coding to efficiently reduce file size while ensuring perfect reconstruction.  
- **Binary Encoding Tree Reconstruction**  
  - Stores the Huffman tree within the compressed file for accurate decoding.  
- **High Compression Efficiency**  
  - Achieves significant file size reduction (e.g., **6337 KB → 3597 KB**), optimizing storage and transmission.

## 🛠️ Technologies Used
- **Java**  
- **Filesystem Manipulation**  

## 📂 How It Works
1. **Compression**  
   - Reads the input text file.  
   - Builds a frequency table for characters.  
   - Constructs a Huffman tree and generates binary codes.  
   - Encodes the file into a compressed binary format, embedding the Huffman tree.  

2. **Decompression**  
   - Reads the compressed file.  
   - Reconstructs the Huffman tree from the stored metadata.  
   - Decodes the binary data back into the original text.

## 📜 Usage
### Compilation
```sh
g++ -o compressor compressor.java
g++ -o decompressor decompressor.java
```
📺 Demo Video

Link:- 








🔗 Related Links
Huffman Coding - [Wikipedia](https://en.wikipedia.org/wiki/Huffman_coding)
