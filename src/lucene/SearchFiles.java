/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package lucene;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
//import java.util.Date;
//import java.util.Collections;
//import java.util.Comparator;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
//import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

/** Simple command-line based search demo. */
public class SearchFiles {

  private SearchFiles() {}

  /** Simple command-line based search demo. */
  public static void main(String[] args) throws Exception {
    String usage =
      "Usage:\tjava org.apache.lucene.demo.SearchFiles [-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]\n\nSee http://lucene.apache.org/core/4_1_0/demo/ for details.";
    if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
      System.out.println(usage);
      System.exit(0);
    }

    String index = "index";
    String field = "contents";
    String queries = null;
    int repeat = 0;
    boolean raw = false;
    String queryString = null;
    int hitsPerPage = 1000;
    
    for(int i = 0;i < args.length;i++) {
      if ("-index".equals(args[i])) {
        index = args[i+1];
        i++;
      } else if ("-field".equals(args[i])) {
        field = args[i+1];
        i++;
      } else if ("-queries".equals(args[i])) {
        queries = args[i+1];
        i++;
      } else if ("-query".equals(args[i])) {
        queryString = args[i+1];
        i++;
      } else if ("-repeat".equals(args[i])) {
        repeat = Integer.parseInt(args[i+1]);
        i++;
      } else if ("-raw".equals(args[i])) {
        raw = true;
      } else if ("-paging".equals(args[i])) {
        hitsPerPage = Integer.parseInt(args[i+1]);
        if (hitsPerPage <= 0) {
          System.err.println("There must be at least 1 hit per page.");
          System.exit(1);
        }
        i++;
      }
    }
    
    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
    IndexSearcher searcher = new IndexSearcher(reader);
    Analyzer analyzer = new StandardAnalyzer();

    BufferedReader in = null;
    if (queries != null) {
      in = Files.newBufferedReader(Paths.get(queries), StandardCharsets.UTF_8);
    } else {
      in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }
    QueryParser parser = new QueryParser(field, analyzer);
    
    List<String> topic = new ArrayList<String>();
    FileReader fr = new FileReader("C:\\Users\\Naffan\\Desktop\\Ryerson\\Capstone\\topics.txt");
    StringBuffer sb = new StringBuffer();
    while(fr.ready()) {
    	char c = (char) fr.read();
    	if(c == '\n') {
    		topic.add(sb.toString());
    		sb = new StringBuffer();
    	}
    	else {
    		sb.append(c);
    	}
    }
    fr.close();
    if(sb.length() > 0) {
    	topic.add(sb.toString());
    }
    
    for (int j = 0; j < topic.size(); j++) {
    	topic.set(topic.indexOf(topic.get(j)), topic.get(j).split("\\:")[1]);
    }
    String fname = "tfidf-results.test";
    BufferedWriter out = new BufferedWriter(new FileWriter(fname, true),32768);
    for(int e = 0; e < topic.size(); e++){
    	System.out.println(topic.get(e));
     // if (queries == null && queryString == null) {                        // prompt the user
     //   System.out.println("Enter query: ");
      //}
     
      String line = topic.get(e);

      if (line == null || line.length() == -1) {
        break;
      }

      line = line.trim();
      if (line.length() == 0) {
        break;
      }
      
      Query query = parser.parse(line);
      //System.out.println("Searching for: " + query.toString(field));
            
			/*
			 * if (repeat > 0) { // repeat & time as benchmark Date start = new Date(); for
			 * (int i = 0; i < repeat; i++) { searcher.search(query, 100); } Date end = new
			 * Date(); System.out.println("Time: "+(end.getTime()-start.getTime())+"ms"); }
			 */
      doPagingSearch(in, out, searcher, query, hitsPerPage, raw, queries == null && queryString == null, topic.indexOf(topic.get(e)));

//      if (queryString != null) {
//        break;
//      }
    }
    reader.close();
    out.close();
  }

  /**
   * This demonstrates a typical paging search scenario, where the search engine presents 
   * pages of size n to the user. The user can then go to the next page if interested in
   * the next hits.
   * 
   * When the query is executed for the first time, then only enough results are collected
   * to fill 5 result pages. If the user wants to page beyond this limit, then the query
   * is executed another time and all hits are collected.
   * 
   */
  public static void doPagingSearch(BufferedReader in, BufferedWriter out, IndexSearcher searcher, Query query, 
                                     int hitsPerPage, boolean raw, boolean interactive, int topic_index) throws IOException {
 
    // Collect enough docs to show 5 pages
    TopDocs results = searcher.search(query, hitsPerPage);
    ScoreDoc[] hits = results.scoreDocs;
    
    int numTotalHits = Math.toIntExact(results.totalHits.value);
//     TotalHitCountCollector collector = new TotalHitCountCollector();
//    System.out.println(numTotalHits + " total matching documents");

//    int start = 0;
//    int end = Math.min(numTotalHits, hitsPerPage);
        
//    while (true) {
//      if (end > hits.length) {
//        System.out.println("Only results 1 - " + hits.length +" of " + numTotalHits + " total matching documents collected.");
//        System.out.println("Collect more (y/n) ?");
//        String line = in.readLine();
//        if (line.length() == 0 || line.charAt(0) == 'n') {
//          break;
//        }
     //int count = collector.getTotalHits();
     if(numTotalHits > 0) {
        hits = searcher.search(query, numTotalHits).scoreDocs;
     }  
      //}
      
//      end = Math.min(hits.length, start + hitsPerPage);
      
     for (int i = 0; i < numTotalHits; i++) {
 //       if (raw) {                              // output raw format
 //         System.out.println("doc="+hits[i].doc+" score="+hits[i].score);
 //         continue;
 //       }

        Document doc = searcher.doc(hits[i].doc);
        String str = "";
        String path = doc.get("path");
        Float score = hits[i].score;
        File f = new File(path);
        if (score > 0.0) {
          str = topic_index+1 + " " + "Q0" + " " + f.getName() + " " + (i+1) + " " + score + " " + "Default";
          appendToFile(out,str);
//          if (title != null) {
//            System.out.println("   Title: " + doc.get("title"));
//          }
        } 
//        else {
//          System.out.println((i+1) + ". " + "No score for this document");
//        }
                  
      }

//      if (!interactive || end == 0) {
//        break;
//      }

			/*
			 * if (numTotalHits >= end) { boolean quit = false; while (true) {
			 * System.out.print("Press "); if (start - hitsPerPage >= 0) {
			 * System.out.print("(p)revious page, "); } if (start + hitsPerPage <
			 * numTotalHits) { System.out.print("(n)ext page, "); }
			 * System.out.println("(q)uit or enter number to jump to a page.");
			 * 
			 * String line = in.readLine(); if (line.length() == 0 || line.charAt(0)=='q') {
			 * quit = true; break; } if (line.charAt(0) == 'p') { start = Math.max(0, start
			 * - hitsPerPage); break; } else if (line.charAt(0) == 'n') { if (start +
			 * hitsPerPage < numTotalHits) { start+=hitsPerPage; } break; } else { int page
			 * = Integer.parseInt(line); if ((page - 1) * hitsPerPage < numTotalHits) {
			 * start = (page - 1) * hitsPerPage; break; } else {
			 * System.out.println("No such page"); } } } if (quit) break; end =
			 * Math.min(numTotalHits, start + hitsPerPage); }
			 */
    //}
  }
  
  public static void appendToFile(BufferedWriter out, String str) {
	  try {
		  out.write(str);
		  out.newLine();
		  out.flush();
	  }
	  catch (IOException e) {
		  System.out.println("exception occurred" + e);
	  }
  }
}