/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package citereco;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * Indexes the CiteSeer full-paper files
 * @author dwaipayan
 */

class CiteSeerIndexer {

    Properties  prop;               // prop of the init.properties file
    String      collPath;           // path of the collection
    String      collSpecPath;       // path of the collection spec file
    boolean     boolIndexExists;    // boolean flag to indicate whether the index exists or not

    File        indexFile;          // place where the index will be stored
    PaperAnalyzer paperAnalyzer;
    Analyzer    analyzer;          // the paper analyzer
    IndexWriter indexWriter;
    File        collDir;              // collection Directory
    int         fileIndexedCounter;
    boolean     boolIndexFromSpec;  // true; false if indexing from collPath
    int         docIndexedCounter;  // document indexed counter
    boolean     boolDumpIndex;      // true if want ot dump the entire collection
    String      dumpPath;           // path of the file in which the dumping to be done

    /* initialised together */
    String      doiCidPairPath;     // path of the file DoiCid
    DoiCid      doiCid;
    HashMap<String, DoiCidPair> doiCidMap;

    static final public String FIELD_ID = "doi";
    static final public String FIELD_CONTENT = "content";
    static final public String FIELD_CID="cid";

    String doi = "";
    String content = "";

    /**
     * 
     * @param propFile: path of the properties file
     * @throws IOException 
     */
    public CiteSeerIndexer(String propFile) throws IOException {

        prop = new Properties();
        try {
            prop.load(new FileReader(propFile));
        } catch (IOException ex) {
            System.err.println("Error: Properties file missing");
            System.exit(1);
        }
        //----- Properties file loaded

        //+++++ setting the paper-analyzer
        paperAnalyzer = new PaperAnalyzer(propFile);
        analyzer = paperAnalyzer.setAnalyzer();
        //----- analyzer set with paper-analyzer

        /* collection path setting */
        if(prop.containsKey("CScollSpec")) {
            boolIndexFromSpec = true;
        }
        else if(prop.containsKey("CScollPath")) {
            boolIndexFromSpec = false;
            collPath = prop.getProperty("CScollPath");
            collDir = new File(collPath);
            if (!collDir.exists() || !collDir.canRead()) {
                System.err.println("Collection directory '" +collDir.getAbsolutePath()+ "' does not exist or is not readable");
                System.exit(1);
            }
        }
        else {
            System.err.println("Neither collPath not collSpec is set");
            System.exit(1);
        }
        /* collection path set */

        indexFile = new File(prop.getProperty("CSIndexPath"));

        Directory indexDir = FSDirectory.open(indexFile.toPath());

        if (DirectoryReader.indexExists(indexDir)) {
            System.err.println("Index exists in "+indexFile.getAbsolutePath());
            boolIndexExists = true;
        }
        else {
            System.out.println("Creating the index in: " + indexFile.getAbsolutePath());
            boolIndexExists = false;

            IndexWriterConfig iwcfg = new IndexWriterConfig(analyzer);
            iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            indexWriter = new IndexWriter(indexDir, iwcfg);

        }

        boolDumpIndex = Boolean.parseBoolean(prop.getProperty("dumpIndex","false"));
        if(boolIndexExists == true && boolDumpIndex == true){
            dumpPath = prop.getProperty("dumpPath");
        }

        doiCidPairPath = prop.getProperty("doiCidMapPath");

        doiCid = new DoiCid();

        try {
            doiCidMap = doiCid.makeDoiCidMap(doiCidPairPath);
        } catch (Exception ex) {
            System.out.println("Error: in making doiCidMap");
            ex.printStackTrace();
        }
    }

    /**
     * 
     * @param doi: Unique doc-id to be used as index-key
     * @param content: Raw content of the paper 
     * @return
     * @throws IOException 
     */
    Document constructDoc(String doi, String content) throws IOException {

        Document doc = new Document();

        // doi
        doc.add(new StringField(FIELD_ID, doi, Field.Store.YES));

        // cid
        int cid = doiCidMap.get(doi).getCID();

        doc.add(new IntField(FIELD_CID, cid, Field.Store.YES));
        //System.out.println(doi+" "+doiCidList.get(index).getCID());

        // rest of the paper content
        content = PaperAnalyzer.refineTexts(content);

        doc.add(new Field(FIELD_CONTENT, content, Field.Store.YES,
            Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));

        return doc;
    }

    /**
     * Creates the index 
     * @throws Exception 
     */
    public void createIndex() throws Exception {

        if (indexWriter == null ) {
            System.err.println("Index already exists at " + indexFile.getName() + ". Skipping...");
            return;
        }

        System.out.println("Indexing started");

        if (boolIndexFromSpec) {
            /* if collectionSpec is present, then index from the spec file*/
            String specPath = prop.getProperty("CScollSpec");
            System.out.println("Reading from spec file at: "+specPath);
            try (BufferedReader br = new BufferedReader(new FileReader(specPath))) {
                String line;
                while ((line = br.readLine()) != null) {
                    //System.out.println(line);
                    indexFile(new File(line));
                }
            }
        }
        else {
            if (collDir.isDirectory())
                indexDirectory(collDir);
            else
                indexFile(collDir);
        }

        indexWriter.close();
        System.out.println("Indexing ends");
        System.out.println(fileIndexedCounter + " files indexed");
    }

    private void indexDirectory(File dir) throws Exception {
        File[] files = dir.listFiles();
        //System.out.println("Indexing directory: "+files.length);
        for (File f : files) {
            if (f.isDirectory()) {
                System.out.println("Indexing directory: " + f.getName());
                indexDirectory(f);  // recurse
            }
            else {
                indexFile(f);
            }
        }
    }

    void indexFile(File file) throws Exception {

        Document doc;

        System.out.println((fileIndexedCounter+1)+": Indexing file: " + file.getAbsolutePath());

        parsePaper(file);

        doc = constructDoc(doi, content);
        indexWriter.addDocument(doc);
        fileIndexedCounter++;
    }


    /**
     * Sets: 1: doi (from name of the file)
     *       2: content (from the content of the file)
     * @param file
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private void parsePaper(File file) throws FileNotFoundException, IOException {
        String filename = file.getName();
        //System.out.println("File name: "+filename);
        doi = file.getName().substring(0, filename.lastIndexOf("."));
        //System.out.println(doi);

        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        String str = new String(data, "UTF-8");
        str = str.replaceAll("-\n", "");
        str = str.replaceAll("[^a-zA-Z]", " ");
//        str = str.replaceAll("\\w_", " ");
        content = str;
    }

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            args = new String[2];
            //System.out.print("Usage: java CiteSeerIndexer <prop-file>");
            args[0] = "/home/dwaipayan/citereco/citeseer.index.properties";
        }

        CiteSeerIndexer indexer = new CiteSeerIndexer(args[0]);

        /* if 'index' is set 'true' in init.properties AND no index not exists in the path */
        if (!indexer.boolIndexExists) {
            long start = System.currentTimeMillis();
            indexer.createIndex();
            long end = System.currentTimeMillis();
            long timeTaken = end - start;
            System.out.println("Time taken: "+ timeTaken);
            System.out.format("%d min, %d sec\n", 
                TimeUnit.MILLISECONDS.toMinutes(timeTaken),
                TimeUnit.MILLISECONDS.toSeconds(timeTaken) - 
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeTaken))
            );
        }
    }
}
