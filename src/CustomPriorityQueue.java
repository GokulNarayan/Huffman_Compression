
// import statements:
import java.util.Iterator;
import java.util.LinkedList;

// Custom priority queue which breaks ties in fair manner. 
public class CustomPriorityQueue<E extends Comparable<? super E>> {
	// instance vars:
	private LinkedList<E> myCon;

	// constructor
	// initializes the internal container (LinkedList)
	public CustomPriorityQueue() {
		myCon = new LinkedList<E>();
	}

	// method adds an item to the queue in the correct position.
	// pre: item != null
	// pots: item is added to the list in the correct position.
	public void enqueue(E item) {
		// check precon:
		if (item == null) {
			throw new IllegalArgumentException("invalid node");
		}
		// variable stores the index at which the element is to be inserted.
		int index = 0;
		boolean positionFound = false;
		// iterator of the list created.
		Iterator<E> it = myCon.iterator();
		// iterate over elements in the list until the insert position is found.
		while (it.hasNext() && !positionFound) {
			// get the current element.
			E current = it.next();
			// if the item is smaller than current, the position is found.
			if (item.compareTo(current) < 0) {
				positionFound = true;
			} else {
				index++;
			}
		}
		// insert element in the queue at the correct position.
		myCon.add(index, item);
	}

	// method removes the first element in the queue
	// pre: the queue cannot be empty.
	// post: the element is removed from the internal container.
	public E dequeue() {
		// check precon:
		if (myCon.size() == 0) {
			throw new IllegalStateException("queue cannot be empty.");
		}
		// remove the first element from the list.
		return myCon.remove(0);
	}

	// method returns the value held by the first element in the queue.
	// pre: queue cannot be empty.
	public E peek() {
		// check precon:
		if (myCon.size() == 0) {
			throw new IllegalStateException("queue cannot be empty.");
		}
		return myCon.getFirst();
	}

	// method returns the size of the queue.
	// pre: none
	public int size() {
		// return the size of the internal list.
		return myCon.size();
	}
}
