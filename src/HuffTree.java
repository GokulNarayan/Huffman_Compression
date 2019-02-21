
//import statments:
import java.util.HashMap;
import java.util.Map;

// represents the Huffman tree used for compression and decompression. 
public class HuffTree {

	// instance vars:
	private TreeNode root;
	private TreeNode current;
	private String treeRepresentation;
	// class constants:
	private static final int INTERNAL_NODE_VALUE = -1;
	private static final char ZERO = '0';
	private static final char ONE = '1';

	// constructor:
	// "default" constructor
	public HuffTree() {
		root = null;
		current = null;
		treeRepresentation = "";
	}

	// Constructor:
	// data: CustomPriorityQueue containing all the nodes from which the tree is to
	// pre: data!=null
	// be constructed.
	public HuffTree(CustomPriorityQueue<TreeNode> data) {
		// check precon:
		if (data == null) {
			throw new IllegalArgumentException("data can't be null");
		}
		// loop runs until the queue has 2 or more elements.
		while (data.size() >= 2) {
			// leftSubTree = first element in queue, rightSubTree = second element in queue.
			TreeNode leftSubTree = data.dequeue();
			TreeNode rightSubTree = data.dequeue();
			// create a new TreeNode with the leftSubTree, rightSubTree and internal node
			// value.
			TreeNode toInsert = new TreeNode(leftSubTree, INTERNAL_NODE_VALUE, rightSubTree);
			// add the new node to the queue.
			data.enqueue(toInsert);
		}
		// the root of the tree is the last remaining node.
		root = data.dequeue();
		// get the treeRepresentation.
		treeRepresentation = createTreeRepresentation(root);
	}

	// Constructor
	// tree: String form of the tree (tree format header data)
	// pre: none
	// post: the tree is rebuilt.
	// rebuilds the tree from the tree format.
	public HuffTree(String tree) {
		// set the treeRepresentation to the param.
		treeRepresentation = tree;
		// A mutable string is used to reconstruct the tree.
		MutableString treeRep = new MutableString(treeRepresentation);
		// set root to the TreeNode returned by the helper method.
		root = constructTree(treeRep);
	}

	// helper method which creates the tree from the tree representation.
	// treeRepresentation: MutableString version of the tree.
	private TreeNode constructTree(MutableString treeRepresentation) {
		final int RADIX = 2;
		// create a new TreeNode set to null.
		TreeNode n = null;
		// check if the treeRepresenation has anymore characters.
		if (treeRepresentation.getString().length() > 0) {
			// check if the first char in the string is 1 or 0.
			if (treeRepresentation.getString().charAt(0) == ONE) {
				// if the char was one, the next BITS_PER_WORD + 1 chars represent a value.
				String binaryOfValue = treeRepresentation.getString().substring(1,
						1 + (IHuffConstants.BITS_PER_WORD + 1));
				// get the integer value from the binary representation.
				int value = Integer.valueOf(binaryOfValue, RADIX);
				// create a new TreeNode with the value and frequency set to 0.
				n = new TreeNode(value, 0);
				// update the treeRepresentation
				treeRepresentation
						.setString(treeRepresentation.getString().substring(1 + (IHuffConstants.BITS_PER_WORD + 1)));
			} else {
				// create a new node with internal node value and frequency set to 0.
				n = new TreeNode(INTERNAL_NODE_VALUE, 0);
				// get the remaining portion of the tree representation.
				String remainingTree = treeRepresentation.getString().substring(1);
				// update the tree representation.
				treeRepresentation.setString(remainingTree);
				// update the left and right subtree.
				n.setLeft(constructTree(treeRepresentation));
				n.setRight(constructTree(treeRepresentation));
			}
		}
		// return the node.
		return n;
	}

	// method used to start traversals of the tree (used for decompression)
	// pre: none
	// post: the current node is the root of the tree.
	public void startTraversal() {
		current = root;
	}

