/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package citereco;

import java.util.Comparator;

/**
 *
 * @author dwaipayan
 */
class TrecRes implements Comparable<TrecRes>, Comparator<TrecRes> {
    int query_num;
    String q0;
    String docId;
    int rank;
    float score;
    String run_name;
    int luceneDocId;

    public TrecRes() {
        query_num = 0;
        q0 = "";
        docId = "";
        rank = 0;
        score = (float) 0.0;
        run_name = "";
    }

    public TrecRes(int query_num, String docId, int rank, float score, String run_name) {
        this.query_num = query_num;
        this.docId = docId;
        this.rank = rank;
        this.score = score;
        this.run_name = run_name;
    }

    @Override
    public int compareTo(TrecRes t) {
        if(t.docId.equals(this.docId)){
            return (this.rank - t.rank);
        }
        else
            return this.docId.compareTo(t.docId);
    }

    @Override
    public int compare(TrecRes t1, TrecRes t2) {
        if(t1.docId.equals(t2.docId)){
            return (t1.rank - t2.rank);
        }
        else
            return t1.docId.compareTo(t2.docId);
    }
}
