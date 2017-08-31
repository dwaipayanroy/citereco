package citereco;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class DocVector {
    Properties prop;        // the properties file
    IndexReader reader;
    IndexSearcher searcher;

    public DocVector(String propFile) throws IOException {
        prop = new Properties();
        prop.load(new FileReader(propFile));
        Directory directory = FSDirectory.open(new File(prop.getProperty("indexPath")).toPath());
        reader = DirectoryReader.open(directory);
        searcher = new IndexSearcher(reader);
    }

    private void getDocumentVector(String doi, String fieldName) throws ParseException, IOException {
    /*  Give the term vector of document 'doi' of the field 'fieldName' 
        fieldName = 'abstract', 'contexts', 'title'     */
        ScoreDoc[] hits;
        TopDocs topDocs;

        Analyzer analyzer = new StandardAnalyzer();
        QueryParser queryParser = new QueryParser(CSXIndexer.FIELD_ID, analyzer);
        Query query = queryParser.parse(doi);
        topDocs = searcher.search(query, 1);

        hits = topDocs.scoreDocs;
        if(hits == null) {
            System.out.println("Document not found");
        }
        else {
            Document d = searcher.doc(hits[0].doc);
            System.out.println("Lucene DocNum: "+hits[0].doc);
            System.out.println(doi + ": " + d.get(CSXIndexer.FIELD_CID));
            Terms vector = reader.getTermVector(hits[0].doc, fieldName);

            TermsEnum termsEnum = null;
            termsEnum = vector.iterator();
            BytesRef text;
            while ((text = termsEnum.next()) != null) {
                String term = text.utf8ToString();
                System.out.print(term+" ");
                System.out.println(termsEnum.totalTermFreq());
            }
        }
    }

    public static void main(String[] args) throws IOException, ParseException {
        if(args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java DocVector <prop-file>");
            args[0] = "/home/dwaipayan/citereco/init.properties";
            //System.exit(0);
        }

        DocVector docVector = new DocVector(args[0]);
        
        String doi = "10.1.1.1.1997";
        docVector.getDocumentVector(doi, CSXIndexer.FIELD_ABSTRACT);
    }
}