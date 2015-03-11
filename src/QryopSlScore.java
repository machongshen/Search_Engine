/**
 *  This class implements the SCORE operator for all retrieval models.
 *  The single argument to a score operator is a query operator that
 *  produces an inverted list.  The SCORE operator uses this
 *  information to produce a score list that contains document ids and
 *  scores.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlScore extends QryopSl {
	private int ctf;
	private String field;
	private long P;
	/**
	 * Construct a new SCORE operator. The SCORE operator accepts just one
	 * argument.
	 * 
	 * @param q
	 *            The query operator argument.
	 * @return @link{QryopSlScore}
	 */
	public QryopSlScore(Qryop q) {
		this.args.add(q);
	}

	/**
	 * Construct a new SCORE operator. Allow a SCORE operator to be created with
	 * no arguments. This simplifies the design of some query parsing
	 * architectures.
	 * 
	 * @return @link{QryopSlScore}
	 */
	public QryopSlScore() {
	}

	/**
	 * Appends an argument to the list of query operator arguments. This
	 * simplifies the design of some query parsing architectures.
	 * 
	 * @param q
	 *            The query argument to append.
	 */
	public void add(Qryop a) {
		this.args.add(a);
	}

	/**
	 * Evaluate the query operator.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluate(RetrievalModel r) throws IOException {

		if (r instanceof RetrievalModelUnrankedBoolean) {
			return (evaluateBoolean(r));
		}
		if (r instanceof RetrievalModelRankedBoolean) {
			return (evaluateRankBoolean(r));
		}
		if (r instanceof RetrievalModelBM25) {

			return (evaluateBM25(r));
		}
		if (r instanceof RetrievalModelIndri) {

			return (evaluateIndri(r));
		}
		return null;
	}

	/**
	 * Evaluate the query operator for boolean retrieval models.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateRankBoolean(RetrievalModel r) throws IOException {

		// Evaluate the query argument.

		QryResult result = args.get(0).evaluate(r);

		// Each pass of the loop computes a score for one document. Note:
		// If the evaluate operation above returned a score list (which is
		// very possible), this loop gets skipped.

		for (int i = 0; i < result.invertedList.df; i++) {

			// DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
			// Unranked Boolean. All matching documents get a score of 1.0.
			// System.out.println(result.invertedList.getTf(i));
			result.docScores.add(result.invertedList.postings.get(i).docid,
					result.invertedList.getTf(i));

		}

		// The SCORE operator should not return a populated inverted list.
		// If there is one, replace it with an empty inverted list.

		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
	}
	/**
	 * Evaluate the query operator for boolean retrieval models.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateBM25(RetrievalModel r) throws IOException {
		QryResult result = args.get(0).evaluate(r);
		String field = result.invertedList.field;
		double k3 = QryEval.k_3;
		double k1 = QryEval.k_1;
		int df = result.invertedList.df;
		int qtf = 1;
		double b = QryEval.b;
		if (field == null)
		{
			field = "body";
		}
		
		double avgdoclength = QryEval.READER.getSumTotalTermFreq(field)
				/ (double) QryEval.READER.getDocCount(field);
		double idf = Math.log((QryEval.READER.getDocCount(field) - df + 0.5)
				/ (df + 0.5));
		double weight = (k3 + 1) * qtf / (double) (k3 + qtf);
		double score = 0;
		//System.out.println("bad");
		for (int i = 0; i < df; i++) {
			int tf = result.invertedList.postings.get(i).tf;

			long doclen = 0;
			doclen = QryEval.doclen.getDocLength(field,
					result.invertedList.postings.get(i).docid);
			double ftf = tf
					/ (double) (tf + k1 * (1 - b + b * doclen / avgdoclength));
			score = idf * ftf * weight;
			result.docScores.add(result.invertedList.postings.get(i).docid,
					(double) score);

		}

		if (result.invertedList.df > 0)
			result.invertedList = new InvList();
		return result;

	}
	public QryResult evaluateIndri(RetrievalModel r) throws IOException {
		// System.out.println("good");
		QryResult result = args.get(0).evaluate(r);
		this.field = result.invertedList.field;
		if (result.invertedList.df <= 0)
			return result;
		int df = result.invertedList.df;
		double lambda = QryEval.lambda;
		int mu = QryEval.mu;
		this.ctf = result.invertedList.ctf;
		if (this.field == null)
		{
			this.field = "body";
		}
		this.P = QryEval.READER.getSumTotalTermFreq(field);
		// System.out.println("good"+QryEval.READER.getSumTotalTermFreq(field));
		double pmle = (double) ctf / QryEval.READER.getSumTotalTermFreq(field);

		double score = 0;
		// System.out.println("good"+df);
		for (int i = 0; i < df; i++) {
			long doclen = QryEval.doclen.getDocLength(field,
					result.invertedList.postings.get(i).docid);

			double middle = (result.invertedList.postings.get(i).tf + (mu * pmle))
					/ (doclen + mu);

			score = ((1.0 - lambda) * middle) + (lambda * pmle);
			// System.out.println(score);
			result.docScores.add(result.invertedList.postings.get(i).docid,
					(double) score);
		}

		// result.docScores. = (float) defaultScore;
		if (result.invertedList.df > 0)
			result.invertedList = new InvList();
		return result;
	}
	/**
	 * Evaluate the query operator for boolean retrieval models.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateBoolean(RetrievalModel r) throws IOException {

		// Evaluate the query argument.

		QryResult result = args.get(0).evaluate(r);

		// Each pass of the loop computes a score for one document. Note:
		// If the evaluate operation above returned a score list (which is
		// very possible), this loop gets skipped.

		for (int i = 0; i < result.invertedList.df; i++) {

			// DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
			// Unranked Boolean. All matching documents get a score of 1.0.

			result.docScores.add(result.invertedList.postings.get(i).docid,
					(float) 1.0);
		}

		// The SCORE operator should not return a populated inverted list.
		// If there is one, replace it with an empty inverted list.

		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
	}

	/*
	 * Calculate the default score for a document that does not match the query
	 * argument. This score is 0 for many retrieval models, but not all
	 * retrieval models.
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
		if (r instanceof RetrievalModelIndri) {
			//QryResult resultdefalut = args.get(0).evaluate(r);
			
			// int df = result.invertedList.df;
			double lambda = QryEval.lambda;
			int mu = QryEval.mu;
			//int ctf = resultdefalut.invertedList.ctf;
			//System.out.println(field);
			if (field == null)
			{
				field ="body";
			}
			double pmle = (double) ctf/ P;
			// System.out.println(pmle);
			long doclen = QryEval.doclen.getDocLength(field, (int) docid);
			double middle = ((mu * pmle)) / (doclen + mu);
			double score = ((1.0 - lambda) * middle) + (lambda * pmle);
			//System.out.println(score+"score");
			return score;
		}
		return 0.0;
	}

	/**
	 * Return a string version of this query operator.
	 * 
	 * @return The string version of this query operator.
	 */
	public String toString() {

		String result = new String();

		for (Iterator<Qryop> i = this.args.iterator(); i.hasNext();)
			result += (i.next().toString() + " ");

		return ("#SCORE( " + result + ")");
	}

}
