/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package citereco;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 *
 * @author dwaipayan
 */
class DoiCidPair {
    String      doi;
    int         cid;

    public DoiCidPair(String doi, int cid) {
        this.doi = doi;
        this.cid = cid;
    }

    public String getDOI() { return doi; }
    public int getCID() { return cid; }
    
}

/**
 * Makes a HashMap of (Doi-Cid) Pair with Doi as value and return.
 * Functions: 
 ***    public HashMap(String, DoiCidPair) makeDoiCidMap(String doiCidFilePath)
 * @author dwaipayan
 */
public class DoiCid {

    /**
     * Makes a HashMap of (Doi-Cid) Pair with Doi as Key and return.
     * @param doiCidFilePath: path of the doi-cid-pair file
     * @return doiCidMap: doiCidMap of all the papers
     */
    public HashMap<String, DoiCidPair> makeDoiCidMap(String doiCidFilePath) {

        BufferedReader br;
        String s;
        HashMap <String, DoiCidPair> doiCidMap = new HashMap<>();

        try {
            br = new BufferedReader(new FileReader(doiCidFilePath));

            String []tokens;
            while ((s = br.readLine()) != null) {
                tokens = s.split(" ");  // tokens[0] = DOI, tokens[1] = CID
                doiCidMap.put(tokens[0], 
                    new DoiCidPair(tokens[0], Integer.parseInt(tokens[1])));
            }

        } catch (FileNotFoundException ex) {
            System.err.println("Error: DOI-ClusterId-map file not present at: "
                + doiCidFilePath);
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Error: IOException occured while reading from "
                + "the DOI-ClusterId-map file");
            System.exit(1);
        }

        return doiCidMap;
    }

    /**
     * 
     * @param cidListPath
     * @return 
     */
    public HashMap<Integer, Integer> makeCidMap(String cidListPath){

        BufferedReader br;
        String s;
        HashMap <Integer, Integer> cidMap = new HashMap<>();
        try {
            br = new BufferedReader(new FileReader(cidListPath));

            while ((s = br.readLine()) != null) {
                cidMap.put(Integer.parseInt(s), Integer.parseInt(s));
            }

        } catch (FileNotFoundException ex) {
            System.err.println("Error: ClusterIdList-map file not present at: "
                + cidListPath);
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Error: IOException occured while reading from "
                + "the DOI-ClusterId-map file");
            System.exit(1);
        }

        return cidMap;
    }
}
