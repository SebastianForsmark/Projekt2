import se.kth.id1020.TinySearchEngineBase;
import se.kth.id1020.util.Attributes;
import se.kth.id1020.util.Document;
import se.kth.id1020.util.Sentence;
import se.kth.id1020.util.Word;

import java.util.*;

public class TinySearchEngine implements TinySearchEngineBase {
    private static HashMap<String, ArrayList<DocumentProperties>> cache = new HashMap<String, ArrayList<DocumentProperties>>();
    private HashMap<String, ArrayList<DocumentProperties>> words = new HashMap<String, ArrayList<DocumentProperties>>();
    private HashMap<Document, Integer> wordsPerDoc = new HashMap<Document, Integer>();
    private List<Document> documents = new LinkedList<Document>();

    TinySearchEngine() {
        words = new HashMap<String, ArrayList<DocumentProperties>>();
    }


    public void preInserts() {

    }

    public void insert(Sentence sentence, Attributes attributes) {

        // How many times does a word appear in a document? => docProp
        // How many words are there in a document? => Keep hashMap for <doc, #words>
        // How many documents in total? => Keep list of all docs while indexing save its length when done
        // How many documents contain a word? => length of docProp list

        updateStatistics(sentence, attributes);
        for (Word word : sentence.getWords()
                ) {
            boolean alreadyInList = false;
            if (!words.containsKey(word.word)) // && !isPunctuation(word.toString())) // If the hashmap does not contain the word, add it.
            {
                ArrayList<DocumentProperties> arrayList = new ArrayList<DocumentProperties>();
                arrayList.add(new DocumentProperties(attributes));

                words.put(word.word, arrayList);

            } else //if(!isPunctuation(word.toString()))
            {
                List<DocumentProperties> list = words.get(word.word);
                for (DocumentProperties docP : list) {
                    if (docP.getDocument() == attributes.document) {
                        docP.count++; // Update the occurrence of the word in a document
                        alreadyInList = true;
                    }
                }
                if (!alreadyInList) {
                    list.add(new DocumentProperties(attributes)); // Or add it to the list
                }

            }
        }
    }

    public void postInserts() {
        System.out.println(words.size());

    }

