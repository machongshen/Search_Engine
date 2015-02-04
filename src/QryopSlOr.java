/**
 *  This class implements the NEAR operator for all retrieval models.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

import javax.swing.event.ListSelectionEvent;



public class QryopSlOr extends QryopSl {

    /**
     * It is convenient for the constructor to accept a variable number of
     * arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
     * 
     * @param q
     *            A query argument (a query operator).
     */
    public QryopSlOr(Qryop... q) {
	for (int i = 0; i < q.length; i++)
	    this.args.add(q[i]);
    }

    /**
     * Appends an argument to the list of query operator arguments. This
     * simplifies the design of some query parsing architectures.
     * 
     * @param {q} q The query argument (query operator) to append.
     * @return void
     * @throws IOException
     */
    public void add(Qryop a) {
	this.args.add(a);
    }

    /**
     * Evaluates the query operator, including any child operators and returns
     * the result.
     * 
     * @param r
     *            A retrieval model that controls how the operator behaves.
     * @return The result of evaluating the query.
     * @throws IOException
     */
    public QryResult evaluate(RetrievalModel r) throws IOException {
	//if (r instanceof RetrievalModelUnrankedBoolean)
	return (evaluateBoolean(r));
	//return null;
    }

    /**
     * Evaluates the query operator for boolean retrieval models, including any
     * child operators and returns the result.
     * 
     * @param r
     *            A retrieval model that controls how the operator behaves.
     * @return The result of evaluating the query.
     * @throws IOException
     */
    public QryResult evaluateBoolean(RetrievalModel r) throws IOException {

		// Initialization
		
		allocArgPtrs(r);
		QryResult result = new QryResult();
		long start = System.currentTimeMillis(); 
		
		// Sort the arguments so that the shortest lists are first. This
		// improves the efficiency of exact-match AND without changing
		// the result.
		//System.out.println("size:"+this.argPtrs.get(0).scoreList.scores.size());
		//result.docScores.scores.get(index);


		// Exact-match AND requires that ALL scoreLists contain a
		// document id. Use the first (shortest) list to control the
		// search for matches.

		// Named loops are a little ugly. However, they make it easy
		// to terminate an outer loop from within an inner loop.
		// Otherwise it is necessary to use flags, which is also ugly.
		 
		//ArgPtr ptr0 = (this.argPtrs.get(0));
		//System.out.println(ptr0.invList.postings.size());
		//System.out.println(ptr0.scoreList.scores.size());
		Map<Integer, Double> orList = new HashMap<Integer, Double> ();
		for (int i = 0 ; i < this.argPtrs.size(); i++){
		    ArgPtr ptri = this.argPtrs.get(i);
			for (int j = 0 ; j < ptri.scoreList.scores.size(); j++){
			  // System.out.println("size:"+ this.argPtrs.get(0).scoreList.getDocidScore(j));
			    int docid = ptri.scoreList.getDocid(j);
			    double score = ptri.scoreList.getDocidScore(j);
				if (!orList.containsKey(docid)){
				orList.put(docid,score);
				
				}else if (orList.get(docid) < score){
				    orList.put( docid,score);
				} 
			}
			}
		
		for (int k : orList.keySet()){
		    result.docScores.add(k, orList.get(k));	   
		}
		
		freeArgPtrs();
		long end =System.currentTimeMillis();
		System.out.println("time for finding this query " + (double)(start+end)/1000 );
		return result;
	}

    /*
     * Calculate the default score for the specified document if it does not
     * match the query operator. This score is 0 for many retrieval models, but
     * not all retrieval models.
     * 
     * @param r A retrieval model that controls how the operator behaves.
     * 
     * @param docid The internal id of the document that needs a default score.
     * 
     * @return The default score.
     */
    public double getDefaultScore(RetrievalModel r, long docid)
	    throws IOException {

	if (r instanceof RetrievalModelUnrankedBoolean)
	    return (0.0);

	return 0.0;
    }

    /*
     * Return a string version of this query operator.
     * 
     * @return The string version of this query operator.
     */
    public String toString() {
	String result = new String();

	for (int i = 0; i < this.args.size(); i++)
	    result += this.args.get(i).toString() + " ";
	return ("#OR( " + result + ")");
    }
}
