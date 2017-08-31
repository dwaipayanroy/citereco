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
import java.util.List;
import java.util.Properties;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author dwaipayan
 */
class DumpContext {

    Properties prop;        // the properties file
    IndexReader reader;
    IndexSearcher searcher;
    PaperAnalyzer paperAnalyzer;

//    QueryParser queryParser;
    MultiFieldQueryParser mFQueryParser;
    String queryFile;
    
    public DumpContext(String propFile) throws IOException {
        prop = new Properties();
        prop.load(new FileReader(propFile));

        String indexDirectoryPath = prop.getProperty("indexPath");
        reader = DirectoryReader.open(FSDirectory.open(new File(indexDirectoryPath).toPath()));
        searcher = new IndexSearcher(reader);

        String []fields = new String[]{"abstract", "title", "contexts"};
        paperAnalyzer = new PaperAnalyzer(propFile);
    }

    public void close() throws IOException{
        reader.close();
    }

    List<CSQuery> constructQueries() throws Exception {
        queryFile = prop.getProperty("query.file");
        CSQueryParser parser = new CSQueryParser(queryFile, prop);

        parser.parse();
        return parser.queries;
    }



    private void dumpContext() throws IOException, Exception {
        List<CSQuery> queries = constructQueries();
        /* queries has all the raw data read from the query file like: 
            query_num, paper_title, paper_abtract, context etc.
        */

        String contextFile = prop.getProperty("index.dumpContext");
        FileWriter fw = new FileWriter(contextFile);
//        System.out.println(queries.size());
        for (CSQuery q : queries) {
            String query_str="";
            System.out.print("Query:\t"+q.query_num);
            System.out.println(":\t"+q.context);

            paperAnalyzer.setAnalyzer();
            String[] terms =  q.getContextOfQuery(paperAnalyzer.getAnalyzer(), q.context).split("\\s+");
//            query_str = q.getContextOfQuery(paperAnalyzer.getAnalyzer(), q.context).split("\\s+");
            for (String term : terms) {
                query_str = query_str+" "+term;
            }

            query_str = query_str.replace(" ", "\n");
//            System.out.println(query_str);
            fw.write(query_str.toString());
        }
        fw.close();

    }

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            args = new String[2];
            args[0] = "/home/dwaipayan/citereco/init.properties";
        }

        DumpContext dumpContext = new DumpContext(args[0]);
        dumpContext.dumpContext();
        dumpContext.close();
    }

}