	// method used to update the current node of the tree.
	// dir = the direction in which the tree must be traversed.
	// pre: dir == 1 or 0 and current must not equal null.
	// returns the value held by the current node.
	public int updateCurrent(int dir) {
		// check precon:
		if ((dir != 1 && dir != 0) || current == null || (current.getLeft() == null && current.getRight() == null)) {
			throw new IllegalStateException("invalid direction or current is equal to null or current is a leaf");
		}

		// if dir = 1, move to the right subtree
		// if dir = 0, move to the left subtree.
		if (dir == 1) {
			current = current.getRight();
		} else {
			current = current.getLeft();
		}
		// return the value of current.
		return current.getValue();
	}

	// method used to reset traversal.
	// pre: none.
	// post: current = root.
	public void resetTraversal() {
		// set current back to root.
		current = root;
	}

	// Mutable representation of the String.
	private static class MutableString {
		// instance vars
		private String myString;

		// constructor.
		// sets myString to input.
		public MutableString(String input) {
			myString = input;
		}

		// method returns myString
		// pre: none
		// post: myString's value is returned.
		public String getString() {
			return myString;
		}

		// method sets myString to a new value.
		// pre: none
		// post: myString's value is altered.
		public void setString(String newString) {
			myString = newString;
		}
	}

	// method finds the codes for each chunk value.
	// pre: none
	// post: map containing the codes is returned.
	public Map<Integer, String> getCodes() {
		final String EMPTY_STRING = "";
		// create the map.
		Map<Integer, String> codes = new HashMap<>();
		// call recursive helper method to populate the map.
		findTreeCodings(root, EMPTY_STRING, codes);
		return codes;
	}

	// recursive helper method
	// n = current node
	// path = current path to get to n.
	// codeMap = map to populate.
	private void findTreeCodings(TreeNode n, String path, Map<Integer, String> codeMap) {
		// base case: if a leaf is reached, put the value and the path into the map.
		if (n.isLeaf()) {
			codeMap.put(n.getValue(), path);
		} else {
			// move to the node on the left and update path (+"0")
			findTreeCodings(n.getLeft(), path + ZERO, codeMap);
			// move to the node on the right and update path (+"1")
			findTreeCodings(n.getRight(), path + ONE, codeMap);
		}
	}

	// method creates the string representation of the tree (tree header format).
	// n = current node
	// pre: none
	// post: the entire string representation is returned.
	private String createTreeRepresentation(TreeNode n) {
		// create string builder to make method more efficient
		StringBuilder output = new StringBuilder();
		// check if n is a leaf.
		if (n.isLeaf()) {
			// add '1' to the representation.
			output.append(ONE);
			// get the binary representation of the value
			String binaryFormOfValue = Integer.toBinaryString(n.getValue());
			// BITS_PER_WORD +1 bits must be used to represent the value
			// adjust the binary form to add leading zeroes.
			String adjuster = getBitsAdjuster(binaryFormOfValue.length());
			binaryFormOfValue = adjuster + binaryFormOfValue;
			// add the binary form of the value to the representation.
			output.append(binaryFormOfValue);
		} else {
			// add '0' to the representation (internal node)
			output.append(ZERO);
			// create the left subtree.
			output.append(createTreeRepresentation(n.getLeft()));
			// create the right subtree.
			output.append(createTreeRepresentation(n.getRight()));
		}
		return output.toString();
	}

	// method returns the string representation of the tree.
	// pre: none
	public String getTreeRepresentaion() {
		return treeRepresentation;
	}

	// method returns the size of the string representation.
	// pre: none
	public int getTreeBitSize() {
		return treeRepresentation.length();
	}

	// helper method used to create the tree representation
	// length = length of the binary form of the value.
	// pre: none
	// post: returns a string with a certain number of leading zeroes.
	private String getBitsAdjuster(int length) {
		// string builder used to make method more efficient.
		StringBuilder sb = new StringBuilder();
		// find the number of leading zeroes required.
		int bitsRequired = (IHuffConstants.BITS_PER_WORD + 1) - length;
		// add bitsRequired number of zeroes to the string builder.
		for (int i = 0; i < bitsRequired; i++) {
			sb.append("0");
		}
		return sb.toString();
	}
}
