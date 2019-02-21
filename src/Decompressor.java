
//import statements:
import java.io.IOException;

// Decompressor stores data required for decompression and writes the decompressed file. 
public class Decompressor {

	// instance vars:
	private HuffTree myTree;
	// class constant:
	private static final int INTERNAL_NODE_VALUE = -1;

	// Constructor:
	public Decompressor() {
		// intialize the tree.
		myTree = new HuffTree();
	}

	// method determines if the file to decompress is actually compressed
	// reader: BitInputStream object connected to the compressed file.
	// returns true if the file was compressed (using Huffman), false otherwise.
	// pre: reader != null (handled by SimpleHuffProcessor)
	public boolean isCompressedFile(BitInputStream reader) throws IOException {
		boolean isCompressed = false;
		// get the value of the first BITS_PER_INT bits.
		int inBits = reader.readBits(IHuffConstants.BITS_PER_INT);
		// determine if the value is equal to the MAGIC_NUMBER.
		isCompressed = inBits == IHuffConstants.MAGIC_NUMBER;
		return isCompressed;
	}

	// method constructs the tree using the header data contained in the compressed
	// file.
	// reader: BitInputStream connected to the compressed file.
	// pre: reader != null (handled by SimpleHuffProcessor)
	public void constructTree(BitInputStream reader) throws IOException {
		// get the headerFormat.
		int headerFormat = reader.readBits(IHuffConstants.BITS_PER_INT);
		// check which headerFormat was used.
		if (headerFormat == IHuffConstants.STORE_COUNTS) {
			// call the appropriate helper method (constructTreeWithCounts())
			constructTreeWithCounts(reader);
		} else if (headerFormat == IHuffConstants.STORE_TREE) {
			// call the appropriate helper method (constructTreeWithCounts())
			constructTreeWithTreeRep(reader);
		}
	}

	// helper method:
	// constructs tree using the count format
	// reader: BitInputStream object connected to the compressed file.
	private void constructTreeWithCounts(BitInputStream reader) throws IOException {
		// create a priority queue,
		// custom priority queue used to break ties in fair manner.
		CustomPriorityQueue<TreeNode> q = new CustomPriorityQueue<>();
		// read the first ALPH_SIZE number of BITS_PER_INT
		for (int i = 0; i < IHuffConstants.ALPH_SIZE; i++) {
			// get the frequency of the chunk.
			int currentFrequency = reader.readBits(IHuffConstants.BITS_PER_INT);
			if (currentFrequency != 0) {
				// if the frequency was not zero, create a TreeNode with value = i
				// and frequency = currentFrequency.
				TreeNode toAdd = new TreeNode(i, currentFrequency);
				// add the node to the queue.
				q.enqueue(toAdd);
			}
		}
		// add a node representing the pseudoEOF chunk.
		q.enqueue(new TreeNode(IHuffConstants.PSEUDO_EOF, 1));
		// create the tree.
		myTree = new HuffTree(q);
	}

	// helper method:
	// constructs the tree using the tree format.
	// reader: BitInputStream object connected to the compressed file.
	private void constructTreeWithTreeRep(BitInputStream reader) throws IOException {
		// get the size of the tree.
		int sizeOfTree = reader.readBits(IHuffConstants.BITS_PER_INT);
		// variable to keep track of number of bits read.
		int count = 0;
		// StringBuilder used to make method more efficient.
		StringBuilder treeRepresentation = new StringBuilder();
		// loop reads sizeOfTree number of bits from the file.
		while (count < sizeOfTree) {
			// get the currentBit.
			int currentBit = reader.readBits(1);
			// append it to the StringBuilder.
			treeRepresentation.append(currentBit);
			count++;
		}
		// create the tree using the tree representation.
		myTree = new HuffTree(treeRepresentation.toString());
	}

	// method writes to the uncompressed file.
	// reader: BitInputStream connected to the compressed file.
	// writer: BitOutputStream connected to the decompressed file.
	// returns the number of bits written to the decompressed file.
	// pre: reader != null & writer != null (handled by SimpleHuffProcessor)
	public int decompress(BitInputStream reader, BitOutputStream writer) throws IOException {
		boolean done = false;
		boolean decompressSuccessful = true;
		int bitsWritten = 0;
		// set the current node of the tree to the root.
		myTree.startTraversal();
		// read the bits in the file.
		while (!done) {
			// get the current bit.
			int bit = reader.readBits(1);
			// if bit == -1, we have reached the end of the file.
			if (bit == -1) {
				// technically, the end of the file would never be reached if compressed
				// properly.
				decompressSuccessful = false;
				done = true;
			} else {
				// update the current node in the tree.
				// get the value held by the current node.
				int value = myTree.updateCurrent(bit);
				// ensure the current node is not an internal node.
				if (value != INTERNAL_NODE_VALUE) {
					// check if the pseudoEOF has been reached.
					if (value == IHuffConstants.PSEUDO_EOF) {
						// done reading the file.
						done = true;
					} else {
						// write the value as a BITS_PER_WORD chunk.
						writer.writeBits(IHuffConstants.BITS_PER_WORD, value);
						// update the number of bits written.
						bitsWritten += IHuffConstants.BITS_PER_WORD;
						// reset the current node of the tree to the root.
						myTree.resetTraversal();
					}
				}
			}
		}
		if (decompressSuccessful) {
			return bitsWritten;
		} else {
			return -1;
		}
	}

}
