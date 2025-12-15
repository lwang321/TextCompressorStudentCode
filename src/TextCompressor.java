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
 *  @author Zach Blick, Lucas Wang
 */
public class TextCompressor {

    // Use 14-bit codes for Alice, 19 for Shakespeare (the number of bits per code determines how many possible codes there are; for longer text files this number will be larger)
    public static int BITSPERCHAR = 8;
    public static int MAX_CODE = (1 << BITSPERCHAR);

    private static void compress() {

        String bigstr = BinaryStdIn.readString();
        // Use TST to store our codes
        TST tst = new TST();

        // Initialize TST with the first 128 ASCII values; the rest of the TST is built off them
        for (char i=0;i<=0x7F;i++) {
            tst.insert(String.valueOf(i), i);
        }

        // The first "code" starts with 0x81, as 0x80 is taken by EOF
        int codenum = 0x81;

        // C is the char that we will be adding on to the end of our String when checking if a String of letters exists as a code or not
        int index = 0;

        while (index < bigstr.length()) {

            // Lastworking stores the last string that is found in the TST, and its code will be written out as output
            String lastworking = tst.getLongestPrefix(bigstr, index);
            index += lastworking.length();
            //

            if (index < bigstr.length()) {
                String s = lastworking + bigstr.charAt(index);
                if (codenum < MAX_CODE) {
                    tst.insert(s, codenum);
                    codenum++;
                }
            }

            // Updating c so the last character from BinaryStdIn that breaks the while loop can still be used in the next iteration of the loop


            // Add last working string + next character to TST, check if all codes are used first; then, update the number of codes used so far


            BinaryStdOut.write(tst.lookup(lastworking), BITSPERCHAR);
        }

        // Edge case: last character

        // Write out EOF
        BinaryStdOut.write(0x80, BITSPERCHAR);
        
        BinaryStdOut.close();
    }

    private static void expand() {
        // TODO: Complete the expand() method

        // Map[] stores the Strings each code corresponds to; size of Map is 2^BITSPERCHAR, or the number of unique codes possible
        String map[] = new String[MAX_CODE];

        int code = BinaryStdIn.readInt(BITSPERCHAR);
        String codestring, lookaheadstring;

        // The first "code" starts with 0x81, as 0x80 is taken by EOF
        int codenum = 0x81;

        while (!BinaryStdIn.isEmpty()) {
            // Lookahead is used to generate Strings for codes
            int lookahead = BinaryStdIn.readInt(BITSPERCHAR);

            // Converting code to String
            if (code > 0x80) {
                codestring = map[code];
            }
            else {
                codestring = String.valueOf((char) code);
            }

            // Need to use a for loop when writing out because we want to still write out 8-bit chars
            for (int i = 0; i < codestring.length(); i++) {
                BinaryStdOut.write(codestring.charAt(i), 8);
            }

            // Look for EOF
            if (lookahead == 0x80) {
                break;
            }
            // Convert lookahead from code to String
            if (lookahead > 0x80) {
                // Edge case: What if our lookahead code doesn't yet exist? Then just add the first character of codestring to the end of it
                if (map[lookahead] == null) {
                    lookaheadstring = codestring + codestring.charAt(0);
                }
                // Normal case: just look up in map
                else {
                    lookaheadstring = map[lookahead];
                }
            }
            else {
                lookaheadstring = String.valueOf((char) lookahead);
            }

            // Add the new string to map; check for boundary to not go out of bounds, only use first letter of lookaheadstring because when we created the codes, we only had one more letter than the working code
            if (codenum < MAX_CODE) {
                map[codenum] = codestring + lookaheadstring.charAt(0);
                codenum++;
            }

            // Update code
            code = lookahead;
        }
        BinaryStdOut.close();
    }

    public static void main(String[] args) {

        if (args.length == 2) {
            BITSPERCHAR = Integer.parseInt(args[1]);
            MAX_CODE = (1 << BITSPERCHAR);
        }

        if      (args[0].equals("-")) compress();
        else if (args[0].equals("+")) expand();
        else throw new IllegalArgumentException("Illegal command line argument");
    }
}