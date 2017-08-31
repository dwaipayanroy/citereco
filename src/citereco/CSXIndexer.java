/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package citereco;

import static citereco.PaperAnalyzer.removeAnchor;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

/**
 *
 * @author dwaipayan
 */

class CSXIndexer {

    Properties  prop;            // prop of the init.properties file
    String      collPath;            // path of the collection
    File        indexFile;              // place where the index will be stored
    PaperAnalyzer paperAnalyzer;
    Analyzer    analyzer;          // the paper analyzer
    IndexWriter indexWriter;
    File        collDir;              // collection Directory
    int         fileIndexedCounter; 
    String      doi_cid_map_path;    // path of the file DoiCid
    boolean     boolIndexExists;
    boolean     boolIndexFromSpec;  // true; false if indexing from collPath
    int         docIndexedCounter;  // document indexed counter
    boolean     boolDumpIndex;      // true if want ot dump the entire collection
    String      dumpPath;           // path of the file in which the dumping to be done

    /* initialised together */
    String      doiCidPairPath;     // path of the file DoiCid
    DoiCid      doiCid;
    HashMap<String, DoiCidPair> doiCidMap;

    static final public String FIELD_ID = "doi";
    static final public String FIELD_CID = "cid";
    static final public String FIELD_TITLE = "title";
    static final public String FIELD_ABSTRACT = "abstract";
    static final public String FIELD_CONTEXTS = "contexts";

    String doi = "";
    String title = "";
    String abstrct = "";
    StringBuilder contexts = new StringBuilder();

