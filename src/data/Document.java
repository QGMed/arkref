package data;
import java.util.*;
import java.io.*;

import parsestuff.AnalysisUtilities;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.Pair;

public class Document {
	public ArrayList<Sentence> sentences;
	public ArrayList<Mention> mentions;
	public HashMap<Tree,Mention> node2mention;  // BUG: same-looking node multiple trees
	public RefGraph refGraph;
	
	public Document() {
		sentences = new ArrayList<Sentence>();
		mentions = new ArrayList<Mention>();
		node2mention = new HashMap<Tree,Mention>();
		refGraph = new RefGraph();
	}
	
	public static Document loadFiles(String baseFilename) throws IOException {
		Document d = new Document();
		String parseFilename = baseFilename + ".parse";
		String neFilename = baseFilename = baseFilename + ".ner";
		BufferedReader parseR = new BufferedReader(new FileReader(parseFilename));
		BufferedReader nerR = new BufferedReader(new FileReader(neFilename));
		String parse; String ner;
		int curSentId=0;
		while ( (parse = parseR.readLine()) != null) {
			parse = parse.replace("=H ", " ");
			Tree tree = AnalysisUtilities.getInstance().readTreeFromString(parse);
			ner = nerR.readLine();
			Sentence sent = new Sentence(++curSentId);
			sent.setStuff(tree, ner);
			d.sentences.add(sent);
		}
		return d;
	}
	
	/** goes backwards through document **/
	public Iterable<Mention> prevMentions(final Mention start) {
		return new Iterable<Mention>() {
			public Iterator<Mention> iterator() {
				return new MentionRevIterIter(start);
			}
		};
	}
	public class MentionRevIterIter implements Iterator<Mention> {
		int mi = -1;
		public MentionRevIterIter(Mention start) {
			for (int i=0; i < mentions.size(); i++) {
				if (mentions.get(i) == start) {
					this.mi = i;
					break;
				}
			} 
			assert mi != -1;
		}
		@Override
		public boolean hasNext() {
			return mi > 0;
		}

		@Override
		public Mention next() {
			if (mi==-1) return null;
			mi--;
			if (mi==-1) return null;
			return mentions.get(mi);
			
		}

		// why-t-f did i write this?
//		if (!filterToRemaining) {
//			mi--;				
//		} else {
//			do {
//				mi--;
//				if (refGraph.needsReso(mentions.get(mi))) break;
//			} while (mi != -1);
//		}

		@Override
		public void remove() {
			System.out.println("bad");			
		}
		
	}
}