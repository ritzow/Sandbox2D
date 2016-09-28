package util;

public final class MatrixMath {
	
	//Cannot instantiate MatrixMath
	private MatrixMath() {}
	
	/** return the transpose of a 4 by 4 matrix **/
	public static final float[] getTranspose(float[] matrix) {
		return new float[] {
				matrix[0], matrix[4], matrix[8], matrix[12],
				matrix[1], matrix[5], matrix[9], matrix[13],
				matrix[2], matrix[6], matrix[10], matrix[14],
				matrix[3], matrix[7], matrix[11], matrix[15],
		};
	}
	
	public static float[] multiply(float[] a, float[] b) {
		float[] result = new float[16];
		for(int i = 0; i < 4; i++){
			for(int j = 0; j < 4; j++){
				for(int k = 0; k < 4; k++){
					set(result, i, j, get(result, i, j) + get(a, i, k) * get(b, k, j));
				}
			}
		}
		return result;
	}
	
	private static float get(float[] array, int row, int column) {
		return array[(row * 4) + column];
	}
	
	private static void set(float[] array, int row, int column, float value) {
		array[(row * 4) + column] =  value;
	}
}