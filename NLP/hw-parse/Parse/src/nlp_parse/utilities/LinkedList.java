package nlp_parse.utilities;

import java.util.Iterator;

/**
 * Created by Sindhuula.
 */
public class LinkedList<T> implements Iterable<T> {
    private Node<T> first;
    private Node<T> last;


    public void addNode(T value) {
        Node<T> node = new Node<T>(value);
        if (first == null || last == null) {
            first = node;
            last = first;
        } else {
            last.setNextNode(node);
            last = node;
        }
    }

    public Node<T> getFirstNode() {
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
        private Node<T> current;

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
