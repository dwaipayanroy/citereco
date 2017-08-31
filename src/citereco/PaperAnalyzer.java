/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package citereco;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

/**
 * Sets EnglishAnalyzer with stoplist provided in .prop file
 * @author dwaipayan
 */
class PaperAnalyzer {
    Properties prop;
    Analyzer analyzer;

    public PaperAnalyzer(String propFile) throws IOException {
        prop = new Properties();
        prop.load(new FileReader(propFile));
//        setAnalyzer();
    }

    
    public Analyzer setAnalyzer() {
        String stopFile = prop.getProperty("stopFile");
        List<String> stopwords = new ArrayList<>();

        String line;
        try {
            FileReader fr = new FileReader(stopFile);
            BufferedReader br = new BufferedReader(fr);
            while ( (line = br.readLine()) != null ) {
                stopwords.add(line.trim());
            }
            br.close(); fr.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        analyzer = new EnglishAnalyzer(StopFilter.makeStopSet(stopwords));

        return analyzer;
    }

    public Analyzer getAnalyzer() { return analyzer; }

    public static String refineSpecialChars(String txt) {
        if(txt!=null)
            txt = txt.replaceAll("\\p{Punct}+", " ");
        return txt;
    }
    
    /* remove the anchor point info in txt starting from fromIndex */
    public static String removeAnchor(String txt, int fromIndex) {

        if (!txt.contains("=-=") || !txt.contains("-=-"))
            return txt;
        int as = txt.indexOf("=-="); // anchor start
        int ae = txt.indexOf("-=-", as); // anchor end

        String p1 = txt.substring (0, as);

        return p1 + removeAnchor(txt.substring(ae+3), ae+3);
    }

    /**
     * Replaces the text inside =-= xxxxx -=- with the gold citation-id
     * @param txt
     * @param fromIndex
     * @param cid
     * @return 
     */
    public static String replaceRefferenceWithCid(String txt, int fromIndex, int cid) {

        if (!txt.contains("=-=") || !txt.contains("-=-"))
            return txt;
        int as = txt.indexOf("=-="); // anchor start
        int ae = txt.indexOf("-=-", as); // anchor end

        String p1 = txt.substring (0, as)+"=-="+cid+"-=-";

        return p1 + replaceRefferenceWithCid(txt.substring(ae+3), ae+3, cid);
    }

    public static String refineTexts(String txt) {
    /* removes all special characters from txt, removes numericals etc. */

        // removes the urls
        txt = removeUrl(txt);

        // removes any special characters
        txt = refineSpecialChars(txt);

        //txt = removeNumerical(txt);

        return txt;
    }

    public static String removeUrl(String commentstr)
    {
        try {
            String urlPattern = "((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$~_?\\+-=\\\\\\.&]*)";
            Pattern p = Pattern.compile(urlPattern,Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(commentstr);
            int i = 0;
            while (commentstr!=null && m.find()) {
                commentstr = commentstr.replaceAll(m.group(0)," ").trim();
                i++;
            }
            return commentstr;
        }
        catch(Exception e) {
            
        }
        return commentstr;
    }
    
    public static boolean isNumerical(String s) {
        boolean isInt;
        boolean isDouble = false;

        try { 
            Integer.parseInt(s); 
            isInt = true;
        } catch(NumberFormatException e) { 
            isInt = false; 
        }
        
        if(!isInt) {
            try {
                Double.parseDouble(s);
                isDouble = true;
            } catch (NumberFormatException e) {
                isDouble = false;
            }
        }

        return isInt || isDouble;
    }

    public static String removeNumerical(String s) {
        /* removes all numerical tokens present in s */
        StringBuffer finalStr = new StringBuffer();

        String []tokens;
        tokens = s.trim().split(" ");
        for (String token : tokens) {
            if (!(token == null) && !PaperAnalyzer.isNumerical(token)) {
                finalStr.append(token).append(" ");
            }
        }

        return finalStr.toString();
    }
}
