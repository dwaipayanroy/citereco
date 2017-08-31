package citereco;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import static java.lang.Character.isLetter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * @author dwaipayan
 */
public class WordVecs {

    Properties prop;
    int k;
    HashMap<String, WordVec> wordvecmap;
    HashMap<String, List<WordVec>> nearestWordVecsMap; // Store the pre-computed NNs after read from file
    static WordVecs singleTon;

    //ArrayList<WordVec> distList;
    
    public WordVecs() throws FileNotFoundException, IOException {
        prop = new Properties();
        prop.load(new FileReader("../../init.properties"));
        
        //distList = new ArrayList<>(wordvecmap.size());
        k = Integer.parseInt(prop.getProperty("wordvecs.numnearest", "25"));        
        String wordvecFile = prop.getProperty("wordvecs.vecfile");

        wordvecmap = new HashMap();
        try (FileReader fr = new FileReader(wordvecFile);
            BufferedReader br = new BufferedReader(fr)) {
            String line;
            
            while ((line = br.readLine()) != null) {
                WordVec wv = new WordVec(line);
                if(isLegalToken(wv.word))
                    wordvecmap.put(wv.word, wv);
            }
        }
    }
    public WordVecs(String propFile) throws Exception {
        
        prop = new Properties();
        prop.load(new FileReader(propFile));
        
        //distList = new ArrayList<>(wordvecmap.size());
        k = Integer.parseInt(prop.getProperty("wordvecs.numnearest", "25"));        
        String wordvecFile = prop.getProperty("wordvecs.vecfile");

        wordvecmap = new HashMap();
        try (FileReader fr = new FileReader(wordvecFile);
            BufferedReader br = new BufferedReader(fr)) {
            String line;
            
            while ((line = br.readLine()) != null) {
                WordVec wv = new WordVec(line);
                if(isLegalToken(wv.word))
                    wordvecmap.put(wv.word, wv);
            }
        }
    }

    public WordVecs(Properties prop) throws Exception { 

        this.prop = prop;
        //distList = new ArrayList<>(wordvecmap.size());
        k = Integer.parseInt(prop.getProperty("wordvecs.numnearest", "25"));
        String wordvecFile = prop.getProperty("wordvecs.vecfile");
        
        wordvecmap = new HashMap();
        try (FileReader fr = new FileReader(wordvecFile);
            BufferedReader br = new BufferedReader(fr)) {
            String line;
            
            while ((line = br.readLine()) != null) {
                WordVec wv = new WordVec(line);
                if(isLegalToken(wv.word))
                    wordvecmap.put(wv.word, wv);
            }
        }
    }
    
    static public WordVecs createInstance(Properties prop) throws Exception {
        if(singleTon == null) {
            singleTon = new WordVecs(prop);
            singleTon.loadPrecomputedNNs();
            System.out.println("Precomputed NNs loaded");
        }
        return singleTon;
    }
    
    public void computeAndStoreNNs() throws FileNotFoundException {
        String NNDumpPath = prop.getProperty("NNDumpPath");
        if(NNDumpPath!=null) {
            File f = new File(NNDumpPath);
        }
        else {
            System.out.println("Null found");
            return;
        }

        System.out.println("Dumping the NN in: "+ NNDumpPath);
        PrintWriter pout = new PrintWriter(NNDumpPath);

        System.out.println("Precomputing NNs for each word");
//        nearestWordVecsMap = new HashMap<>(wordvecmap.size());
//        System.out.println("Size: "+ wordvecmap.size());

        for (Map.Entry<String, WordVec> entry : wordvecmap.entrySet()) {
            WordVec wv = entry.getValue();
            if(isLegalToken(wv.word)) {
                System.out.println("Precomputing NNs for " + wv.word);
                List<WordVec> nns = getNearestNeighbors(wv.word);
                if (nns != null) {
                    pout.print(wv.word + "\t");
                    for (int i = 0; i < nns.size(); i++) {
                        WordVec nn = nns.get(i);
                        pout.print(nn.word + ":" + nn.querySim + "\t");
                    }
                    pout.print("\n");
//                    nearestWordVecsMap.put(wv.word, nns);
                }
            }
        }
        pout.close();
    }
    
    public void computeAndStoreNNs(String strList) throws FileNotFoundException {
        String NNDumpPath = prop.getProperty("NNDumpPath");
        if(NNDumpPath!=null) {
            File f = new File(NNDumpPath);
        }
        else {
            System.out.println("Null found");
            return;
        }

        System.out.println("Dumping the NN in: "+ NNDumpPath);
        PrintWriter pout = new PrintWriter(NNDumpPath);

        System.out.println("Precomputing NNs for each word");
//        nearestWordVecsMap = new HashMap<>(wordvecmap.size());
//        System.out.println("Size: "+ wordvecmap.size());

        String []terms = strList.split(" ");
        for (String term : terms) {
            System.out.println("Precomputing NNs for " + term);
            List<WordVec> nns = getNearestNeighbors(term);
            if (nns != null) {
                pout.print(term + "\t");
                for (WordVec nn : nns) {
                    pout.print(nn.word + ":" + nn.querySim + "\t");
                }
                pout.print("\n");
//                    nearestWordVecsMap.put(wv.word, nns);
            }
        }
        pout.close();
    }
    
