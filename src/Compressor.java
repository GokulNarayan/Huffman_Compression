
//import statements.
import java.io.IOException;
import java.util.Map;

// Compressor stores all data required for compression and writes the compressed version to a new file. 
public class Compressor {

	// instance vars:
	// array to keep track of frequncies.
	private int[] valFrequencies;
	// HuffTree used to find codes, write header information.
	private HuffTree myTree;
	// map which will contain all the codes for each chunk of bits.
	private Map<Integer, String> codes;
	// variable stores which headerFormat we must write to the compressed file.
	private int headerFormat;
	// variable stores the size of the original file.
	private int originalSizeOfFile;

	// Constructor:
	// in: BitInputStream connected to the file to be compressed.
	// headerFormat: the format in which the header for the compressed file should
	// be written.
	// pre: in != null (handled by SimpleHuffProcessor)
	public Compressor(BitInputStream in, int headerFormat) throws IOException {
		// initialize the headerFormat.
		this.headerFormat = headerFormat;
		// initialize valFrequencies
		valFrequencies = new int[IHuffConstants.ALPH_SIZE];
		// find the frequencies of each BITS_PER_WORD chunk.
		findFrequencies(in);
		// construct the tree.
		myTree = constructHuffTree();
		// find all codes.
		codes = myTree.getCodes();
	}

	// compress method writes the actual compressed file.
	// writer: BitOutputStream connected to the compressed file.
	// reader: BitInputSTream connected to the original file.
	// pre: reader != null & writer != null (handled by SimpleHuffProcessor)
	public void compress(BitOutputStream writer, BitInputStream reader) throws IOException {
		// write the magic number to the compressed file.
		writer.writeBits(IHuffConstants.BITS_PER_INT, IHuffConstants.MAGIC_NUMBER);
		// determine which header must be written
		if (headerFormat == IHuffConstants.STORE_COUNTS) {
			// write the code for the header format. (STORE_COUNTS)
			writer.writeBits(IHuffConstants.BITS_PER_INT, IHuffConstants.STORE_COUNTS);
			// loop through all values in valFrequencies.
			for (int i = 0; i < IHuffConstants.ALPH_SIZE; i++) {
				// write the frequencies of each possible chunk.
				writer.writeBits(IHuffConstants.BITS_PER_INT, valFrequencies[i]);
			}
		} else {
			// write the code for the header format (STORE_TREE)
			writer.writeBits(IHuffConstants.BITS_PER_INT, IHuffConstants.STORE_TREE);
			// write the size of the tree.
			writer.writeBits(IHuffConstants.BITS_PER_INT, myTree.getTreeBitSize());
			// get the tree representation that must be written,
			String treeRepresentationHeader = myTree.getTreeRepresentaion();
			// loop iterates over every character in the tree representation.
			for (int i = 0; i < treeRepresentationHeader.length(); i++) {
				char current = treeRepresentationHeader.charAt(i);
				// write the appropriate bit based on the character.
				// '1' = 1, '0' = 0
				writer.writeBits(1, getBitToWrite(current));
			}
		}
		int inBits = 0;
		// read the original file once again.
		while ((inBits = reader.readBits(IHuffConstants.BITS_PER_WORD)) != -1) {
			// get the code based on the bits read by the reader.
			String newCode = codes.get(inBits);
			// loop iterates over every character of the code.
			for (int i = 0; i < newCode.length(); i++) {
				char current = newCode.charAt(i);
				// write the appropriate bit based on the character.
				// '1' = 1, '0' = 0
				writer.writeBits(1, getBitToWrite(current));
			}
		}
		// write the pseudoEOF character at the end of the compressed file.
		String pseudoEOF = codes.get(IHuffConstants.PSEUDO_EOF);
		for (int i = 0; i < pseudoEOF.length(); i++) {
			char current = pseudoEOF.charAt(i);
			writer.writeBits(1, getBitToWrite(current));
		}
	}

	// helper method to determine which bit value to write.
	// bit: char which stores '0' or '1'.
	private int getBitToWrite(char bit) {
		final char ZERO = '0';
		// return the appropriate int value.
		if (bit == ZERO) {
			return 0;
		} else {
			return 1;
		}
	}

	// method finds the frequencies of each chunk of bits in the original file.
	// in: BitInputStream object connected to the original file.
	// pre: in != null (handled by SimpleHuffProcessor)
	private void findFrequencies(BitInputStream in) throws IOException {
		int valueOfBits = 0;
		int numChunks = 0;
		// loop reads the entire file.
		while ((valueOfBits = in.readBits(IHuffConstants.BITS_PER_WORD)) != -1) {
			// increment the frequency of the chunk found.
			valFrequencies[valueOfBits]++;
			// increment numChunks read.
			numChunks++;
		}
		// calculate the original size of file.
		originalSizeOfFile = numChunks * IHuffConstants.BITS_PER_WORD;
	}

	// method constructs the HuffTree required by the Compressor.
	private HuffTree constructHuffTree() {
		// create a PriorityQueue
		// Custom Priority Queue used to break ties in a fair manner.
		CustomPriorityQueue<TreeNode> q = new CustomPriorityQueue<>();
		// loop through valFrequncies.
		for (int i = 0; i < valFrequencies.length; i++) {
			// if the frequency of the chunk is not zero:
			if (valFrequencies[i] != 0) {
				// create a TreeNode with value = i and frequency = valFrequencies[i]
				TreeNode addToQueue = new TreeNode(i, valFrequencies[i]);
				// add the node to the queue.
				q.enqueue(addToQueue);
			}
		}
		// create a TreeNode to represent the pseudoEOF chunk.
		TreeNode pseudoEOFNode = new TreeNode(IHuffConstants.PSEUDO_EOF, 1);
		// add the node to the queue.
		q.enqueue(pseudoEOFNode);
		// create and return the HuffTree.
		return new HuffTree(q);
	}

	// method returns the number of bits in the original file.
	public int getNumBitsInOriginalFile() {
		return originalSizeOfFile;
	}

	// method determines the number of bits that would be written to the compressed
	// file
	// pre: none
	// post: return the number of bits that would be written.
	public int getNumBitsWritten() {
		// variable stores the number of bits
		// magic number requires BITS_PER_INT number of bits.
		int output = IHuffConstants.BITS_PER_INT;
		// headerFormat requires BITS_PER_INT number of bits.
		output += IHuffConstants.BITS_PER_INT;
		// get the number of bits required by the actual header data.
		output += getHeaderDataSize();
		// loop through all the chunks that have a code assigned to it.
		for (int value : codes.keySet()) {
			// ensure the chunk doesn't represent the pseudoEOF value.
			if (value != IHuffConstants.PSEUDO_EOF) {
				// add the frequency of the chunk * the length of the code to output.
				output += valFrequencies[value] * codes.get(value).length();
			}
		}
		// add the number of bits required to write the pseudoEOF chunk.
		output += codes.get(IHuffConstants.PSEUDO_EOF).length();
		return output;
	}

	// helper method gets the size of the header data included in the compressed
	// file.
	// pre: none
	// post: returns the size (in bits) of the header data.
	private int getHeaderDataSize() {
		if (headerFormat == IHuffConstants.STORE_COUNTS) {
			// the bits required would be number of possible chunks * BITS_PER_INT.
			return IHuffConstants.ALPH_SIZE * IHuffConstants.BITS_PER_INT;
		} else {
			// the bits required would be the size of the tree representation +
			// BITS_PER_INT number of bits to write the size of the tree.
			return myTree.getTreeBitSize() + IHuffConstants.BITS_PER_INT;
		}
	}
}