    public CSXIndexer(String propFile) throws IOException {

        prop = new Properties();
        prop.load(new FileReader(propFile));
        /* property files are loaded */

        /* collection path setting */
        if(prop.containsKey("CSXcollSpec")) {
            boolIndexFromSpec = true;
        }
        else if(prop.containsKey("collPath")) {
            boolIndexFromSpec = false;
            collPath = prop.getProperty("collPath");
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

        paperAnalyzer = new PaperAnalyzer(propFile);
        paperAnalyzer.setAnalyzer();
        analyzer = paperAnalyzer.getAnalyzer();

        indexFile = new File(prop.getProperty("CSXIndexPath"));

        Directory indexDir = FSDirectory.open(indexFile.toPath());

        if (DirectoryReader.indexExists(indexDir)) {
            System.err.println("Index exists in "+indexFile.getAbsolutePath());
            boolIndexExists = true;
        }
        else {
            System.out.println("Creating the index in: " + indexFile.getName());
            boolIndexExists = false;

            IndexWriterConfig iwcfg = new IndexWriterConfig(analyzer);
            iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            indexWriter = new IndexWriter(indexDir, iwcfg);

/*
            System.out.println("Exit(E/e) or, Proceed(P/p)");
            Scanner reader = new Scanner(System.in);
            char c = reader.next().charAt(0);
            if(c == 'P' || c == 'p')
                System.out.println("Proceeding");
            else {
                System.out.println("Terminated.");
                System.exit(1);
            }
*/
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

//    Document constructDoc(String id, String content, String time) throws IOException {
    Document constructDoc(String doi, String title, String abstrct, StringBuilder contexts) throws IOException {

        Document doc = new Document();

        // doi
        doc.add(new StringField(FIELD_ID, doi, Field.Store.YES));

        // cid
        int cid = doiCidMap.get(doi).getCID();
        doc.add(new IntField(FIELD_CID, cid, Field.Store.YES));
        //System.out.println(doi+" "+doiCidMap.get(index).getCID());
        //char ch = (char) System.in.read();
        //

        // title
        title = paperAnalyzer.refineTexts(title);
        doc.add(new Field(FIELD_TITLE, title, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
        // title ends

        // abstract
        abstrct = paperAnalyzer.refineTexts(abstrct);
        doc.add(new Field(FIELD_ABSTRACT, abstrct, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
        // abstract ends

        // contexts
        String contextstr = paperAnalyzer.refineTexts(contexts.toString());
        // removes the =-=XXXXX-=-
        contextstr = removeAnchor(contextstr, 0);

        doc.add(new Field("contexts", contextstr, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
        // contexts ends

        return doc;
    }

    public void createIndex() throws Exception{

        if (indexWriter == null ) {
            System.err.println("Index already exists at " + indexFile.getName() + ". Skipping...");
            return;
        }

        System.out.println("Indexing started");
        if (collDir.isDirectory())
            indexDirectory(collDir);
        else {
            indexFile(collDir);
            fileIndexedCounter++;
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
                System.out.println((fileIndexedCounter+1)+": Indexing file: " + f.getName());
                indexFile(f);
                fileIndexedCounter++;
            }
        }
    }

    void indexFile(File file) throws Exception {

        Document doc;

        String docType = prop.getProperty("docType");

        if (docType.equalsIgnoreCase("xml")) {

            parsePaper(file);

            doc = constructDoc(doi, title, abstrct, contexts);
            indexWriter.addDocument(doc);
        }
    }

    void dumpIndex() {
    /*  Dumps the entire index in a single file.
        Will be used for training word2vec
    */
        String dumpPath = prop.getProperty("index.dumpPath");
        System.out.println("Dumping the index in: "+ dumpPath);
        File f = new File(dumpPath);
/*
        if (f.exists()) {
            System.out.println("Dump existed. Overwrite(Y/N)?");
            Scanner reader = new Scanner(System.in);
            char c = reader.next().charAt(0);
            if(c == 'N' || c == 'n')
                return;
            else
                System.out.println("Dumping...");
        }
*/
        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile.toPath()))) {
            PrintWriter pout = new PrintWriter(dumpPath);
            int maxDoc = reader.maxDoc();

            for (int i = 0; i < maxDoc; i++) {
                Document d = reader.document(i);
                pout.println(d.get(FIELD_ABSTRACT) + " ");
//                System.out.println(d.get(FIELD_ABSTRACT) + " ");
                pout.println(d.get(FIELD_CONTEXTS) + " ");
            }
            System.out.println("Index dumped in: " + dumpPath);
            pout.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void parsePaper(File file) throws ParserConfigurationException, SAXException, IOException {
        /* parses the individual papers (.xml files) and sets 'title','abstract' etc. */

        SAXParserFactory factory = SAXParserFactory.newInstance ();
        SAXParser saxParser = factory.newSAXParser ();
//
        DefaultHandler handler = new DefaultHandler (){

            boolean bdoi = false;
	    boolean btitle = false;
	    boolean babst = false;
	    boolean bcntx = false;
            StringBuilder sb;

            @Override
	    public void startElement (String uri, String localName, String qName, Attributes attributes) throws SAXException
	    {
                sb = new StringBuilder();
                //System.out.println ("Start Element :" + qName);
                if (qName.equalsIgnoreCase ("doi")) bdoi = true;
    
                if (qName.equalsIgnoreCase ("title")) btitle = true;

		if (qName.equalsIgnoreCase ("abstract")) babst = true;

		if (qName.equalsIgnoreCase ("contexts")) bcntx = true;
	    }

            @Override
	    public void endElement (String uri, String localName, String qName) throws SAXException {

                if (bdoi){
                    //System.out.println ("Doi: " + new String (ch, start, length));
                    bdoi = false;
                    doi = sb.toString();
                    contexts = new StringBuilder();
		}

                if (btitle){
                    //System.out.println ("Title: " + new String (ch, start, length));
                    btitle = false;
                    title = sb.toString();
		}

		if (babst) {
                    //System.out.println ("Abstract: " + new String (ch, start, length));
                    babst = false;
                    //System.out.println ("Abstract: " + sb.toString());
                    abstrct = sb.toString();
		}

		if (bcntx) {
                    //System.out.println ("Context: " + new String (ch, start, length));
                    bcntx = false;
                    //System.out.println ("Context: " + sb.toString());
                    contexts.append(" ");
                    contexts.append(sb);
		}
	    }

            @Override
            public void characters (char ch[], int start, int length) {
                if (sb != null)
                    for (int i=start; i<start+length; i++)
                        sb.append(ch[i]);
            }
	};
//
        saxParser.parse (file, handler);
    }

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            args = new String[2];
//            System.out.print("Usage: java Citereco <prop-file>");
            args[0] = "init.properties";
        }

        CSXIndexer indexer = new CSXIndexer(args[0]);

        /* if 'index' is set 'true' in init.properties AND no index not exists in the path */
        if (Boolean.parseBoolean(indexer.prop.getProperty("index"))&&!indexer.boolIndexExists) {
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

        if (Boolean.parseBoolean(indexer.prop.getProperty("index.dump")))
            indexer.dumpIndex();
    }

}
