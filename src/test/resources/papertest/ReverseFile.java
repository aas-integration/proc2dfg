package papertest;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ReverseFile {
	
	public static void main(String[] args) throws IOException {
		String data = readFile(args[0]);
		data = new StringBuilder(data).reverse().toString();
		writeFile(args[1], data);
	}
	
	public static String readFile(String fname) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(fname));
		return new String(encoded, Charset.defaultCharset());
	}

	public static void writeFile(String fname, String data) {
		try (Writer out = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(fname), 
						Charset.defaultCharset()));) {
			out.write(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
