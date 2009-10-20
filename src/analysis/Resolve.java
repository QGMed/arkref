package analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import parsestuff.AnalysisUtilities;
import parsestuff.TregexPatternFactory;
import data.Document;
import data.Mention;
import data.Sentence;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;

public class Resolve{
	public static void go(Document d) {
		System.out.println("\n***  Resolve ***\n");
		Mention antecedent;
		
		for (Mention m : d.getMentions()) {
			System.out.println("trying to resolve:\t" + m);
			
			if (isPronominal(m)) {
				resolvePronoun(m, pronoun(m), d);
			}else if(inAppositiveConstruction(m)){
				resolveAppositive(m, d);
			}else if((antecedent = findAntecendentInPredicateNominativeConstruction(m, d)) != null){
				d.getRefGraph().setRef(m, antecedent);		
			}else{
				//semantics!
			}
		}
	}
	
	


	/**
	 * returns the antecedent NP or null
	 * The way this method is called could be made more efficient.  
	 * It doesn't really need to get called for every mention
	 * 
	 */
	private static Mention findAntecendentInPredicateNominativeConstruction(Mention m, Document d) {
		Tree root = m.getSentence().getRootNode();
		Tree node = m.getNode();
		
		TregexPattern pat = TregexPatternFactory.getPattern("S < NP=np1 <+(VP) (VP < (/^VB.*/ < be|is|was|were|are|being|been) < NP=np2)");
		TregexMatcher matcher = pat.matcher(root);
		while (matcher.find()) {
			if(matcher.getNode("np2") == node){
				Tree ante  = matcher.getNode("np1");
				for(Mention m2: d.getMentions()){
					if(ante == m2.getNode()){
						return m2;
					}
				}
			}
		}
		
		return null;
	}

	
	/**
	 * return true when m is the third child in of a parent who expands as
	 * NP -> NP , NP .*
	 * 
	 * @param m
	 * @return
	 */
	private static boolean inAppositiveConstruction(Mention m) {
		Tree root = m.getSentence().getRootNode();
		Tree node = m.getNode();
		Tree parent = node.parent(root);
		
		if(parent.numChildren()<3){
			return false;
		}else if(!parent.getChild(0).label().value().equals("NP")){
			return false;
		}else if(!parent.getChild(1).label().value().equals(",")){
			return false;
		}else if(parent.indexOf(node) != 2){
			return false;
		}
		
		return true;
	}

	
	public static void resolveAppositive(Mention mention, Document d) {
		Tree root = mention.getSentence().getRootNode();
		Tree node = mention.getNode();
		Tree parent = node.parent(root);
		
		for (Mention cand : d.prevMentions(mention)) {
			if(cand.getNode() == parent){
				d.getRefGraph().setRef(mention, cand);
				break;
			}
		}
		
		
		Mention ref = d.getRefGraph().getFinalResolutions().get(mention);
		System.out.printf("RESOLVE M%-3d -> M%-3d    %20s    ->   %-20s\n", 
				mention.getID(), ref.getID(), AnalysisUtilities.abbrevTree(mention.getNode()),
				 AnalysisUtilities.abbrevTree(ref.getNode()));
//		System.out.printf("RESOLVE M%-3d %s  ->  M%-3d %s\n", mention.id, d.refGraph.finalResolutions.get(mention));
	}
	
	
	
