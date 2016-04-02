/**
 * My version of linked list to be used by the parser
 */
package nlp_parse.data_structures;

import java.util.Iterator;

public class LinkedList<T> implements Iterable<T> {
    private NodeList<T> first;
    private NodeList<T> last;

    //Add a new node to the Linked list
    public void addNode(T value) {
        NodeList<T> node = new NodeList<T>(value);
        if (first == null || last == null) {
            first = node;
            last = first;
        } else {
            last.setNextNode(node);
            last = node;
        }
    }

    //Return node at the head of the list
    public NodeList<T> getFirstNode() {
        return first;
    }

    @Override
    public String toString() {
        if (first == null) {
            return "[]";
        }
        return "[" + first.toString() + "]";
    }

    @Override
    public Iterator<T> iterator() {
        return new listIterator();
    }

    private class listIterator implements Iterator<T> {
        private NodeList<T> current;
        public listIterator() {
            current = first;
        }
        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public T next() {
            if(current == null) {
                return null;
            }
            T value = current.getNodeValue();
            current = current.getNextNode();
            return value;
        }
    }
}
