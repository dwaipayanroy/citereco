/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package citereco;

/**
 *
 * @author dwaipayan
 */
class PerTermStat {
    String term;
    long cf;
    long df;

    public PerTermStat() {        
    }

    public PerTermStat(String term, long cf, long df) {
        this.term = term;
        this.cf = cf;
        this.df = df;
    }
}