	public static void resolvePronoun(Mention mention, String pronoun, Document d) {
		System.out.println("trying to resolve as a pronoun:\t" + mention);
		
		ArrayList<Mention> candidates = new ArrayList<Mention>();
		
		for (Mention cand : d.prevMentions(mention)) {
			boolean match;
			
			if (SyntacticPaths.aIsDominatedByB(mention, cand)){ // I-within-I constraint 
				match = false;
			}else if (personhood(pronoun).equals("NONPER")) {    // e.g. "it"
				if (!isPronominal(cand)) {
					match = !cand.neType().equals("PERSON");
				} else {
					String g2 = gender(cand);
					System.out.println("gender "+g2+"  for  "+cand);
					if (g2.equals("M") || g2.equals("F")) {
						match = false;
					} else if (number(cand).equals("SG")) {
						match = true;
					} else { 
						match = true;  // ??  "it" -> "the store" i suppose.
					}					
				}
			} else if (personhood(pronoun).equals("PER")) {
				if (isPronominal(cand)) {
					match = gender(cand).equals(gender(mention));
				} else {
					// should use namelist here
					match = cand.neType().equals("PERSON");
				}
			} else {
				match = false;
			}			
			if (match) {
				System.out.println("yay    typematch: " + cand);
				candidates.add(cand);
			} else {
				System.out.println("reject mismatch:  " + cand);
			}
		}
		if (candidates.size() == 0) {
			System.out.println("No legal candidates");
			d.getRefGraph().setNullRef(mention);
		} else if (candidates.size() == 1) {
			System.out.println("Single legal resolution");
			d.getRefGraph().setRef(mention, candidates.get(0));
		} else if (candidates.size() > 1) {
			System.out.println("Finding pronoun antecedent by shortest syntactic path");
			d.getRefGraph().setRef(mention, SyntacticPaths.findBestCandidateByShortestPath(mention, candidates, d)); 
		}
		Mention ref = d.getRefGraph().getFinalResolutions().get(mention);
		System.out.printf("RESOLVE M%-3d -> M%-3d    %20s    ->   %-20s\n", 
				mention.getID(), ref.getID(), AnalysisUtilities.abbrevTree(mention.getNode()),
				 AnalysisUtilities.abbrevTree(ref.getNode()));
//		System.out.printf("RESOLVE M%-3d %s  ->  M%-3d %s\n", mention.id, d.refGraph.finalResolutions.get(mention));
	}


	
	// TODO (PRP$ Its)
	public static boolean isPronominal(Mention m) {
		TregexMatcher matcher = TregexPatternFactory.getPattern("NP <<# /^PRP/ !> NP").matcher(m.getNode());
		return matcher.find();
	}
	
	public static String pronoun(Mention m) {
		TregexPattern pat = TregexPatternFactory.getPattern("NP=np <<# /^PRP/=pronoun !> NP");
		TregexMatcher matcher = pat.matcher(m.getNode());
		if (matcher.find()) {
			Tree PRP = matcher.getNode("pronoun");
			return pronoun(PRP);
		} else {
			return null;
		}
	}
	
	public static String pronoun(Tree PRP) {
		Tree c = PRP.getChild(0);
		assert c.isLeaf();
		String p = c.label().toString().toLowerCase();
		return p;
	}
	
	public static String gender(Mention m) {
		if (isPronominal(m)) {
			String p = pronoun(m);
			if (p.matches("^(he|him|his)$")) {
				return "M";
			} else if (p.matches("^(she|her|hers)$")) {
				return "F";
			} else if (p.matches("^(it|its)$")) {
				return "N";  // neuter
			} else {
				return null;   // no decision
			}
		}
		// else name lists, i guess
		return null;
	}
	
	public static String personhood(Mention m) {
		if (isPronominal(m)) {
			String p = pronoun(m);
			return personhood(p);
		}
		return null;
	}
	
	public static String personhood(String pronoun) {
		if (pronoun.matches("^(he|him|his|she|her|hers)$")) {
			return "PER";
		} else if (pronoun.matches("^(it|its)$")) {
			return "NONPER";
		}
		return null;
	}
	
	public static String number(Mention m) {
		if (isPronominal(m)) {
			String p = pronoun(m);
			if (p.matches("^(they|them|these|those|we|us)$")) {
				return "PL";
			} else {  //if (p.matches("^(it|its|that|this|he|him|his|she|her)$")) {
				return "SG";
			}
		}
		return null;
	}
}