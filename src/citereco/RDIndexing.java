/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package citereco;

import common.EnglishAnalyzerWithSmartStopword;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 *
 * @author dwaipayan
 */

public class RDIndexing {

    Properties  prop;
    String      rdContentDirPath;   // directory path, where the reference directed content files are saved
    String      rdIndexPath;        // index path of the reference directed content
    Analyzer    analyzer;
    IndexWriter indexWriter;
    String      dumpPath;
    
    int         fileIndexedCounter; 
    int         clustersCount;      // number of clusters saved

    static final public String FIELD_ID = "docid";
    static final public String FIELD_BOW = "content";       // ANALYZED bow content


    public RDIndexing(String propPath) throws IOException {

        // +++++ Properties file loading
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
        // ----- Properties file loaded

        // +++++ setting path in which the RD contents are stored
        if(prop.containsKey("RD_ContentDirPath")) {
            rdContentDirPath = prop.getProperty("RD_ContentDirPath");
            File fl = new File(rdContentDirPath);
            //if file not exists: 
            if(!fl.exists()) {
                System.err.println("Error: "+rdContentDirPath+" not exists");
                System.exit(1);
                //System.out.println(fl.delete());
            }
        }

        else {
            System.err.println("Error: Prop file missing RD_ContentDirPath");
            System.exit(1);
        }
        // ----- 

        // +++++ path in which the RDIndex will be stored
        if(prop.containsKey("RD_IndexDirPath")) {
            rdIndexPath = prop.getProperty("RD_IndexDirPath");
            Directory indexDir = FSDirectory.open(new File(rdIndexPath).toPath());

            if (DirectoryReader.indexExists(indexDir)) {
                System.err.println("Index exists in "+rdIndexPath);
                System.exit(1);
            }
            else {
                // setting the analyzer with WhiteSpace Analyzer as the content will be already analyzed
                IndexWriterConfig iwcfg = new IndexWriterConfig(new WhitespaceAnalyzer());
                iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
                indexWriter = new IndexWriter(indexDir, iwcfg);
                System.out.println("RDIndex will be created in: "+rdIndexPath);
            }
        }
        else {
            System.err.println("Error: Prop file missing RD_IndexDirPath");
            System.exit(1);
        }
        // -----

        // +++++ setting analyzer
        EnglishAnalyzerWithSmartStopword obj = new EnglishAnalyzerWithSmartStopword();
        analyzer = obj.setAndGetEnglishAnalyzerWithSmartStopword();
        // ----- analyzer set 

        // +++++ setting dumpPath
        if(prop.containsKey("dumpPath")) {
            dumpPath = prop.getProperty("dumpPath");
            System.out.println("The index will be dumped in: "+dumpPath);

            File fl = new File(dumpPath);
            //if file exists, delete it
            if(fl.exists()) {
                System.err.println("Error: file exists in dumpPath: "+dumpPath);
                System.exit(1);
                //System.out.println(fl.delete());
            }
        }
        // ----- dumpPath set
    }


    public void indexRDContentFile(File file) throws FileNotFoundException, IOException {

        int cid = Integer.parseInt(file.getName()); // cid = filename = doc-name
        BufferedReader br = new BufferedReader(new FileReader(file));
        StringBuffer sb = new StringBuffer();
        String line = null;
 
        while((line = br.readLine())!=null)
            sb.append(line).append(" ");
        br.close();

        //System.out.println("RAW: "+sb.toString());
        String searchPattern = "=-=\\d+-=-";
        String replacePattern = String.format(" cluster%d ", cid);  // observe the white-spaces before and after the cluster#
        String processed = sb.toString().replaceAll(searchPattern, replacePattern);


        StringBuffer tokenizedContentBuff = new StringBuffer();

        TokenStream stream = analyzer.tokenStream(RDIndexing.FIELD_BOW, 
            new StringReader(processed));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
        stream.reset();

        while (stream.incrementToken()) {
            String term = termAtt.toString();
            tokenizedContentBuff.append(term).append(" ");
        }
        stream.end();
        stream.close();

        //System.out.println("TOKENIZED:"+tokenizedContentBuff.toString());
        //char ch = (char) System.in.read();

        Document doc = new Document();
        doc.add(new IntField(RDIndexing.FIELD_ID, cid, Field.Store.YES));
        doc.add(new Field(RDIndexing.FIELD_BOW, tokenizedContentBuff.toString(), 
            Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.YES));
        // NOTE: Here, the whitespace analyzer is used 

        indexWriter.addDocument(doc);
        System.out.println(++fileIndexedCounter + " indexed cluster-count");

        // dumping the content
        if(prop.containsKey("dumpPath")) {
            FileWriter fileWritter = new FileWriter(dumpPath, true);
            BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
            bufferWritter.write(tokenizedContentBuff.toString()+"\n");
            bufferWritter.close();
        }
        //
    }

    /**
     * Creates the Reference Directed Index
     * @throws java.lang.Exception
     */
    public void createRDIndex() throws Exception {

        File rdContentDirFile = new File(rdContentDirPath);
        File[] files = rdContentDirFile.listFiles();
        for (File file : files) {
            System.out.println("Indexing file: " + file.getAbsolutePath());
            indexRDContentFile(file);
        }
        indexWriter.close();
    }

    public static void main(String[] args) throws Exception {

        System.out.println("This program runs on the output by the program: "
            + "RDFileWriting\n");
        String usage = "Usage java RDIndexing <prop.path>\n"
            + "Property file must contain:\n"
            + "1. RD_ContentDirPath = dir. path in which the RD contents are stored\n"
            + "2. RD_IndexDirPath = dir. path in which the RD index will be stored\n"
            + "3. stopFile = path of the stopword list file\n"
            + "4. [OPTIONAL] dumpPath = path of the file to dump the content";
        System.out.println(usage);

        args = new String[2];
        args[0] = "/home/dwaipayan/citereco/rdi.properties";
        RDIndexing rdi = new RDIndexing(args[0]);
        rdi.createRDIndex();
        System.out.println(rdi.fileIndexedCounter+" cluster information indexed");
    }

}
