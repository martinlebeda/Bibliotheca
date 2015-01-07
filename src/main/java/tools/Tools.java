package tools;

import java.text.Normalizer;


/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 21.8.14
 */
public class Tools {

    public static String removeDiacritics(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

}
