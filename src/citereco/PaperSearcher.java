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
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author dwaipayan
 */
class PaperSearcher {

    Properties prop;        // the properties file
    IndexReader reader;
    IndexSearcher searcher;
    PaperAnalyzer paperAnalyzer;

//    QueryParser queryParser;
    MultiFieldQueryParser mFQueryParser;
    String queryFile;

    List<CSQuery> queries;

    String resultsFile; // path of the result file

    WordVecs wv;

    int num_wanted;
    String run_name;

    public PaperSearcher(String rPropFile) throws IOException, Exception {
        prop = new Properties();
        prop.load(new FileReader(rPropFile));

        String indexDirectoryPath = prop.getProperty("indexPath");
        reader = DirectoryReader.open(FSDirectory.open(new File(indexDirectoryPath).toPath()));
        searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new LMJelinekMercerSimilarity((float) 0.5));
        searcher.setSimilarity(new BM25Similarity (1.4F, 1.0F));
        paperAnalyzer = new PaperAnalyzer(rPropFile);
//        queryParser = new QueryParser(Version.LUCENE_4_9, "abstract", new StandardAnalyzer(Version.LUCENE_4_9));

        String []fields = new String[]{"abstract", "title", "contexts"};

        /* using the same analyzer which is used for indexing */
        Analyzer engAnalyzer = paperAnalyzer.getAnalyzer();
        mFQueryParser = new MultiFieldQueryParser(fields, engAnalyzer);
        /* */
//        mFQueryParser = new MultiFieldQueryParser(fields, new StandardAnalyzer());
        
