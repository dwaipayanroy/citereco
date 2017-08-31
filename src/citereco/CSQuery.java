/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package citereco;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.search.Query;

/**
 *
 * @author dwaipayan
 */
class CSQuery {
//    public String filename;
    public int paper_num;
    public int query_num;
    public int cid;
    public String doi;
    public String paper_title;
    public String paper_abstract;
    public String context;
    public StringBuffer contexts;
    public StringBuffer other;
    public Query  luceneQuery;

//    public Query getQuery(Analyzer analyzer) throws Exception {
//        BooleanQuery q = new BooleanQuery();
//        Term thisTerm;
//        
//        String[] terms = analyzeQuery(analyzer, paper_title, paper_abstract, context).split("\\s+");
//        for (String term : terms) {
//            thisTerm = new Term(term);
//            Query tq = new TermQuery(thisTerm);
//            q.add(tq, BooleanClause.Occur.SHOULD);
//        }
//        return q;
//    }

    private String analyzeQuery(Analyzer analyzer, String title, String abstrct, String context, int queryFieldFlag) throws IOException {
        StringBuffer buff = new StringBuffer(); 
        TokenStream stream;
        CharTermAttribute termAtt;

        /* Adding Title and Abstract (TA) in the query */
        if (queryFieldFlag == 1) {
            /* using TAC in the query */
            title = PaperAnalyzer.refineTexts(title);
            stream = analyzer.tokenStream(CSXIndexer.FIELD_TITLE, new StringReader(title));
            termAtt = stream.addAttribute(CharTermAttribute.class);
            stream.reset();

            while (stream.incrementToken()) {
                String term = termAtt.toString();
                if(term!=null && !PaperAnalyzer.isNumerical(term))
                    buff.append(term).append(" ");
            }
            stream.end();
            stream.close();

            abstrct = PaperAnalyzer.refineTexts(abstrct);
            stream = analyzer.tokenStream(CSXIndexer.FIELD_ABSTRACT, new StringReader(abstrct));
            termAtt = stream.addAttribute(CharTermAttribute.class);
            stream.reset();

            while (stream.incrementToken()) {
                String term = termAtt.toString();
                if(term!=null && !PaperAnalyzer.isNumerical(term))
                    buff.append(term).append(" ");
            }

            stream.end();
            stream.close();
        }
        /* === Added Title and Abstract (TA) in the query === */

        /* Adding context (C) in the query */
        context = PaperAnalyzer.removeAnchor(context, 0);
        context = PaperAnalyzer.refineTexts(context);
        stream = analyzer.tokenStream(CSXIndexer.FIELD_CONTEXTS, new StringReader(context));
        termAtt = stream.addAttribute(CharTermAttribute.class);
        stream.reset();

        while (stream.incrementToken()) {
            String term = termAtt.toString();
            if(term!=null && !PaperAnalyzer.isNumerical(term))
                buff.append(term).append(" ");
        }

        stream.end();
        stream.close();
        /* === Added context (C) in the query === */

        return buff.toString();
    }

    public String getBOWQuery(Analyzer analyzer, int queryFieldFlag) throws Exception {
        String q = "";

        String[] terms = analyzeQuery(analyzer, paper_title, paper_abstract, context, queryFieldFlag).split("\\s+");
        for (String term : terms) {
            q = q+" "+term;
        }
/*        q=q.replaceAll(":", " "); */
        return q;
    }

    /* expand to form the query */
    public String getWVExpandedBOWQuery(Analyzer analyzer, WordVecs wv, int queryFieldFlag) throws IOException {
        String q="";

        String[] terms = expandQuery(analyzer, paper_title, paper_abstract, context, wv, queryFieldFlag).split("\\s+");
        for (String term : terms) {
            q = q+" "+term;
        }
//        q=q.replaceAll(":", " ");
        return q;
    }

    /* expands the fields of the query */
    /* which fields: so far, only 'context' */
    private String expandQuery(Analyzer analyzer, String title, String abstrct, String context, WordVecs wv, int queryFieldFlag) throws IOException {
        StringBuffer buff = new StringBuffer(); 
        TokenStream stream;
        CharTermAttribute termAtt;

        /* Adding Title and Abstract (TA) in the query */
        if (queryFieldFlag == 1) {
            /* using TAC in the query */
            /* not expanding the title and abstract of the query paper */
            title = PaperAnalyzer.refineTexts(title);
            stream = analyzer.tokenStream(CSXIndexer.FIELD_TITLE, new StringReader(title));
            termAtt = stream.addAttribute(CharTermAttribute.class);
            stream.reset();

            while (stream.incrementToken()) {
                String term = termAtt.toString();
                term = term.toLowerCase();
                if(term!=null && !PaperAnalyzer.isNumerical(term))
                    buff.append(term).append(" ");
            }
            stream.end();
            stream.close();

            abstrct = PaperAnalyzer.refineTexts(abstrct);
            stream = analyzer.tokenStream(CSXIndexer.FIELD_ABSTRACT, new StringReader(abstrct));
            termAtt = stream.addAttribute(CharTermAttribute.class);
            stream.reset();

            while (stream.incrementToken()) {
                String term = termAtt.toString();
                term = term.toLowerCase();
                if(term!=null && !PaperAnalyzer.isNumerical(term))
                    buff.append(term).append(" ");
            }

            stream.end();
            stream.close();
            /* not expanding the title and abstract of the query paper ends */
        }
        /* === Added Title and Abstract (TA) in the query === */


        /* Adding context (C) in the query */
        /* expanding the query by adding the similar words of contexts into the query */
        context = PaperAnalyzer.removeAnchor(context, 0);
        context = PaperAnalyzer.refineTexts(context);
        stream = analyzer.tokenStream(CSXIndexer.FIELD_CONTEXTS, new StringReader(context));
        termAtt = stream.addAttribute(CharTermAttribute.class);
        stream.reset();

        while (stream.incrementToken()) {
            String term = termAtt.toString();
            term = term.toLowerCase();
            /* expanding */
//            List<WordVec> nns = wv.getNearestNeighbors(term);
            if(term!=null && !PaperAnalyzer.isNumerical(term)) {
                buff.append(term).append(" ");
                List<WordVec> nns = wv.getKNearestNeighbors(term, 1);
                if(nns != null) {
                    for (WordVec nn : nns) {
                        if(nn.word!=null && !PaperAnalyzer.isNumerical(nn.word))
                            buff.append(nn.word).append(" ");
                    }
                }
                /* added all the similar words of 'term' into query */
            }
        }

        stream.end();
        stream.close();
        /* expanding ends */

        return buff.toString();
    }

    String getContextOfQuery(Analyzer analyzer, String context) throws IOException {
        context = PaperAnalyzer.removeAnchor(context, 0);
        context = PaperAnalyzer.refineTexts(context);
        TokenStream stream = analyzer.tokenStream(CSXIndexer.FIELD_CONTEXTS, new StringReader(context));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
        stream.reset();


        StringBuffer buff = new StringBuffer();
        while (stream.incrementToken()) {
            String term = termAtt.toString();
            if(term!=null && !PaperAnalyzer.isNumerical(term))
                buff.append(term).append(" ");
        }
        stream.end();
        stream.close();

        return buff.toString();
    }

}

class query_paperNum_cid_map {
    String doi;
    int cid;
    int paper_num;

    public query_paperNum_cid_map(int paper_num, String doi, int cid) {
        this.doi = doi;
        this.cid = cid;
        this.paper_num = paper_num;
    }
}
