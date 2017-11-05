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
    private boolean simpleInput = false;

    TinySearchEngine() {
        words = new HashMap<String, ArrayList<DocumentProperties>>();
    }


    public void preInserts() {

    }

    /**
     * Receives a list of Words, records statistics regarding their use and places them in a map <code>words</code>
     * @param sentence A list of Words to insert
     * @param attributes The Attributes for the words in the sentence including the document they belong in.
     */
    public void insert(Sentence sentence, Attributes attributes) {

        // NOTE: updateStatistics and several other things were created in preparation for Relevance "tf-idf" calculations
        // however I had trouble and do did not have enough time to create the algorithm and sorting for it so if you
        // see strange code that seems out of place then that's probably why.

        updateStatistics(sentence, attributes);

        for (Word word : sentence.getWords()
                ) {
            boolean alreadyInList = false;
            if (!words.containsKey(word.word)  && !isPunctuation(word.toString())) // If the hashmap does not contain the word, add it.
            {
                ArrayList<DocumentProperties> arrayList = new ArrayList<DocumentProperties>();
                arrayList.add(new DocumentProperties(attributes));

                words.put(word.word, arrayList);

            } else if(!isPunctuation(word.toString()))
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

    }

    /**
     * Searches the map <code>words</code> using prefix notation.
     * @param s The search query in prefix notation.
     * @return The list of documents fitting the search query.
     */
    public List<Document> search(String s) {
        ArrayList<ArrayList<DocumentProperties>> setsOfResults = new ArrayList<ArrayList<DocumentProperties>>();
        ArrayList<DocumentProperties> result = new ArrayList<DocumentProperties>();
        LinkedList<Character> operators = new LinkedList<Character>();
        String[] keys = queryParser(s).split("( )|((\\()|(\\)))");
        for (int i = 0; i < keys.length; i++) {


            if (keys[i].equalsIgnoreCase("orderby")) {
                if (!setsOfResults.isEmpty())
                    if (!operators.isEmpty())
                         result.addAll(combineSubsets(setsOfResults, operators));
                    else
                        result.addAll(setsOfResults.get(0));

                try {
                    String[] queryKeyWords = {keys[i + 1], keys[i + 2]};
                    OrderByHandler.handleQueryKeyWords(queryKeyWords, result);
                } catch (Exception e) {
                    OrderByHandler.handleQueryError();
                    return null;
                }
                break;
            }

            // Simple input refers to single words inputs with no operator
            if (simpleInput) {
                result.addAll(union(words.get(keys[0]), words.get(keys[0])));
                continue;
            }

            if (isOperator(keys[i])) {
                operators.add((keys[i].charAt(0)));
                continue;
            }



            if (!keys[i].equalsIgnoreCase("")) // Pressed for time and had trouble removing "", it keeps appearing as the first key.
            {
                if (!isOperator(keys[i])) {
                    String cacheKey = buildCacheKey(keys[i]);

                    if (isCached(cacheKey)) {
                        setsOfResults.add(new ArrayList<DocumentProperties>(retrieveFromCache(cacheKey)));

                    } else {
                        result.addAll(applyOperator(keys[i]));
                        setsOfResults.add(new ArrayList<DocumentProperties>(result));
                        placeInCache(cacheKey, result);
                        result.clear();
                    }
                }
                if (i == keys.length - 1) {
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
        return queryParser(s);
    }


    // *********************************************************  OPERATORS  *********************************************************************************


    private boolean isOperator(String s) {
        return s.equalsIgnoreCase("+") | s.equalsIgnoreCase("|") | s.equalsIgnoreCase("-");
    }


    /**
     * Helper method for the operator methods.
     * @param key The key containing the 2 words and their operator.
     * @return The list of DocumentProperties after applying the operator.
     */
    private List<DocumentProperties> applyOperator(String key) {
        String[] splitKey = key.split("(?<=\\|)|(?<=\\+)|(?<=-)|(?=\\|)|(?=\\+)|(?=-)");

        char c = splitKey[1].charAt(0);
        ArrayList<DocumentProperties> word1 = words.get(splitKey[0]);
        ArrayList<DocumentProperties> word2 = words.get(splitKey[2]);


        if (c == '+')
            return intersection(word1, word2);
        if (c == '|')
            return union(word1, word2);
        if (c == '-')
            return difference(word1, word2);
        else return null;

    }

    /**
     * Overloaded method in order to make the union of 2 ArrayList's of DocumentProperties possible
     * @param c The Operator.
     * @param word1 The first word.
     * @param word2 The second word.
     * @return The list of DocumentProperties after applying the operator.
     */
    private List<DocumentProperties> applyOperator(Character c, ArrayList<DocumentProperties> word1, ArrayList<DocumentProperties> word2) {

        if (c == '+')
            return intersection(word1, word2);
        if (c == '|')
            return union(word1, word2);
        if (c == '-')
            return difference(word1, word2);
        else return null;

    }

    /**
     * Applies the intersection operator on two lists of DocumentProperties
     * @param word1 The first word.
     * @param word2 The second word.
     * @return All the documents containing both terms or more generally the intersection of both sub-queries results.
     */
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

    /**
     * Applies the union operator on two lists of DocumentProperties
     * @param word1 The first word.
     * @param word2 The second word.
     * @return All the documents that contain either one of the terms or more generally the union of both sub-queries results.
     */
    private ArrayList<DocumentProperties> union(ArrayList<DocumentProperties> word1, ArrayList<DocumentProperties> word2) {
        ArrayList<DocumentProperties> toReturn = new ArrayList<DocumentProperties>();
        toReturn.addAll(word1);

        for (DocumentProperties docProp : word2) {
            if (!toReturn.contains(docProp))
                toReturn.add(docProp);
        }
        return toReturn;
    }

    /**
     * Applies the difference operator on two lists of DocumentProperties
     * @param word1 The first word.
     * @param word2 The second word.
     * @return All the documents that contain the first term but not the second one, or more generally the set difference between the two sub-query results.
     */
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

    /**
     * Since the search method creates subsets for each set of 2 search terms, this method combines them using the remaining operators.
     * @param subsets The sets of 2 words after one of the 3 operations has been applied to each set.
     * @param operators The operators which are applied between the sets in order.
     * @return Returns a combined ArrayList of DocumentProperties which should reflect the final result before ordering.
     */
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


    /**
     * Receives a string in prefix notation and translates it into infix notation.
     * @param s The search string in prefix notation.
     * @return If the search is not in prefix notation (simple input) it returns the original string, otherwise it returns infix notation.
     */
    private String queryParser(String s) {
        if (s.charAt(0) != '|' && s.charAt(0) != '+' && s.charAt(0) != '-') {
            simpleInput = true;
            return s;
        }
        simpleInput = false;
        StringBuilder infix = new StringBuilder();
        LinkedList<Character> infixOperators = new LinkedList<Character>();
        LinkedList<String> subSets = new LinkedList<String>();
        String[] keys = s.split(" ");

        for (int i = 0; i < keys.length; i++) {

            if (i > 0 && !keys[i].equalsIgnoreCase("orderby"))
                if (!isOperator(keys[i - 1]) && !isOperator(keys[i])) {
                    subSets.add("(" + keys[i - 1] + infixOperators.getLast().toString() + keys[i] + ")");
                    infixOperators.removeLast();
                }

                // The second condition is a dirty fix to ensure it does everything in proper order for different input sizes.
            if (i == keys.length - 1 | (keys[i].equalsIgnoreCase("orderby"))) {
                for (int j = 0; j < subSets.size(); j++) {
                    infix.append(subSets.get(j));
                    if (!infixOperators.isEmpty()) {
                        infix.append(infixOperators.getLast());
                        infixOperators.removeLast();
                    }
                }

                if (keys[i].equalsIgnoreCase("orderby")) {
                    infix.append(keys[i] + " " + keys[i + 1] + " " + keys[i + 2]);
                    break;
                }
            }

            if (isOperator(keys[i])) {
                infixOperators.add(keys[i].charAt(0));
            }


        }
        return infix.toString();
    }


    private boolean isPunctuation(String s) {
        String[] id = s.split("\"");
        return id[2].equalsIgnoreCase(" // PUNCTUATION}");

    }

    /**
     * Prepares data to be used for relevance tf-idf calculations.
     * @param sentence The sentence being inserted into the map <code>words</code>.
     * @param attributes The sentences Attributes including which document it belongs to.
     */
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

    /***
     * Places a key and it's commutative twin with data into the cache
     * @param cacheKey The key identifying the query.
     * @param result The Arraylist of DocumentProperties which was the result of the query.
     */
    private void placeInCache(String cacheKey, ArrayList<DocumentProperties> result) {

        cache.put(cacheKey, result);

        String[] comCacheKey = cacheKey.split(" ");
        String mirrorKey;
        if (comCacheKey[1].equalsIgnoreCase("+") | comCacheKey[1].equalsIgnoreCase("|")) {
            mirrorKey = comCacheKey[2] + " " + comCacheKey[1] + " " + comCacheKey[0];
            cache.put(mirrorKey, result);
        }
    }

    private boolean isCached(String cacheKey) {
        return cache.containsKey(cacheKey);
    }


    private ArrayList<DocumentProperties> retrieveFromCache(String cacheKey) {
        System.out.println("Retrieved "+cacheKey+" from cache!");
        return cache.get(cacheKey);
    }


    private String buildCacheKey(String keywords) {
        String[] splitKey = keywords.split("(?<=\\|)|(?<=\\+)|(?<=-)|(?=\\|)|(?=\\+)|(?=-)");
        StringBuilder cacheKey = new StringBuilder();
        cacheKey.append(splitKey[0] + " " + splitKey[1] + " " + splitKey[2]);
        return cacheKey.toString();
    }


}



