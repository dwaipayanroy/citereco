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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author dwaipayan
 */
class PaperSearcherExpanded {

    Properties prop;        // the properties file
    IndexReader reader;
    IndexSearcher searcher;
    PaperAnalyzer paperAnalyzer;

//    QueryParser queryParser;
    MultiFieldQueryParser mFQueryParser;
    String queryFile;
    
    int num_wanted;
    String run_name;

    public PaperSearcherExpanded(String rPropFile) throws IOException {
        prop = new Properties();
        prop.load(new FileReader(rPropFile));

        String indexDirectoryPath = prop.getProperty("indexPath");
        reader = DirectoryReader.open(FSDirectory.open(new File(indexDirectoryPath).toPath()));
        searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new LMJelinekMercerSimilarity((float) 0.5));
//        searcher.setSimilarity(new DFRSimilarity(new BasicModelBE(), new AfterEffectL (), new NormalizationH1()));
        paperAnalyzer = new PaperAnalyzer(rPropFile);
//        queryParser = new QueryParser(Version.LUCENE_4_9, "abstract", new StandardAnalyzer(Version.LUCENE_4_9));

        String []fields = new String[]{"abstract", "title", "context"};
        mFQueryParser = new MultiFieldQueryParser(fields, new StandardAnalyzer());
        
        num_wanted = Integer.parseInt(prop.getProperty("retrieve.num_wanted","100"));
        run_name = prop.getProperty("retrieve.run_name");
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



    private void retrieveAll() throws IOException, Exception {
        ScoreDoc[] hits = null;
        TopDocs topDocs = null;

        String resultsFile; // path of the result file
        
        resultsFile = prop.getProperty("resPath");
        FileWriter fw = new FileWriter(resultsFile);

        List<CSQuery> queries = constructQueries();
//        System.out.println(queries.size());
        for (CSQuery q : queries) {
            String query_str;
            System.out.println("Query: "+q.query_num);
            int invalid_doc=0;

            TopScoreDocCollector collector = TopScoreDocCollector.create(num_wanted);
            paperAnalyzer.setAnalyzer();
            query_str = q.getBOWQuery(paperAnalyzer.getAnalyzer(), 1);
            query_str = paperAnalyzer.refineTexts(query_str);

            List<TrecRes> resList = new ArrayList<>();

//            searcher.search(query, collector);
            searcher.search(mFQueryParser.parse(query_str), collector);
            topDocs = collector.topDocs();
            hits = topDocs.scoreDocs;
            if(hits == null)
                System.out.println("Nothing found");

            int hits_length = hits.length;
            for (int i = 0; i < hits_length; ++i) {
                Document d = searcher.doc(hits[i].doc);
                int ret_val = Integer.parseInt(d.get(CSXIndexer.FIELD_CID));
                String ret_cid;
                ret_cid = (String) ((ret_val==0)?"invalid"+invalid_doc++:Integer.toString(ret_val));

                resList.add(new TrecRes(q.query_num, ret_cid, i, hits[i].score, run_name));
            }
            
            Collections.sort (resList);

            List<TrecRes> finalResList = new ArrayList<>();
            finalResList.add(new TrecRes(q.query_num, resList.get(0).docId, resList.get(0).rank, resList.get(0).score, run_name));
            String last_docid = resList.get(0).docId;

            for(int i=1; i<hits_length; i++) {
                if(last_docid.equals(resList.get(i).docId)) {
                }
                else {
                    last_docid = resList.get(i).docId;
                    finalResList.add(new TrecRes(q.query_num, resList.get(i).docId, resList.get(i).rank, resList.get(i).score, run_name));
                }
            }

            Collections.sort (finalResList, new Comparator<TrecRes>(){
                @Override
                public int compare(TrecRes r1,TrecRes r2){
                    return r1.rank - r2.rank;
                }}
            );

            StringBuffer buff = new StringBuffer();

            int res_length = finalResList.size();
            for (int i = 0; i < res_length; ++i) {
                buff.append(finalResList.get(i).query_num).append("\tQ0\t").
                    append(finalResList.get(i).docId).append("\t").
                    append(i).append("\t").
                    append(finalResList.get(i).score).append("\t").
                    append(run_name).append("\n");                
            }
            fw.write(buff.toString());
/*
            for (int i = 0; i < hits_length; ++i) {
                int docId = hits[i].doc;
                Document d = searcher.doc(docId);
                int ret_cid = Integer.parseInt(d.get(CSXIndexer.FIELD_CID));
                buff.append(q.query_num).append("\tQ0\t").
                    append((ret_cid==0)?"invalid"+invalid_doc++:ret_cid).append("\t").
                    append((i)).append("\t").
                    append(hits[i].score).append("\t").
                    append(run_name).append("\n");                
            }
            fw.write(buff.toString());
*/
        }
        fw.close();

    }
    
    public static void main(String[] args) throws Exception {

        // TODO code application logic here
        if (args.length == 0) {
            args = new String[2];
            //System.out.println("Usage: java PaperSearcher <prop-file>");
            args[0] = "init.properties";
            //args[0] = "retrieve.peoperties";
        }

        PaperSearcherExpanded searcher = new PaperSearcherExpanded(args[0]);
        searcher.retrieveAll();
        searcher.close();
    }

}
