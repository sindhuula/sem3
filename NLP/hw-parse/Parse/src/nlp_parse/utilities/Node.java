package nlp_parse.utilities;

/**
 * Created by Sindhuula.
 *
 */
public class Node<T> {
    private T value;
    private Node<T> next;

    public Node(T value) {
        this.value = value;
    }

    public Node<T> getNextNode() {
        return next;
    }

    public void setNextNode(Node<T> next) {
        this.next = next;
    }

    public T getNodeValue() {
        return value;
    }

    public boolean hasNextNode() {
        return next != null;
    }

    @Override
    public String toString() {
        String str = value.toString();
        if (hasNextNode()) {
            str += "; " + next.toString();
        }
        return str;
    }

}
