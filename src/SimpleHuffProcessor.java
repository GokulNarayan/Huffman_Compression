
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// class compresses and decompresses files.
public class SimpleHuffProcessor implements IHuffProcessor {
	// instance vars:
	// myViewer required to display messages to user.
	private IHuffViewer myViewer;
	// myCompressor stores all data required for compression.
	private Compressor myCompressor;

	// compress method: method writes the compressed version to a file.
	// in: InputStream connected to the file to be compressed.
	// out: OutputStream connected to the output file.
	// force: boolean which determines if the output file is to be written
	// pre: in != null & out != null
	// Method returns the number of bits written to the compressed file.
	public int compress(InputStream in, OutputStream out, boolean force) throws IOException {
		// check precon:
		if (in == null || out == null) {
			throw new IllegalArgumentException("InputStream and OutputStream cannot equal null");
		}

		// get the number of bits in the original file.
		int bitsInOriginalFile = myCompressor.getNumBitsInOriginalFile();
		// get the number of bits that would be written to the compressed file.
		int bitsWritten = myCompressor.getNumBitsWritten();
		// check if the compressed file should be written.
		// if force is not true, the number of bits written should be less than the
		// number of bits in the original file.
		if (force || (bitsInOriginalFile - bitsWritten) > 0) {
			// create a BitOutputStream from the OutputStream. use BufferedOutputStream to
			// make method faster.
			BitOutputStream writer = new BitOutputStream(new BufferedOutputStream(out));
			// create a BitInputStream from the InputStream. use BufferedInputStream to make
			// method faster.
			BitInputStream reader = new BitInputStream(new BufferedInputStream(in));
			// call the compressor's compress method to write to the file.
			myCompressor.compress(writer, reader);
			// close the reader and writer.
			writer.close();
			reader.close();
			showString("bits written: " + bitsWritten);
			return bitsWritten;
		} else {
			myViewer.showError("compressed file was larger than original");
		}
		// if no compression was done, return -1.
		return -1;
	}

	// method makes calls to methods in myCompressor, prepares data required for
	// compression.
	// in: InputStream connected to the file to be compressed.
	// headerFormat: determines which format the file is to be compressed in.
	// pre: in != null
	// method return the number of bits that would be saved by compressing the file.
	public int preprocessCompress(InputStream in, int headerFormat) throws IOException {
		// check precon:
		if (in == null) {
			throw new IllegalArgumentException("InputStream cannot equal null");
		}

		// create a BitInputStream from the InputStream. use BufferedInputStream to make
		// method faster.
		BitInputStream reader = new BitInputStream(new BufferedInputStream(in));
		// initialize myCompressor.
		myCompressor = new Compressor(reader, headerFormat);
		// close the reader.
		reader.close();
		// find the bits saved by compression.
		int bitsSaved = myCompressor.getNumBitsInOriginalFile() - myCompressor.getNumBitsWritten();
		showString("bits saved: " + bitsSaved);
		return bitsSaved;
	}

	public void setViewer(IHuffViewer viewer) {
		myViewer = viewer;
	}

	// method creates an uncompressed file from a compressed file.
	// in: InputStream connected to a compressed file.
	// out: OutputStream connected to the uncompressed file.
	// pre: in!=null & out!=null
	// method returns the number of bits written to the uncompressed file.
	public int uncompress(InputStream in, OutputStream out) throws IOException {
		// check precon:
		if (in == null || out == null) {
			throw new IllegalArgumentException("InputStream and OutputStream not equal to null");
		}

		// create a BitInputStream object from the InputStream.
		BitInputStream reader = new BitInputStream(new BufferedInputStream(in));
		// create a BitOutputStream object from the OutputStream.
		BitOutputStream writer = new BitOutputStream(new BufferedOutputStream(out));
		int result = 0;
		// create a Decompressor object.
		Decompressor myDecompressor = new Decompressor();
		// ensure the file being read is a compressed file (compressed using Huffman)
		boolean isCompressed = myDecompressor.isCompressedFile(reader);
		if (isCompressed) {
			// reconstruct the tree using information from the header.
			myDecompressor.constructTree(reader);
			// get the number of bits written.
			result = myDecompressor.decompress(reader, writer);
			// if the PEOF was missing, the compressed file was not properly formed.
			if (result == -1) {
				myViewer.showError("File not compressed properly, missing Pseudo-EOF");
			}
		} else {
			myViewer.showError("File not compressed");
		}
		showString("bits written: " + result);
		// close the reader and writer.
		reader.close();
		writer.close();
		return result;
	}

	private void showString(String s) {
		if (myViewer != null)
			myViewer.update(s);
	}
}
