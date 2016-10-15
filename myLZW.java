import java.lang.*;
import java.util.*;

public class myLZW {
    private static final int R = 256; // number of input chars
    private static int L; // number of codewords = 2^W
    private static int W; // codeword width
	private static final int MAXWIDTH = 16; //maximum width of codewords
	private static final int MAXWORDS = 65536; //maximum number of codewords
	private static final int MINWIDTH = 9; //minimum codeword width
	private static final int MINWORDS = 512;//minumum number of codewords
	private static int compMode; //relays which letter argument was input
	private static boolean isMonitoring = false; //whether or not monitoring has started
	private static double newRatio; //new ratio to be compared
	private static double oldRatio; //old ratio to be compared to
	private static double compressedData; //number of bits of compressed data
	private static double uncompressedData; //number of bits of uncompressed data
	
	
	 public static void main(String[] args) {
	        if (args[0].equals("-")){
				if (args[1].equals("n")){
					compMode = 0;
					compress();
				}else if (args[1].equals("r")) {
					compMode = 1;
					compress();
				}else if(args[1].equals("m")){
					compMode = 2;
					compress();
				}
			}		 		
	        else if (args[0].equals("+")){
					expand();
			}	 
	        else throw new IllegalArgumentException("Illegal command line argument");
	    }

    public static void compress() { 
		
		W = MINWIDTH; //start codeword width at 9 bits
		L = (int)Math.pow(2, W); //sets number of codewords to 2^W (W = number of bits)
		compressedData = 0;
		uncompressedData = 0;
		newRatio = 0.0;
		oldRatio = 0.0;
		
		if(compMode == 0){
			System.err.println("DO NOTHING MODE");
			BinaryStdOut.write('n', 8);
		}
		if(compMode == 1){
			System.err.println("RESET MODE");
			BinaryStdOut.write('r', 8);
		}
		if(compMode == 2){
			System.err.println("MONITOR MODE");
			BinaryStdOut.write('m', 8);
		}
		
        String input = BinaryStdIn.readString();
        TST<Integer> st = new TST<Integer>();
        for (int i = 0; i < R; i++)
            st.put("" + (char) i, i);
        int code = R+1;  // R is codeword for EOF
        while (input.length() > 0) {
           	String s = st.longestPrefixOf(input);  // Find max prefix match s.
			uncompressedData = uncompressedData + (s.length()*8);
            BinaryStdOut.write(st.get(s), W);      // Print s's encoding.
			compressedData = compressedData + W;
			newRatio = uncompressedData / compressedData;
           	int t = s.length();
// N MODE  --> Do nothing if compression codebook is full
			if(compMode == 0){ 
				if (t < input.length() && code < L){    // Add s to symbol table.
			       	st.put(input.substring(0, t + 1), code++);
				}
				if((code == L) && (W < MAXWIDTH)){ //if all codewords used up at current width and width hasnt maxed out
					W += 1; // increase width by 1 bit
					L = (int)Math.pow(2, W); //increase number of possible codewords to reflect new length
					st.put(input.substring(0, t + 1), code++); //add s to symbol table
				}
			}
//R MODE --> Reset if compression codebook is full
			if(compMode == 1){ 
				if (t < input.length() && code < L){    // Add s to symbol table.
					st.put(input.substring(0, t + 1), code++);
				}
				if((code == L) && (W < MAXWIDTH)){ //if all codewords used up at current width and width hasnt maxed out
					W += 1; // increase width by 1 bit
					L = (int)Math.pow(2, W); //increase number of possible codewords to reflect new length
					st.put(input.substring(0, t + 1), code++); //add s to symbol table
				}
				if(code == MAXWORDS){ // if symbol table is full because all possible codewords have been used up to 16 bits
					System.err.println("RESET CODE BOOK");
					W = MINWIDTH; //reset codeword width back to 9 bits 
					L = MINWORDS; //adjust possible codeword number to reflect new length
					st = new TST<Integer>(); //reset tree
					for (int i = 0; i < R; i++){
					   	st.put("" + (char) i, i);
					}
					code = R+1;
				}
			}
//M MODE --> Monitor ratio and reset compression codebook when ratio hits 1.1
			if(compMode == 2){
				if (t < input.length() && code < L){    // Add s to symbol table.
					st.put(input.substring(0, t + 1), code++);
				}
				if((code == L) && (W < MAXWIDTH)){ //if all codewords used up at current width and width hasnt maxed out
					W += 1; // increase width by 1 bit
					L = (int)Math.pow(2, W); //increase number of possible codewords to reflect new length
					st.put(input.substring(0, t + 1), code++); //add s to symbol table
				}
				if(code == MAXWORDS && isMonitoring == false){ //when initial symbols are used up and ratio isnt being watched
					oldRatio = newRatio;
					System.err.println("MONITOR INITIALIZED");
					isMonitoring = true; //begin monitoring
				}
				if (isMonitoring == true){//once monitoring has been initialized, begin watching ratio
					if((oldRatio / newRatio) > 1.1){
						System.err.println("STARTING RATIO OVER");
						oldRatio = 0;
						newRatio = 0;
						W = MINWIDTH; //reset codeword width back to 9 bits 
						L = (int)Math.pow(2, W); //adjust possible codeword number to reflect new length
						st = new TST<Integer>(); //reset tree
						for (int i = 0; i < R; i++){
							st.put("" + (char) i, i);
						}
						code = R+1;
						isMonitoring = false;
					}
				}
			}	
            input = input.substring(t);            // Scan past s in input.
        }
        BinaryStdOut.write(R, W);
        BinaryStdOut.close();
		System.err.println("FINISHED COMPRESSION");
    } 

