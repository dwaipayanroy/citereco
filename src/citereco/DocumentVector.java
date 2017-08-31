/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package citereco;

import java.io.IOException;
import java.util.HashMap;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author dwaipayan
 */
public class DocumentVector {
    HashMap<String, PerTermStat>    docVec;
    int                             size;

    public DocumentVector() {
        docVec = new HashMap<>();
    }

    public DocumentVector(HashMap<String, PerTermStat> docVec, int size) {
        this.docVec = docVec;
        this.size = size;
    }

    /**
     * Returns the document vector for a document with lucene-docid=luceneDocId
     * Returns dv containing 
     *      1) docVec: a HashMap of (term,PerTermStat) type
     *      2) size : size of the document
     * @param luceneDocId
     * @param cs
     * @return
     * @throws IOException 
     */
    public DocumentVector getDocumentVector(int luceneDocId, CollectionStatistics cs) throws IOException {

        DocumentVector dv = new DocumentVector();
        int docSize = 0;

        if(cs.indexReader==null) {
            System.out.println("Error: null == indexReader in showDocumentVector(int,IndexReader)");
            System.exit(1);
        }

        // term vector for this document and field, or null if term vectors were not indexed
        Terms terms = cs.indexReader.getTermVector(luceneDocId, "content");
        if(null == terms) {
            System.err.println("Error: Term vectors not indexed");
            System.exit(1);
        }

        System.out.println(terms.size());
        TermsEnum iterator = terms.iterator();
        BytesRef byteRef = null;

        //* for each word in the document
        while((byteRef = iterator.next()) != null) {
            String term = new String(byteRef.bytes, byteRef.offset, byteRef.length);
            //int docFreq = iterator.docFreq();            // df of 'term'
            long termFreq = iterator.totalTermFreq();    // tf of 'term'
            //System.out.println(term+": tf: "+termFreq);
            docSize += termFreq;

            //* termFreq = cf, in a document; 1 = df, in a document
            dv.docVec.put(term, new PerTermStat(term, termFreq, 1));
        }
        dv.size = docSize;
        //System.out.println("DocSize: "+docSize);

        return dv;
    }

}
