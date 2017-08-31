/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package citereco;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author dwaipayan
 */
class CSQueryParser {
    Properties prop;
    StringBuffer        buff;      // Accumulation buffer for storing the current topic
    String              queryFileName;

    String title = "";
    String abstrct = "";
    StringBuilder contexts = new StringBuilder();
    StringBuilder other = new StringBuilder();
    

    public List<CSQuery> queries;
    final static String[] tags = {"title", "abstract", "query", "other"};    

    public CSQueryParser(String qFileName, Properties prop) {
       this.queryFileName = qFileName;
       buff = new StringBuffer();
       queries = new ArrayList<>();
        this.prop = prop;
    }

    public void parse() throws Exception {
    /* 
    *     sets paper_num, paper_title, paper_abstract, context,
    */
        FileReader fr = new FileReader(queryFileName);
        BufferedReader br = new BufferedReader(fr);
        String line;

        /* making the paper-num_cid map for the query */
        Scanner s = new Scanner(new File(prop.getProperty("query.paperNum_cid_map_path")));
        List<query_paperNum_cid_map> paperNum_cid_map = new ArrayList<>();
        while(s.hasNext()) {
            int p_num = s.nextInt();
            String doi = s.next();
            int cid = s.nextInt();
            paperNum_cid_map.add(new query_paperNum_cid_map(p_num, doi, cid));
        }
//        System.out.println(paperNum_cid_map.size());
        /* ends*/


        StringBuffer txtbuff = new StringBuffer();
        while ((line = br.readLine()) != null)
            txtbuff.append(line).append("\n");
        String content = txtbuff.toString();

        org.jsoup.nodes.Document jdoc = Jsoup.parse(content);
        Elements docElts = jdoc.select("top");

//        System.out.println(docElts.size());
        int query_paper_num = 0;
        for (Element docElt : docElts) {
            Element elt;
            CSQuery common_query = new CSQuery();
            

            elt = docElt.select("paper_num").first();
            common_query.paper_num = Integer.parseInt(elt.text());
            elt = docElt.select("paper_title").first();
            common_query.paper_title = elt.text();
            elt = docElt.select("paper_abstract").first();
            common_query.paper_abstract = elt.text();

            Elements elts = docElt.select("other");
            common_query.other=new StringBuffer();
            for (Element e : elts) {
                common_query.other.append(e.text());
            }

            Elements textElts = docElt.select("text");
            Elements queryElts = docElt.select("query_num");
            for (int i=0; i<queryElts.size(); i++) {
                CSQuery query = new CSQuery();
                query.paper_num = common_query.paper_num;
                query.cid = paperNum_cid_map.get(query_paper_num).cid;
                query.paper_abstract = common_query.paper_abstract;
                query.paper_title = common_query.paper_title;
                query.other = common_query.other;

                query.query_num = Integer.parseInt(queryElts.get(i).text());
                query.context = textElts.get(i).text();
                queries.add(query);
            }
            query_paper_num++;
        }

    }
}
