import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class QryopSINear extends QryopIl {
    int dist;

    public QryopSINear(int near, Qryop... q) {
	for (int i = 0; i < q.length; i++)
	    this.args.add(q[i]);
	this.dist = near;
    }

    @Override
    public void add(Qryop q) throws IOException {
	// TODO Auto-generated method stub
	this.args.add(q);
    }

    public QryResult evaluate(RetrievalModel r) throws IOException {
	// TODO Auto-generated method stub
	allocArgPtrs(r);
	QryResult result = new QryResult();
	// System.out.println(this.argPtrs.size());
	// for (int i = 0; i < this.argPtrs.size(); i++) {
	// ArgPtr ptri = this.argPtrs.get(i);
	// System.out.println("posting size:" + ptri.invList.postings.size());
	// System.out.println("posting size:"
	// + ptri.invList.postings.get(0).positions);
	// ptri.nextDoc++;
	// // for (int j =0; j < )
	// if (ptri.invList.postings.size() == 0)
	// continue;
	// System.out.println(ptri.invList.postings.get(0).positions);
	//
	// }


	ArgPtr ptr0 = this.argPtrs.get(0);
	EVALUATEDOCUMENTS: for (; ptr0.nextDoc < ptr0.invList.postings.size(); ptr0.nextDoc++) {

	    int ptr0Docid = ptr0.invList.getDocid(ptr0.nextDoc);

	    double docScore = 1.0;

	    // Do the other query arguments have the ptr0Docid?

	    for (int j = 1; j < this.argPtrs.size(); j++) {

		ArgPtr ptrj = this.argPtrs.get(j);

		while (true) {

		    if (ptrj.nextDoc >= ptrj.invList.postings.size())
			break EVALUATEDOCUMENTS; // No more docs can match
		    else if (ptrj.invList.getDocid(ptrj.nextDoc) > ptr0Docid)
			continue EVALUATEDOCUMENTS; // The ptr0docid can't
						    // match.
		    else if (ptrj.invList.getDocid(ptrj.nextDoc) < ptr0Docid)
			ptrj.nextDoc++; // Not yet at the right doc.
		    else

			break; // ptrj matches ptr0Docid
		}
	    }
	    // The ptr0Docid matched all query arguments for postions, so save
	    // it.
	    // This is the position matching algorithm.
	    // System.out.println("doc"+this.argPtrs.get(0).invList.postings.get(3).positions);
	    //test all the position for docid.
	    
	    List<Integer> list = new ArrayList<Integer>();
	    List<Integer> comlist = new ArrayList<Integer>();
	    int compare = 0;
	    for (int k = 0; k < this.argPtrs.get(0).invList.postings.size(); k++) {
		if (ptr0Docid == this.argPtrs.get(0).invList.postings.get(k).docid) {
		    list = this.argPtrs.get(0).invList.postings.get(k).positions;
		    compare = k;
		    break;
		}
	    }
	  //  System.out.println("docid" + compare);
	  //  System.out.println("apple list" + list);
	    comlist = list;
	    for (int i = 1; i < this.argPtrs.size(); i++) {
		list = new ArrayList<Integer>(comlist);
		comlist = new ArrayList<Integer>();
		//find same docid
		  for (int k = 0; k < this.argPtrs.get(i).invList.postings.size(); k++) {
			if (ptr0Docid == this.argPtrs.get(i).invList.postings.get(k).docid) {
			    compare = k;
			    break;
			}
		    }
		for (int k : list) {
		   if (this.argPtrs.get(i).invList.postings.size() < compare ){
		       continue;
		   }
		    for (int j = 0; j < this.argPtrs.get(i).invList.postings
			    .get(compare).positions.size(); j++) {
			// System.out.println("good"+this.argPtrs.get(i).invList.postings
			// .get(compare).positions.size());
			if (this.argPtrs.get(i).invList.postings.get(compare).positions
				.get(j) - k > 0
				&& this.argPtrs.get(i).invList.postings
					.get(compare).positions.get(j) - k <= dist) {
			    if (!comlist.contains(this.argPtrs.get(i).invList.postings
				    .get(compare).positions.get(j)))
				comlist.add(this.argPtrs.get(i).invList.postings
				    .get(compare).positions.get(j));
			    break;
				
			}
		    }
		}
	    }
	  
	    if (comlist.size() != 0) {
		
		result.invertedList.appendPosting(ptr0Docid, comlist);
	    }
	}
	// result.invertedList.field = new String
	// (this.argPtrs.get(0).invList.field);

	freeArgPtrs();
	return result;
    }

    @Override
    public String toString() {
	// TODO Auto-generated method stub
	String result = new String();
	int c = 0;
	for (Iterator<Qryop> i = this.args.iterator(); i.hasNext();)
	    result += (i.next().toString() + " ");
	
	return ("#NEAR/" + c + result + ")");
    }

}
