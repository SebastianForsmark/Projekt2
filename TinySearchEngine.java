import se.kth.id1020.TinySearchEngineBase;
import se.kth.id1020.util.Attributes;
import se.kth.id1020.util.Document;
import se.kth.id1020.util.Word;

import java.util.*;

public class TinySearchEngine implements TinySearchEngineBase {

    private HashMap<Word,List<DocumentProperties>> words;

    TinySearchEngine() {
        words = new HashMap<Word, List<DocumentProperties>>();
    }

    public void insert(Word word, Attributes attributes) {

        if (!words.containsKey(word)) // If the hashmap does not contain the word, add it.
        {
            List<DocumentProperties> linkedList = new LinkedList<DocumentProperties>();
            linkedList.add(new DocumentProperties(attributes));

            words.put(word,linkedList);

        } else // If the hashmap does contain the word
            {

            List<DocumentProperties> list = words.get(word);
            for (DocumentProperties docP : list) {
                if (docP.getDocument() == attributes.document) {
                    docP.count++; // Update the occurrence of the word in a document
                    return;
                }
            }
            list.add(new DocumentProperties(attributes)); // Or add it to the list
        }

    }

    public List<Document> search(String s) {

        LinkedList<DocumentProperties> result = new LinkedList<DocumentProperties>();
        String[] keys = s.split(" ");
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].toLowerCase().equals("orderby")) {
                // Search phase over
                try {
                    String[] queryKeyWords = {keys[i + 1], keys[i + 2]};
                    QueryKeyWordHandler.handleQueryKeyWords(queryKeyWords, result);
                } catch (Exception e) {
                    QueryKeyWordHandler.handleQueryError();
                    return null;
                }
                break;
            }

            Word currentWord = findWord(keys[i]);
            if (currentWord!=null) {
                for (DocumentProperties docP : words.get(currentWord)) {
                    // Manual result.contains(docP) to access the one that is equal
                    boolean contains = false;
                    for(int j = 0; j < result.size(); j++) {
                        DocumentProperties resultDocP = result.get(j);
                        if(resultDocP.getDocument() == docP.getDocument()) {
                            contains = true;
                            resultDocP.combinedCount += docP.count;
                            resultDocP.combinedOccurrence = Math.min(resultDocP.combinedOccurrence, docP.getOccurrence());
                            break; // Only one of the same document in the list (Union)
                        }
                    }
                    // Add document to result if it isn't already containing it, reset combinedCount and combinedOccurrence
                    if(!contains) {
                        docP.combinedCount = docP.count;
                        docP.combinedOccurrence = docP.getOccurrence();
                        result.add(docP);
                    }
                }

            }
        }

        LinkedList<Document> resultDocuments = new LinkedList<Document>();
        //System.out.println("Occurrences: ");
        for (DocumentProperties docP : result) {
            resultDocuments.add(docP.getDocument());
            //System.out.print("  " + docP.combinedOccurrence);
        }
        //System.out.println();

        return resultDocuments;

    }

    private Word findWord(String s)
    {
        for (Word word:words.keySet()) {
            String[] id = word.toString().split("\"");
            if (id[1].equalsIgnoreCase(s))
                return word;
        }
        return null;
        }
    }
/*

    // Returns the index where word is (or should be put if not existing)
    private int binarySearch(String word) {
        if (words.isEmpty())
            return 0;

        int lo = 0;
        int hi = words.size() - 1;

        int mid = -1;
        // Binary search for index, return index of word if found
        while (lo <= hi) {
            mid = lo + (hi - lo) / 2;
            if (word.compareTo(getWordStringAt(mid)) < 0)
                hi = mid - 1;
            else if (word.compareTo(getWordStringAt(mid)) > 0)
                lo = mid + 1;
            else {
                wordFound = true;
                return mid;
            }

        }

        wordFound = false;
        return mid;
    }

    // Returns the string word at given index in list of words
    private String getWordStringAt(int index) {
        return words.get(index).getKey().word;
    }
*/


