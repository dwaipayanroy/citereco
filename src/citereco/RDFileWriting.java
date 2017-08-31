/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package citereco;

import static citereco.PaperAnalyzer.replaceRefferenceWithCid;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.index.IndexWriter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author dwaipayan
 */

class CidContextPair {
    String  context;
    int     cid;

    public CidContextPair(String context, int cid) {
        this.context = context;
        this.cid = cid;
    }
}

public class RDFileWriting {

    Properties  prop;
    String      collPath;           // path of the collection
    File        collDir;            // collection Directory
    String      collSpecPath;
    String      rdContentDirPath;   // directory path, where to save the reference directed content files
    String      rdIndexPath;        // index path of the reference directed content

    boolean     boolIndexFromSpec;  // true; false if indexing from collPath
    int         fileIndexedCounter; 
    int         clustersCount;      // number of clusters saved
    IndexWriter indexWriter;

    /* initialised together */
    String      doiCidPairPath;     // path of the file DoiCid
    String      cidListPath;
    DoiCid      doiCid;
//    HashMap<String, DoiCidPair> doiCidMap;

    String doi = "";
    String title = "";
    String abstrct = "";
    String context = "";
    StringBuilder contexts = new StringBuilder();
    List<CidContextPair> contextCidPair = new ArrayList();

    HashMap<Integer, Integer> cidMap;