    public static void expand() {
		W = MINWIDTH; //start codeword width at 9 bits
		L = MINWORDS;
		
		//checks first character in compressed file to see what mode it was compressed by
       	char mode = BinaryStdIn.readChar(8);
		if (mode == 'n'){
			compMode = 0;
			System.err.println("DO NOTHING DECOMPRESS MODE");
		}else if (mode == 'r') {
			compMode = 1;
			System.err.println("RESET DECOMPRESS MODE");
		}else if(mode == 'm'){
			compMode = 2;
			System.err.println("MONITOR DECOMPRESS MODE");
		}

		String[] st = new String[MAXWORDS];
        int i; // next available codeword value
		
        // initialize symbol table with all 1-character strings
        for (i = 0; i < R; i++)
            st[i] = "" + (char) i;
        st[i++] = "";                        // (unused) lookahead for EOF

        int codeword = BinaryStdIn.readInt(W);
        if (codeword == R) return;           // expanded message is empty string
        String val = st[codeword];

		while (true) {
            BinaryStdOut.write(val);
			compressedData = compressedData + (val.length()*8);
            codeword = BinaryStdIn.readInt(W);
			uncompressedData = uncompressedData + W;
			newRatio = compressedData/uncompressedData;
            if (codeword == R) break;
            String s = st[codeword];
//N-EXPAND MODE --> does nothing when codebook is full            
			if(compMode == 0) { 
				if (i == codeword) s = val + val.charAt(0);   // special case hack
				if (i < L) st[i++] = val + s.charAt(0);
				if((i==L-1) && (W < MAXWIDTH)){ //if all codewords used up at current width and width hasnt maxed out
					W+=1;
					L = (int)Math.pow(2, W);
					st[i++] = val + s.charAt(0);
				}
				val = s;
			}
//R-EXPAND MODE --> resets when expansion codebook is full
			if(compMode == 1) {
				if (i == codeword) s = val + val.charAt(0);   // special case hack
				if (i < L) st[i++] = val + s.charAt(0);
				if((i==L-1) && (W < MAXWIDTH)){ //if all codewords used up at current width and width hasnt maxed out
					W+=1;
					L = (int)Math.pow(2, W);
					st[i++] = val + s.charAt(0);
				}
				val = s;
				if(i == MAXWORDS-1) {
					System.err.println("EXPAND CODEBOOK RESET");
					W = MINWIDTH;
					L = MINWORDS;
					st = new String[MAXWORDS];
					for (i = 0; i < R; i++){
					    st[i] = "" + (char) i;
					}
					st[i++] = "";                        // (unused) lookahead for EOF
					codeword = BinaryStdIn.readInt(W);
					if (codeword == R) return;           // expanded message is empty string
					val = st[codeword];
				}
			}
//M-EXPAND MODE --> monitors ratio and resets expansion codebook when ratio hits 1.1
			if(compMode == 2) {
				if (i == codeword) s = val + val.charAt(0);   // special case hack
				if (i < L) st[i++] = val + s.charAt(0);
				if((i==L-1) && (W < MAXWIDTH)){ //if all codewords used up at current width and width hasnt maxed out
					W+=1;
					L = (int)Math.pow(2, W);
					st[i++] = val + s.charAt(0);
				}
				val = s;
				if(i == MAXWORDS-1 && isMonitoring == false) {
					oldRatio = newRatio;
					System.err.println("MONITOR INITIALIZED");
					isMonitoring = true; //begin monitoring				}
				if(i == MAXWORDS-1 && isMonitoring == true){
					if((oldRatio/newRatio)>1.1){
						System.err.println("STARTING RATIO OVER");
						W = MINWIDTH;
						L = MINWORDS;
						oldRatio = 0;
						newRatio = 0;
						st = new String[MAXWORDS];
						for (i = 0; i < R; i++){
							st[i] = "" + (char) i;
						}
						st[i++] = "";                        // (unused) lookahead for EOF
						codeword = BinaryStdIn.readInt(W);
						if (codeword == R) return;           // expanded message is empty string
						val = st[codeword];	
						isMonitoring = false;
					}
				}
			}
        }
		}
        BinaryStdOut.close();
		System.err.println("FINISHED EXPANSION");
    }
}
