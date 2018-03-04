package function.plugin.plugins.imageProcessing;
import java.util.Objects;

/* DCT plugin
   v 1.0, Sep. 18, 2001
   Werner Bailer <werner@wbailer.com> */
public class NaiveDCT {
	
	private int N;
	private double[][] cosn;
	private double[][] icosn;
	private double factor;
	private double An;
	private double sq2 = Math.sqrt(2);
	
	public NaiveDCT(int length)
	{
		this.N = length;
		this.factor = Math.PI / this.N;
		this.An = sq2/Math.sqrt(this.N);
		
		// Pre-calculate the cosine terms.
		this.cosn = new double[this.N][this.N];
		for (int k = 0; k < this.N; k++)
		{
			for (int n = 0; n < this.N; n++)
			{
				cosn[k][n] = Math.cos((n + 0.5) * k * this.factor);
			}
		}
		
		this.cosn = new double[this.N][this.N];
		for (int i = 0; i < this.N; i++)
		{
			for (int j = 1; j < this.N; j++)
			{
				icosn[i][j] = Math.cos(j * (i + 0.5) * this.factor);
			}
		}
	}

	/**
	 * Computes the scaled DCT type II on the specified array, returning a new array.
	 * The array length can be any value, starting from zero. The returned array has the same length.
	 * <p>For the formula, see <a href="https://en.wikipedia.org/wiki/Discrete_cosine_transform#DCT-II">
	 * Wikipedia: Discrete cosine transform - DCT-II</a>.</p>
	 * @param vector the vector of numbers to transform
	 * @return an array representing the DCT of the given vector
	 * @throws NullPointerException if the array is {@code null}
	 */
	public double[] transform(double[] vector)
	{
		Objects.requireNonNull(vector);
		if(vector.length != this.N)
		{
			return null;
		}
		double[] result = new double[this.N];
		for (int k = 0; k < vector.length; k++) {
			double sum = 0;
			for (int n = 0; n < vector.length; n++)
				sum += vector[n] * cosn[k][n];
			result[k] = this.An * sum;
		}
		result[0] = result[0]/sq2;
		return result;
	}
	
	
	/**
	 * Computes the scaled DCT type III on the specified array, returning a new array.
	 * The array length can be any value, starting from zero. The returned array has the same length.
	 * <p>For the formula, see <a href="https://en.wikipedia.org/wiki/Discrete_cosine_transform#DCT-III">
	 * Wikipedia: Discrete cosine transform - DCT-III</a>.</p>
	 * @param vector the vector of numbers to transform
	 * @return an array representing the DCT of the given vector
	 * @throws NullPointerException if the array is {@code null}
	 */
	public double[] inverseTransform(double[] vector)
	{
		Objects.requireNonNull(vector);
		if(vector.length != this.N)
		{
			return null;
		}
		double[] result = new double[this.N];
		for (int i = 0; i < vector.length; i++) {
			double sum = vector[0] / sq2;
			for (int j = 1; j < vector.length; j++)
				sum += vector[j] * icosn[i][j];
			result[i] = this.An * sum;
		}
		return result;
	}
}