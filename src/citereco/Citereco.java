/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package citereco;


import java.io.FileReader;
import java.util.Properties;

/**
 *
 * @author dwaipayan
 */


public class Citereco {

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        Properties prop;
        // TODO code application logic here
        if (args.length == 0) {
            args = new String[2];
            System.out.print("Usage: java Citereco <prop-file>");
            args[0] = "init.properties";
            //args[1] = "retrieve.peoperties";
            args[1] = "init.properties";
        }

        prop = new Properties();
        prop.load(new FileReader(args[0]));
        

        if(Boolean.parseBoolean(prop.getProperty("index"))) {
            CSXIndexer indexer = new CSXIndexer(args[0]);
            indexer.createIndex();
        }

        if(Boolean.parseBoolean(prop.getProperty("search"))) {
            PaperSearcher searcher = new PaperSearcher(args[1]);
        }
    }
}
