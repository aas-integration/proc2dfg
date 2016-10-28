/**
 * 
 */
package prog2dfg.inlinetest;

/**
 * @author schaef
 *
 */
public class TestFile {
	
	StringBuilder sb = new StringBuilder();
	
	public static void main(String[] args) throws Exception {
		TestFile m = new TestFile() ;
		m.run(args);
	}

	public void run (String[] args) {
		sb.append("Wtf");
		toInline(sb, args.length);
		toInline(sb, 8);
		System.out.println(sb.toString());		
	}
	
	private void toInline(StringBuilder sb, int x) {
		sb.append(x);
		x++;
		System.out.println("Yo!");
	}

 
}
