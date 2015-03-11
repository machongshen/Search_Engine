import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class QryopBM25Sum extends QryopSl {

	public QryopBM25Sum(Qryop... q) {
		for (int i = 0; i < q.length; i++)
			this.args.add(q[i]);
	}

	public void add(Qryop a) {
		this.args.add(a);
	}
	@Override
	public QryResult evaluate(RetrievalModel r) throws IOException {
		// TODO Auto-generated method stub
		// Initialization
		allocArgPtrs(r);
		QryResult result = new QryResult();
		//System.out.println(this.argPtrs.size());
		HashMap<Integer, Double> scoremap = new LinkedHashMap<Integer, Double>();
		// boolean flag = (this.argPtrs.size() == 1);
		for (int j = 0; j < this.argPtrs.size(); j++) {	
			ArgPtr ptrj = this.argPtrs.get(j);
			
			while (ptrj.nextDoc < ptrj.scoreList.scores.size()) {
				if (scoremap.containsKey(ptrj.scoreList.getDocid(ptrj.nextDoc))) {
					scoremap.put(ptrj.scoreList.getDocid(ptrj.nextDoc), scoremap.get(ptrj.scoreList.getDocid(ptrj.nextDoc))
							+ ptrj.scoreList.getDocidScore(ptrj.nextDoc));
				} else {
					scoremap.put(ptrj.scoreList.getDocid(ptrj.nextDoc),
							ptrj.scoreList.getDocidScore(ptrj.nextDoc));
				}
				ptrj.nextDoc++;
			}
		
		}
		for (int i : scoremap.keySet()) {
			//System.out.println(scoremap.get(i));
			result.docScores.add(i, scoremap.get(i));
		}
		
		return result;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

}
