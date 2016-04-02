/**
 * This is my version of a node for the linked list
 */
package nlp_parse.data_structures;

/**
 * Created by Sindhuula on 03/14/2016
 */
public class NodeList<T> {
    private T value;
    private NodeList<T> next;
    public NodeList(T value) {
        this.value = value;
    }

    //Return the next node in the list
    public NodeList<T> getNextNode() {
        return next;
    }

    //Add new node to the list
    public void setNextNode(NodeList<T> next) {
        this.next = next;
    }

    //Return the value of a node
    public T getNodeValue() {
        return value;
    }

    //Check if we've reached the end of the list
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
