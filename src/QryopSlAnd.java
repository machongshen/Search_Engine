/**
 *  This class implements the AND operator for all retrieval models.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlAnd extends QryopSl {

	/**
	 * It is convenient for the constructor to accept a variable number of
	 * arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
	 * 
	 * @param q
	 *            A query argument (a query operator).
	 */
	public QryopSlAnd(Qryop... q) {
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

		if (r instanceof RetrievalModelIndri) {
			return (eval_Indri(r));
		}
		return (evaluateBoolean(r));
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
		// Sort the arguments so that the shortest lists are first. This
		// improves the efficiency of exact-match AND without changing
		// the result.

		for (int i = 0; i < (this.argPtrs.size() - 1); i++) {
			for (int j = i + 1; j < this.argPtrs.size(); j++) {
				if (this.argPtrs.get(i).scoreList.scores.size() > this.argPtrs
						.get(j).scoreList.scores.size()) {
					ScoreList tmpScoreList = this.argPtrs.get(i).scoreList;
					this.argPtrs.get(i).scoreList = this.argPtrs.get(j).scoreList;
					this.argPtrs.get(j).scoreList = tmpScoreList;
				}
			}
		}

		// Exact-match AND requires that ALL scoreLists contain a
		// document id. Use the first (shortest) list to control the
		// search for matches.

		// Named loops are a little ugly. However, they make it easy
		// to terminate an outer loop from within an inner loop.
		// Otherwise it is necessary to use flags, which is also ugly.

		ArgPtr ptr0 = this.argPtrs.get(0);
		// result.invertedList.field = new String
		// (this.argPtrs.get(0).invList.field);

		EVALUATEDOCUMENTS : for (; ptr0.nextDoc < ptr0.scoreList.scores.size(); ptr0.nextDoc++) {

			int ptr0Docid = ptr0.scoreList.getDocid(ptr0.nextDoc);

			double docScore = ptr0.scoreList.getDocidScore(ptr0.nextDoc);
			// System.out.println(ptr0.scoreList.getDocidScore(ptr0.nextDoc));
			// Do the other query arguments have the ptr0Docid?

			boolean flag = (this.argPtrs.size() == 1);
			for (int j = 1; j < this.argPtrs.size(); j++) {
				ArgPtr ptrj = this.argPtrs.get(j);
				while (ptrj.nextDoc < ptrj.scoreList.scores.size()) {
					// System.out.println("term in other docs"+ptrj.scoreList.getDocidScore(ptrj.nextDoc));
					if (ptrj.scoreList.getDocid(ptrj.nextDoc) > ptr0Docid)
						continue EVALUATEDOCUMENTS; // The ptr0docid can't
					// match.
					else {
						if (ptrj.scoreList.getDocid(ptrj.nextDoc) < ptr0Docid) {
							ptrj.nextDoc++; // Not yet at the right doc.
						} else {
							docScore = Math.min(
									ptrj.scoreList.getDocidScore(ptrj.nextDoc),
									docScore);
							flag = true;
							break; // ptrj matches ptr0Docid
						}
					}
				}
			}

			// The ptr0Docid matched all query arguments, so save it.
			// System.out.println("ptrdocid:"+docScore);
			if (flag) {

				result.docScores.add(ptr0Docid, docScore);
			}
		}

		freeArgPtrs();

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
		else if (r instanceof RetrievalModelIndri) {
			// Initialization
			// allocArgPtrs(r);
			double result = 1.0;
			for (int i = 0; i < args.size(); i++) {
				result *= ((QryopSl) args.get(i)).getDefaultScore(r, docid);
			}
			result = Math.pow(result, 1.0 / args.size());
			return result;
		}
		return 0.0;
	}

	public QryResult eval_Indri(RetrievalModel r) throws IOException {

		allocArgPtrs(r);
		QryResult result = new QryResult();

		
		double factor = 1.0 / argPtrs.size();
		List<Integer> list= new ArrayList<Integer>();
		for (int i = 0; i < argPtrs.size(); i++) {
			list.add(0);
		}
		int min = Find(list);
		
		while (list.get(min) < Integer.MAX_VALUE) {
			double docScore = 1.0;
			int id = argPtrs.get(min).scoreList.getDocid(list.get(min));
			for (int i = 0; i < argPtrs.size(); i++) {
				// if no such doc
				if (list.get(i) == Integer.MAX_VALUE
						|| argPtrs.get(i).scoreList.getDocid(list.get(i)) != id) {
					docScore *= ((QryopSl) args.get(i)).getDefaultScore(r,
							(long) id);
				} else {// if have same doc
					docScore *= argPtrs.get(i).scoreList.getDocidScore(list.get(i));
				    	list.set(i, list.get(i)+1);
					if (list.get(i) >= argPtrs.get(i).scoreList.scores.size())
						list.set(i, Integer.MAX_VALUE)  ;

				}
			}
			docScore = Math.pow(docScore, factor);
			result.docScores.add(id, docScore);
			min = Find(list);
		}
		
		freeArgPtrs();
		return result;
	}
	public int Find(List<Integer> list) {
		int min = Integer.MAX_VALUE;
		int id = 0;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) == Integer.MAX_VALUE) {
				continue;
			}
			if (argPtrs.get(i).scoreList.getDocid(list.get(i)) < min) {
				min = argPtrs.get(i).scoreList.getDocid(list.get(i));
				id = i;
			}
		}
		return id;
	}

	public String toString() {
		String result = new String();
		for (int i = 0; i < this.args.size(); i++)
			result += this.args.get(i).toString() + " ";
		return ("#AND( " + result + ")");
	}

}