        num_wanted = Integer.parseInt(prop.getProperty("retrieve.num_wanted","100"));
        run_name = prop.getProperty("retrieve.run_name");
        resultsFile = prop.getProperty("resPath")+"1.4-1.0-100.res";
        wv = new WordVecs(rPropFile);
        /* setting query list*/
        queries = constructQueries();
        /* queries has all the raw data read from the query file like: 
            query_num, paper_title, paper_abtract, context etc.
        */
    }

    public PaperSearcher(String rPropFile, int setSimilarityFlag, int num_ret, float param1, float param2, int setDocFieldFlag, int setQueryFieldFlag, int useWVFlag) throws IOException, Exception {
        prop = new Properties();
        prop.load(new FileReader(rPropFile));

        String indexDirectoryPath = prop.getProperty("indexPath");
        reader = DirectoryReader.open(FSDirectory.open(new File(indexDirectoryPath).toPath()));

        /* setting the similarity function */
        /* 1-BM25, 2-LM-JM, 3-LM-D, 4-DefaultLuceneSimilarity */
        setSimilarityFn_ResFileName(num_ret, setSimilarityFlag, param1, param2, setDocFieldFlag, setQueryFieldFlag);
        /* */
        paperAnalyzer = new PaperAnalyzer(rPropFile);
//        queryParser = new QueryParser(Version.LUCENE_4_9, "abstract", new StandardAnalyzer(Version.LUCENE_4_9));

        /* setting the fields in which the searching will be perfomed */
        String []fields = setDocFieldsToSearch(setDocFieldFlag);
//        String []fields = new String[]{"abstract", "title", "contexts"};
        /* === */

        /* using the same analyzer which is used for indexing */
        Analyzer engAnalyzer = paperAnalyzer.getAnalyzer();
        mFQueryParser = new MultiFieldQueryParser(fields, engAnalyzer);
        /* setting 'fields' to search in the document end */
        /* */

        /* max number of files to return for each query */

//        num_wanted = Integer.parseInt(prop.getProperty("retrieve.num_wanted","100"));
        num_wanted = num_ret;
//        run_name = prop.getProperty("retrieve.run_name");
//        resultsFile = prop.getProperty("resPath");
        if(useWVFlag==1)
            wv = new WordVecs(rPropFile);
        
        /* setting query list*/
        queries = constructQueries();
        /* queries has all the RAW data read from the query file like: 
            query_num, paper_title, paper_abtract, context etc.
        */
        System.out.println("Result will be saved in: "+resultsFile);

    }

    public void close() throws IOException{
        reader.close();
    }

    /*
    * setFieldsFlag-
    *** 1: fields[] = {"abstract", "title", "contexts"}
    *** 2: fields[] = {"abstract", "title"}
    *** 3: fields[] = {"contexts"}
    */
    private String[] setDocFieldsToSearch(int setFieldsFlag) {
        String []fields = new String[3];

        switch (setFieldsFlag) {
            case 1:
                fields = new String[]{"abstract", "title", "contexts"};
                System.out.println("Setting fields: abstract, title, contexts");
                break;
            case 2:
                fields = new String[]{"abstract", "title"};
                System.out.println("Setting fields: abstract, title");
                break;
        }
        return fields;
    }


    List<CSQuery> constructQueries() throws Exception {
        queryFile = prop.getProperty("query.file");
        CSQueryParser parser = new CSQueryParser(queryFile, prop);

        parser.parse();
        return parser.queries;
    }

    /*
    * setSimilarityFlag-
    *** 1: BM25Similarity
    *** 2: LMJelinekMercerSimilarity
    *** 3: LMDirichletSimilarity
    *** 4: DefaultSimilarity
    * BM25Similarity-            f1: k1,    f2: b
    * LMJelinekMercerSimilarity- f1: lambda f2: dummy
    * LMDirichletSimilarity-     f1: mu     f2: dummy
    * DefaultSimilarity-         f1: dummy  f2: dummy
    *
    * setDocField- Search Document:  1-TAC, 2-TA
    * setQueryField- Search Query:  1-TAC, 2-C
    */
    private void setSimilarityFn_ResFileName(int num_ret, int setSimilarityFlag, float f1, float f2, int setDocField, int setQueryField) {
        String dField[] = {"Dtac", "Dta"};
        String qField[] = {"Qtac", "Qc"};
        searcher = new IndexSearcher(reader);
//        num_wanted = num_ret;

        switch(setSimilarityFlag){
            case 1:
//                searcher.setSimilarity(new BM25Similarity (1.4F, 1.0F));
                System.out.println("Setting BM25 with k1: "+f1+" b: "+f2);
                searcher.setSimilarity(new BM25Similarity (f1, f2));
                run_name = "bm25-"+f1+"-"+f2+"-"+num_ret;
                break;
            case 2:
                System.out.println("Setting LMJelinekMercer with lambda: "+f1);
                searcher.setSimilarity(new LMJelinekMercerSimilarity(f1));
                run_name = "lm-jm-"+f1+"-"+num_ret;
                break;
            case 3:
                System.out.println("Setting LMDirichlet with lambda: "+f1);
                searcher.setSimilarity(new LMDirichletSimilarity(f1));
                run_name = "lm-d-"+f1+"-"+num_ret;
                break;
            case 4:
                System.out.println("Setting DefaultSimilarity of Lucene: ");
                searcher.setSimilarity(new DefaultSimilarity());
                run_name = "default-lucene-"+num_ret;
                break;
        }
        run_name = run_name +"-"+dField[setDocField-1]+"-"+qField[setQueryField-1];
        resultsFile = run_name;
        resultsFile = resultsFile + ".res";
    }

    private void retrieveAll(int queryFieldFlag, int useW2Vflag) throws IOException, Exception {
        ScoreDoc[] hits = null;
        TopDocs topDocs = null;

//        queries = constructQueries();
//        /* queries has all the raw data read from the query file like: 
//            query_num, paper_title, paper_abtract, context etc.
//        */

        if(useW2Vflag != 1) {

            System.out.println("Using BOW query:");

            FileWriter fw = new FileWriter(resultsFile);

            int query_searched_count = 0;
            for (CSQuery q : queries) {
                String query_str;
                System.out.println("Query: "+q.query_num+": ");

                paperAnalyzer.setAnalyzer();
                query_str = q.getBOWQuery(paperAnalyzer.getAnalyzer(), queryFieldFlag);

    //            System.out.println(query_str);

                TopScoreDocCollector collector = TopScoreDocCollector.create(num_wanted);
    //            searcher.search(query, collector);
                searcher.search(mFQueryParser.parse(query_str), collector);
                topDocs = collector.topDocs();
                hits = topDocs.scoreDocs;
                if(hits == null)
                    System.out.println("Nothing found");

                int invalid_doc=0;
                /* deduplication of documents in result */
                List<TrecRes> resList = new ArrayList<>();

                int hits_length = hits.length;
                for (int i = 0; i < hits_length; ++i) {
                    Document d = searcher.doc(hits[i].doc);
                    int ret_val = Integer.parseInt(d.get(CSXIndexer.FIELD_CID));
                    String ret_cid;
                    ret_cid = (String) ((ret_val==0)?"-"+ ++invalid_doc:Integer.toString(ret_val));

                    TrecRes tres = new TrecRes(q.query_num, ret_cid, i, hits[i].score, run_name);
                    tres.luceneDocId = hits[i].doc;
                    resList.add(tres);
                }

                Collections.sort (resList);

                List<TrecRes> finalResList = new ArrayList<>();
    //            finalResList.add(new TrecRes(q.query_num, resList.get(0).docId, resList.get(0).rank, resList.get(0).score, run_name));
                String last_docid = "-1";

                for(int i=0; i<hits_length; i++) {
                    if((last_docid.equals(resList.get(i).docId)) || (q.cid == Integer.parseInt(resList.get(i).docId))) {
                    }
                    else {
                        last_docid = resList.get(i).docId;
                        finalResList.add(new TrecRes(q.query_num, resList.get(i).docId, resList.get(i).rank, resList.get(i).score, run_name));
                    }
                }

                if (Boolean.parseBoolean(prop.getProperty("usetm", "false")))
                    rerankUsingTM(finalResList);
                
                Collections.sort (finalResList, new Comparator<TrecRes>(){
                    @Override
                    public int compare(TrecRes r1,TrecRes r2){
                        return r1.rank - r2.rank;
                    }}
                );
                /* decuplication ends */

                /* writing the result in file */
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
                /* writing the result in file ends */
                query_searched_count++;
            }
            System.out.println(query_searched_count + " queries searched");
            fw.close();
        }

        else {
            System.out.println("Using word2vec to expand the query:");
            wv.loadPrecomputedNNs();
            resultsFile = "wvExpand-"+resultsFile;
            run_name = "wvExpand-"+run_name;
            FileWriter fw = new FileWriter(resultsFile);

//            BooleanQuery.setMaxClauseCount(5120);
            int query_searched_count = 0;
            for (CSQuery q : queries) {
                String query_str;
                System.out.println("Query: "+q.query_num+": ");

                paperAnalyzer.setAnalyzer();
                query_str = q.getWVExpandedBOWQuery(paperAnalyzer.getAnalyzer(), wv, queryFieldFlag);

    //            System.out.println(query_str);

                TopScoreDocCollector collector = TopScoreDocCollector.create(num_wanted);
    //            searcher.search(query, collector);
                /* == */
                boolean retry = true;
                while (retry) {
                    try {
                        retry = false;
                        searcher.search(mFQueryParser.parse(query_str), collector);
                    }
                    catch (BooleanQuery.TooManyClauses e) {
                        // Double the number of boolean queries allowed.
                        // The default is in org.apache.lucene.search.BooleanQuery and is 1024.
                        String defaultQueries = Integer.toString(BooleanQuery.getMaxClauseCount());
                        int oldQueries = Integer.parseInt(System.getProperty("org.apache.lucene.maxClauseCount", defaultQueries));
                        int newQueries = oldQueries * 2;
                        System.err.printf("Too many hits for query: " + oldQueries + ".  Increasing to " + newQueries, e);
                        System.setProperty("org.apache.lucene.maxClauseCount", Integer.toString(newQueries));
                        BooleanQuery.setMaxClauseCount(newQueries);
                        retry = true;
                    }
                }
                /* */
                topDocs = collector.topDocs();
                hits = topDocs.scoreDocs;
                if(hits == null)
                    System.out.println("Nothing found");

                int invalid_doc=0;
                /* deduplication of documents in result */
                List<TrecRes> resList = new ArrayList<>();

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
    //            finalResList.add(new TrecRes(q.query_num, resList.get(0).docId, resList.get(0).rank, resList.get(0).score, run_name));
                String last_docid = "-1";

                for(int i=0; i<hits_length; i++) {
                    if((last_docid.equals(resList.get(i).docId)) || (q.cid == Integer.parseInt(resList.get(i).docId))) {
                        /* document-deduplication || query-file-retrieved */
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
                /* decuplication ends */

                /* writing the result in file */
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
                /* writing the result in file ends */
                query_searched_count++;
            }
            System.out.println(query_searched_count + " queries searched");
            fw.close();
        }

    }

    /* reranking using Translation Model */
    void rerankUsingTM(List<TrecRes> retList) throws IOException {
        BytesRef term = null;
        int N = reader.numDocs();
        
        for (TrecRes trecRes : retList) {
            Terms terms = reader.getTermVector(trecRes.luceneDocId, CSXIndexer.FIELD_CONTEXTS);
            if (terms == null || terms.size() == 0)
                continue;            
            
            float score = 0;
            TermsEnum termsEnum = terms.iterator(); // access the terms for this field
            while ((term = termsEnum.next()) != null) {// explore the terms for this field
                Term t = new Term(CSXIndexer.FIELD_CONTEXTS, term);
                DocsEnum docsEnum = termsEnum.docs(null, null); // enumerate through documents, in this case only one                    
                int docIdEnum;

                while ((docIdEnum = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
                    //get the term frequency in the document
                    int tf = docsEnum.freq();
                    int df = (int)(reader.totalTermFreq(t));
                    float idf = N/(float)df;
                    float tfidf = tf*idf;
                    score += tfidf;
                }                    
            }
            trecRes.score = score;
        }
        Collections.sort (retList, new Comparator<TrecRes>(){
            @Override
            public int compare(TrecRes r1, TrecRes r2){
                return r1.score < r2.score? 1 : r1.score == r2.score? 0 : -1;
            }}
        );
    }
    /* TM reranking ends */

    public static void main(String[] args) throws Exception {
        /*
        args[0] = String init.properties
        args[1] = int flag: 1- BM25, 2- LM-JM, 3- LM-D
        args[2] = int num_ret: number of result to be returned for each query
        args[3] = float param1
        args[4] = float param2 (dummy 0 for LM-*)
        args[5] = int setDocFieldFlag:   1- all-three-fields(TAC), 2- title-abstract(TA)
        args[6] = int setQueryFieldFlag: 1- all-three-fields(TAC), 2- contexts(C)
        args[7] = int useW2Vflag 1: use WV to expand, 2: simple BOW
        */

        // TODO code application logic here
        if (args.length == 0) {
            args = new String[10];
            //System.out.println("Usage: java PaperSearcher <prop-file>");
            args[0] = "init.properties";
            System.out.println("args[0] = String init.properties");
            System.out.println("args[1] = int flag: 1- BM25, 2- LM-JM, 3- LM-D, 4- Default Lucene(Tf-Idf)");
            System.out.println("args[2] = int num_ret: number of result to be returned for each query");
            System.out.println("args[3] = float param1");
            System.out.println("args[4] = float param2 (dummy 0 for LM-*, Default)");
            System.out.println("args[5] = int setDocFieldFlag: 1- all-three-fields(TAC), 2- title-abstract(TA)");
            System.out.println("args[6] = int setQueryFieldFlag: 1- all-three-fields(TAC), 2- contexts(C)");
            System.out.println("args[7] = int useW2Vflag 1: use WV to expand, 2: simple BOW");

            args[0] = "/home/dwaipayan/citereco/init.properties";
            args[1] = "1"; //int flag: 1- BM25, 2- LM-JM, 3- LM-D
            args[2] = "100"; //int num_ret: number of result to be returned for each query
            args[3] = "1.4"; // float param1
            args[4] = "0.5"; // float param2 (dummy 0 for LM-*)
            args[5] = "1"; // int setDocFieldFlag:   1- all-three-fields(TAC), 2- title-abstract(TA)
            args[6] = "1"; // int setQueryFieldFlag: 1- all-three-fields(TAC), 2- contexts(C)
            args[7] = "2"; // int useW2Vflag 1: use WV to expand, 2: simple BOW
            System.exit(1);
        }

        PaperSearcher searcher = new PaperSearcher(args[0], Integer.parseInt(args[1]), 
            Integer.parseInt(args[2]), Float.parseFloat(args[3]), Float.parseFloat(args[4]), 
            Integer.parseInt(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]));
        searcher.retrieveAll(Integer.parseInt(args[6]), Integer.parseInt(args[7]));
        searcher.close();
    }
}