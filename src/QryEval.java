/**
 *  QryEval illustrates the architecture for the portion of a search
 *  engine that evaluates queries.  It is a template for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class QryEval {
    static RetrievalModel model;
    static String usage = "Usage:  java "
	    + System.getProperty("sun.java.command") + " paramFile\n\n";

    // The index file reader is accessible via a global variable. This
    // isn't great programming style, but the alternative is for every
    // query operator to store or pass this value, which creates its
    // own headaches.

    public static IndexReader READER;

    // Create and configure an English analyzer that will be used for
    // query parsing.

    public static EnglishAnalyzerConfigurable analyzer = new EnglishAnalyzerConfigurable(
	    Version.LUCENE_43);
    static {
	analyzer.setLowercase(true);
	analyzer.setStopwordRemoval(true);
	analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
    }

    /**
     * @param args
     *            The only argument is the path to the parameter file.
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
	long start = System.currentTimeMillis();
	// must supply parameter file
	if (args.length < 1) {
	    System.err.println(usage);
	    System.exit(1);
	}

	// read in the parameter file; one parameter per line in format of
	// key=value
	Map<String, String> params = new HashMap<String, String>();
	Scanner scan = new Scanner(new File(args[0]));
	String line = null;
	do {
	    line = scan.nextLine();
	    String[] pair = line.split("=");
	    params.put(pair[0].trim(), pair[1].trim());
	} while (scan.hasNext());
	scan.close();

	// parameters required for this example to run
	if (!params.containsKey("indexPath")) {
	    System.err.println("Error: Parameters were missing.");
	    System.exit(1);
	}

	// open the index
	READER = DirectoryReader.open(FSDirectory.open(new File(params
		.get("indexPath"))));

	if (READER == null) {
	    System.err.println(usage);
	    System.exit(1);
	}

	DocLengthStore s = new DocLengthStore(READER);
	if (params.get("retrievalAlgorithm").equals("UnrankedBoolean")) {
	    model = new RetrievalModelUnrankedBoolean();
	} else {
	    model = new RetrievalModelRankedBoolean();
	}

	// Store the Query
	Map<String, String> map = new LinkedHashMap<String, String>();
	try {
	    BufferedReader br = new BufferedReader(new FileReader(
		    params.get("queryFilePath")));
	    String str = null;
	    while ((str = br.readLine()) != null) {
		str = str.trim();
		String text[] = str.split(":");
		map.put(text[0], text[1]);
	    }
	    br.close();
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	/*
	 * The code below is an unorganized set of examples that show you
	 * different ways of accessing the index. Some of these are only useful
	 * in HW2 or HW3.
	 */

	// Lookup the document length of the body field of doc 0.
	/*
	 * System.out.println(s.getDocLength("body", 0));
	 * 
	 * // How to use the term vector. TermVector tv = new TermVector(1,
	 * "body"); System.out.println(tv.stemString(10)); // get the string for
	 * the 10th stem System.out.println(tv.stemDf(10)); // get its df
	 * System.out.println(tv.totalStemFreq(10)); // get its ctf
	 */

	/**
	 * The index is open. Start evaluating queries. The examples below show
	 * query trees for two simple queries. These are meant to illustrate how
	 * query nodes are created and connected. However your software will not
	 * create queries like this. Your software will use a query parser. See
	 * parseQuery.
	 *
	 * The general pattern is to tokenize the query term (so that it gets
	 * converted to lowercase, stopped, stemmed, etc), create a Term node to
	 * fetch the inverted list, create a Score node to convert an inverted
	 * list to a score list, evaluate the query, and print results.
	 * 
	 * Modify the software so that you read a query from a file, parse it,
	 * and form the query tree automatically.
	 */

	// A one-word query.
	// printResults("pea", (new QryopSlScore(new QryopIlTerm(
	// tokenizeQuery("pea")[0]))).evaluate(model));

	// A more complex query.
	// printResults("#AND (aparagus broccoli cauliflower #SYN(peapods peas))",
	// (new QryopSlAnd(
	// new QryopIlTerm(tokenizeQuery("asparagus")[0]),
	// new QryopIlTerm(tokenizeQuery("broccoli")[0]),
	// new QryopIlTerm(tokenizeQuery("cauliflower")[0]),
	// new QryopIlSyn(
	// new QryopIlTerm(tokenizeQuery("peapods")[0]),
	// new QryopIlTerm(tokenizeQuery("peas")[0])))).evaluate(model));

	// A different way to create the previous query. This doesn't use
	// a stack, but it may make it easier to see how you would parse a
	// query with a stack-based architecture.
	// Qryop op1 = new QryopSlAnd();
	// op1.add (new QryopIlTerm(tokenizeQuery("asparagus")[0]));
	// op1.add (new QryopIlTerm(tokenizeQuery("broccoli")[0]));
	// op1.add (new QryopIlTerm(tokenizeQuery("cauliflower")[0]));
	// Qryop op2 = new QryopIlSyn();
	// op2.add (new QryopIlTerm(tokenizeQuery("peapods")[0]));
	// op2.add (new QryopIlTerm(tokenizeQuery("peas")[0]));
	// op1.add (op2);
	// printResults("#AND (aparagus broccoli cauliflower #SYN(peapods peas))",
	// op1.evaluate(model));

	// Using the example query parser. Notice that this does no
	// lexical processing of query terms. Add that to the query
	// parser.

	/*
	 * Create the trec_eval output. Your code should write to the file
	 * specified in the parameter file, and it should write the results that
	 * you retrieved above. This code just allows the testing infrastructure
	 * to work on QryEval.
	 */
	// TODO write the result into the file;
	BufferedWriter writer = null;

	try {
	    writer = new BufferedWriter(new FileWriter(new File(
		    params.get("trecEvalOutputPath"))));
	    for (String queryNum : map.keySet()) {
		Qryop qTree;
		String query = map.get(queryNum);
		qTree = parseQuery(query);
		String k = printResults(queryNum, query, qTree.evaluate(model));
		writer.write(k);
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	} finally {
	    try {
		writer.close();
	    } catch (Exception e) {
	    }
	}

	// Later HW assignments will use more RAM, so you want to be aware
	// of how much memory your program uses.
	long end = System.currentTimeMillis();
	System.out.println("Total Running Time:"+(long) (end - start));
	printMemoryUsage(true);

    }

    /**
     * Function for analyze query;
     */

    /**
     * Write an error message and exit. This can be done in other ways, but I
     * wanted something that takes just one statement so that it is easy to
     * insert checks without cluttering the code.
     * 
     * @param message
     *            The error message to write before exiting.
     * @return void
     */
    static void fatalError(String message) {
	System.err.println(message);
	System.exit(1);
    }

    /**
     * Get the external document id for a document specified by an internal
     * document id. If the internal id doesn't exists, returns null.
     * 
     * @param iid
     *            The internal document id of the document.
     * @throws IOException
     */
    static String getExternalDocid(int iid) throws IOException {

	Document d = QryEval.READER.document(iid);
	String eid = d.get("externalId");
	return eid;
    }

    /**
     * Finds the internal document id for a document specified by its external
     * id, e.g. clueweb09-enwp00-88-09710. If no such document exists, it throws
     * an exception.
     * 
     * @param externalId
     *            The external document id of a document.s
     * @return An internal doc id suitable for finding document vectors etc.
     * @throws Exception
     */
    static int getInternalDocid(String externalId) throws Exception {
	Query q = new TermQuery(new Term("externalId", externalId));

	IndexSearcher searcher = new IndexSearcher(QryEval.READER);
	TopScoreDocCollector collector = TopScoreDocCollector.create(1, false);
	searcher.search(q, collector);
	ScoreDoc[] hits = collector.topDocs().scoreDocs;

	if (hits.length < 1) {
	    throw new Exception("External id not found.");
	} else {
	    return hits[0].doc;
	}
    }

    /**
     * parseQuery converts a query string into a query tree.
     * 
     * @param qString
     *            A string containing a query.
     * @param qTree
     *            A query tree
     * @throws IOException
     */
    static Qryop parseQuery(String qString) throws IOException {

	Qryop currentOp = null;
	Stack<Qryop> stack = new Stack<Qryop>();

	// Add a default query operator to an unstructured query. This
	// is a tiny bit easier if unnecessary whitespace is removed.

	qString = qString.trim();
	if (qString.charAt(0) != '#' && qString.charAt(1) != 'O'
		|| qString.charAt(1) != 'o') {
	    qString = "#or(" + qString + ")";
	}
	// Tokenize the query.

	StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()",
		true);
	String token = null;

	// Each pass of the loop processes one token. To improve
	// efficiency and clarity, the query operator on the top of the
	// stack is also stored in currentOp.

	while (tokens.hasMoreTokens()) {

	    token = tokens.nextToken();

	    if (token.matches("[ ,(\t\n\r]")) {
		// Ignore most delimiters.
	    } else if (token.equalsIgnoreCase("#and")) {
		currentOp = new QryopSlAnd();
		stack.push(currentOp);

	    } else if (token.equalsIgnoreCase("#or")) {
		currentOp = new QryopSlOr();
		stack.push(currentOp);

	    } else if (token.equalsIgnoreCase("#syn")) {
		token = tokens.nextToken();
		currentOp = new QryopIlSyn();
		stack.push(currentOp);
	    } else if (token.toLowerCase().indexOf("#near/") != -1) {
		String p = token.substring(6, token.length());
		int i = Integer.valueOf(p);
		currentOp = new QryopSINear(i);
		stack.push(currentOp);
	    } else if (token.startsWith(")")) { // Finish current query
						// operator.
		// If the current query operator is not an argument to
		// another query operator (i.e., the stack is empty when it
		// is removed), we're done (assuming correct syntax - see
		// below). Otherwise, add the current operator as an
		// argument to the higher-level operator, and shift
		// processing back to the higher-level operator.

		stack.pop();

		if (stack.empty())

		    break;

		Qryop arg = currentOp;

		currentOp = stack.peek();

		currentOp.add(arg);
	    } else {
		token = "#or(" + token + ")";
		// NOTE: You should do lexical processing of the token before
		// creating the query term, and you should check to see whether
		// the token specifies a particular field (e.g., apple.title).

		if (tokenizeQuery(token).length != 0) {

		    if (token.indexOf(".url") != -1) {
			token = token.replaceAll(".url", "");
			currentOp.add(new QryopIlTerm(tokenizeQuery(token)[0],
				"url"));
		    } else if (token.indexOf(".inlink") != -1) {

			token = token.replaceAll(".inlink", "");

			currentOp.add(new QryopIlTerm(tokenizeQuery(token)[0],
				"inlink"));
		    } else if (token.indexOf(".body") != -1) {
			token = token.replaceAll(".body", "");
			currentOp.add(new QryopIlTerm(tokenizeQuery(token)[0],
				"body"));
		    } else if (token.indexOf(".keywords") != -1) {
			token = token.replaceAll(".keywords", "");
			currentOp.add(new QryopIlTerm(tokenizeQuery(token)[0],
				"keywords"));
		    } else if (token.indexOf(".title") != -1) {
			token = token.replaceAll(".title", "");
			currentOp.add(new QryopIlTerm(tokenizeQuery(token)[0],
				"title"));
		    } else {
			System.out.println(token);
			currentOp.add(new QryopIlTerm(tokenizeQuery(token)[0]));
		    }
		} else {
		    // System.out.println (token);
		    continue;
		    // currentOp.add(new QryopIlTerm(token));
		}

	    }
	}

	// A broken structured query can leave unprocessed tokens on the
	// stack, so check for that.

	if (tokens.hasMoreTokens()) {
	    System.err
		    .println("Error:  Query syntax is incorrect.  " + qString);
	    return null;
	}

	return currentOp;
    }

    /**
     * Print a message indicating the amount of memory used. The caller can
     * indicate whether garbage collection should be performed, which slows the
     * program but reduces memory usage.
     * 
     * @param gc
     *            If true, run the garbage collector before reporting.
     * @return void
     */
    public static void printMemoryUsage(boolean gc) {

	Runtime runtime = Runtime.getRuntime();

	if (gc) {
	    runtime.gc();
	}

	System.out
		.println("Memory used:  "
			+ ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L))
			+ " MB");
    }

    /**
     * Print the query results.
     * 
     * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO
     * THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
     * 
     * QueryID Q0 DocID Rank Score RunID
     * 
     * @param queryName
     *            Original query.
     * @param result
     *            Result object generated by {@link Qryop#evaluate()}.
     * @throws IOException
     */
    static String printResults(String queryNum, String queryName,
	    QryResult result) throws IOException {

	String s = "";
	Map<String, Double> map = new HashMap<String, Double>();
	// int count = 0;
	if (result.docScores.scores.size() < 1) {
	    s = queryNum + " " + "Q0 " + "dummy" + " " + "1" + " "
		    + "0.000000000000 " + "fubar\n";
	    return s;
	} else {
	   
	}
	 for (int i = 0; i < result.docScores.scores.size(); i++) {

		map.put(getExternalDocid(result.docScores.getDocid(i)),
			result.docScores.getDocidScore(i));

	    }
	// transfer the HashMap to the TreeMap
	 TreeMap<String, Double> treemap = new TreeMap<String, Double>(map);

	if (model instanceof RetrievalModelRankedBoolean) {
	   
	   
	    int count = 0;
	    map = sort(treemap);
	    for (String k : map.keySet()) {

		s += queryNum + " " + "Q0 " + k + " " + (count + 1) + " "
			+ treemap.get(k) + "00000000000 " + "fubar\n";
		if (count == 99) {
		    break;
		}
		count++;
	    }

	} else {
	    int count = 0;
	    for (String k : treemap.keySet()) {

		s += queryNum + " " + "Q0 " + k + " " + (count + 1) + " "
			+ map.get(k) + "00000000000 " + "fubar\n";
		if (count == 99) {
		    break;
		}
		count++;
	    }
	}
	return s;
    }

    static <K, V extends Comparable<V>> Map<K, V> sort(final Map<K, V> map) {
	Comparator<K> valueComparator = new Comparator<K>() {
	    public int compare(K k1, K k2) {
		int compare = map.get(k2).compareTo(map.get(k1));
		if (compare == 0)
		    return 1;
		else
		    return compare;
	    }
	};
	Map<K, V> sort = new TreeMap<K, V>(valueComparator);
	sort.putAll(map);
	return sort;
    }

    /**
     * Print the experiment results.
     * 
     * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO
     * THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
     * 
     * QueryID Q0 DocID Rank Score RunID
     * 
     * @param queryName
     *            Original query.
     * @param result
     *            Result object generated by {@link Qryop#evaluate()}.
     * @throws IOException
     */
    static String experimentPrint(String queryNum, String queryName,
	    QryResult result) throws IOException {

	String s = "";
	Map<Integer, Double> map = new HashMap<Integer, Double>();
	// int count = 0;
	if (result.docScores.scores.size() < 1) {
	    s = queryNum + " " + "Q0 " + "dummy" + " " + "1" + " "
		    + "0.000000000000 " + "fubar\n";
	    return s;
	} 
	 for (int i = 0; i < result.docScores.scores.size(); i++) {
	     
		map.put(result.docScores.getDocid(i),
			result.docScores.getDocidScore(i));

	    }
	
	List<Map.Entry<Integer,Double>> mappingList = new ArrayList<Map.Entry<Integer,Double>>(map.entrySet());
	Collections.sort(mappingList, new Comparator<Map.Entry<Integer,Double>>(){
	    public int compare(Map.Entry<Integer,Double> mapping1,Map.Entry<Integer,Double> mapping2){
	               
		if((int)(mapping2.getValue()-mapping1.getValue())>0)
		    return 1;
		else if ((int)(mapping2.getValue()-mapping1.getValue())<0)
		    return -1;
		else {
		    return mapping1.getKey().compareTo(mapping2.getKey());
		}
	    }
	   }); 
	for(Entry<Integer, Double> mapping:mappingList){
	    
	    System.out.println(mapping.getKey()+":"+mapping.getValue());
	    
	}
	   
	
	 
	// transfer the HashMap to the TreeMap
	 /*
	 TreeMap<String, Double> treemap = new TreeMap<String, Double>(map);

	if (model instanceof RetrievalModelRankedBoolean) {
	   
	   
	    int count = 0;
	    map = sort(treemap);
	    for (String k : map.keySet()) {

		s += queryNum + " " + "Q0 " + k + " " + (count + 1) + " "
			+ treemap.get(k) + "00000000000 " + "fubar\n";
		if (count == 99) {
		    break;
		}
		count++;
	    }

	} else {
	    int count = 0;
	    for (String k : treemap.keySet()) {

		s += queryNum + " " + "Q0 " + k + " " + (count + 1) + " "
			+ map.get(k) + "00000000000 " + "fubar\n";
		if (count == 99) {
		    break;
		}
		count++;
	    }
	}
	*/
	return s;

    }

    /**
     * Given a query string, returns the terms one at a time with stopwords
     * removed and the terms stemmed using the Krovetz stemmer.
     * 
     * Use this method to process raw query terms.
     * 
     * @param query
     *            String containing query
     * @return Array of query tokens
     * @throws IOException
     */
    static String[] tokenizeQuery(String query) throws IOException {

	TokenStreamComponents comp = analyzer.createComponents("dummy",
		new StringReader(query));
	TokenStream tokenStream = comp.getTokenStream();

	CharTermAttribute charTermAttribute = tokenStream
		.addAttribute(CharTermAttribute.class);
	tokenStream.reset();

	List<String> tokens = new ArrayList<String>();
	while (tokenStream.incrementToken()) {
	    String term = charTermAttribute.toString();
	    tokens.add(term);
	}
	return tokens.toArray(new String[tokens.size()]);
    }
}