    public List<WordVec> getPrecomputedNNs(String queryWord) {
        return nearestWordVecsMap.get(queryWord);
    }
    
    public List<WordVec> getNearestNeighbors(String queryWord) {
        ArrayList<WordVec> distList = new ArrayList<>(wordvecmap.size());
        
        WordVec queryVec = wordvecmap.get(queryWord);
        if (queryVec == null)
            return null;
        
        for (Map.Entry<String, WordVec> entry : wordvecmap.entrySet()) {
            WordVec wv = entry.getValue();
            if (wv.word.equals(queryWord))
                continue;
            wv.querySim = queryVec.cosineSim(wv);
            distList.add(wv);
        }
        Collections.sort(distList);
        return distList.subList(0, Math.min(k, distList.size()));        
    }
    
    public List<WordVec> getKNearestNeighbors(String queryWord, int K) {
        ArrayList<WordVec> distList = new ArrayList<>(wordvecmap.size());
        int count = 0;
//        System.out.println("In KNN: "+queryWord);
        WordVec queryVec = wordvecmap.get(queryWord);
        if (queryVec == null)
            return null;
        
        for (Map.Entry<String, WordVec> entry : wordvecmap.entrySet()) {
            WordVec wv = entry.getValue();
            if (wv.word.equals(queryWord))
                continue;
            wv.querySim = queryVec.cosineSim(wv);
            distList.add(wv);
        }
        Collections.sort(distList);
        return distList.subList(0, Math.min(K, distList.size()));        
    }
    
    public WordVec getVec(String word) {
        return wordvecmap.get(word);
    }

    public float getSim(String u, String v) {
        WordVec uVec = wordvecmap.get(u);
        WordVec vVec = wordvecmap.get(v);
        if (uVec == null || vVec == null) {
//            System.err.println("words not found...<" + ((uVec == null)?u:v) + ">");
            return 0;
        }

        return uVec.cosineSim(vVec);
    }

    private boolean isLegalToken(String word) {
        boolean flag = true;
        for ( int i=0; i< word.length(); i++) {
//            if(isDigit(word.charAt(i))) {
//                flag = false;
//                break;
            if(isLetter(word.charAt(i))) {
                continue;
            }
            else {
                flag = false;
                break;
            }
        }
        return flag;
    }
    
    public void loadPrecomputedNNs() throws FileNotFoundException, IOException {
        nearestWordVecsMap = new HashMap<>();
        String NNDumpPath = prop.getProperty("NNDumpPath");
        if (NNDumpPath == null) {
            System.out.println("NNDumpPath Null while reading");
            return;
        }
        System.out.println("Reading from the NN dump at: "+ NNDumpPath);
        File NNDumpFile = new File(NNDumpPath);
        
        try (FileReader fr = new FileReader(NNDumpFile);
            BufferedReader br = new BufferedReader(fr)) {
            String line;
            
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, " \t:");
                List<String> tokens = new ArrayList<>();
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    tokens.add(token);
                }
                List<WordVec> nns = new LinkedList();
                int len = tokens.size();
                //System.out.print(tokens.get(0)+" > ");
                for (int i=1; i < len-1; i+=2) {
                    nns.add(new WordVec(tokens.get(i), Float.parseFloat(tokens.get(i+1))));
                    //System.out.print(tokens.get(i) + ":" + tokens.get(i+1));
                }
                nearestWordVecsMap.put(tokens.get(0), nns);
            }
            System.out.println("NN dump has been reloaded");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
	if(args.length == 0) {
	    args[0] = "init.properties";
	}
        try {
	    WordVecs qe = new WordVecs(args[0]);
            FileReader fr = new FileReader(qe.prop.getProperty("toExpand.context.words"));
            BufferedReader br = new BufferedReader(fr);
            String line;
            StringBuffer stringBuffer = new StringBuffer();
            while ((line = br.readLine()) != null) {
                stringBuffer.append(line).append(" ");
            }

            qe.computeAndStoreNNs(stringBuffer.toString());
            //qe.loadPrecomputedNNs();
//            List<WordVec> nwords = qe.getNearestNeighbors("conclus");
//            for (WordVec word : nwords) {
//                System.out.println(word.word + "\t" + word.querySim);
 //           }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
