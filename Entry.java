import se.kth.id1020.util.Word;

import java.util.List;
import java.util.Map;

public class Entry implements Map.Entry<Word, List<DocumentProperties>> {

    final Word key;
    final List<DocumentProperties> value;

    public Entry(Word key, List<DocumentProperties> value) {
        this.key = key;
        this.value = value;
    }

    public String toString() {
        return key + " : " + value;
    }


    public Word getKey() {
        return key;
    }

    public List<DocumentProperties> getValue() {
        return value;
    }

    public List<DocumentProperties> setValue(List<DocumentProperties> documents) {
        return null;
    }
}