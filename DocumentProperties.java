import se.kth.id1020.util.Attributes;
import se.kth.id1020.util.Document;

import java.util.Comparator;

public class DocumentProperties {

    public static final Comparator<DocumentProperties> BY_COUNT = new ByCount();
    public static final Comparator<DocumentProperties> BY_POPULARITY = new ByPopularity();
    public static final Comparator<DocumentProperties> BY_OCCURRENCE = new ByOccurrence();

    // 16 +1*4 + 8 + 4 = 32B (+ comparators)
    private final Attributes attributes;
    public int count = 1; // Only created when word occurred
    public int combinedCount = 0; // If multiple words in same document are to be ordered by count
    public int combinedOccurrence = 0;

    public DocumentProperties(Attributes attributes) {
        this.attributes = attributes;
    }

    public Document getDocument() {
        return attributes.document;
    }

    public int getPopularity() {
        return attributes.document.popularity;
    }

    public int getOccurrence() {
        return attributes.occurrence;
    }

    public boolean equals(Object o) {
        if(o == null)
            return false;

        if(! (this.getClass() == o.getClass()))
            return false;

        // They are instances of same class, cast and compare documents
        if(this.getDocument() == ((DocumentProperties)o).getDocument())
            return true;

        return false;
    }

    private static class ByCount implements Comparator<DocumentProperties> {

        public int compare(DocumentProperties docP1, DocumentProperties docP2) {
            return docP1.combinedCount - docP2.combinedCount; // No risk of overflow since these ints cant be negative
        }
    }

    private static class ByPopularity implements Comparator<DocumentProperties> {

        public int compare(DocumentProperties docP1, DocumentProperties docP2) {
            return docP1.getPopularity() - docP2.getPopularity();
        }
    }

    private static class ByOccurrence implements Comparator<DocumentProperties> {

        public int compare(DocumentProperties docP1, DocumentProperties docP2) {
            return docP1.combinedOccurrence - docP2.combinedOccurrence;
        }
    }

}
