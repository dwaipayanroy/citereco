/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package citereco;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Properties;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author dwaipayan
 */

class TermPosition {
    String  term;
    int     position;

    public TermPosition(String term, int position) {
        this.term = term;
        this.position = position;
    }
    
}
public class DumpIndex {

    Properties  prop;               // prop of the init.properties file
    File        indexFile;          // place where the index will be stored
    boolean     boolIndexExists;    // boolean flag to indicate whether the index exists or not
    int         docIndexedCounter;  // document indexed counter
    boolean     boolDumpIndex;      // true if want ot dump the entire collection
    String      dumpPath;           // path of the file in which the dumping to be done
    String      field;              // field to dump from each document

    static final public String FIELD_ID = "docid";
    static final public String FIELD_BOW = "content";       // ANALYZED bow content
    static final public String FIELD_RAW = "raw-content";   // raw, UNANALYZED content

    private DumpIndex(String propFile) throws Exception {

        prop = new Properties();
        try {
            prop.load(new FileReader(propFile));
        } catch (IOException ex) {
            System.err.println("Error: Properties file missing");
            System.exit(1);
        }
        //----- Properties file loaded

        /* index path setting */
        indexFile = new File(prop.getProperty("indexPath"));
        Directory indexDir = FSDirectory.open(indexFile.toPath());

        if (DirectoryReader.indexExists(indexDir)) {
            System.out.println("Index exists in "+indexFile.getAbsolutePath());
            boolIndexExists = true;
            dumpPath = prop.getProperty("dumpPath");
            if(dumpPath == null) {
                System.err.println("Error: dumpPath missing in prop file\n");
                System.exit(1);
            }
        }
        else {
            System.err.println("Error: Index does not exists.\n");
            boolIndexExists = true;
        }
        /* index path set */
        field = prop.getProperty("field");
        if(null == field) {
            System.err.println("Error: Fields to dump missing in prop");
            System.exit(1);
        }
    }

    private DumpIndex(String indexPath, String dumpPath, String field) throws Exception {

        /* index path setting */
        indexFile = new File(indexPath);
        Directory indexDir = FSDirectory.open(indexFile.toPath());

        if (DirectoryReader.indexExists(indexDir)) {
            System.out.println("Index exists in "+indexFile.getAbsolutePath());
            boolIndexExists = true;
            this.dumpPath = dumpPath;
            if(dumpPath == null) {
                System.err.println("Error: dumpPath missing in prop file\n");
                System.exit(1);
            }
        }
        else {
            System.err.println("Error: Index does not exists.\n");
            boolIndexExists = true;
        }
        /* index path set */
        this.field = field;
    }

    private void dumpIndexUnanalyzed() {

        System.out.println("Dumping unanalyzed index in: "+ dumpPath);

        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile.toPath()))) {
            PrintWriter pout = new PrintWriter(dumpPath);
            int maxDoc = reader.maxDoc();
            for (int i = 0; i < maxDoc; i++) {
                Document d = reader.document(i);
                //System.out.print(d.get(FIELD_BAG_OF_WORDS) + " ");
                pout.print(d.get(field) + " ");
                pout.print("\n");
            }
            System.out.println("Index dumped in: " + dumpPath);
            pout.close();
        }
        catch(IOException e) {
            System.err.println("Error: indexFile reading error");
            e.printStackTrace();
        }
    }

    /**
     * Dumps the analyzed index in the specified path
     */
    private void dumpIndex() {

        System.out.println("Dumping the index in: "+ dumpPath);

        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile.toPath()))) {
            FileWriter dumpFW = new FileWriter(dumpPath);
            int maxDoc = reader.maxDoc();
//            String docContent[] = new String[maxDocLength];
            ArrayList<TermPosition> docContent;

            for (int i = 0; i < maxDoc; i++) {
                docContent = new ArrayList<>();
                Document d = reader.document(i);
                System.out.println(i+": <"+d.get("docid")+">");
                //System.out.print(d.get(FIELD_BOW) + " ");
                Terms vector = reader.getTermVector(i, field);

                TermsEnum termsEnum = null;
                termsEnum = vector.iterator();
                BytesRef text;
                while ((text = termsEnum.next()) != null) {
                    String term = text.utf8ToString();
                    //System.out.print(term+": ");
                    DocsAndPositionsEnum docsPosEnum = termsEnum.docsAndPositions(null, null, DocsAndPositionsEnum.FLAG_FREQS); 
                    docsPosEnum.nextDoc();

                    int freq=docsPosEnum.freq();
                    for(int k=0; k<freq; k++){
                        int position=docsPosEnum.nextPosition();
                        //System.out.print(position+" ");
                        docContent.add(new TermPosition(term, position));
                    }
                    //System.out.println("");
                }
                //Collections.sort(docContent, (TermPosition t1, TermPosition t2) -> t1.position - t2.position);
                Collections.sort (docContent, new Comparator<TermPosition>(){
                    @Override
                    public int compare(TermPosition r1, TermPosition r2){
                        return r1.position - r2.position;
                    }}
                );
                for (TermPosition termPos : docContent) {
//                    System.out.print(docContent.get(j).term+" ");
                    dumpFW.write(termPos.term + " ");
                }
                dumpFW.write("\n");
                //System.out.println("");
            }

            System.out.println("Index dumped in: " + dumpPath);
            dumpFW.close();
        } catch(IOException e) {
            System.err.println("Error: indexFile reading error");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {

        String usage = "java DumpIndex"
            + " -index INDEX_PATH -dump DUMP_PATH -field FIELD_TO_DUMP\n\n"
            + "This dumps the lucene index from INDEX_PATH to DUMP_PATH.\n"
            + "FIELD_TO_DUMP: Indicates the field to be dumped\n";

        DumpIndex dumpIndex;
        if(args.length == 0) {
            System.out.println(usage);
//            System.exit(1);
            args = new String[2];
            args[0] = "/home/dwaipayan/citereco/citeseer.index.properties";
            dumpIndex = new DumpIndex(args[0]);
        }
        else {
            String indexPath = null;
            String dumpPath = null;
            String propPath = null;
            String field = null;

            for(int i=0;i<args.length;i++) {
                if (null != args[i]) switch (args[i]) {
                    case "-index":
                        indexPath = args[i+1];
                        i++;
                        break;
                    case "-dump":
                        dumpPath = args[i+1];
                        i++;
                        break;
                    case "-field":
                        field = args[i+1];
                        i++;
                        break;
                }
            }
            if ( indexPath == null || dumpPath == null || propPath == null ) {
                System.err.println("Usage: " + usage);
                System.exit(1);
            }
            dumpIndex = new DumpIndex(indexPath, dumpPath, field);
        }

        DumpIndex dump = new DumpIndex(args[0]);

        if(dump.boolIndexExists == true) {
            dump.dumpIndex();
        }
    }

}
