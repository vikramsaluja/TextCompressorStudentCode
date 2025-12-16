/******************************************************************************
 *  Compilation:  javac TextCompressor.java
 *  Execution:    java TextCompressor - < input.txt   (compress)
 *  Execution:    java TextCompressor + < input.txt   (expand)
 *  Dependencies: BinaryIn.java BinaryOut.java
 *  Data files:   abra.txt
 *                jabberwocky.txt
 *                shakespeare.txt
 *                virus.txt
 *
 *  % java DumpBinary 0 < abra.txt
 *  136 bits
 *
 *  % java TextCompressor - < abra.txt | java DumpBinary 0
 *  104 bits    (when using 8-bit codes)
 *
 *  % java DumpBinary 0 < alice.txt
 *  1104064 bits
 *  % java TextCompressor - < alice.txt | java DumpBinary 0
 *  480760 bits
 *  = 43.54% compression ratio!
 ******************************************************************************/

/**
 *  The {@code TextCompressor} class provides static methods for compressing
 *  and expanding natural language through textfile input.
 *
 *  @author Zach Blick, Vikram Saluja
 */
public class TextCompressor {

    // ASCII values go to 256
    private static final int R = 128;
    // Each word is going to be 8 bits
    private static final int W = 12;
    private static final int L = 1 << W;
    public static final int EOF = R;

    private static void compress() {
        String input = BinaryStdIn.readString();

        // Create TST
        TST dictionary = new TST();

        // Initialize the diction with single character strings
        for(int i = 0; i < R; i++){
            dictionary.insert("" + (char) i, i);
        }

        // Next available code which is the first after EOF (129)
        int nextCode = EOF + 1;

        int index = 0;

        // Main LZW Compression Loop
        while(index < input.length()){

            // Find the longest prefix of input starting at index that is in the TST
            String prefix = dictionary.getLongestPrefix(input, index);

            // Look up its code and write it out as an 8 bit codeword
            int code = dictionary.lookup(prefix);
            BinaryStdOut.write(code,W);

            int prefixLength = prefix.length();
            int lookAhead = index + prefixLength;

            // If there's room in the dictionary and a next character exists, then add prefix + nextChar as a new entry.
            if(nextCode < L && lookAhead < input.length()){
                char nextChar = input.charAt(lookAhead);
                String newString = prefix + nextChar;
                dictionary.insert(newString, nextCode++);
            }

            // Move forward by the length of the matched prefix
            index += prefixLength;
        }

        // Write EOF code so expand() knows when to stop
        BinaryStdOut.write(EOF, W);

        BinaryStdOut.close();
    }

    private static void expand() {

        String[] dictionary = new String[L];

        int nextCode = 0;

        // Intialize dictionary with ASCII characters
        for(nextCode = 0; nextCode < R; nextCode++){
            dictionary[nextCode] = "" + (char) nextCode;;
        }

        // EOF slot
        dictionary[EOF] = "";

        // First available code for new dictionary entries is 129
        nextCode = EOF + 1;

        int codeword = BinaryStdIn.readInt(W);

        // If first code is EOF, the input is empty
        if(codeword ==  EOF){
            BinaryStdOut.close();
            return;
        }

        // The string corresponding to the first code
        String val = dictionary[codeword];

        while(true){

            // Output the current decoded string
            BinaryStdOut.write(val);

            // Read next codeword
            codeword = BinaryStdIn.readInt(W);

            // End when EOF is reached
            if (codeword == EOF) break;

            String entry;

            // First check if the code exists in the dictionary
            if (dictionary[codeword] != null) {
                entry = dictionary[codeword];
            }
            // Check for the edge case where codeword == nextCode
            else {
                entry = val + val.charAt(0);
            }

            // Add new dictionary entry if there is left over space
            if (nextCode < L) {
                dictionary[nextCode++] = val + entry.charAt(0);
            }

            val = entry;
        }

        BinaryStdOut.close();

    }

    public static void main(String[] args) {
        if      (args[0].equals("-")) compress();
        else if (args[0].equals("+")) expand();
        else throw new IllegalArgumentException("Illegal command line argument");
    }
}
