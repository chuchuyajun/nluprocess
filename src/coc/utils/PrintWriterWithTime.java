package coc.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

public class PrintWriterWithTime extends PrintWriter {
	// a new line will start, append the time into the new line
	private boolean isNewLine = true;

	static SimpleDateFormat fmt = new SimpleDateFormat("MM-dd-yy HH:mm:ss:SSS");
	static {
		fmt.setCalendar(new GregorianCalendar());
	}

	public PrintWriterWithTime(File file) throws FileNotFoundException {
		super(file);
	}

	public PrintWriterWithTime(File file, String csn) throws FileNotFoundException, UnsupportedEncodingException {
		super(file, csn);
	}

	public PrintWriterWithTime(OutputStream out) {
		super(out, false);
	}

	public PrintWriterWithTime(OutputStream out, boolean autoFlush) {
		super(new BufferedWriter(new OutputStreamWriter(out)), autoFlush);
	}

	public PrintWriterWithTime(Writer out) {
		super(out, false);
	}

	public PrintWriterWithTime(Writer out, boolean autoFlush) {
		super(out, autoFlush);
	}

	public PrintWriterWithTime(String fileName) throws FileNotFoundException {
		super(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName))), false);
	}

	public PrintWriterWithTime(String fileName, String csn) throws FileNotFoundException, UnsupportedEncodingException {
		super(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), csn)), false);
	}

	public void println() {
		super.println();
		this.isNewLine = true;
	}

	public void write(String s, int off, int len) {
		if (isNewLine) {
			this.writeTime();
			this.isNewLine = false;
		}
		super.write(s, off, len);
	}

	public void write(char buf[], int off, int len) {
		if (isNewLine) {
			this.writeTime();
			this.isNewLine = false;
		}
		super.write(buf, off, len);
	}

	private void writeTime() {
		String dateString = getDateString();
		super.write(dateString, 0, dateString.length());
	}
	
	private String getDateString() {
		String dateString = fmt.format(new Date()) + "  ";
		return dateString;
	}

}
