/**
 * This is the data structure for pairwise analysis
 */
package nlp_parse.data_structures;

public class Pairs<A,B> {
    private A a;
    private B b;

    public Pairs(A a, B b) {
        this.a = a;
        this.b = b;
    }
    //Return wordA or wordB
    public A getFirst() {
        return a;
    }
    public B getSecond() {
        return b;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Pairs<?,?>) {
            Pairs<?,?> p = (Pairs<?,?>)o;
            if (safeEquals(a, p.getFirst()) && safeEquals(b, p.getSecond())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37*result + (a == null ? 0 : a.hashCode());
        result = 37*result + (b == null ? 0 : b.hashCode());
        return result;
    }


    private static boolean safeEquals(Object o1, Object o2) {
        if (o1 == null || o2 == null) {
            return o1 == o2;
        } else {
            return o1.equals(o2);
        }
    }
}
