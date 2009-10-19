package data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public class EntityGraph {
	public HashMap<Mention, HashSet<Mention>> mention2corefs;
	
	public EntityGraph(Document d) {
		mention2corefs = new HashMap();
		for (Mention m : d.mentions) { 
			mention2corefs.put(m, new HashSet());
			mention2corefs.get(m).add(m);
		}
	}
	public void addPair(Mention m1, Mention m2) {
		// Strategy: always keep mention2corefs a complete record of all coreferents for that mention
		// So all we do is merge
		Set<Mention> corefs1 = (Set<Mention>) mention2corefs.get(m1).clone();
		Set<Mention> corefs2 = (Set<Mention>) mention2corefs.get(m2).clone();
		for (Mention n1 : corefs1) {
			for (Mention n2 : corefs2) {
				mention2corefs.get(n1).add(n2);
				mention2corefs.get(n2).add(n1);
			}
		}
	}
	public boolean isSingleton(Mention m) {
		return mention2corefs.get(m).size()==1;
	}
	public String entName(Mention m) {
		return entName(mention2corefs.get(m));
	}
	public String entName(Set<Mention> corefs) {
		ArrayList<Integer> L = new ArrayList();
		for (Mention m : corefs) {
			L.add(m.id);
		}
		Collections.sort(L);
		return StringUtils.join(L, "_");
	}
}