    public RDFileWriting(String propPath) {

        try {
            prop = new Properties();
            prop.load(new FileReader(propPath));
        } catch (FileNotFoundException ex) {
            System.err.println("Error: Properties file missing");
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Error: IOException while loading prop file");
            System.exit(1);
        }
        //----- Properties file loaded

        //* collection path setting 
        if(prop.containsKey("CSXCollSpec")) {
            boolIndexFromSpec = true;
            collSpecPath = prop.getProperty("CSXCollSpec");
        }
        else if(prop.containsKey("CSXCollPath")) {
            boolIndexFromSpec = false;
            collPath = prop.getProperty("CSXCollPath");
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
        //* collection path set 

        //+++++ path in which all the reference-directed documents will be stored
        if(prop.containsKey("RD_ContentDirPath")) {
            rdContentDirPath = prop.getProperty("RD_ContentDirPath");
            File dir = new File(rdContentDirPath);
            if (!dir.exists())
                if (dir.mkdir())
                    System.out.println("Directory created for storing Reference "
                            + "directed informations, in: "+rdContentDirPath);
                else
                    System.err.println("Failed to created directory for storing "
                            + "Reference directed informations, in: "+rdContentDirPath);
        }
        else {
            System.err.println("Error: path missing on where to save the rdi documents");
            System.exit(1);
        }
        //-----

        cidListPath = prop.getProperty("cidListPath");
//        doiCidPairPath = prop.getProperty("doiCidMapPath");
//
        doiCid = new DoiCid();

        try {
            cidMap = doiCid.makeCidMap(cidListPath);
        } catch (Exception ex) {
            System.out.println("Error: in making cidMap");
            System.err.println(ex.toString());
            System.exit(1);
        }
    }

    private void processDirectory(File dir) throws Exception {
        File[] files = dir.listFiles();
        //System.out.println("Indexing directory: "+files.length);
        for (File f : files) {
            if (f.isDirectory()) {
                System.out.println("Indexing directory: " + f.getName());
                processDirectory(f);  // recurse
            }
            else {
                processFile(f);
            }
        }
    }

    /**
     * Puts the (context,Cid) pair information of the processed paper in the HashMap
     * @param contextCidpair 
     */
    private void putContextInFile(List<CidContextPair> contextCidpair) throws IOException {

        for (CidContextPair pair : contextCidpair) {
            int currentCid = pair.cid;

            Integer value = cidMap.get(currentCid);
            if(null != value){
                //System.out.println("Before replace:\n"+pair.context);
                String currentContext = replaceRefferenceWithCid(pair.context, 0, currentCid);
                // currentContext contains the context with ref. replaced by the gold cluster id
                //System.out.println("After replace:\n"+currentContext);
                //char ch = (char) System.in.read();

                String currentFilePath = String.format("%s/%d", rdContentDirPath, currentCid);
                FileWriter fw = new FileWriter(currentFilePath, true);

                fw.write(currentContext+"\n");
                fw.close();
            }
            /*
            else {
                System.out.println(currentCid+" not found");
                char ch = (char) System.in.read();
            }
            */
        }
    }

    void processFile(File file) throws Exception {

        Document doc;

        System.out.println((fileIndexedCounter+1)+": processing file: " + file.getAbsolutePath());

        parsePaper(file);

        putContextInFile(contextCidPair);
        //doc = constructDoc(doi, title, abstrct, contexts, contextCidPair);
        //indexWriter.addDocument(doc);
        fileIndexedCounter++;
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
            boolean bcid = false;
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

                if (qName.equalsIgnoreCase ("clusterid")) bcid = true;
	    }

            @Override
	    public void endElement (String uri, String localName, String qName) throws SAXException {

                if (bdoi){
                    //System.out.println ("Doi: " + new String (ch, start, length));
                    bdoi = false;
                    doi = sb.toString();
                    contexts = new StringBuilder();
                    context = null;
                    contextCidPair = new ArrayList();
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
                    context = sb.toString();
		}

                if(bcid) {
                    bcid = false;
                    if(null != context) {
//                        System.out.println(Integer.parseInt(sb.toString())+":"+context);
                        contextCidPair.add(new CidContextPair(context, Integer.parseInt(sb.toString())));
                    }
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


    /**
     * Creates the Reference Directed (RD) content of the collection
     * Create separate files for each of the clusters present in the collection
     *  with the content: words that are used for citing that paper
     * @throws Exception 
     */
    public void createRDContent() throws Exception {

        System.out.println("Indexing started");

        if (boolIndexFromSpec) {
            //* if collectionSpec is present, then index from the spec file
            System.out.println("Reading from spec file at: "+collSpecPath);
            try (BufferedReader br = new BufferedReader(new FileReader(collSpecPath))) {
                String line;
                while ((line = br.readLine()) != null) {
                    //System.out.println(line);
                    processFile(new File(line));
                }
            }
        }
        else {
            if (collDir.isDirectory())
                processDirectory(collDir);
            else
                processFile(collDir);
        }

        /*
        // To write the content of the HashMap into files:
        //  1. Filename: entry.getkey() i.e. the cluster id
        //  2. File content: entry.getValue() i.e. the referencing context
        String fileName;
        clustersCount = 0;
        for(Map.Entry<Integer, String> entry : contextInfo.entrySet()){
            int cid = entry.getKey();

            clustersCount++;
            String content = entry.getValue();

            fileName = String.format("%s/%d", rdContentDirPath, cid);
            FileWriter writer = new FileWriter(fileName);
            writer.write(content);
            writer.close();
        }
        System.out.println("Total clusters saved: "+clustersCount);
        */
    }

    public void indexFile(File file) throws FileNotFoundException, IOException {

        int cid = Integer.parseInt(file.getName());
        BufferedReader br = new BufferedReader(new FileReader(file));
        StringBuffer sb = new StringBuffer();
        String line = null;
 
        while((line = br.readLine())!=null){
            sb.append(line).append(" ");
        }
        Document doc = new Document();
        doc.add(new IntField("docId", cid, Field.Store.YES));
        doc.add(new Field("content", sb.toString(), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));

        indexWriter.addDocument(doc);
        System.out.println(++fileIndexedCounter + " indexing clusters");
    }

    /**
     * Creates the Reference Directed Index
     * @throws java.lang.Exception
     */
    public void createRDIndex() throws Exception {
        rdIndexPath = prop.getProperty("rdIndexPath");

        if(null == rdIndexPath) {
            System.err.println("Error: rdIndexPath missing in prop file");
            System.exit(1);
        }

        File rdContentDirFile = new File(rdContentDirPath);
        File[] files = rdContentDirFile.listFiles();
        for (File file : files) {
            System.out.println("Indexing file: " + file.getAbsolutePath());
            processFile(file);
        }

    }

    public static void main(String[] args) throws Exception {

        if(args.length!=1) {
            System.err.println("Usage: java %s <prop.path>");
            System.out.println("Content of prop file:\n"
                    + "CSXCollSpec = <path-of-xml-spec-file>\n" +
                    "RD_ContentDirPath = <path-in-which-rd-files-will-be-stored>\n");
            System.exit(1);
        }
//        args = new String[2];
//        args[0] = "/home/dwaipayan/citereco/rdi.properties";
        RDFileWriting rdi = new RDFileWriting(args[0]);
        rdi.createRDContent();
        rdi.fileIndexedCounter = 0;
    }

}
