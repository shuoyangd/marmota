package edu.jhu.marmota.util;

public class Numbers {

	private Numbers() {
		
	}
	
	static public Integer[] int2Integer(int[] intArray) {
		Integer[] res = new Integer[intArray.length];
		for (int i = 0; i < intArray.length; i++) {
			res[i] = intArray[i];
		}
		return res;
	}
	
	static public int[] Integer2int(Integer[] IntegerArray) {
		int[] res = new int[IntegerArray.length];
		for (int i = 0; i < IntegerArray.length; i++) {
			res[i] = IntegerArray[i];
		}
		return res;
	}
}
