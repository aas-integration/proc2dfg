/**
 * 
 */
package prog2dfg;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Test;

import com.sri.prog2dfg.SootToGraph;
import com.sri.prog2dfg.Util;
import com.sri.prog2dfg.soot.DfgBuilder;

/**
 * @author schaef
 *
 */
public class InliningTest {

	private static final String userDir = System.getProperty("user.dir") + "/";
	private static final String testRoot = userDir + "src/test/resources/";
	
	@Test
	public void test() {
		File testFileDir = new File(testRoot+"prog2dfg/inlinetest/");
		Collection<File> files = collectFiles(testFileDir);
		runOnFiles(files);
	}

	private Collection<File> collectFiles(File f) {
		Collection<File> sourceFiles= new LinkedList<File>();
		if (f.isDirectory()) {
			File[] files = f.listFiles();
			if (files!=null)  {
				for (File sub : files) {
					sourceFiles.addAll(collectFiles(sub));
				}				
			}
		} else {
			if (f.getName().endsWith(".java")) {
				sourceFiles.add(f);
			}
		}
		return sourceFiles;
	}
	
	public void runOnFiles(Collection<File> sourceFiles) {
		File classFileDir = null;
		try {
			classFileDir = Util.compileJavaFiles(sourceFiles.toArray(new File[sourceFiles.size()]));
			
			File outdir = new File(userDir + "output");
			if (!outdir.exists() || !outdir.isDirectory()) {
				outdir.mkdir();
			}
			
			SootToGraph s2g = new SootToGraph();
			Map<String, DfgBuilder> graphs = s2g.run(classFileDir.getAbsolutePath(), null);
			Util.generateDotFiles(outdir, graphs, true);
			 
		} catch (Throwable e) {
			e.printStackTrace();
			fail("Not yet implemented ");			
		} finally {
			if (classFileDir != null) {
				try {
					Util.delete(classFileDir);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
