package sysc3303.a1.group3;

import java.io.OutputStream;
import java.io.PrintStream;

public class TeePrintStream extends PrintStream {
    private final PrintStream second;

    public TeePrintStream(OutputStream main, PrintStream second) {
        super(main);
        this.second = second;
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        try {
            super.write(buf, off, len);   // Write to file
            second.write(buf, off, len);  // Write to console
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void flush() {
        super.flush();
        second.flush();
    }
}
