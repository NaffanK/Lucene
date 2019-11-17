package lucene;

import java.util.Comparator;

public class MyComparator implements Comparator<String>{
	  public int compare(String entry, String compare) {
		  int indexOf = entry.indexOf(":");
		  String substring = entry.substring(0, indexOf-1);
		  int indexOf1 = compare.indexOf(":");
		  String substring1 = compare.substring(0, indexOf1-1);
		  return Integer.valueOf(substring) - Integer.valueOf(substring1);
	  }
	}