    public List<Document> search(String s) {
        ArrayList<ArrayList<DocumentProperties>> setsOfResults = new ArrayList<ArrayList<DocumentProperties>>();
        ArrayList<DocumentProperties> result = new ArrayList<DocumentProperties>();
        ArrayList<DocumentProperties> partialResult = new ArrayList<DocumentProperties>();
        LinkedList<Character> operators = new LinkedList<Character>();
        String[] keys = s.split(" ");

        for (int i = 0; i < keys.length; i++) {

            if (keys.length == 1 | keys.length == 4) {
                result.addAll(union(words.get(keys[0]), words.get(keys[0])));
                break;
            }

            if (isOperator(keys[i])) {
                operators.add((keys[i].charAt(0)));
                continue;
            }

            if (keys[i].equalsIgnoreCase("orderby")) {
                if (!setsOfResults.isEmpty())
                    result.addAll(combineSubsets(setsOfResults, operators));

                try {
                    String[] queryKeyWords = {keys[i + 1], keys[i + 2]};
                    OrderByHandler.handleQueryKeyWords(queryKeyWords, result);
                } catch (Exception e) {
                    OrderByHandler.handleQueryError();
                    return null;
                }
                break;
            }

            if (i > 0) {
                if (!isOperator(keys[i - 1]) && !isOperator(keys[i])) { // If I now have 2 search terms in a row
                    String cacheKey = buildCacheKey(operators.getLast().toString(), keys[i - 1], keys[i]);

                    if (isCached(cacheKey)) {
                        setsOfResults.add(new ArrayList<DocumentProperties>(retrieveCache(cacheKey)));
                        operators.remove(operators.getLast());
                    } else {
                        result.addAll(applyOperator(operators.getLast(), (words.get(keys[i - 1])), words.get(keys[i])));
                        setsOfResults.add(new ArrayList<DocumentProperties>(result));
                        placeInCache(cacheKey, result);
                        result.clear();
                        operators.remove(operators.getLast());
                    }
                }
                if (i == keys.length - 1) // more than 1 search term and no orderby specified
                {
                    if (operators.isEmpty())
                        result.addAll(setsOfResults.get(0));
                    else
                        result.addAll(combineSubsets(setsOfResults, operators));
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

    public String infix(String s) {
        return null;
    }


    // *********************************************************  OPERATORS  *********************************************************************************

    private List<DocumentProperties> applyOperator(Character c, ArrayList<DocumentProperties> word1, ArrayList<DocumentProperties> word2) {
        if (c == '+')
            return intersection(word1, word2);
        if (c == '|')
            return union(word1, word2);
        if (c == '-')
            return difference(word1, word2);
        else return null;

    }

    private ArrayList<DocumentProperties> intersection(ArrayList<DocumentProperties> word1, ArrayList<DocumentProperties> word2) {
        ArrayList<DocumentProperties> firstWordDocuments = new ArrayList<DocumentProperties>();
        firstWordDocuments.addAll(word1);
        ArrayList<DocumentProperties> toReturn = new ArrayList<DocumentProperties>();
        for (DocumentProperties secondWord : word2) {
            if (firstWordDocuments.contains(secondWord))
                toReturn.add(secondWord);

        }
        return toReturn;
    }

    private ArrayList<DocumentProperties> union(ArrayList<DocumentProperties> word1, ArrayList<DocumentProperties> word2) {
        ArrayList<DocumentProperties> toReturn = new ArrayList<DocumentProperties>();
        toReturn.addAll(word1);

        for (DocumentProperties docProp : word2) {
            if (!toReturn.contains(docProp))
                toReturn.add(docProp);
        }
        return toReturn;
    }


    private ArrayList<DocumentProperties> difference(ArrayList<DocumentProperties> word1, ArrayList<DocumentProperties> word2) {
        ArrayList<DocumentProperties> secondWordDocuments = new ArrayList<DocumentProperties>();
        secondWordDocuments.addAll(word2);

        ArrayList<DocumentProperties> toReturn = new ArrayList<DocumentProperties>();
        for (DocumentProperties firstWord : word1) {
            if (!secondWordDocuments.contains(firstWord))
                toReturn.add(firstWord);

        }
        return toReturn;
    }

    private ArrayList<DocumentProperties> combineSubsets(ArrayList<ArrayList<DocumentProperties>> subsets, LinkedList<Character> operators) {
        ArrayList<DocumentProperties> combinedSubsets = new ArrayList<DocumentProperties>();
        ArrayList<DocumentProperties> toReturn = new ArrayList<DocumentProperties>();
        combinedSubsets.addAll(applyOperator(operators.getLast(), subsets.get(0), subsets.get(1))); // Initialize by combining the 2 first subsets
        operators.remove(operators.getLast());

        for (int j = 0; j < operators.size(); j++) {
            toReturn.addAll(applyOperator(operators.getLast(), combinedSubsets, subsets.get(j + 2))); // Update the total result by combining the previous combination with the new
            combinedSubsets.addAll(toReturn);
            operators.remove(operators.getLast());
        }

        return toReturn;
    }

    // *********************************************************  UTIL  *********************************************************************************

    private boolean isPunctuation(String s) {
        String[] id = s.split("\"");
        return id[2].equalsIgnoreCase(" // PUNCTUATION}");

    }

    private boolean isOperator(String s) {
        return s.equalsIgnoreCase("+") | s.equalsIgnoreCase("|") | s.equalsIgnoreCase("-");
    }


    private void updateStatistics(Sentence sentence, Attributes attributes) {
        if (!documents.contains(attributes.document)) { // Create a list of all documents to count at the end.
            documents.add(attributes.document);
        }
        int wordsInSentence = 0;
        for (Word word : sentence.getWords()) {

            if (!isPunctuation(word.toString()))
                wordsInSentence++;
        }
        int wordsInDoc;
        if (wordsPerDoc.get(attributes.document) != null)
            wordsInDoc = wordsPerDoc.get(attributes.document) + wordsInSentence;
        else
            wordsInDoc = wordsInSentence;

        wordsPerDoc.put(attributes.document, wordsInDoc); // Add the number of words in this sentence to the existing tally of words for the document
    }


    // *********************************************************  CACHE METHODS *********************************************************************************


    private void placeInCache(String cacheKey, ArrayList<DocumentProperties> result) {
        cache.put(cacheKey, result);
    }

    private boolean isCached(String cacheKey) {
        return cache.containsKey(cacheKey);
    }

    private ArrayList<DocumentProperties> retrieveCache(String cacheKey) {
        return cache.get(cacheKey);
    }

    private String buildCacheKey(String operation, String keyWord1, String keyWord2) {
        StringBuilder cacheKey = new StringBuilder();
        cacheKey.append(operation + " ");
        cacheKey.append(keyWord1 + " ");
        cacheKey.append(keyWord2);
        return cacheKey.toString();
    }

}



