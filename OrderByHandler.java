import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;

public class OrderByHandler {

    public static void handleQueryKeyWords(String[] keyWords, ArrayList<DocumentProperties> list) throws Exception {
        String property = keyWords[0];
        String direction = keyWords[1];

        Comparator<DocumentProperties> c;
        boolean ascending;

        if (property.equalsIgnoreCase("count")) {
            c = DocumentProperties.BY_COUNT;
        } else if (property.equalsIgnoreCase("popularity")) {
            c = DocumentProperties.BY_POPULARITY;
        } else if (property.equalsIgnoreCase("occurrence")) {
            c = DocumentProperties.BY_OCCURRENCE;
            //System.out.println();
            //for(int i = 0; i < list.size(); i++)
            //    System.out.print(list.get(i).getOccurrence() + "  ");
        } else {
            //handleQueryError();
            throw new Exception("False query");
        }

        if (direction.equalsIgnoreCase("asc")) {
            ascending = true;
        } else if (direction.equalsIgnoreCase("desc")) {
            ascending = false;
        } else {
            //handleQueryError();
            throw new Exception("False query");
        }

        BubbleSort.sort(list, c, ascending);
    }

    public static void handleQueryError() {
        System.out.println(
                "Error, Query aborted: Query keywords format should be: orderby \"Property\" \"Direction\"\n" +
                        "Property = count, popularity, occurrence\n" +
                        "Direction: asc, desc"
        );
    }

}
