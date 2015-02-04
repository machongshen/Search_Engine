/**
 *  This class implements the document score list data structure
 *  and provides methods for accessing and manipulating them.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;
import java.util.*;

import org.apache.lucene.document.Document;

public class ScoreList {

    // A little utilty class to create a <docid, score> object.

    protected class ScoreListEntry {
	int docid;
	double score;

	private ScoreListEntry(int docid, double score) {
	    this.docid = docid;
	    this.score = score;
	}
    }

    List<ScoreListEntry> scores = new ArrayList<ScoreListEntry>();

    /**
     * Append a document score to a score list.
     * 
     * @param docid
     *            An internal document id.
     * @param score
     *            The document's score.
     * @return void
     */
    public void add(int docid, double score) {
	scores.add(new ScoreListEntry(docid, score));
    }

    /**
     * Get the n'th document id.
     * 
     * @param n
     *            The index of the requested document.
     * @return The internal document id.
     */
    public int getDocid(int n) {
	return this.scores.get(n).docid;
    }

    /**
     * Get the score of the n'th document.
     * 
     * @param n
     *            The index of the requested document score.
     * @return The document's score.
     */
    public double getDocidScore(int n) {
	return this.scores.get(n).score;
    }

    static String getExternalDocid(int iid) throws IOException {
	System.out.println("good");
	Document d = QryEval.READER.document(iid);
	String eid = d.get("externalId");
	return eid;
    }

    // Sort Algorithm
    public void nameSort(List<ScoreListEntry> list) {
	Comparator<ScoreList.ScoreListEntry> comparator = new Comparator<ScoreList.ScoreListEntry>() {

	    @Override
	    public int compare(ScoreList.ScoreListEntry o1,
		    ScoreList.ScoreListEntry o2) {
		String s1, s2;

		try {
		    s1 = getExternalDocid(o1.docid);
		    s2 = getExternalDocid(o2.docid);
		    if (s1.compareTo(s2) > 0) {
			// System.out.println(o1.docid);
			return 1;
		    } else
			return -1;
		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}

		// TODO Auto-generated method stub
		return -1;
	    }

	};
	Collections.sort(list, comparator);
    }

    public void scoreSort(List<ScoreListEntry> list) {

	Comparator<ScoreList.ScoreListEntry> comparator = new Comparator<ScoreList.ScoreListEntry>() {
	    @Override
	    public int compare(ScoreList.ScoreListEntry o1,
		    ScoreList.ScoreListEntry o2) {
		double s1, s2;
		String p1, p2;
		s1 = o1.score;
		s2 = o2.score;
		if (s1 < s2) {
		    // System.out.println(o1.docid);
		    return 1;
		}
		if (s1 == s2) {
		    try {
			p1 = getExternalDocid(o1.docid);
			p2 = getExternalDocid(o2.docid);
			if (p1.compareTo(p2) > 0) {
			    // System.out.println(o1.docid);
			    return 1;
			} else
			    return -1;
		    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		    }
		}
		// TODO Auto-generated method stub
		return -1;
	    }

	};
	Collections.sort(list, comparator);

    }
}
